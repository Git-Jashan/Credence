package com.jsingh.credence.domain.engine

import com.jsingh.credence.domain.models.Transaction
import com.jsingh.credence.domain.models.TrustPortfolio
import com.jsingh.credence.domain.models.Vitals
import kotlin.math.roundToInt

object ScoreCalculator {

    fun calculateScore(transactions: List<Transaction>): TrustPortfolio {
        if (transactions.isEmpty()) {
            return TrustPortfolio(0, "Building", 0.0, Vitals(0, 0, 0.0, 0, 0))
        }

        // 1. Calculate basic totals
        val totalIncome = transactions.filter { !it.isExpense }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.isExpense }.sumOf { it.amount }

        // 2. Vitals Calculation (Simplified for MVP)
        val txnsPerMonth = if (transactions.isNotEmpty()) transactions.size / 6 else 0 // Assuming 6 month statement
        val inflowOutflow = if (totalExpense > 0) totalIncome / totalExpense else 1.0
        val incomeConsistency = if (totalIncome > 0) 85 else 40 // Mocked logic: 85% if they have income
        val payerDiversity = transactions.filter { !it.isExpense }.distinctBy { it.merchantName }.size

        // 3. Compute Base Score (Max 900)
        var baseScore = 300 // Starting score

        // Add points for good inflow ratio
        if (inflowOutflow > 1.2) baseScore += 150
        else if (inflowOutflow > 1.0) baseScore += 80

        // Add points for frequency (shows active business)
        if (txnsPerMonth > 50) baseScore += 200
        else if (txnsPerMonth > 20) baseScore += 100

        // Add points for payer diversity (not just relying on one customer)
        if (payerDiversity > 10) baseScore += 150
        else if (payerDiversity > 3) baseScore += 75

        // Cap score at 900
        val finalScore = baseScore.coerceAtMost(900)

        // 4. Determine Tier
        val tier = when {
            finalScore >= 750 -> "Gold"
            finalScore >= 600 -> "Silver"
            else -> "Building"
        }

        // 5. Calculate Recommended Loan Limit (e.g., 20% of 6-month income)
        val safeLimit = (totalIncome * 0.20).roundToInt().toDouble()

        val vitals = Vitals(
            incomeConsistency = incomeConsistency,
            transactionFrequency = txnsPerMonth,
            inflowOutflowRatio = inflowOutflow,
            longevityMonths = 6, // Assuming 6-month statement uploaded
            payerDiversity = payerDiversity
        )

        return TrustPortfolio(
            score = finalScore,
            tier = tier,
            safeLoanLimit = safeLimit,
            vitals = vitals,
            recentTransactions = transactions.take(5) // Just keep a preview for the UI
        )
    }
}