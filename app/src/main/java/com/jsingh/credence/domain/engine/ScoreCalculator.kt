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

    private const val W_INCOME_CONSISTENCY = 0.25
    private const val W_INFLOW_OUTFLOW = 0.20
    private const val W_TXN_FREQUENCY = 0.15
    private const val W_LONGEVITY = 0.15
    private const val W_PAYER_DIVERSITY = 0.15
    private const val W_LEAN_RESILIENCE = 0.10

    private data class MonthAgg(val income: Double, val expense: Double) {
        val net: Double get() = income - expense
    }

    data class FactorScores(
        val incomeConsistency: Int,
        val transactionFrequency: Int,
        val inflowOutflowRatio: Int,
        val longevity: Int,
        val payerDiversity: Int,
        val leanPeriodResilience: Int
    )

    fun calculateScore(transactions: List<Transaction>, bankName: String = "Verified Bank"): TrustPortfolio {
        if (transactions.isEmpty()) {
            return TrustPortfolio(
                score = 0,
                tier = "Building",
                safeLoanLimit = 0.0,
                vitals = Vitals(0, 0, 0.0, 0, 0),
                bankName = bankName
            )
        }

        // 1. Calculate Real Timespan
        val earliestMs = transactions.minOf { it.timestamp }
        val latestMs = transactions.maxOf { it.timestamp }

        // ✨ FIX: Ensure timespan accurately calculates the total duration in months
        val spanDays = ((latestMs - earliestMs) / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(1)
        val spanMonths = (spanDays / 30.0).roundToInt().coerceAtLeast(1)
        val periodLabel = buildPeriodLabel(earliestMs, latestMs, spanMonths)

        // 2. Bucket by YearMonth
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

        // Use spanMonths (total calendar duration) instead of activeMonths to calculate frequency
        val txnsPerMonth = (transactions.size.toDouble() / spanMonths).roundToInt()

        val payerDiversity = transactions.filter { !it.isExpense }.distinctBy { it.merchantName }.size

        // --- Factor 1: Income Consistency ---
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
        val txnFrequencyScore = ((txnsPerMonth / 60.0) * 100).roundToInt().coerceIn(0, 100)

        // --- Factor 3: Inflow / Outflow Ratio ---
        val inflowOutflowRatio = if (totalExpense > 0) totalIncome / totalExpense else if (totalIncome > 0) 2.0 else 1.0
        val inflowOutflowScore = (((inflowOutflowRatio - 0.5) / 1.0) * 100).roundToInt().coerceIn(0, 100)

        // --- Factor 4: Longevity ---
        val longevityScore = ((spanMonths / 12.0) * 100).roundToInt().coerceIn(0, 100)

        // --- Factor 5: Payer Diversity ---
        val payerDiversityScore = ((payerDiversity / 12.0) * 100).roundToInt().coerceIn(0, 100)

        // --- Factor 6: Lean-Period Resilience ---
        val avgMonthlyIncome = totalIncome / activeMonths.size.coerceAtLeast(1)
        val worstMonthNet = activeMonths.minOfOrNull { it.net } ?: 0.0
        val leanResilienceScore: Int = if (avgMonthlyIncome <= 0.0) {
            30
        } else {
            val dipRatio = worstMonthNet / avgMonthlyIncome
            when {
                dipRatio >= 0 -> 100
                dipRatio <= -1.0 -> 0
                else -> ((1.0 + dipRatio) * 100).roundToInt()
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

        // 3. Penalty / Risk Deduction
        val bounceCount = transactions.count { it.category == "Penalties & Fees" }
        val penaltyDeduction = (bounceCount * 5).coerceAtMost(25)

        val weightedFraction =
            (factorScores.incomeConsistency * W_INCOME_CONSISTENCY +
                    factorScores.inflowOutflowRatio * W_INFLOW_OUTFLOW +
                    factorScores.transactionFrequency * W_TXN_FREQUENCY +
                    factorScores.longevity * W_LONGEVITY +
                    factorScores.payerDiversity * W_PAYER_DIVERSITY +
                    factorScores.leanPeriodResilience * W_LEAN_RESILIENCE) / 100.0

        val finalScore = ((weightedFraction * 100) - penaltyDeduction).roundToInt().coerceIn(0, 100)

        val tier = when {
            finalScore >= 80 -> "Gold"
            finalScore >= 55 -> "Silver"
            else -> "Building"
        }

        // Recommend limit based on Tier safety
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
            longevityMonths = spanMonths,
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
            statementMonths = spanMonths
        )
    }

    private fun buildPeriodLabel(earliestMs: Long, latestMs: Long, months: Int): String {
        val start = Instant.ofEpochMilli(earliestMs).atZone(ZONE).format(PERIOD_FMT)
        val end = Instant.ofEpochMilli(latestMs).atZone(ZONE).format(PERIOD_FMT)
        val durationLabel = if (months <= 1) "1 month" else "$months months"
        return if (start == end) "$durationLabel ($start)" else "$durationLabel ($start \u2013 $end)"
    }
}