package com.jsingh.credence.domain.models

data class TrustPortfolio(
    val score: Int,                 // e.g., 750
    val tier: String,               // e.g., "Gold", "Silver", "Building"
    val safeLoanLimit: Double,
    val vitals: Vitals,
    val recentTransactions: List<Transaction> = emptyList()
)

data class Vitals(
    val incomeConsistency: Int,
    val transactionFrequency: Int,
    val inflowOutflowRatio: Double,
    val longevityMonths: Int,
    val payerDiversity: Int
)