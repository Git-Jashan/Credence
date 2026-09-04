package com.jsingh.credence.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsingh.credence.domain.models.TrustPortfolio
import com.jsingh.credence.ui.theme.PrimaryGold
import com.jsingh.credence.ui.theme.SilverAccent
import com.jsingh.credence.ui.theme.SuccessGreen

/**
 * Visualizes the raw Vitals driving the Trust Score.
 * Upgraded to reflect true Tier-1 signals (Debt, Bounces) and correctly format
 * raw counts (like transaction volume and payer diversity) into accurate progress bars.
 */
@Composable
fun UnifiedInsightsCard(portfolio: TrustPortfolio) {
    val v = portfolio.vitals
    val dangerRed = Color(0xFFEF4444)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // --- TIER 2 & 3 SIGNALS (Health & Activity) ---
            InsightRow(
                label = "Income Consistency",
                value = "${v.incomeConsistency}%",
                progress = v.incomeConsistency / 100f
            )
            Spacer(modifier = Modifier.height(14.dp))
            InsightRow(
                label = "Transaction Volume",
                value = "${v.transactionFrequency} / mo",
                // Engine considers ~30 txns/month as max score
                progress = (v.transactionFrequency / 30f).coerceIn(0f, 1f)
            )
            Spacer(modifier = Modifier.height(14.dp))
            InsightRow(
                label = "Income Sources",
                value = "${v.payerDiversity} unique",
                // Engine considers ~5 distinct payers as max diversity score
                progress = (v.payerDiversity / 5f).coerceIn(0f, 1f)
            )
            Spacer(modifier = Modifier.height(14.dp))
            InsightRow(
                label = "Statement History",
                value = "${v.longevityMonths} months",
                // 24 months treated as a strong baseline for progress max
                progress = (v.longevityMonths / 24f).coerceIn(0f, 1f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // --- TIER 1 SIGNALS (Risk & Debt) ---
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Debt-to-Income (FOIR)", color = SilverAccent, fontSize = 13.sp)
                Spacer(modifier = Modifier.weight(1f))

                val dtiPercent = (v.debtToIncomeRatio * 100).toInt()
                Text(
                    text = if (v.debtToIncomeRatio >= 1.0) ">100%" else "$dtiPercent%",
                    // Anything over 40% DTI is considered risky in traditional lending
                    color = if (v.debtToIncomeRatio > 0.40) dangerRed else SuccessGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Existing EMIs Found", color = SilverAccent, fontSize = 13.sp)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (v.estimatedEMI > 0) "₹${v.estimatedEMI.toInt()} / mo" else "None",
                    color = if (v.estimatedEMI > 0) PrimaryGold else Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Bounces & Penalties", color = SilverAccent, fontSize = 13.sp)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (v.bounceCount > 0) "${v.bounceCount} detected" else "0 (Clean)",
                    color = if (v.bounceCount > 0) dangerRed else SuccessGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun InsightRow(label: String, value: String, progress: Float) {
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(label, color = SilverAccent, fontSize = 13.sp)
            Spacer(modifier = Modifier.weight(1f))
            Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = progress.coerceIn(0f, 1f),
            modifier = Modifier.fillMaxWidth().height(5.dp),
            color = PrimaryGold,
            trackColor = Color(0xFF27272A)
        )
    }
}