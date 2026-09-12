package com.jsingh.credence.domain.parser

import com.jsingh.credence.domain.models.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

data class ParsedStatementResult(
    val bankName: String?,
    val accountHolderName: String?,
    val maskedAccountNumber: String?,
    val closingBalance: Double?,
    val transactions: List<Transaction>
)

object StatementParser {

    suspend fun processStatement(rawText: String): ParsedStatementResult = withContext(Dispatchers.Default) {
        val txns = mutableListOf<Transaction>()

        // 1. Identity & Metadata Extraction
        val bankRegex = """(?i)(HDFC|SBI|State Bank of India|ICICI|Axis|Kotak|Bank of Baroda|Punjab National Bank|Canara|Union Bank|IDFC|IndusInd)""".toRegex()
        val nameRegex = """(?i)(?:Account Name|Customer Name|Name|Statement of)[\s:-]*(?:Mr\.|Ms\.|Mrs\.|M/s)?\s*([A-Za-z\s]{4,40})(?:\(|$)""".toRegex()
        val accountRegex = """(?i)(?:A/c|Account No|Acc No|Account Number)[\s:-]*([a-zA-Z0-9Xx\*\-\.]{6,20})""".toRegex()
        val balanceRegex = """(?i)Balance.*?(?:Rs\.?|INR)?\s*([\d,]+\.\d{2})""".toRegex()

        val parsedBank = bankRegex.find(rawText)?.groupValues?.get(1)?.trim()
        val parsedName = nameRegex.find(rawText)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
        val parsedAccount = accountRegex.find(rawText)?.groupValues?.get(1)?.trim()?.replace(Regex("[^0-9X*]"), "")
        val parsedBalance = balanceRegex.findAll(rawText).lastOrNull()?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()

        // 2. The Bulletproof Row Regex
        val rowRegex = """^\s*(?:\d{1,5}\s+)?(\d{1,2}[-/.\s][A-Za-z0-9]{2,9}[-/.\s]\d{2,4})\s+(?:\d{1,2}[-/.\s][A-Za-z0-9]{2,9}[-/.\s]\d{2,4}\s+)?(.*?)\s+([\d,]+\.\d{2,3}(?:\s*[CD]r|\s*[CD]R)?)(?:\s+([\d,]+\.\d{2,3}(?:\s*[CD]r|\s*[CD]R)?))?(?:\s+([\d,]+\.\d{2,3}(?:\s*[CD]r|\s*[CD]R)?))?\s*$""".toRegex()

        val lines = rawText.lines()
        var previousBalance = 0.0

        for (line in lines) {
            val clean = line.trim()
            val match = rowRegex.find(clean) ?: continue

            val date = match.groupValues[1]
            val rawDesc = match.groupValues[2].trim()

            // FIX: If date is completely unparseable, drop the row. Bad data ruins scoring.
            val timestamp = parseDateToMillis(date) ?: continue

            val amounts = listOfNotNull(
                match.groupValues[3].takeIf { it.isNotBlank() },
                match.groupValues[4].takeIf { it.isNotBlank() },
                match.groupValues[5].takeIf { it.isNotBlank() }
            ).map {
                it.replace(",", "").replace(Regex("(?i)\\s*[CD]R"), "").trim().toDoubleOrNull() ?: 0.0
            }

            if (amounts.isEmpty()) continue

            val currentBalance = amounts.last()
            val isDebit: Boolean
            val amount: Double

            // 3. Smart Accounting Fallback Engine
            if (rawDesc.contains("/DR/", ignoreCase = true) || rawDesc.contains(" DEBIT", ignoreCase = true) || rawDesc.endsWith(" DR", ignoreCase = true)) {
                isDebit = true
                amount = amounts.first()
            } else if (rawDesc.contains("/CR/", ignoreCase = true) || rawDesc.contains(" CREDIT", ignoreCase = true) || rawDesc.endsWith(" CR", ignoreCase = true)) {
                isDebit = false
                amount = amounts.first()
            } else {
                if (previousBalance > 0.0 && currentBalance > 0.0) {
                    val diff = currentBalance - previousBalance
                    if (abs(diff) > 0.5) {
                        isDebit = diff < 0
                        amount = abs(diff)
                    } else {
                        isDebit = true
                        amount = amounts.first()
                    }
                } else {
                    // FIX: If we can't mathematically prove it, and there's no text marker, skip it.
                    // Assuming the first row is a debit causes massive statistical errors in small files.
                    continue
                }
            }

            previousBalance = currentBalance
            val cleanMerchant = extractMerchantName(rawDesc)
            val category = classifyCategory(cleanMerchant, rawDesc)
            val txnType = if (rawDesc.contains("UPI", ignoreCase = true)) "UPI" else "BANK"

            if (amount > 0.0) {
                txns.add(
                    Transaction(
                        title = cleanMerchant,
                        amount = amount,
                        isExpense = isDebit,
                        category = category,
                        date = date,
                        time = "",
                        timestamp = timestamp,
                        merchantName = cleanMerchant,
                        transactionType = txnType,
                        account = parsedAccount ?: "Bank Account",
                        source = "STATEMENT",
                        phoneNumber = null,
                        runningBalance = currentBalance // NEW: Passed to calculator for Liquidity scoring
                    )
                )
            }
        }

        ParsedStatementResult(
            bankName = parsedBank ?: "Verified Bank",
            accountHolderName = parsedName ?: "Verified Business",
            maskedAccountNumber = parsedAccount,
            closingBalance = parsedBalance ?: previousBalance,
            transactions = txns.reversed()
        )
    }

