package com.jsingh.credence.domain.engine

import com.jsingh.credence.domain.models.Transaction
import com.jsingh.credence.domain.models.TrustPortfolio
import com.jsingh.credence.domain.models.Vitals
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

object ScoreCalculator {

    private val PERIOD_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy")
    private val ZONE: ZoneId = ZoneId.systemDefault()

    // BEHAVIORAL WEIGHTS (Scale/Turnover is strictly used for Capacity, not Trust)
    private const val W_CASHFLOW_HEALTH = 0.25
    private const val W_LIQUIDITY_CUSHION = 0.25
    private const val W_INCOME_CONSISTENCY = 0.20
    private const val W_PAYER_DIVERSITY = 0.15
    private const val W_DEBT_DISCIPLINE = 0.15

    private data class MonthAgg(val income: Double, val expense: Double, val minBalance: Double)

    // SMART INTERPOLATION: Automatically handles normal (higher=better) and inverted (lower=better) ranges
    private fun interpolate(value: Double, minVal: Double, maxVal: Double, minScore: Double, maxScore: Double): Double {
        if (minVal < maxVal) { // Normal Range (e.g., Liquidity: 500 to 15000)
            if (value <= minVal) return minScore
            if (value >= maxVal) return maxScore
        } else { // Inverted Range (e.g., Debt Ratio: 0.50 to 0.0)
            if (value >= minVal) return minScore
            if (value <= maxVal) return maxScore
        }
        val ratio = (value - minVal) / (maxVal - minVal)
        return minScore + (ratio * (maxScore - minScore))
    }

