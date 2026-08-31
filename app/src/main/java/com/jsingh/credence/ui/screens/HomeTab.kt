package com.jsingh.credence.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsingh.credence.domain.models.TrustPortfolio
import kotlin.math.roundToInt

@Composable
fun HomeTab(
    portfolio: TrustPortfolio?,
    userName: String,
    onUploadClick: () -> Unit,
    onNavigateToLoans: () -> Unit
) {
    val listings = remember(portfolio) { if (portfolio != null) LoanCatalog.build(portfolio) else emptyList() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BgBlack).padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text("Dashboard", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
        }

        item {
            if (portfolio == null) {
                UploadPromptCard(onUploadClick)
            } else {
                val maxLimit = portfolio.safeLoanLimit.toFloat()
                var sliderValue by remember { mutableFloatStateOf(maxLimit * 0.25f) }

                val reqAmt = sliderValue.toDouble()
                val utilization = reqAmt / maxLimit.toDouble()
                val riskPenalty = (utilization * 50.0).roundToInt()
                val dynamicScore = (portfolio.score - riskPenalty).coerceIn(0, 100)

                Column {
                    CreditSimulatorWidget(
                        dynamicScore = dynamicScore,
                        requestedAmount = reqAmt,
                        maxLimit = maxLimit,
                        sliderValue = sliderValue,
                        onSliderChange = { sliderValue = it }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    QuickActionsRow(onNavigateToLoans, onUploadClick)
                }
            }
        }

        if (portfolio != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text("Activity", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("See All", color = PrimaryGold, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { onNavigateToLoans() })
                }
                Spacer(modifier = Modifier.height(16.dp))
                PortfolioSnapshotCard(onClick = onNavigateToLoans)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text("For You", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))

                val topOffer = listings.firstOrNull()
                topOffer?.let {
                    // ✨ RENAMED TO MarketLoanCard!
                    MarketLoanCard(
                        title = it.title,
                        amount = formatInr(it.amount),
                        rate = it.rateLabel,
                        badge = it.badge,
                        isHighlighted = true,
                        ctaLabel = "Review Offer",
                        onApply = onNavigateToLoans
                    )
                }
            }

            item {
                DataFreshnessStrip(portfolio = portfolio, onUpdateClick = onUploadClick)
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditSimulatorWidget(dynamicScore: Int, requestedAmount: Double, maxLimit: Float, sliderValue: Float, onSliderChange: (Float) -> Unit) {
    val progress = (dynamicScore / 100f).coerceIn(0f, 1f)
    val (tierColor, riskText) = when {
        dynamicScore >= 70 -> SuccessGreen to "Low Risk"
        dynamicScore >= 45 -> WarnAmber to "Moderate Risk"
        else -> DangerRed to "High Risk"
    }

    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing), label = "gauge")

    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(CardDark).padding(24.dp)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("LOAN ANALYSER", color = SilverAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(formatInr(requestedAmount), color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.5).sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(tierColor))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(riskText, color = tierColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(76.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 6.dp.toPx()
                        val arcSize = Size(size.width - stroke, size.height - stroke)
                        val topLeft = Offset(stroke / 2, stroke / 2)

                        drawArc(color = Color(0xFF27272A), startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(width = stroke, cap = StrokeCap.Round), size = arcSize, topLeft = topLeft)
                        drawArc(color = tierColor, startAngle = -90f, sweepAngle = 360f * animatedProgress, useCenter = false, style = Stroke(width = stroke, cap = StrokeCap.Round), size = arcSize, topLeft = topLeft)
                    }
                    Text(text = "$dynamicScore", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Slider(
                value = sliderValue,
                onValueChange = onSliderChange,
                valueRange = 1000f..maxLimit,
                modifier = Modifier.height(20.dp),
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = PrimaryGold, inactiveTrackColor = Color(0xFF27272A))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("₹1,000", color = SilverAccent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text("Limit: ${formatInr(maxLimit.toDouble())}", color = SilverAccent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun QuickActionsRow(onNavigateToLoans: () -> Unit, onUploadClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        QuickActionButton("Market", Icons.Default.Storefront, onNavigateToLoans)
        QuickActionButton("Tracker", Icons.Default.DataUsage, onNavigateToLoans)
        QuickActionButton("History", Icons.Default.History, onNavigateToLoans)
        QuickActionButton("Update", Icons.Default.Sync, onUploadClick)
    }
}

@Composable
fun QuickActionButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(CardDark), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = PrimaryGold, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = SilverAccent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PortfolioSnapshotCard(onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(CardDark).clickable { onClick() }.padding(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(InfoBlue.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Payments, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("PM SVaNidhi", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Next EMI: ₹1,250 • Due 05 Sep", color = SilverAccent, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color(0xFF27272A))
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(PrimaryGold.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Autorenew, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("MFI Micro-Loan", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Status: Verifying Documents", color = WarnAmber, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun DataFreshnessStrip(portfolio: TrustPortfolio, onUpdateClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(100.dp)).background(CardDark).clickable { onUpdateClick() }.padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text("Verified via ${portfolio.bankName} • ${portfolio.statementMonths} Mos", color = SilverAccent, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SilverAccent, modifier = Modifier.size(18.dp))
    }
}