    private fun extractMerchantName(raw: String): String {
        return try {
            val upiIdRegex = """([a-zA-Z0-9.\-_]+@[a-zA-Z]+)""".toRegex()
            val upiMatch = upiIdRegex.find(raw)
            if (upiMatch != null) return upiMatch.groupValues[1].lowercase()

            val clean = raw.replace(Regex("(?i)(UPI/(?:DR|CR)/[\\d]+/|NEFT/|IMPS/|RTGS/|ACH/|POS/|BIL/)"), "")
            val parts = clean.split(Regex("[/\\-]"))
            val name = parts.firstOrNull { it.isNotBlank() && !it.all { char -> char.isDigit() } }
            name?.take(25)?.trim() ?: raw.take(20).trim()
        } catch (e: Exception) {
            raw.take(20).trim()
        }
    }

    private fun classifyCategory(merchant: String, rawDesc: String): String {
        val lower = merchant.lowercase()
        val rawLower = rawDesc.lowercase()
        return when {
            rawLower.contains("bounce") || rawLower.contains("ach return") || rawLower.contains("insufficient") || rawLower.contains("penalty") -> "Penalties & Fees"
            lower.contains("zepto") || lower.contains("blinkit") || lower.contains("dmart") -> "Groceries"
            lower.contains("zomato") || lower.contains("swiggy") -> "Food & Dining"
            lower.contains("jio") || lower.contains("airtel") || lower.contains("electricity") -> "Utilities"
            lower.contains("fuel") || lower.contains("petrol") || lower.contains("hpcl") -> "Fuel & Auto"
            else -> "General" // Categories are used strictly for UI pies, NOT for underwriting score.
        }
    }

    private fun parseDateToMillis(dateStr: String): Long? {
        val cleanDate = dateStr.replace(Regex("[-. ]+"), "/")
        val formatsToTry = listOf("dd/MM/yyyy", "dd/MM/yy", "d/M/yyyy", "dd/MMM/yyyy", "d/MMM/yyyy")

        for (format in formatsToTry) {
            try {
                val sdf = SimpleDateFormat(format, Locale.ENGLISH).apply { isLenient = false }
                return sdf.parse(cleanDate)?.time
            } catch (e: Exception) { continue }
        }
        return null // Drops the row instead of defaulting to today
    }
}