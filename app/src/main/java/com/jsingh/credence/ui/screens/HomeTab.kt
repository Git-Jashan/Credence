package com.jsingh.credence.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsingh.credence.domain.models.TrustPortfolio
import com.jsingh.credence.ui.components.TrustScoreGaugeCard

@Composable
fun HomeTab(
    portfolio: TrustPortfolio?,
    userName: String,
    onUploadClick: () -> Unit,
    onNavigateToLoans: () -> Unit
) {
    val listings = remember(portfolio) { if (portfolio != null) LoanCatalog.build(portfolio) else emptyList() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ==========================================
        // 1. PERSONALIZED HEADER
        // ==========================================
        item {
            val firstName = userName.split(" ").firstOrNull() ?: "there"
           Text("Trust Dashboard", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        // ==========================================
        // 2. THE BIG NUMBER & COACHING
        // ==========================================
        item {
            if (portfolio == null) {
                UploadPromptCard(onUploadClick)
            } else {
                Column {
                    TrustScoreGaugeCard(portfolio)

                    Spacer(modifier = Modifier.height(16.dp))

                    ScoreCoachingStrip(portfolio.tier)

                    Spacer(modifier = Modifier.height(18.dp))

                    QuickActionsRow(onNavigateToLoans, onUploadClick)
                }
            }
        }

        if (portfolio != null) {

            // ==========================================
            // 3. ✨ PRO-FINTECH PORTFOLIO SNAPSHOT
            // ==========================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Portfolio Snapshot", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("View All", color = SilverAccent, fontSize = 13.sp, modifier = Modifier.clickable { onNavigateToLoans() })
                }
                Spacer(modifier = Modifier.height(12.dp))
                PortfolioSnapshotCard(onClick = onNavigateToLoans)
            }

            // ==========================================
            // 4. NEW LISTING FOR YOU
            // ==========================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("New Listing for You", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("Market", color = PrimaryGold, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { onNavigateToLoans() })
                }
                Spacer(modifier = Modifier.height(16.dp))

                val topOffer = listings.firstOrNull()
                topOffer?.let {
                    LoanOfferCard(
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

            // ==========================================
            // 5. DATA FRESHNESS STRIP
            // ==========================================
            item {
                DataFreshnessStrip(portfolio = portfolio, onUpdateClick = onUploadClick)
            }
        }
        item { Spacer(modifier = Modifier.height(2.dp)) }
    }
}

// ==========================================
// Coaching Strip
// ==========================================
@Composable
fun ScoreCoachingStrip(tier: String) {
    val (icon, tint, message) = when (tier.lowercase()) {
        "prime", "gold" -> Triple(
            Icons.Default.VerifiedUser,
            PrimaryGold,
            "Excellent standing! You qualify for maximum limits and the lowest MFI interest rates."
        )
        "trusted", "silver" -> Triple(
            Icons.Default.ThumbUp,
            SuccessGreen,
            "Good reliability! Maintain steady daily transactions for 2 more months to unlock Prime."
        )
        else -> Triple(
            Icons.Default.Lightbulb,
            InfoBlue,
            "Avoid bank penalties and keep your balance above zero to build lender trust."
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.1f))
            .border(1.dp, tint.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(message, color = Color.White, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

// ==========================================
// 4-Grid Quick Actions
// ==========================================
@Composable
fun QuickActionsRow(onNavigateToLoans: () -> Unit, onUploadClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        QuickActionButton("Market", Icons.Default.Storefront, onNavigateToLoans)
        QuickActionButton("Applications", Icons.Default.Assignment, onNavigateToLoans)
        QuickActionButton("Ongoing", Icons.Default.AccountBalanceWallet, onNavigateToLoans)
        QuickActionButton("Update", Icons.Default.Sync, onUploadClick)
    }
}

@Composable
fun QuickActionButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(CardDark)
                .border(1.dp, Color(0xFF27272A), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = PrimaryGold, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, color = SilverAccent, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

// ==========================================
// ✨ COOKED: Full-Width Stacked Ledger View
// ==========================================
@Composable
fun PortfolioSnapshotCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF27272A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ------------------------------------------
            // ITEM 1: Active Loan & EMI
            // ------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(InfoBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))

                // Detailed Text
                Column(modifier = Modifier.weight(1f)) {
                    Text("PM SVaNidhi Scheme", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("EMI: ₹1,250", color = SilverAccent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF3F3F46)))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Due 05 Sep", color = WarnAmber, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Chevron Arrow
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF3F3F46))
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFF27272A))
            Spacer(modifier = Modifier.height(16.dp))

            // ------------------------------------------
            // ITEM 2: Application Progress
            // ------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(PrimaryGold.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Autorenew, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))

                // Detailed Progress Data
                Column(modifier = Modifier.weight(1f)) {
                    Text("MFI Micro-Loan", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { 0.5f },
                            modifier = Modifier.width(60.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = PrimaryGold,
                            trackColor = Color(0xFF27272A)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifying Docs", color = SilverAccent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // Status Pill
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(PrimaryGold.copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("PENDING", color = PrimaryGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }
        }
    }
}

// ==========================================
// Data Freshness Strip
// ==========================================
@Composable
fun DataFreshnessStrip(portfolio: TrustPortfolio, onUpdateClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardDark, RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF27272A), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = SuccessGreen,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Verified via ${portfolio.bankName}",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "${portfolio.statementMonths} months data • ${portfolio.recentTransactions.size} txns",
                color = SilverAccent,
                fontSize = 12.sp
            )
        }
    }
}