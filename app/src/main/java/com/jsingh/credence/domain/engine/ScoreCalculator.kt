package com.jsingh.credence.domain.engine

import com.jsingh.credence.domain.models.Transaction
import com.jsingh.credence.domain.models.TrustPortfolio
import com.jsingh.credence.domain.models.Vitals
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlin.math.sqrt

object ScoreCalculator {

    private val PERIOD_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy")
    private val ZONE: ZoneId = ZoneId.systemDefault()

    // Rebalanced for actual lender priorities
    private const val W_LIQUIDITY_BUFFER = 0.30   // Tier 1: Do they have cash on hand?
    private const val W_DEBT_BURDEN = 0.25        // Tier 1: Are they already overloaded?
    private const val W_INCOME_CONSISTENCY = 0.20 // Tier 2: Is income reliable?
    private const val W_PAYER_DIVERSITY = 0.15    // Tier 2: Risk of single-point failure
    private const val W_TXN_FREQUENCY = 0.10      // Tier 3: General activity

    private data class MonthAgg(val income: Double, val expense: Double) {
        val net: Double get() = income - expense
    }

    fun calculateScore(transactions: List<Transaction>, bankName: String = "Verified Bank"): TrustPortfolio {
        // Fallback for empty statements
        if (transactions.isEmpty()) {
            return TrustPortfolio(score = 0, tier = "Building", safeLoanLimit = 0.0, vitals = Vitals(0, 0, 0.0, 0, 0), bankName = bankName)
        }

        val earliestMs = transactions.minOf { it.timestamp }
        val latestMs = transactions.maxOf { it.timestamp }
        val spanDays = ((latestMs - earliestMs) / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(1)
        val spanMonths = (spanDays / 30.0).roundToInt().coerceAtLeast(1)
        val periodLabel = buildPeriodLabel(earliestMs, latestMs, spanMonths)

        // 1. Basic Aggregation
        val monthly: Map<YearMonth, MonthAgg> = transactions
            .groupBy { Instant.ofEpochMilli(it.timestamp).atZone(ZONE).toLocalDate().let { d -> YearMonth.of(d.year, d.month) } }
            .mapValues { (_, txns) ->
                MonthAgg(
                    income = txns.filter { !it.isExpense }.sumOf { it.amount },
                    expense = txns.filter { it.isExpense && it.category != "Penalties & Fees" }.sumOf { it.amount }
                )
            }

        val activeMonths = monthly.values.toList()
        val totalIncome = activeMonths.sumOf { it.income }
        val totalExpense = activeMonths.sumOf { it.expense }
        val avgMonthlyIncome = if (spanMonths > 0) totalIncome / spanMonths else totalIncome
        val avgMonthlyExpense = if (spanMonths > 0) totalExpense / spanMonths else totalExpense

        // 2. TIER 1: Debt Obligation Detection (EMIs)
        // Look for exact matching expense amounts > 500 across multiple months
        val expenses = transactions.filter { it.isExpense && it.category != "Penalties & Fees" }
        val estimatedMonthlyEMI = expenses.groupBy { it.amount }
            .filter { (amount, txns) ->
                amount > 500.0 && txns.distinctBy { Instant.ofEpochMilli(it.timestamp).atZone(ZONE).month }.size > 1
            }
            .keys.sum()

        // Calculate Debt Burden Score (Lower score if EMI consumes > 40% of income)
        val debtToIncomeRatio = if (avgMonthlyIncome > 0) estimatedMonthlyEMI / avgMonthlyIncome else 1.0
        val debtBurdenScore = (100 * (1.0 - (debtToIncomeRatio / 0.4).coerceAtMost(1.0))).roundToInt()

        // 3. TIER 1: Liquidity Buffer (Average/Min Balance)
        // NOTE: Requires 'balance' field in Transaction model
        val validBalances = transactions.mapNotNull { it.balance }.filter { it > 0 }
        val avgBalance = if (validBalances.isNotEmpty()) validBalances.average() else 0.0
        val liquidityBufferScore = if (avgMonthlyExpense > 0) {
            // Good score if average balance covers at least 1 month of expenses
            (100 * (avgBalance / avgMonthlyExpense).coerceAtMost(1.0)).roundToInt()
        } else 50

        // 4. TIER 1: Bounces and Penalties (Hard Defaults)
        val bounceCount = transactions.count { it.category == "Penalties & Fees" }
        val penaltyDeduction = (bounceCount * 15).coerceAtMost(45) // Harsher penalty for real underwriting

        // 5. TIER 2: Income Consistency (Strict Math)
        val incomeConsistencyScore: Int = if (activeMonths.size < 2) {
            if (totalIncome > 0) 50 else 0 // Less forgiving for new businesses
        } else {
            val incomes = activeMonths.map { it.income }
            val mean = incomes.average()
            if (mean <= 0.0) 0 else {
                val variance = incomes.sumOf { (it - mean) * (it - mean) } / incomes.size
                val stdDev = sqrt(variance)
                // Removed the artificial 0.5 cap - punishes high volatility accurately
                (100 * (1.0 - (stdDev / mean).coerceAtMost(1.0))).roundToInt()
            }
        }

        // 6. TIER 3: Breadth and Activity
        val txnsPerMonth = (transactions.size.toDouble() / spanMonths).roundToInt()
        val txnFrequencyScore = ((txnsPerMonth / 30.0) * 100).roundToInt().coerceIn(0, 100)

        val payerDiversity = transactions.filter { !it.isExpense }.distinctBy { it.merchantName }.size
        val payerDiversityScore = ((payerDiversity / 5.0) * 100).roundToInt().coerceIn(0, 100)

        // 7. Final Score Calculation
        val weightedFraction =
            (liquidityBufferScore * W_LIQUIDITY_BUFFER +
                    debtBurdenScore * W_DEBT_BURDEN +
                    incomeConsistencyScore * W_INCOME_CONSISTENCY +
                    payerDiversityScore * W_PAYER_DIVERSITY +
                    txnFrequencyScore * W_TXN_FREQUENCY) / 100.0

        val baseScore = ((weightedFraction * 100) - penaltyDeduction).roundToInt().coerceIn(0, 100)

        // Map to actual UI Tiers
        val tier = when {
            baseScore >= 75 -> "Gold"
            baseScore >= 50 -> "Silver"
            else -> "Building"
        }

        // Realistic Safe Loan Limit (FOIR Method: Income - Living Expenses - Existing EMI)
        val disposableIncome = (avgMonthlyIncome - avgMonthlyExpense - estimatedMonthlyEMI).coerceAtLeast(0.0)
        val safeLoanLimit = disposableIncome * 3.0 // Can afford this EMI for 3 months safely

        return TrustPortfolio(
            score = baseScore,
            tier = tier,
            safeLoanLimit = safeLoanLimit.roundToInt().toDouble(),
            // Ensure you add bounceCount and estimatedEMI to your Vitals data class!
            vitals = Vitals(incomeConsistencyScore, txnsPerMonth, debtToIncomeRatio, spanMonths, payerDiversity),
            recentTransactions = transactions.take(5),
            bankName = bankName,
            statementPeriodLabel = periodLabel,
            statementMonths = spanMonths
        )
    }

    private fun buildPeriodLabel(earliestMs: Long, latestMs: Long, months: Int): String {
        val start = Instant.ofEpochMilli(earliestMs).atZone(ZONE).format(PERIOD_FMT)
        val end = Instant.ofEpochMilli(latestMs).atZone(ZONE).format(PERIOD_FMT)
        return if (start == end) "1 month ($start)" else "$months months ($start \u2013 $end)"
    }
}