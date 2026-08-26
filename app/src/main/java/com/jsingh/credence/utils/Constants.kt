package com.jsingh.credence.utils

import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/** ₹ formatting kept top-level for easy access anywhere in the UI */
fun formatInr(value: Double): String = "\u20B9${value.roundToInt()}"

object Constants {
    // --- UI CONSTANTS ---
    val TIER_ORDER = listOf("Building", "Silver", "Gold")

    // --- PARSER CONSTANTS ---
    const val NO_VALUE = "UNKNOWN"
    val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val PDF_DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yy")

    /** Cleans up messy bank narrative strings into readable merchant names */
    fun cleanStatementMerchant(rawName: String): String {
        return rawName
            // Added MMT, BIL, and INB to catch more generic bank prefixes
            .replace(Regex("(?i)^(IMPS|NEFT|RTGS|UPI|FT|MMT|BIL|INB)-?"), "")
            .replace(Regex("[^a-zA-Z0-9 ]"), "")
            .trim()
            .take(30)
    }

    /**
     * Upgraded to catch common micro-business & gig worker expenses
     * (Fuel, Telecom, B2B supplies).
     */
    fun guessCategoryLocally(merchant: String): String {
        val lower = merchant.lowercase()
        return when {
            lower.contains("swiggy") || lower.contains("zomato") || lower.contains("blinkit") -> "Food & Delivery"
            lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") -> "Shopping"
            lower.contains("jio") || lower.contains("airtel") || lower.contains("vi") || lower.contains("recharge") -> "Telecom/Utility"
            lower.contains("petrol") || lower.contains("fuel") || lower.contains("hpcl") || lower.contains("ioc") || lower.contains("bpcl") -> "Fuel & Transport"
            lower.contains("traders") || lower.contains("wholesale") || lower.contains("enterprises") || lower.contains("agency") -> "B2B Supplies"
            lower.contains("interest") || lower.contains("salary") || lower.contains("credited") -> "Income"
            else -> "General"
        }
    }

    /**
     * Ordered so more specific names (e.g. "IDFC FIRST") are checked before generic ones.
     * Matched against the raw, lowercased PDF text dump of the statement.
     */
    private val BANK_KEYWORDS: List<Pair<String, List<String>>> = listOf(
        "HDFC Bank" to listOf("hdfc bank", "hdfc"),
        "State Bank of India" to listOf("state bank of india", "sbi"),
        "ICICI Bank" to listOf("icici bank", "icici"),
        "Axis Bank" to listOf("axis bank"),
        "IDFC FIRST Bank" to listOf("idfc first", "idfc bank"),
        "Kotak Mahindra Bank" to listOf("kotak mahindra", "kotak bank"),
        "Punjab National Bank" to listOf("punjab national bank", "pnb"),
        "Bank of Baroda" to listOf("bank of baroda"),
        "Bank of India" to listOf("bank of india"),
        "Canara Bank" to listOf("canara bank"),
        "Union Bank of India" to listOf("union bank of india"),
        "IndusInd Bank" to listOf("indusind bank"),
        "Yes Bank" to listOf("yes bank"),
        "Federal Bank" to listOf("federal bank"),
        "IDBI Bank" to listOf("idbi bank"),
        "Central Bank of India" to listOf("central bank of india"),
        "Indian Bank" to listOf("indian bank"),
        "UCO Bank" to listOf("uco bank"),
        "Paytm Payments Bank" to listOf("paytm payments bank")
    )

    /** Best-effort bank identification from the raw text extracted off the uploaded PDF. */
    fun detectBankName(rawText: String): String {
        val lower = rawText.lowercase()
        for ((display, keys) in BANK_KEYWORDS) {
            if (keys.any { lower.contains(it) }) return display
        }
        return "Bank statement"
    }
}