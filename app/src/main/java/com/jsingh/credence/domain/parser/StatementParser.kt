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

        // ==========================================
        // 1. IDENTITY & METADATA EXTRACTION
        // ==========================================
        // Added more banks and Axis/ICICI specific keywords (Customer Name, Account Number)
        val bankRegex = """(?i)(HDFC|SBI|State Bank of India|ICICI|Axis|Kotak|Bank of Baroda|Punjab National Bank|Canara|Union Bank|IDFC|IndusInd)""".toRegex()
        val nameRegex = """(?i)(?:Account Name|Customer Name|Name|Statement of)[\s:-]*(?:Mr\.|Ms\.|Mrs\.|M/s)?\s*([A-Za-z\s]{4,40})(?:\(|$)""".toRegex()
        val accountRegex = """(?i)(?:A/c|Account No|Acc No|Account Number)[\s:-]*([a-zA-Z0-9Xx\*\-\.]{6,20})""".toRegex()
        val balanceRegex = """(?i)Balance.*?(?:Rs\.?|INR)?\s*([\d,]+\.\d{2})""".toRegex()

        val parsedBank = bankRegex.find(rawText)?.groupValues?.get(1)?.trim()
        val parsedName = nameRegex.find(rawText)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
        val parsedAccount = accountRegex.find(rawText)?.groupValues?.get(1)?.trim()?.replace(Regex("[^0-9X*]"), "")
        val parsedBalance = balanceRegex.findAll(rawText).lastOrNull()?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()

        // ==========================================
        // 2. THE BULLETPROOF ROW REGEX
        // ==========================================
        // - `(?:\d{1,5}\s+)?` allows optional Serial Numbers (ICICI format)
        // - `\d{1,2}` allows single digit dates (1/5/23 instead of 01/05/23)
        // - `[-/.\s]` allows spaces in dates (01 May 2023)
        // - `(?:\s*[CD]r|\s*[CD]R)?` safely ignores Cr/Dr attached to amounts (5000.00Cr)
        val rowRegex = """^\s*(?:\d{1,5}\s+)?(\d{1,2}[-/.\s][A-Za-z0-9]{2,9}[-/.\s]\d{2,4})\s+(?:\d{1,2}[-/.\s][A-Za-z0-9]{2,9}[-/.\s]\d{2,4}\s+)?(.*?)\s+([\d,]+\.\d{2,3}(?:\s*[CD]r|\s*[CD]R)?)(?:\s+([\d,]+\.\d{2,3}(?:\s*[CD]r|\s*[CD]R)?))?(?:\s+([\d,]+\.\d{2,3}(?:\s*[CD]r|\s*[CD]R)?))?\s*$""".toRegex()

        val lines = rawText.lines()
        var previousBalance = 0.0

        for (line in lines) {
            val clean = line.trim()
            val match = rowRegex.find(clean) ?: continue

            val date = match.groupValues[1]
            val rawDesc = match.groupValues[2].trim()

            // Safely strip commas and Cr/Dr before converting to Double
            val amounts = listOfNotNull(
                match.groupValues[3].takeIf { it.isNotBlank() },
                match.groupValues[4].takeIf { it.isNotBlank() },
                match.groupValues[5].takeIf { it.isNotBlank() }
            ).map {
                it.replace(",", "")
                    .replace(Regex("(?i)\\s*[CD]R"), "")
                    .trim()
                    .toDoubleOrNull() ?: 0.0
            }

            if (amounts.isEmpty()) continue

            val currentBalance = amounts.last()
            val isDebit: Boolean
            val amount: Double

            // ==========================================
            // 3. SMART ACCOUNTING FALLBACK ENGINE
            // ==========================================
            // First check explicit text markers
            if (rawDesc.contains("/DR/", ignoreCase = true) || rawDesc.contains(" DEBIT", ignoreCase = true) || rawDesc.endsWith(" DR", ignoreCase = true)) {
                isDebit = true
                amount = amounts.first()
            } else if (rawDesc.contains("/CR/", ignoreCase = true) || rawDesc.contains(" CREDIT", ignoreCase = true) || rawDesc.endsWith(" CR", ignoreCase = true)) {
                isDebit = false
                amount = amounts.first()
            } else {
                // If no text markers, use pure mathematics: Balance Difference.
                // We use > 0.5 to prevent floating-point precision errors (e.g., 0.00001 drift)
                if (previousBalance > 0.0 && currentBalance > 0.0) {
                    val diff = currentBalance - previousBalance
                    if (abs(diff) > 0.5) {
                        isDebit = diff < 0
                        amount = abs(diff)
                    } else {
                        // If balance didn't change (e.g. 0.0 txn fee), fallback to first column
                        isDebit = true
                        amount = amounts.first()
                    }
                } else {
                    // Absolute fallback if it's the very first row
                    isDebit = true
                    amount = amounts.first()
                }
            }

            previousBalance = currentBalance

            val cleanMerchant = extractMerchantName(rawDesc)
            val category = classifyCategory(cleanMerchant, rawDesc)
            val txnType = if (rawDesc.contains("UPI", ignoreCase = true)) "UPI" else "BANK"
            val timestamp = parseDateToMillis(date)

            // Only add valid transactions (ignores 0.00 balance forward rows)
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
            transactions = txns.reversed() // Reverse to ensure chronological order for the Calculator
        )
    }

    private fun extractMerchantName(raw: String): String {
        return try {
            // Extracts UPI handles (e.g., rohan@ybl)
            val upiIdRegex = """([a-zA-Z0-9.\-_]+@[a-zA-Z]+)""".toRegex()
            val upiMatch = upiIdRegex.find(raw)
            if (upiMatch != null) return upiMatch.groupValues[1].lowercase()

            // Cleans bank-specific jargon
            val clean = raw.replace(Regex("(?i)(UPI/(?:DR|CR)/[\\d]+/|NEFT/|IMPS/|RTGS/|ACH/|POS/|BIL/)"), "")
            val parts = clean.split(Regex("[/\\-]"))

            // Finds the first part that isn't purely numbers
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
            rawLower.contains("bounce") || rawLower.contains("ach return") || rawLower.contains("insufficient") || rawLower.contains("penalty") || rawLower.contains("chq rtn") -> "Penalties & Fees"
            lower.contains("zepto") || lower.contains("blinkit") || lower.contains("instamart") || lower.contains("dmart") || lower.contains("reliance") -> "Groceries"
            lower.contains("zomato") || lower.contains("swiggy") || lower.contains("dominos") || lower.contains("kfc") -> "Food & Dining"
            lower.contains("jio") || lower.contains("airtel") || lower.contains("vi") || lower.contains("recharge") || lower.contains("electricity") -> "Utilities"
            lower.contains("fuel") || lower.contains("petrol") || lower.contains("hpcl") || lower.contains("bpcl") || lower.contains("indian oil") -> "Fuel & Auto"
            lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") -> "Shopping"
            else -> "General"
        }
    }

    // ==========================================
    // 4. MULTI-FORMAT DATE ENGINE
    // ==========================================
    private fun parseDateToMillis(dateStr: String): Long {
        // Normalizes all separators to slashes
        val cleanDate = dateStr.replace(Regex("[-. ]+"), "/")

        // Includes patterns for single-digit days and 2-digit years
        val formatsToTry = listOf(
            "dd/MM/yyyy",
            "dd/MM/yy",
            "d/M/yyyy",
            "d/M/yy",
            "dd/MMM/yyyy",
            "d/MMM/yyyy",
            "dd/MMM/yy",
            "d/MMM/yy"
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

        return System.currentTimeMillis() // Absolute fallback so the app doesn't crash
    }
}