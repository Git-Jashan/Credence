package com.jsingh.credence.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsingh.credence.domain.models.TrustPortfolio
import com.jsingh.credence.ui.theme.PrimaryGold
import com.jsingh.credence.ui.theme.SilverAccent
import com.jsingh.credence.ui.theme.SuccessGreen
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TrustScoreGaugeCard(portfolio: TrustPortfolio) {
    // 1. Core Data Setup
    val progress = (portfolio.score / 100f).coerceIn(0f, 1f)

    // ✨ FIX: Updated to map to the new Trust Level language from ScoreCalculator
    val tierColor = when (portfolio.tier.lowercase()) {
        "prime" -> PrimaryGold // Prime replaces Gold
        "trusted" -> SuccessGreen // Trusted replaces Silver
        else -> Color(0xFF3B82F6) // Building gets a calm InfoBlue
    }

    val formattedLimit = NumberFormat.getNumberInstance(Locale("en", "IN")).format(portfolio.safeLoanLimit.toInt())

    // 2. Fluid Animation
    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animationPlayed = true }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) progress else 0f,
        animationSpec = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
        label = "gaugeAnimation"
    )

    // 3. Metallic/Glassmorphism Background Brush
    val cardBackgroundBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF18181B), // Deep Base
            Color(0xFF18181B),
            tierColor.copy(alpha = 0.12f) // Light tier reflection on the top-right edge
        ),
        start = Offset(0f, Float.POSITIVE_INFINITY),
        end = Offset(Float.POSITIVE_INFINITY, 0f)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(24.dp, RoundedCornerShape(20.dp), spotColor = tierColor.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF27272A).copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBackgroundBrush)
                .padding(horizontal = 24.dp, vertical = 24.dp), // Perfected Breathing Room
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            // ==========================================
            // LEFT: The Advanced Trigonometric Gauge
            // ==========================================
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(110.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 8.dp.toPx()
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    val topLeft = Offset(stroke / 2, stroke / 2)
                    val sweep = 270f
                    val startAngle = -225f

                    // Dotted/Dashed Digital Background Track
                    drawArc(
                        color = Color(0xFF27272A),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(
                            width = stroke,
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()), 0f)
                        ),
                        size = arcSize,
                        topLeft = topLeft
                    )

                    // The Solid Foreground Gradient Progress
                    drawArc(
                        brush = Brush.linearGradient(
                            colors = listOf(tierColor.copy(alpha = 0.3f), tierColor),
                            start = Offset(0f, size.height),
                            end = Offset(size.width, 0f)
                        ),
                        startAngle = startAngle,
                        sweepAngle = sweep * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                        size = arcSize,
                        topLeft = topLeft
                    )

                    // Glowing Orbital Tip using Trigonometry
                    if (animatedProgress > 0f) {
                        val currentAngle = startAngle + (sweep * animatedProgress)
                        val angleInRadians = (currentAngle * Math.PI / 180.0).toFloat()
                        val radius = arcSize.width / 2

                        val dotX = center.x + radius * cos(angleInRadians)
                        val dotY = center.y + radius * sin(angleInRadians)

                        // Outer Glow
                        drawCircle(
                            color = tierColor.copy(alpha = 0.4f),
                            radius = 6.dp.toPx(),
                            center = Offset(dotX, dotY)
                        )
                        // Inner Core
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = Offset(dotX, dotY)
                        )
                    }
                }

                // Centered Score Text
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = 4.dp)) {
                    Text(
                        text = "${portfolio.score}",
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "/ 100",
                        color = SilverAccent.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ==========================================
            // RIGHT: Data & Mini-Graph Stack
            // ==========================================
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 24.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Tier Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(tierColor.copy(alpha = 0.15f))
                        .border(1.dp, tierColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = null, tint = tierColor, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))

                    // ✨ FIX: Removed the word "Tier", updated formatting
                    Text(
                        text = "${portfolio.tier} STATUS".uppercase(),
                        color = tierColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Borrowing Power Label
                Text(
                    text = "PRE-APPROVED LIMIT",
                    color = SilverAccent,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Formatted Currency
                Text(
                    text = "₹$formattedLimit",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(8.dp))

                // The "Data Depth" Mini-Graph
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("DATA CONFIDENCE", color = SilverAccent.copy(alpha = 0.6f), fontSize = 8.sp, letterSpacing = 0.5.sp, modifier = Modifier.padding(bottom = 1.dp, end = 4.dp))

                    // Bar 1 (Small)
                    Box(modifier = Modifier.size(width = 4.dp, height = 6.dp).clip(RoundedCornerShape(2.dp)).background(tierColor.copy(alpha = 0.4f)))
                    // Bar 2 (Medium)
                    Box(modifier = Modifier.size(width = 4.dp, height = 10.dp).clip(RoundedCornerShape(2.dp)).background(tierColor.copy(alpha = 0.7f)))
                    // Bar 3 (Tall/Glowing)
                    Box(modifier = Modifier.size(width = 4.dp, height = 14.dp).clip(RoundedCornerShape(2.dp)).background(tierColor).shadow(4.dp, spotColor = tierColor))
                }
            }
        }
    }
}