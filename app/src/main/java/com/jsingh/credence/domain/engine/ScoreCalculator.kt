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

/**
 * Rule-based, explainable scoring engine — no black-box model, every factor below maps directly
 * to a sub-score you can show the user (see [FactorScores]). Six factors, weighted composite,
 * banded into Building / Silver / Gold, matching the project's scoring spec.
 */
object ScoreCalculator {

    private val PERIOD_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy")
    private val ZONE: ZoneId = ZoneId.systemDefault()

    // Weights sum to 1.0. Income consistency and inflow/outflow carry the most weight because
    // they're the strongest signals of repayment ability; longevity and lean-period resilience
    // carry less because a short but clean statement shouldn't be penalized too harshly.
    private const val W_INCOME_CONSISTENCY = 0.25
    private const val W_INFLOW_OUTFLOW = 0.20
    private const val W_TXN_FREQUENCY = 0.15
    private const val W_LONGEVITY = 0.15
    private const val W_PAYER_DIVERSITY = 0.15
    private const val W_LEAN_RESILIENCE = 0.10

    private data class MonthAgg(val income: Double, val expense: Double) {
        val net: Double get() = income - expense
    }

    /** Per-factor 0–100 sub-scores, for showing "why this score" in the UI instead of a black box. */
    data class FactorScores(
        val incomeConsistency: Int,
        val transactionFrequency: Int,
        val inflowOutflowRatio: Int,
        val longevity: Int,
        val payerDiversity: Int,
        val leanPeriodResilience: Int
    )

