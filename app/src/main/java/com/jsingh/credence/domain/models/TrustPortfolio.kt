package com.jsingh.credence.domain.models

data class TrustPortfolio(
    val score: Int,                 // e.g., 750
    val previousScore: Int? = null, // last computed score, for the Home screen movement badge (null = no prior reading yet)
    val tier: String,               // e.g., "Gold", "Silver", "Building"
    val safeLoanLimit: Double,
    val vitals: Vitals,
    val recentTransactions: List<Transaction> = emptyList(),
    val activeLoans: List<ActiveLoan> = emptyList(), // surfaced on Home (next EMI due) and MyScore (full list)
    val bankName: String = "Bank statement",
    val statementPeriodLabel: String = "",
    val statementMonths: Int = 0
)

data class Vitals(
    val incomeConsistency: Int,
    val transactionFrequency: Int,

    // ✨ RESTORED: The Engine and UI need this specific variable for the Cashflow Metric Card
    val inflowOutflowRatio: Double,

    val longevityMonths: Int,
    val payerDiversity: Int,

    // ✨ KEPT YOUR UPGRADES: Safely tucked at the end with default values so they don't break the Engine
    val debtToIncomeRatio: Double = 0.0,
    val bounceCount: Int = 0,
    val estimatedEMI: Double = 0.0
)

enum class LoanStatus { ACTIVE, OVERDUE, CLOSED }

/** A loan the user currently has running, surfaced on both Home and the Profile ("My Score") tab. */
data class ActiveLoan(
    val id: String,
    val lenderName: String,
    val badge: String,
    val principal: Double,
    val outstanding: Double,
    val nextEmiAmount: Double,
    val nextEmiDate: String,
    val tenureMonths: Int,
    val monthsPaid: Int,
    val status: LoanStatus = LoanStatus.ACTIVE
) {
    val progress: Float
        get() = if (tenureMonths <= 0) 0f else (monthsPaid.toFloat() / tenureMonths.toFloat()).coerceIn(0f, 1f)
}