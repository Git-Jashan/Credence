package com.jsingh.credence.ui.screens

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
import com.jsingh.credence.domain.models.ActiveLoan
import com.jsingh.credence.domain.models.LoanStatus
import com.jsingh.credence.domain.models.TrustPortfolio
import com.jsingh.credence.ui.components.TrustScoreGaugeCard
import com.jsingh.credence.ui.components.UnifiedInsightsCard
import com.jsingh.credence.ui.theme.PrimaryGold
import com.jsingh.credence.ui.theme.SilverAccent
import com.jsingh.credence.ui.theme.SuccessGreen

@Composable
fun HomeTab(
    portfolio: TrustPortfolio?,
    userName: String,
    onUploadClick: () -> Unit,
    onNavigateToLoans: () -> Unit,
    onNavigateToActiveLoan: (String) -> Unit = {}
) {
    val listings = remember(portfolio) { if (portfolio != null) LoanCatalog.build(portfolio) else emptyList() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. PERSONALIZED HEADER
        item {
            Spacer(modifier = Modifier.height(8.dp))
            val firstName = userName.split(" ").firstOrNull() ?: "there"
            Text("Welcome back, $firstName", color = SilverAccent, fontSize = 14.sp, letterSpacing = 1.sp)
            Text("Trust Dashboard", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }

        // 2. THE BIG NUMBER & COACHING
        item {
            if (portfolio == null) {
                UploadPromptCard(onUploadClick)
            } else {
                Column {
                    TrustScoreGaugeCard(portfolio)

                    portfolio.previousScore?.let { previous ->
                        Spacer(modifier = Modifier.height(10.dp))
                        ScoreMovementBadge(current = portfolio.score, previous = previous)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ✨ UPGRADED: Authentic Coaching Strip
                    ScoreCoachingStrip(portfolio.tier)

                    Spacer(modifier = Modifier.height(24.dp))

                    // ✨ NEW: Quick Actions
                    QuickActionsRow(onNavigateToLoans, onUploadClick)
                }
            }
        }

        if (portfolio != null) {

            // 3. WHAT'S DUE
            val nextDue = portfolio.activeLoans
                .filter { it.status == LoanStatus.ACTIVE }
                .minByOrNull { it.nextEmiDate }

            if (nextDue != null) {
                item {
                    Text("Your Active Loan", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    ActiveLoanStatusCard(loan = nextDue, onClick = { onNavigateToActiveLoan(nextDue.id) })
                }
            } else {
                // 4. THE INSTANT ACTION
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Top Pre-Approved Match", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = onNavigateToLoans) {
                            Text("View Market", color = PrimaryGold, fontSize = 13.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    val topOffer = listings.firstOrNull()
                    topOffer?.let {
                        LoanOfferCard(
                            title = it.title,
                            amount = formatInr(it.amount),
                            rate = it.rateLabel,
                            badge = it.badge,
                            isHighlighted = true,
                            ctaLabel = "Claim Funds",
                            onApply = onNavigateToLoans
                        )
                    }
                }
            }

            // 5. THE PROOF
            item {
                Text("Verification Engine", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))
                UnifiedInsightsCard(portfolio)
            }

            // 6. UPGRADED DATA FRESHNESS
            item {
                DataFreshnessStrip(portfolio = portfolio, onUpdateClick = onUploadClick)
            }
        }
        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

// ==========================================
// ✨ UPGRADED: Authentic Coaching Strip
// ==========================================
@Composable
fun ScoreCoachingStrip(tier: String) {
    val (icon, tint, message) = when (tier) {
        "Prime" -> Triple(
            Icons.Default.VerifiedUser,
            PrimaryGold,
            "Excellent standing! You qualify for maximum limits and the lowest MFI interest rates."
        )
        "Trusted" -> Triple(
            Icons.Default.ThumbUp,
            SuccessGreen,
            "Good reliability! Maintain steady daily transactions for 2 more months to unlock Prime."
        )
        else -> Triple(
            Icons.Default.Lightbulb,
            Color(0xFF3B82F6),
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
        Text(message, color = Color(0xFFE4E4E7), fontSize = 13.sp, lineHeight = 18.sp)
    }
}

// ==========================================
// Quick Actions Row
// ==========================================
@Composable
fun QuickActionsRow(onNavigateToLoans: () -> Unit, onUploadClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickActionButton("Schemes", Icons.Default.AccountBalance, onNavigateToLoans)
        QuickActionButton("Lenders", Icons.Default.Storefront, onNavigateToLoans)
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
                .background(Color(0xFF18181B))
                .border(1.dp, Color(0xFF27272A), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = PrimaryGold, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = SilverAccent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ==========================================
// Score movement
// ==========================================
@Composable
fun ScoreMovementBadge(current: Int, previous: Int) {
    val delta = current - previous
    if (delta == 0) return

    val isUp = delta > 0
    val color = if (isUp) SuccessGreen else Color(0xFFEF4444)
    val icon = if (isUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward

    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            "${if (isUp) "+" else ""}$delta this week",
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ==========================================
// Active Loan status
// ==========================================
@Composable
fun ActiveLoanStatusCard(loan: ActiveLoan, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(loan.lenderName, color = SilverAccent, fontSize = 13.sp)
                    Text(
                        "Next EMI: ${formatInr(loan.nextEmiAmount)}",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Due", color = SilverAccent, fontSize = 12.sp)
                    Text(loan.nextEmiDate, color = PrimaryGold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { loan.progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = PrimaryGold,
                trackColor = Color(0xFF27272A),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "${loan.monthsPaid} of ${loan.tenureMonths} months paid — on-time payments raise your score",
                color = SilverAccent,
                fontSize = 12.sp
            )
        }
    }
}

// ==========================================
// Upgraded Data freshness strip
// ==========================================
@Composable
fun DataFreshnessStrip(portfolio: TrustPortfolio, onUpdateClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF18181B), RoundedCornerShape(16.dp))
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = SuccessGreen,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Verified via ${portfolio.bankName}",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "${portfolio.statementMonths} months data • ${portfolio.recentTransactions.size} txns",
                color = SilverAccent,
                fontSize = 12.sp
            )
        }
        TextButton(onClick = onUpdateClick) {
            Text("Update", color = PrimaryGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}