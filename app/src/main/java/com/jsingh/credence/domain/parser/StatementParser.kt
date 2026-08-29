package com.jsingh.credence.domain.parser

import com.jsingh.credence.domain.models.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

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

        // 1. Identity & Bank Extraction
        val bankRegex = """(?i)(HDFC|SBI|State Bank of India|ICICI|Axis|Kotak|Bank of Baroda|Punjab National Bank|Canara|Union Bank)""".toRegex()
        val nameRegex = """(?i)(?:Account Name|Name|Statement of)[\s:-]*(?:Mr\.|Ms\.|Mrs\.)?\s*([A-Za-z\s]{4,40})(?:\(|$)""".toRegex()
        val accountRegex = """(?i)(?:A/c|Account No|Acc No)[\s:-]*([Xx\*\d]{6,20})""".toRegex()
        val balanceRegex = """(?i)Balance.*?(?:Rs\.?|INR)?\s*([\d,]+\.\d{2})""".toRegex()

        val parsedBank = bankRegex.find(rawText)?.groupValues?.get(1)?.trim()
        val parsedName = nameRegex.find(rawText)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
        val parsedAccount = accountRegex.find(rawText)?.groupValues?.get(1)?.trim()
        val parsedBalance = balanceRegex.findAll(rawText).lastOrNull()?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()

        // 2. The Ultimate Row Regex
        // -> ^\s* allows leading spaces.
        // -> \w{2,3} catches letters (Jan, Feb) OR numbers (01, 02) in the month slot.
        // -> (.*?) allows descriptions of any length or character.
        val rowRegex = """^\s*(\d{2}[-/.]\w{2,3}[-/.]\d{2,4})\s+(?:\d{2}[-/.]\w{2,3}[-/.]\d{2,4}\s+)?(.*?)\s+([\d,]{1,15}\.\d{2})(?:\s+([\d,]{1,15}\.\d{2}))?(?:\s+([\d,]{1,15}\.\d{2}))?""".toRegex()

        val lines = rawText.lines()
        var previousBalance = 0.0

        for (line in lines) {
            val clean = line.trim()
            val match = rowRegex.find(clean) ?: continue

            val date = match.groupValues[1]
            val rawDesc = match.groupValues[2].trim()

            // Extract all valid numbers from the end of the line
            val amounts = listOfNotNull(
                match.groupValues[3].takeIf { it.isNotBlank() },
                match.groupValues[4].takeIf { it.isNotBlank() },
                match.groupValues[5].takeIf { it.isNotBlank() }
            ).map { it.replace(",", "").toDoubleOrNull() ?: 0.0 }

            if (amounts.isEmpty()) continue

            val currentBalance = amounts.last()
            val isDebit: Boolean
            val amount: Double

            // Smart Debit/Credit Deduction
            if (rawDesc.contains("/DR/", ignoreCase = true) || rawDesc.contains(" DEBIT", ignoreCase = true) || rawDesc.endsWith(" DR", ignoreCase = true)) {
                isDebit = true
                amount = amounts.first()
            } else if (rawDesc.contains("/CR/", ignoreCase = true) || rawDesc.contains(" CREDIT", ignoreCase = true) || rawDesc.endsWith(" CR", ignoreCase = true)) {
                isDebit = false
                amount = amounts.first()
            } else {
                if (previousBalance > 0.0 && currentBalance > 0.0) {
                    val diff = currentBalance - previousBalance
                    isDebit = diff < 0
                    amount = Math.abs(diff)
                } else {
                    isDebit = true
                    amount = amounts.first()
                }
            }

            previousBalance = currentBalance

            val cleanMerchant = extractMerchantName(rawDesc)
            val category = classifyCategory(cleanMerchant, rawDesc)
            val txnType = if (rawDesc.contains("UPI", ignoreCase = true)) "UPI" else "BANK"

            // ✨ FIX: Advanced Date Parsing Engine
            val timestamp = parseDateToMillis(date)

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
                        phoneNumber = null
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

            val clean = raw.replace(Regex("(?i)(UPI/(?:DR|CR)/[\\d]+/|NEFT/|IMPS/|RTGS/|ACH/|POS/)"), "")
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
            lower.contains("zepto") || lower.contains("blinkit") || lower.contains("instamart") || lower.contains("dmart") -> "Groceries"
            lower.contains("zomato") || lower.contains("swiggy") || lower.contains("dominos") || lower.contains("kfc") -> "Food & Dining"
            lower.contains("jio") || lower.contains("airtel") || lower.contains("vi") || lower.contains("recharge") -> "Utilities"
            lower.contains("fuel") || lower.contains("petrol") || lower.contains("hpcl") || lower.contains("bpcl") -> "Fuel & Auto"
            lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") -> "Shopping"
            else -> "General"
        }
    }

    // ✨ FIX: Multi-Format Date Engine
    private fun parseDateToMillis(dateStr: String): Long {
        val cleanDate = dateStr.replace("-", "/").replace(".", "/")
        val formatsToTry = listOf(
            "dd/MM/yyyy",
            "dd/MM/yy",
            "dd/MMM/yyyy", // catches 01/Jan/2023
            "dd/MMM/yy"    // catches 01/Jan/23
        )

        for (format in formatsToTry) {
            try {
                val sdf = SimpleDateFormat(format, Locale.ENGLISH)
                sdf.isLenient = false
                val parsedDate = sdf.parse(cleanDate)
                if (parsedDate != null) {
                    return parsedDate.time
                }
            } catch (e: Exception) {
                // Ignore and try the next format
            }
        }

        return System.currentTimeMillis() // Absolute fallback
    }
}