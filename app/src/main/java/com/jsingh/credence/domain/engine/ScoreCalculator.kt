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

    // Professional Lending Weights
    private const val W_TURNOVER_VOLUME = 0.25      // Scale of cash passing through
    private const val W_CASHFLOW_HEALTH = 0.25      // Inflow vs Outflow balance
    private const val W_INCOME_CONSISTENCY = 0.20   // Low volatility across months
    private const val W_PAYER_DIVERSITY = 0.15      // Customer/source spread
    private const val W_DEBT_DISCIPLINE = 0.15      // Verified EMI capacity

    private data class MonthAgg(val income: Double, val expense: Double)

    fun calculateScore(transactions: List<Transaction>, bankName: String = "Verified Bank"): TrustPortfolio {
        if (transactions.isEmpty()) {
            return TrustPortfolio(
                score = 0, tier = "Building", safeLoanLimit = 0.0, bankName = bankName,
                vitals = Vitals(incomeConsistency = 0, transactionFrequency = 0, inflowOutflowRatio = 0.0, longevityMonths = 0, payerDiversity = 0)
            )
        }

        val earliestMs = transactions.minOf { it.timestamp }
        val latestMs = transactions.maxOf { it.timestamp }
        val spanDays = ((latestMs - earliestMs) / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(1)
        val spanMonths = max(1, (spanDays / 30.0).roundToInt())
        val periodLabel = buildPeriodLabel(earliestMs, latestMs, spanMonths)

        // 1. Monthly Aggregation
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

        // =========================================================================
        // 1. CASHFLOW HEALTH (Normalized to real-world accounts)
        // =========================================================================
        // In real business accounts, Inflow ~= Outflow. A ratio of 0.95 to 1.1x is standard and healthy.
        val inflowOutflowRatio = if (totalExpense > 0) totalIncome / totalExpense else if (totalIncome > 0) 1.5 else 1.0
        val cashflowScore = when {
            inflowOutflowRatio >= 1.20 -> 100
            inflowOutflowRatio >= 1.00 -> 85
            inflowOutflowRatio >= 0.90 -> 70
            inflowOutflowRatio >= 0.75 -> 50
            else -> 30
        }

        // =========================================================================
        // 2. TURNOVER VOLUME & REVENUE STRENGTH
        // =========================================================================
        // Businesses with healthy throughput score high on baseline capacity
        val turnoverScore = when {
            avgMonthlyIncome >= 150000 -> 100
            avgMonthlyIncome >= 80000  -> 85
            avgMonthlyIncome >= 40000  -> 75
            avgMonthlyIncome >= 20000  -> 65
            avgMonthlyIncome > 0       -> 50
            else                       -> 20
        }

        // =========================================================================
        // 3. TRUE EMI IDENTIFICATION (Eliminates "Ghost EMIs")
        // =========================================================================
        // Checks specifically for loan / financing indicators in transaction text
        val loanKeywords = listOf("nach", "ach", "ecs", "loan", "emi", "bajaj", "finance", "capital", "cholamandalam", "hdb", "idfc", "muthoot")
        val verifiedLoanTxns = transactions.filter { txn ->
            txn.isExpense && loanKeywords.any { kw ->
                txn.merchantName.lowercase().contains(kw) || txn.title.lowercase().contains(kw)
            }
        }

        // Recurring exact amounts with loan keywords
        val estimatedMonthlyEMI = verifiedLoanTxns.groupBy { it.amount }
            .filter { (_, txns) -> txns.size >= 2 }
            .keys.sum()

        val debtToIncomeRatio = if (avgMonthlyIncome > 0) estimatedMonthlyEMI / avgMonthlyIncome else 0.0
        val debtScore = when {
            debtToIncomeRatio == 0.0 -> 95  // Clean slate, minimal existing debt
            debtToIncomeRatio <= 0.25 -> 85 // Safe leverage
            debtToIncomeRatio <= 0.40 -> 65 // Moderate leverage
            else -> 40                      // Over-leveraged
        }

        // =========================================================================
        // 4. INCOME CONSISTENCY (Volatility)
        // =========================================================================
        val incomeConsistencyScore: Int = if (activeMonths.size < 2) {
            if (totalIncome > 0) 75 else 30
        } else {
            val incomes = activeMonths.map { it.income }
            val mean = incomes.average()
            if (mean <= 0.0) 30 else {
                val variance = incomes.sumOf { (it - mean) * (it - mean) } / incomes.size
                val stdDev = sqrt(variance)
                // CV (Coefficient of Variation): stdDev / mean. CV < 0.35 is very stable.
                val cv = stdDev / mean
                when {
                    cv <= 0.25 -> 95
                    cv <= 0.45 -> 80
                    cv <= 0.70 -> 65
                    else -> 45
                }
            }
        }

        // =========================================================================
        // 5. TRANSACTION ACTIVITY & DIVERSITY
        // =========================================================================
        val txnsPerMonth = (transactions.size.toDouble() / spanMonths).roundToInt()
        val payerDiversity = transactions.filter { !it.isExpense }.distinctBy { it.merchantName }.size
        val diversityScore = when {
            payerDiversity >= 12 -> 100
            payerDiversity >= 6  -> 85
            payerDiversity >= 3  -> 70
            payerDiversity >= 1  -> 55
            else                 -> 35
        }

        // =========================================================================
        // 6. PENALTY DEDUCTIONS (Bounces & Return Charges)
        // =========================================================================
        val bounceCount = transactions.count { it.category == "Penalties & Fees" }
        val penaltyDeduction = (bounceCount * 12).coerceAtMost(36)

        // =========================================================================
        // FINAL SCORE ASSEMBLY
        // =========================================================================
        val weightedTotal = (
                turnoverScore * W_TURNOVER_VOLUME +
                        cashflowScore * W_CASHFLOW_HEALTH +
                        incomeConsistencyScore * W_INCOME_CONSISTENCY +
                        diversityScore * W_PAYER_DIVERSITY +
                        debtScore * W_DEBT_DISCIPLINE
                )

        val finalScore = (weightedTotal - penaltyDeduction).roundToInt().coerceIn(30, 95)

        val tier = when {
            finalScore >= 75 -> "Gold"
            finalScore >= 55 -> "Silver"
            else -> "Building"
        }

        // =========================================================================
        // REALISTIC SAFE LOAN LIMIT (Banking Turnover Standard)
        // =========================================================================
        // Baseline: 2.0x Monthly Inflow.
        // Adjusted down if existing EMIs are high, adjusted up for clean records.
        val baseCapacity = avgMonthlyIncome * 2.0
        val remainingCapacity = max(0.0, baseCapacity - (estimatedMonthlyEMI * 12.0))
        val safeLoanLimit = max(15000.0, (remainingCapacity * (finalScore / 100.0))).roundToInt().toDouble()

        val finalVitals = Vitals(
            incomeConsistency = incomeConsistencyScore,
            transactionFrequency = txnsPerMonth,
            inflowOutflowRatio = inflowOutflowRatio,
            longevityMonths = spanMonths,
            payerDiversity = payerDiversity,
            debtToIncomeRatio = debtToIncomeRatio,
            bounceCount = bounceCount,
            estimatedEMI = estimatedMonthlyEMI
        )

        return TrustPortfolio(
            score = finalScore,
            tier = tier,
            safeLoanLimit = safeLoanLimit,
            vitals = finalVitals,
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