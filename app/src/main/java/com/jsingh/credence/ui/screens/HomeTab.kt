package com.jsingh.credence.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsingh.credence.domain.models.TrustPortfolio

@Composable
fun HomeTab(portfolio: TrustPortfolio?, onUploadClick: () -> Unit, onNavigateToLoans: () -> Unit) {
    val listings = remember(portfolio) { if (portfolio != null) LoanCatalog.build(portfolio) else emptyList() }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Overview", color = SilverAccent, fontSize = 14.sp, letterSpacing = 1.sp)
            Text("Trust Dashboard", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }

        item {
            if (portfolio == null) UploadPromptCard(onUploadClick) else TrustScoreGaugeCard(portfolio)
        }

        if (portfolio != null) {
            item { TierRoadmap(portfolio.tier) }
            item { ScoreExplainerCard(portfolio) }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Eligible right now", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = onNavigateToLoans) { Text("See all", color = PrimaryGold, fontSize = 13.sp) }
                }
                Spacer(modifier = Modifier.height(12.dp))
                val topScheme = listings.firstOrNull { it.category == ListingCategory.SCHEME }
                val topLender = listings.firstOrNull { it.category == ListingCategory.LENDER }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    topScheme?.let { LoanOfferCard(it.title, formatInr(it.amount), it.rateLabel, it.badge, true, "View scheme details", onNavigateToLoans) }
                    topLender?.let { LoanOfferCard(it.title, formatInr(it.amount), it.rateLabel, it.badge, false, "View lender offers", onNavigateToLoans) }
                }
            }
            item { StatementSummaryCard(portfolio) }
        }
        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

@Composable
fun TrustScoreGaugeCard(portfolio: TrustPortfolio) {
    val isGold = portfolio.tier == "Gold"
    val ringColor = if (isGold) PrimaryGold else Color.White
    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animationPlayed = true }
    val animatedFraction by animateFloatAsState(if (animationPlayed) (portfolio.score.toFloat() / 900f).coerceIn(0f, 1f) else 0f, tween(1400, easing = FastOutSlowInEasing), label = "")

    Box(modifier = Modifier.fillMaxWidth().shadow(20.dp, RoundedCornerShape(24.dp), spotColor = ringColor.copy(alpha = 0.45f)).clip(RoundedCornerShape(24.dp)).background(if (isGold) GoldGradient else SilverGradient).padding(1.dp)) {
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp)).background(CardDark).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("CREDENCE TRUST SCORE", color = SilverAccent, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ringColor.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp)) { Text("${portfolio.tier} tier", color = ringColor, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 14.dp.toPx()
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(Color(0xFF27272A), -215f, 250f, false, style = Stroke(stroke, cap = StrokeCap.Round), size = arcSize, topLeft = Offset(stroke / 2, stroke / 2))
                    drawArc(ringColor, -215f, 250f * animatedFraction, false, style = Stroke(stroke, cap = StrokeCap.Round), size = arcSize, topLeft = Offset(stroke / 2, stroke / 2))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${portfolio.score}", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("out of 900", color = SilverAccent, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFF27272A))
            Spacer(modifier = Modifier.height(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("BORROWING POWER", color = SilverAccent, fontSize = 10.sp, letterSpacing = 1.sp)
                Text(formatInr(portfolio.safeLoanLimit), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TierRoadmap(currentTier: String) {
    val currentIndex = TIER_ORDER.indexOf(currentTier).coerceAtLeast(0)
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardDark).border(1.dp, Color(0xFF27272A), RoundedCornerShape(16.dp)).padding(16.dp)) {
        Column {
            Text("Tier Roadmap", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TIER_ORDER.forEachIndexed { index, tier ->
                    val reached = index <= currentIndex
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(if (reached) PrimaryGold else Color(0xFF27272A)).border(1.dp, if (index == currentIndex) Color.White else Color.Transparent, CircleShape), contentAlignment = Alignment.Center) {
                            if (reached) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BgBlack, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(tier, color = if (reached) Color.White else SilverAccent, fontSize = 11.sp, fontWeight = if (index == currentIndex) FontWeight.Bold else FontWeight.Normal)
                    }
                    if (index != TIER_ORDER.lastIndex) Box(modifier = Modifier.weight(0.6f).height(2.dp).background(if (index < currentIndex) PrimaryGold else Color(0xFF27272A)))
                }
            }
        }
    }
}

@Composable
fun ScoreExplainerCard(portfolio: TrustPortfolio) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(PrimaryGold.copy(alpha = 0.1f)).border(1.dp, PrimaryGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryGold)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Why your score looks like this", color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Your income consistency is ${portfolio.vitals.incomeConsistency}%, calculated from ${portfolio.vitals.transactionFrequency} transactions/mo across ${portfolio.vitals.payerDiversity} distinct payers.", color = Color.White, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
fun StatementSummaryCard(portfolio: TrustPortfolio) {
    val total = portfolio.recentTransactions.size
    val expenseCount = portfolio.recentTransactions.count { it.isExpense }
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardDark).border(1.dp, Color(0xFF27272A), RoundedCornerShape(16.dp)).padding(16.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = SilverAccent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Parsed on-device — nothing leaves your phone", color = SilverAccent, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text("$total", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text("Transactions", color = SilverAccent, fontSize = 11.sp) }
                Column { Text("${total - expenseCount}", color = SuccessGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text("Income", color = SilverAccent, fontSize = 11.sp) }
                Column { Text("$expenseCount", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text("Expenses", color = SilverAccent, fontSize = 11.sp) }
            }
        }
    }
}