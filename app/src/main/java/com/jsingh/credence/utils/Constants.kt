package com.jsingh.credence.utils

import java.time.format.DateTimeFormatter

object Constants {
    const val NO_VALUE = "UNKNOWN"
    val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val PDF_DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yy")

    fun cleanStatementMerchant(rawName: String): String {
        return rawName
            .replace(Regex("(?i)^(IMPS|NEFT|RTGS|UPI|FT)-?"), "")
            .replace(Regex("[^a-zA-Z0-9 ]"), "")
            .trim()
            .take(30)
    }

    fun guessCategoryLocally(merchant: String): String {
        val lower = merchant.lowercase()
        return when {
            lower.contains("swiggy") || lower.contains("zomato") -> "Food"
            lower.contains("amazon") || lower.contains("flipkart") -> "Shopping"
            lower.contains("interest") -> "Income"
            else -> "General"
        }
    }
}