    fun calculateScore(transactions: List<Transaction>, bankName: String = "Verified Bank"): TrustPortfolio {
        if (transactions.isEmpty()) return fallbackEmptyPortfolio(bankName)

        val earliestMs = transactions.minOf { it.timestamp }
        val latestMs = transactions.maxOf { it.timestamp }
        val spanDays = ((latestMs - earliestMs) / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(1)
        val spanMonths = max(1, (spanDays / 30.0).roundToInt())
        val periodLabel = buildPeriodLabel(earliestMs, latestMs, spanMonths)

        // 1. CONFIDENCE LEVEL & DAMPENING FACTOR
        val (confidenceLevel, confidenceMultiplier) = when {
            spanMonths < 3 -> "Low (Thin File)" to 0.50       // Slashes loan limit by 50%
            spanMonths < 6 -> "Medium (Building)" to 0.75     // Slashes loan limit by 25%
            else -> "High (Established)" to 1.0               // Full capacity unlocked
        }

        // 2. MONTHLY AGGREGATION & LIQUIDITY
        val monthly: Map<YearMonth, MonthAgg> = transactions
            .groupBy { Instant.ofEpochMilli(it.timestamp).atZone(ZONE).toLocalDate().let { d -> YearMonth.of(d.year, d.month) } }
            .mapValues { (_, txns) ->
                MonthAgg(
                    income = txns.filter { !it.isExpense }.sumOf { it.amount },
                    expense = txns.filter { it.isExpense && it.category != "Penalties & Fees" }.sumOf { it.amount },
                    minBalance = txns.minOfOrNull { it.runningBalance } ?: 0.0
                )
            }

        val activeMonths = monthly.values.toList()
        val totalIncome = activeMonths.sumOf { it.income }
        val totalExpense = activeMonths.sumOf { it.expense }
        val avgMonthlyIncome = if (spanMonths > 0) totalIncome / spanMonths else totalIncome

        // 3. SCORE: LIQUIDITY CUSHION (Range adjusted for realistic micro-merchant buffers)
        val avgMinBalance = activeMonths.map { it.minBalance }.average()
        val liquidityScore = interpolate(avgMinBalance, minVal = 500.0, maxVal = 15000.0, minScore = 30.0, maxScore = 100.0)

        // 4. SCORE: CASHFLOW HEALTH
        val inflowOutflowRatio = if (totalExpense > 0) totalIncome / totalExpense else 1.0
        val cashflowScore = interpolate(inflowOutflowRatio, minVal = 0.70, maxVal = 1.15, minScore = 30.0, maxScore = 100.0)

        // 5. SCORE: TRUE EMI IDENTIFICATION
        val strictLoanKeywords = listOf("nach", "ach", "ecs", "emi")
        val verifiedLoanTxns = transactions.filter { txn ->
            txn.isExpense && strictLoanKeywords.any { kw -> txn.rawDescContains(kw) }
        }
        val estimatedMonthlyEMI = verifiedLoanTxns.groupBy { it.amount }
            .filter { (_, txns) -> txns.size >= 2 }
            .keys.sum()

        val debtToIncomeRatio = if (avgMonthlyIncome > 0) estimatedMonthlyEMI / avgMonthlyIncome else 0.0
        val debtScore = interpolate(debtToIncomeRatio, minVal = 0.50, maxVal = 0.0, minScore = 30.0, maxScore = 100.0)

        // 6. SCORE: INCOME CONSISTENCY (Volatility)
        var cv = 0.0
        val incomeConsistencyScore = if (activeMonths.size < 2) 50.0 else {
            val incomes = activeMonths.map { it.income }
            val mean = incomes.average()
            if (mean <= 0.0) 30.0 else {
                val variance = incomes.sumOf { (it - mean) * (it - mean) } / incomes.size
                cv = sqrt(variance) / mean
                interpolate(cv, minVal = 0.60, maxVal = 0.15, minScore = 40.0, maxScore = 100.0)
            }
        }

        // 7. SCORE: PAYER DIVERSITY
        val payerDiversity = transactions.filter { !it.isExpense }.distinctBy { it.merchantName }.size
        val diversityScore = interpolate(payerDiversity.toDouble(), minVal = 1.0, maxVal = 15.0, minScore = 35.0, maxScore = 100.0)

        // 8. PENALTY DEDUCTIONS
        val bounceCount = transactions.count { it.category == "Penalties & Fees" }
        val penaltyDeduction = (bounceCount * 12).coerceAtMost(36).toDouble()

        // ASSEMBLE FINAL SCORE
        val weightedTotal = (
                liquidityScore * W_LIQUIDITY_CUSHION +
                        cashflowScore * W_CASHFLOW_HEALTH +
                        incomeConsistencyScore * W_INCOME_CONSISTENCY +
                        diversityScore * W_PAYER_DIVERSITY +
                        debtScore * W_DEBT_DISCIPLINE
                )

        val finalScore = (weightedTotal - penaltyDeduction).roundToInt().coerceIn(30, 95)
        val tier = if (finalScore >= 75) "Gold" else if (finalScore >= 55) "Silver" else "Building"

        // ACTIONABLE DECISION NARRATIVE (The "Why")
        val narrative = buildActionableNarrative(
            confidenceLevel = confidenceLevel,
            bounces = bounceCount,
            debtRatio = debtToIncomeRatio,
            cv = cv,
            minBalance = avgMinBalance,
            avgIncome = avgMonthlyIncome
        )

        // CAPACITY CALCULATION (DSCR Method + Confidence Dampening + Margin Floor)
        val avgOperatingExpense = max(0.0, (totalExpense / spanMonths) - estimatedMonthlyEMI)

        // The Margin Floor: If actual surplus drops below 15% of income due to cash withdrawals, assume a 15% profit margin.
        val actualSurplus = avgMonthlyIncome - avgOperatingExpense
        val netOperatingSurplus = max(avgMonthlyIncome * 0.15, actualSurplus)

        // Never commit more than 40% of NET cash surplus to a loan.
        val maxSafeEMI = max(0.0, (netOperatingSurplus * 0.40) - estimatedMonthlyEMI)

        // Apply the confidence dampener: Thin files get lower limits regardless of score
        val rawLimit = maxSafeEMI * 12.0 * (finalScore / 100.0)
        val safeLoanLimit = max(0.0, rawLimit * confidenceMultiplier).roundToInt().toDouble()

        val finalVitals = Vitals(
            incomeConsistency = incomeConsistencyScore.roundToInt(),
            transactionFrequency = (transactions.size.toDouble() / spanMonths).roundToInt(),
            inflowOutflowRatio = inflowOutflowRatio,
            longevityMonths = spanMonths,
            payerDiversity = payerDiversity,
            debtToIncomeRatio = debtToIncomeRatio,
            bounceCount = bounceCount,
            estimatedEMI = estimatedMonthlyEMI,
            avgMinBalance = avgMinBalance
        )

        return TrustPortfolio(
            score = finalScore, tier = tier, safeLoanLimit = safeLoanLimit, vitals = finalVitals,
            recentTransactions = transactions.take(5), bankName = bankName, statementPeriodLabel = periodLabel,
            statementMonths = spanMonths, confidenceLevel = confidenceLevel, decisionNarrative = narrative
        )
    }

    private fun buildActionableNarrative(
        confidenceLevel: String,
        bounces: Int,
        debtRatio: Double,
        cv: Double,
        minBalance: Double,
        avgIncome: Double
    ): String {
        // Emojis are vital here: The UI uses them to secretly color-code the boxes (red/yellow/green), then strips them out visually.
        if (bounces >= 3) return "🔴 High Risk: $bounces banking penalties detected. Action: Maintain adequate balance before auto-debit dates to rebuild trust."
        if (confidenceLevel.contains("Low")) return "🟡 Thin File: Fundamentals look fine, but history is limited. Action: Route daily business sales through this account for 60 more days to unlock higher tier limits."
        if (debtRatio > 0.35) return "🟡 Over-Leveraged: Existing EMIs consume ${(debtRatio * 100).toInt()}% of monthly income. Action: Clear existing micro-loans before seeking new capital."
        if (cv > 0.50) return "🟡 Volatile Cashflow: Monthly income fluctuates heavily. Action: Build a steady ₹${max(1000.0, avgIncome * 0.1).toInt()} minimum balance cushion to protect against slow weeks."
        if (minBalance < 1500) return "🟡 Liquidity Warning: Account frequently empties. Action: Retain a minimum buffer of ₹1,500 at month-end to improve risk profile and loan capacity."

        return "🟢 Prime Profile: Highly stable income, healthy liquidity cushion, and clean repayment history. Approved for maximum tier capacity."
    }

    private fun Transaction.rawDescContains(kw: String): Boolean {
        return this.merchantName.lowercase().contains(kw) || this.title.lowercase().contains(kw)
    }

    private fun buildPeriodLabel(earliestMs: Long, latestMs: Long, months: Int): String {
        val start = Instant.ofEpochMilli(earliestMs).atZone(ZONE).format(PERIOD_FMT)
        val end = Instant.ofEpochMilli(latestMs).atZone(ZONE).format(PERIOD_FMT)
        return if (start == end) "1 month ($start)" else "$months months ($start \u2013 $end)"
    }

    private fun fallbackEmptyPortfolio(bankName: String) = TrustPortfolio(
        score = 0, tier = "Building", safeLoanLimit = 0.0, bankName = bankName,
        vitals = Vitals(0, 0, 0.0, 0, 0, 0.0, 0, 0.0, 0.0),
        recentTransactions = emptyList(), statementPeriodLabel = "No Data", statementMonths = 0,
        confidenceLevel = "None", decisionNarrative = "No transactional data found. Please upload a valid statement."
    )
}