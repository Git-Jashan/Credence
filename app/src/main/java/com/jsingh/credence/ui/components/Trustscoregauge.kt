package com.jsingh.credence.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsingh.credence.domain.models.TrustPortfolio
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

// Local colors to ensure no import conflicts
private val PrimaryGold = Color(0xFFEAB308)
private val BrightSilver = Color(0xFFE4E4E7)
private val SuccessGreen = Color(0xFF10B981)
private val WarnAmber = Color(0xFFF59E0B)
private val DangerRed = Color(0xFFEF4444)
private val InfoBlue = Color(0xFF3B82F6)
private val SilverAccent = Color(0xFFA1A1AA)
private val CardDark = Color(0xFF18181B)

@Composable
fun TrustScoreGaugeCard(portfolio: TrustPortfolio) {
    // 1. Core Data Setup
    val progress = (portfolio.score / 100f).coerceIn(0f, 1f)

    // Tier mapping for the main gauge colors
    val tierColor = when (portfolio.tier.lowercase()) {
        "prime", "gold" -> PrimaryGold
        "trusted" -> SuccessGreen
        "silver" -> BrightSilver
        else -> InfoBlue
    }

    // Dynamic confidence logic based on score
    val (confidenceText, confidenceColor) = when {
        portfolio.score >= 80 -> "High" to SuccessGreen
        portfolio.score >= 40 -> "Medium" to WarnAmber
        else -> "Low" to DangerRed
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

    // 3. Premium Glassmorphism Background
    val cardBackgroundBrush = Brush.linearGradient(
        colors = listOf(
            CardDark,
            CardDark,
            tierColor.copy(alpha = 0.08f)
        ),
        start = Offset(0f, Float.POSITIVE_INFINITY),
        end = Offset(Float.POSITIVE_INFINITY, 0f)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = tierColor.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBackgroundBrush)
                .border(1.dp, Color(0xFF27272A), RoundedCornerShape(20.dp))
               // This perfectly counters the open "gap" at the bottom of the gauge!
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            // ==========================================
            // LEFT: The Smooth Trigonometric Gauge
            // ==========================================
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(110.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 8.dp.toPx()
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    val topLeft = Offset(stroke / 2, stroke / 2)
                    val sweep = 270f
                    val startAngle = -225f

                    // Smooth, solid background track
                    drawArc(
                        color = Color(0xFF27272A).copy(alpha = 0.5f),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                        size = arcSize,
                        topLeft = topLeft
                    )

                    // The Solid Foreground Gradient Progress
                    drawArc(
                        brush = Brush.linearGradient(
                            colors = listOf(tierColor.copy(alpha = 0.2f), tierColor),
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

                        drawCircle(
                            color = tierColor.copy(alpha = 0.4f),
                            radius = 6.dp.toPx(),
                            center = Offset(dotX, dotY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = Offset(dotX, dotY)
                        )
                    }
                }

                // Centered Score Text
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = (-4).dp)) {
                    Text(
                        text = "${portfolio.score}",
                        color = Color.White,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "OUT OF 100",
                        color = SilverAccent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // ==========================================
            // RIGHT: Perfectly Vertically Centered Content
            // ==========================================
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 24.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                // TOP: Borrowing Power Limit
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "SAFE LIMIT",
                        color = SilverAccent,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "₹$formattedLimit",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // BOTTOM: Dynamic Confidence Pill Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Confidence:",
                        color = SilverAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(2.dp))

                    // ✨ FIX 2: Added a tinted pill background explicitly around the confidence status
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(tierColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = confidenceText.uppercase(),
                            color = tierColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}