    fun calculateScore(transactions: List<Transaction>, bankName: String = "Bank statement"): TrustPortfolio {
        if (transactions.isEmpty()) {
            return TrustPortfolio(0, "Building", 0.0, Vitals(0, 0, 0.0, 0, 0), bankName = bankName)
        }

        // 1. Real span of the uploaded statement — never assume 6 months.
        val earliestMs = transactions.minOf { it.timestamp }
        val latestMs = transactions.maxOf { it.timestamp }
        val spanDays = ((latestMs - earliestMs) / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(1)
        val months = (spanDays / 30.0).roundToInt().coerceAtLeast(1)
        val periodLabel = buildPeriodLabel(earliestMs, latestMs, months)

        // 2. Bucket every transaction into its calendar month so each factor below can look at
        // month-to-month behaviour instead of just lump-sum totals.
        val monthly: Map<YearMonth, MonthAgg> = transactions
            .groupBy { Instant.ofEpochMilli(it.timestamp).atZone(ZONE).toLocalDate().let { d -> YearMonth.of(d.year, d.month) } }
            .mapValues { (_, txns) ->
                MonthAgg(
                    income = txns.filter { !it.isExpense }.sumOf { it.amount },
                    expense = txns.filter { it.isExpense }.sumOf { it.amount }
                )
            }
        val activeMonths = monthly.values.toList()
        val monthCount = activeMonths.size.coerceAtLeast(1)

        val totalIncome = activeMonths.sumOf { it.income }
        val totalExpense = activeMonths.sumOf { it.expense }
        val txnsPerMonth = (transactions.size.toDouble() / months).roundToInt()
        val payerDiversity = transactions.filter { !it.isExpense }.distinctBy { it.merchantName }.size

        // --- Factor 1: Income Consistency ---
        // Real coefficient-of-variation across months, not a hardcoded 85/40. With only one
        // month of data there's nothing to compare against, so we fall back to a capped partial
        // score rather than pretending we've measured consistency.
        val incomeConsistencyScore: Int = if (activeMonths.size < 2) {
            if (totalIncome > 0) 55 else 20
        } else {
            val incomes = activeMonths.map { it.income }
            val mean = incomes.average()
            if (mean <= 0.0) {
                20
            } else {
                val variance = incomes.sumOf { (it - mean) * (it - mean) } / incomes.size
                val stdDev = sqrt(variance)
                val coefficientOfVariation = stdDev / mean
                (100 * (1.0 - coefficientOfVariation.coerceIn(0.0, 1.0))).roundToInt()
            }
        }

        // --- Factor 2: Transaction Frequency ---
        // Continuous scale instead of two buckets — 60+ txns/month is treated as a fully active,
        // established business; scales linearly below that.
        val txnFrequencyScore = ((txnsPerMonth / 60.0) * 100).roundToInt().coerceIn(0, 100)

        // --- Factor 3: Inflow / Outflow Ratio ---
        // Centered so a ratio of 1.0 (breaking even) sits at 50, comfortably positive cashflow
        // (1.5x+) maxes out at 100, and spending more than double income floors at 0.
        val inflowOutflowRatio = if (totalExpense > 0) totalIncome / totalExpense else if (totalIncome > 0) 2.0 else 1.0
        val inflowOutflowScore = (((inflowOutflowRatio - 0.5) / 1.0) * 100).roundToInt().coerceIn(0, 100)

        // --- Factor 4: Longevity ---
        // Was computed but silently discarded before — now it actually contributes. 12 months of
        // history maxes out the factor.
        val longevityScore = ((months / 12.0) * 100).roundToInt().coerceIn(0, 100)

        // --- Factor 5: Payer Diversity ---
        // Not relying on a single customer/employer. 12 distinct payers maxes out the factor.
        val payerDiversityScore = ((payerDiversity / 12.0) * 100).roundToInt().coerceIn(0, 100)

        // --- Factor 6: Lean-Period Resilience (new — was entirely missing before) ---
        // Finds the single worst month by net cashflow and checks how deep that dip went relative
        // to average monthly income. Never dipping negative maxes the factor; a catastrophic worst
        // month (net loss equal to or exceeding a full month's average income) floors it.
        val avgMonthlyIncome = totalIncome / monthCount
        val worstMonthNet = activeMonths.minOfOrNull { it.net } ?: 0.0
        val leanResilienceScore: Int = if (avgMonthlyIncome <= 0.0) {
            30
        } else {
            val dipRatio = worstMonthNet / avgMonthlyIncome // 0 or positive = never dipped
            when {
                dipRatio >= 0 -> 100
                dipRatio <= -1.0 -> 0
                else -> ((1.0 + dipRatio) * 100).roundToInt() // linear from 100 down to 0
            }
        }.coerceIn(0, 100)

        val factorScores = FactorScores(
            incomeConsistency = incomeConsistencyScore.coerceIn(0, 100),
            transactionFrequency = txnFrequencyScore,
            inflowOutflowRatio = inflowOutflowScore,
            longevity = longevityScore,
            payerDiversity = payerDiversityScore,
            leanPeriodResilience = leanResilienceScore
        )

        // 3. Weighted composite -> 300-900 band, same range typical credit scores use so lenders
        // reading this number don't need it re-explained to them.
        val weightedFraction =
            (factorScores.incomeConsistency * W_INCOME_CONSISTENCY +
                    factorScores.inflowOutflowRatio * W_INFLOW_OUTFLOW +
                    factorScores.transactionFrequency * W_TXN_FREQUENCY +
                    factorScores.longevity * W_LONGEVITY +
                    factorScores.payerDiversity * W_PAYER_DIVERSITY +
                    factorScores.leanPeriodResilience * W_LEAN_RESILIENCE) / 100.0

        val finalScore = (300 + weightedFraction * 600).roundToInt().coerceIn(300, 900)

        val tier = when {
            finalScore >= 750 -> "Gold"
            finalScore >= 600 -> "Silver"
            else -> "Building"
        }

        // 4. Recommended loan limit — scaled by tier so the number reflects demonstrated risk,
        // not just raw income. (Previously a flat 20% regardless of how strong the profile was.)
        val limitMultiplier = when (tier) {
            "Gold" -> 0.25
            "Silver" -> 0.20
            else -> 0.15
        }
        val safeLimit = (totalIncome * limitMultiplier).roundToInt().toDouble()

        val vitals = Vitals(
            incomeConsistency = factorScores.incomeConsistency,
            transactionFrequency = txnsPerMonth,
            inflowOutflowRatio = inflowOutflowRatio,
            longevityMonths = months,
            payerDiversity = payerDiversity
        )

        return TrustPortfolio(
            score = finalScore,
            tier = tier,
            safeLoanLimit = safeLimit,
            vitals = vitals,
            recentTransactions = transactions.take(5),
            bankName = bankName,
            statementPeriodLabel = periodLabel,
            statementMonths = months
        )
    }

    private fun buildPeriodLabel(earliestMs: Long, latestMs: Long, months: Int): String {
        val start = Instant.ofEpochMilli(earliestMs).atZone(ZONE).format(PERIOD_FMT)
        val end = Instant.ofEpochMilli(latestMs).atZone(ZONE).format(PERIOD_FMT)
        val durationLabel = if (months <= 1) "1 month" else "$months months"
        return if (start == end) "$durationLabel ($start)" else "$durationLabel ($start \u2013 $end)"
    }
}