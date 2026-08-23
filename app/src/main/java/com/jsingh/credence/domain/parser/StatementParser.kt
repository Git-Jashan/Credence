package com.jsingh.credence.domain.parser

import android.util.Log
import com.jsingh.credence.domain.models.Transaction
import com.jsingh.credence.utils.Constants // This fixes the unresolved references!
import java.time.LocalDate
import java.time.ZoneId

object StatementParser {
    private const val TAG = "StatementParser"

    fun processStatement(cleanMessage: String): List<Transaction> {
        Log.d(TAG, "Processing bank statement lines...")
        val parsedTransactions = mutableListOf<Transaction>()

        val statementRowRegex = Regex("""^(\d{2}-\d{2}-\d{2})\s+(.*?)\s+(?:-|\w+)?\s*([\d,.]+)\s+([\d,.]+)\s+([\d,.]+)$""")
        val lines = cleanMessage.split("\n")

        for (line in lines) {
            val trimmed = line.trim()
            val match = statementRowRegex.find(trimmed) ?: continue

            try {
                val dateStr = match.groupValues[1]
                val localDate = LocalDate.parse(dateStr, Constants.PDF_DATE_FMT)
                val txnTimestamp = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val displayDate = localDate.format(Constants.DATE_FMT)

                val narration = match.groupValues[2].trim()
                val creditStr = match.groupValues[3].replace(",", "")
                val debitStr = match.groupValues[4].replace(",", "")

                val creditAmount = creditStr.toDoubleOrNull() ?: 0.0
                val debitAmount = debitStr.toDoubleOrNull() ?: 0.0

                if (creditAmount == 0.0 && debitAmount == 0.0) continue

                val isExpense = debitAmount > 0.0
                val finalAmount = if (isExpense) debitAmount else creditAmount

                var merchant = ""
                if (narration.startsWith("UPI/", ignoreCase = true)) {
                    val parts = narration.split("/")
                    if (parts.size >= 4 && parts[3].trim().isNotEmpty()) {
                        merchant = parts[3].trim()
                    }
                } else if (narration.contains("INTEREST CREDIT", ignoreCase = true)) {
                    merchant = "Bank Interest"
                }

                if (merchant.isEmpty()) { merchant = narration.take(25) }

                merchant = Constants.cleanStatementMerchant(merchant)

                val txn = Transaction(
                    title = merchant,
                    amount = finalAmount,
                    isExpense = isExpense,
                    category = Constants.guessCategoryLocally(merchant),
                    date = displayDate,
                    time = "12:00 AM",
                    timestamp = txnTimestamp,
                    merchantName = merchant,
                    transactionType = "BANK",
                    account = "Bank Statement",
                    source = "STATEMENT",
                    phoneNumber = Constants.NO_VALUE
                )
                parsedTransactions.add(txn)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse line: $trimmed", e)
            }
        }
        return parsedTransactions
    }
}