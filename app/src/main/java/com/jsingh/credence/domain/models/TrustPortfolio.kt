package com.jsingh.credence.domain.models

data class TrustPortfolio(
    val score: Int,
    val previousScore: Int? = null,
    val tier: String,
    val safeLoanLimit: Double,
    val vitals: Vitals,
    val recentTransactions: List<Transaction> = emptyList(),
    val activeLoans: List<ActiveLoan> = emptyList(),
    val bankName: String = "Bank statement",
    val statementPeriodLabel: String = "",
    val statementMonths: Int = 0
)

data class Vitals(
    val incomeConsistency: Int,
    val transactionFrequency: Int,
    val inflowOutflowRatio: Double,
    val longevityMonths: Int,
    val payerDiversity: Int,
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