package com.jsingh.credence.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
 * Was referenced from the old MainApp.kt's HomeTab() ("Verification Engine" section)
 * but never actually defined anywhere in the codebase — a dangling reference in the
 * original file, not something this reorg broke. Built here for real, using the
 * Vitals fields already on TrustPortfolio so the "proof" section shows the actual
 * factors behind the score, not a placeholder.
 */
@Composable
fun UnifiedInsightsCard(portfolio: TrustPortfolio) {
    val v = portfolio.vitals

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            InsightRow(
                label = "Income Consistency",
                value = "${v.incomeConsistency}/100",
                progress = v.incomeConsistency / 100f
            )
            Spacer(modifier = Modifier.height(14.dp))
            InsightRow(
                label = "Transaction Frequency",
                value = "${v.transactionFrequency}/100",
                progress = v.transactionFrequency / 100f
            )
            Spacer(modifier = Modifier.height(14.dp))
            InsightRow(
                label = "Payer Diversity",
                value = "${v.payerDiversity}/100",
                progress = v.payerDiversity / 100f
            )
            Spacer(modifier = Modifier.height(14.dp))
            InsightRow(
                label = "Longevity",
                value = "${v.longevityMonths} months",
                progress = (v.longevityMonths / 24f).coerceIn(0f, 1f) // 24 months treated as a strong baseline
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Inflow / Outflow Ratio", color = SilverAccent, fontSize = 13.sp)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = String.format("%.2fx", v.inflowOutflowRatio),
                    color = if (v.inflowOutflowRatio >= 1.0) SuccessGreen else Color(0xFFEF4444),
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