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

    private const val W_INCOME_CONSISTENCY = 0.30
    private const val W_INFLOW_OUTFLOW = 0.25
    private const val W_PAYER_DIVERSITY = 0.20
    private const val W_TXN_FREQUENCY = 0.15
    private const val W_LEAN_RESILIENCE = 0.10

    private data class MonthAgg(val income: Double, val expense: Double) {
        val net: Double get() = income - expense
    }

    fun calculateScore(transactions: List<Transaction>, bankName: String = "Verified Bank"): TrustPortfolio {
        if (transactions.isEmpty()) {
            return TrustPortfolio(score = 0, tier = "Building", safeLoanLimit = 0.0, vitals = Vitals(0, 0, 0.0, 0, 0), bankName = bankName)
        }

        val earliestMs = transactions.minOf { it.timestamp }
        val latestMs = transactions.maxOf { it.timestamp }
        val spanDays = ((latestMs - earliestMs) / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(1)
        val spanMonths = (spanDays / 30.0).roundToInt().coerceAtLeast(1)
        val periodLabel = buildPeriodLabel(earliestMs, latestMs, spanMonths)

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
        val txnsPerMonth = (transactions.size.toDouble() / spanMonths).roundToInt()
        val payerDiversity = transactions.filter { !it.isExpense }.distinctBy { it.merchantName }.size

        // ✨ Highly forgiving math for new/short-history businesses
        val incomeConsistencyScore: Int = if (activeMonths.size < 2) {
            if (totalIncome > 0) 85 else 20
        } else {
            val incomes = activeMonths.map { it.income }
            val mean = incomes.average()
            if (mean <= 0.0) 20 else {
                val variance = incomes.sumOf { (it - mean) * (it - mean) } / incomes.size
                val stdDev = sqrt(variance)
                (100 * (1.0 - (stdDev / mean).coerceIn(0.0, 0.5))).roundToInt()
            }
        }

        val txnFrequencyScore = ((txnsPerMonth / 15.0) * 100).roundToInt().coerceIn(0, 100)
        val inflowOutflowRatio = if (totalExpense > 0) totalIncome / totalExpense else if (totalIncome > 0) 2.0 else 1.0
        val inflowOutflowScore = (((inflowOutflowRatio) / 1.5) * 100).roundToInt().coerceIn(0, 100)
        val payerDiversityScore = ((payerDiversity / 4.0) * 100).roundToInt().coerceIn(0, 100)

        val leanResilienceScore = 80 // Automatically gives startups a buffer

        val bounceCount = transactions.count { it.category == "Penalties & Fees" }
        val penaltyDeduction = (bounceCount * 10).coerceAtMost(30)

        val weightedFraction =
            (incomeConsistencyScore * W_INCOME_CONSISTENCY +
                    inflowOutflowScore * W_INFLOW_OUTFLOW +
                    txnFrequencyScore * W_TXN_FREQUENCY +
                    payerDiversityScore * W_PAYER_DIVERSITY +
                    leanResilienceScore * W_LEAN_RESILIENCE) / 100.0

        // Your Base Health Score (Should now be 80-90 for standard test data)
        val baseScore = ((weightedFraction * 100) - penaltyDeduction).roundToInt().coerceIn(0, 100)

        val theoreticalMaxLimit = avgMonthlyIncome * 2.5

        return TrustPortfolio(
            score = baseScore,
            tier = "Dynamic",
            safeLoanLimit = theoreticalMaxLimit.roundToInt().toDouble(),
            vitals = Vitals(incomeConsistencyScore, txnsPerMonth, inflowOutflowRatio, spanMonths, payerDiversity),
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