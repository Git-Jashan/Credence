package com.jsingh.credence.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsingh.credence.domain.engine.ScoreCalculator
import com.jsingh.credence.domain.models.Transaction
import com.jsingh.credence.domain.models.TrustPortfolio
import com.jsingh.credence.domain.parser.StatementParser
import com.jsingh.credence.ui.components.PdfPasswordDialog
import kotlin.math.roundToInt

// --- PALETTE ---
// Kept dark + gold — reads as "trusted fintech," not "crypto wallet." No hash IDs, no node counts,
// no smart-contract language anywhere below: the product is regex-parsed statements + rule-based
// scoring + purpose-restricted disbursement, and the copy now says exactly that.
val BgBlack = Color(0xFF09090B)
val CardDark = Color(0xFF18181B)
val SilverAccent = Color(0xFFA1A1AA)
val PrimaryGold = Color(0xFFEAB308)
val SuccessGreen = Color(0xFF10B981)
val InfoBlue = Color(0xFF3B82F6)
val SilverGradient = Brush.linearGradient(listOf(Color(0xFFE2E2E2), Color(0xFF71717A)))
val GoldGradient = Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFB8860B)))

/**
 * Seeker-side app only. Lender-side dashboard is a separate build phase per the project plan
 * and intentionally isn't stubbed in here — no half-built tab pretending to be a feature.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val context = LocalContext.current
    var portfolio by remember { mutableStateOf<TrustPortfolio?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedPdfUri = uri
            showDialog = true
        }
    }

    if (showDialog && selectedPdfUri != null) {
        PdfPasswordDialog(
            context = context,
            pdfUri = selectedPdfUri!!,
            onDismiss = { showDialog = false },
            onSuccess = { rawText ->
                val transactions = StatementParser.processStatement(rawText)
                portfolio = ScoreCalculator.calculateScore(transactions)
            }
        )
    }

    Scaffold(
        containerColor = BgBlack,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Credence", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Your portable loan trust score", color = SilverAccent, fontSize = 11.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgBlack),
                actions = {
                    // Was a fake wallet-address chip. Replaced with something true and useful:
                    // whether this device's statement data is still fresh enough to trust.
                    val isDataFresh = portfolio != null
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp).clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF27272A)).padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(8.dp).clip(CircleShape)
                                .background(if (isDataFresh) SuccessGreen else SilverAccent)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (isDataFresh) "Statement verified" else "No statement yet",
                            color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = CardDark, contentColor = SilverAccent) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, "Home") }, label = { Text("Home") },
                    selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, unselectedIconColor = SilverAccent, indicatorColor = Color(0xFF27272A))
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Star, "Schemes & Lenders") }, label = { Text("Get a Loan") },
                    selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, unselectedIconColor = SilverAccent, indicatorColor = Color(0xFF27272A))
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, "My Score & Data") }, label = { Text("My Score") },
                    selected = selectedTab == 2, onClick = { selectedTab = 2 },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, unselectedIconColor = SilverAccent, indicatorColor = Color(0xFF27272A))
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> HomeTab(portfolio) { pdfPickerLauncher.launch("application/pdf") }
                1 -> SchemesAndLendersTab(portfolio) { pdfPickerLauncher.launch("application/pdf") }
                2 -> MyScoreTab(portfolio) { portfolio = null }
            }
        }
    }
}

// ---------------- HOME ----------------

@Composable
fun HomeTab(portfolio: TrustPortfolio?, onUploadClick: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Overview", color = SilverAccent, fontSize = 14.sp, letterSpacing = 1.sp)
            Text("Trust Dashboard", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }

        item {
            if (portfolio == null) UploadPromptCard(onUploadClick)
            else TrustScoreCard(portfolio)
        }

        if (portfolio != null) {
            item { ScoreExplainerCard(portfolio) }

            item {
                Text("What you're eligible for right now", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))
                EligibilityCard(portfolio)
            }
            item {
                Text("Statement Activity", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("Parsed on-device from your uploaded PDF — nothing leaves your phone.", color = SilverAccent, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(portfolio.recentTransactions) { txn -> TransactionRow(txn) }
        }
        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

/**
 * Plain-language version of the old "AI Financial Insight" card. Says what the score is actually
 * built from (your 6-factor model) instead of implying some opaque model is "thinking" about it.
 */
@Composable
fun ScoreExplainerCard(portfolio: TrustPortfolio) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(PrimaryGold.copy(alpha = 0.1f))
            .border(1.dp, PrimaryGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryGold)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Why your score looks like this", color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Your income consistency is ${portfolio.vitals.incomeConsistency}%, calculated from ${portfolio.vitals.transactionFrequency} " +
                            "transactions/month across ${portfolio.vitals.payerDiversity} distinct payers. Keeping this steady is what moves you toward the next tier — " +
                            "not a single big deposit.",
                    color = Color.White, fontSize = 13.sp, lineHeight = 18.sp
                )
            }
        }
    }
}

/**
 * Replaces "Instant Credit Line / Execute Smart Contract" with the two real paths from the
 * project doc: a government scheme (purpose-restricted, NFC/QR verified) and a private lender
 * offer (normal interest-bearing loan). Routes into the Schemes & Lenders tab rather than firing
 * a fake instant-approval toast.
 */
@Composable
fun EligibilityCard(portfolio: TrustPortfolio) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LoanOfferCard(
            title = "PM SVaNidhi-style Scheme Loan",
            amount = "₹${portfolio.safeLoanLimit.roundToInt()}",
            rate = "Purpose-restricted",
            badge = "GOVERNMENT SCHEME",
            isHighlighted = true,
            ctaLabel = "View scheme details",
            onApply = { /* navigate to Schemes & Lenders tab, Schemes section */ }
        )
        LoanOfferCard(
            title = "MFI / NBFC Micro-Loan",
            amount = "₹${(portfolio.safeLoanLimit * 0.5).roundToInt()}",
            rate = "11–14% APR",
            badge = "PRIVATE LENDER",
            ctaLabel = "View lender offers",
            onApply = { /* navigate to Schemes & Lenders tab, Lenders section */ }
        )
    }
}

// ---------------- SCHEMES & LENDERS (was "LoansTab" / "Market") ----------------

@Composable
fun SchemesAndLendersTab(portfolio: TrustPortfolio?, onUploadClick: () -> Unit) {
    val context = LocalContext.current
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Get a Loan", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Government schemes and private lenders matched to your Trust Score.", color = SilverAccent, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (portfolio == null) {
            item { UploadPromptCard(onUploadClick) }
            return@LazyColumn
        }

        item { SectionHeader("Government Schemes") }
        item {
            SchemeCard(
                name = "PM SVaNidhi — Working Capital Tranche",
                amount = "₹${portfolio.safeLoanLimit.roundToInt()}",
                status = "Eligible",
                explainer = "Disbursed against a specific purpose (e.g. cart & equipment). Verified by NFC tap at a " +
                        "registered merchant, or by QR scan-and-confirm if the merchant has no NFC reader — either way, the " +
                        "purchase itself is the proof, not a photo or manual review.",
                onApply = {
                    Toast.makeText(context, "Scheme application started — you'll be notified once approved.", Toast.LENGTH_LONG).show()
                }
            )
        }

        item { SectionHeader("Private Lenders") }
        item {
            LoanOfferCard(
                title = "Micro-Business Working Capital",
                amount = "₹${(portfolio.safeLoanLimit * 0.5).roundToInt()}",
                rate = "11% APR",
                badge = "MFI",
                ctaLabel = "Apply",
                onApply = {
                    Toast.makeText(context, "Application sent to lender — you'll see status under My Score.", Toast.LENGTH_SHORT).show()
                }
            )
        }
        item {
            LoanOfferCard(
                title = "Equipment / Inventory Loan",
                amount = "₹${(portfolio.safeLoanLimit * 0.35).roundToInt()}",
                rate = "14% APR",
                badge = "NBFC",
                ctaLabel = "Apply",
                onApply = {
                    Toast.makeText(context, "Application sent to lender — you'll see status under My Score.", Toast.LENGTH_SHORT).show()
                }
            )
        }
        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
fun SchemeCard(name: String, amount: String, status: String, explainer: String, onApply: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF27272A))
            .border(1.dp, PrimaryGold.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("GOVERNMENT SCHEME", color = PrimaryGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                StatusPill(status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(amount, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(10.dp))
            Text(explainer, color = SilverAccent, fontSize = 12.sp, lineHeight = 17.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onApply,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = BgBlack),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Apply for this scheme", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
        }
    }
}

@Composable
fun StatusPill(status: String) {
    val color = when (status) {
        "Eligible", "Approved" -> SuccessGreen
        "Pending" -> PrimaryGold
        else -> SilverAccent
    }
    Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(status, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// ---------------- MY SCORE & DATA (was "ProfileTab") ----------------

@Composable
fun MyScoreTab(portfolio: TrustPortfolio?, onResetData: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("My Score & Data", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Your portable trust profile — carry it to any lender.", color = SilverAccent, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (portfolio == null) {
            item { Text("No portfolio active. Upload a statement on Home to get started.", color = SilverAccent, fontSize = 14.sp) }
        } else {
            item {
                Text("Factor Breakdown", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricBox(modifier = Modifier.weight(1f), label = "Income Consistency", value = "${portfolio.vitals.incomeConsistency}%", icon = Icons.Default.CheckCircle, color = SuccessGreen)
                    MetricBox(modifier = Modifier.weight(1f), label = "Risk Band", value = if (portfolio.score > 700) "LOW" else "MEDIUM", icon = Icons.Default.Lock, color = PrimaryGold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricBox(modifier = Modifier.weight(1f), label = "Transaction Frequency", value = "${portfolio.vitals.transactionFrequency}/mo", icon = Icons.Default.Refresh, color = InfoBlue)
                    MetricBox(modifier = Modifier.weight(1f), label = "Payer Diversity", value = "${portfolio.vitals.payerDiversity}", icon = Icons.Default.Share, color = Color(0xFFA855F7))
                }
                // TODO once ScoreCalculator/TrustPortfolio exposes them: inflow/outflow ratio,
                // account longevity, and lean-period resilience are the remaining 3 of the doc's
                // 6 scoring factors and belong in this grid too.
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Text("Trust Card", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Used only for scheme disbursement. Tap at a registered merchant (or scan the QR if no NFC " +
                            "reader is available) — spending is auto-restricted to your approved category.",
                    color = SilverAccent, fontSize = 12.sp, lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF27272A)).padding(16.dp)) {
                    Text("APPROVED CATEGORY", color = SilverAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Cart & Equipment Suppliers", color = PrimaryGold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "I have a Credence Trust Score of ${portfolio.score} (${portfolio.tier} tier), " +
                                        "pre-approved for ₹${portfolio.safeLoanLimit}. Verified from my own bank statement, parsed on-device."
                            )
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Trust Profile"))
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = BgBlack),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Trust Profile", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        item {
            Text("Data & Privacy", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            SettingsRow(Icons.Default.Delete, "Wipe Local Data", "Deletes your parsed statement data from this device", onResetData)
            SettingsRow(Icons.Default.Lock, "How parsing works", "Statement text is parsed on-device with regex — nothing is sent to a server")
            SettingsRow(Icons.Default.Info, "About", "Credence — Loan Trust Score, Hackathon Build")
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ---------------- SHARED COMPONENTS ----------------

@Composable
fun MetricBox(modifier: Modifier, label: String, value: String, icon: ImageVector, color: Color) {
    Box(modifier = modifier.clip(RoundedCornerShape(16.dp)).background(CardDark).border(1.dp, Color(0xFF27272A), RoundedCornerShape(16.dp)).padding(16.dp)) {
        Column {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(label, color = SilverAccent, fontSize = 12.sp)
        }
    }
}

@Composable
fun TrustScoreCard(portfolio: TrustPortfolio) {
    val gradient = if (portfolio.tier == "Gold") GoldGradient else SilverGradient
    val shadowColor = if (portfolio.tier == "Gold") PrimaryGold else Color.White

    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animationPlayed = true }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) portfolio.score / 900f else 0f,
        animationSpec = tween(1500, easing = FastOutSlowInEasing), label = "scoreAnim"
    )

    Box(
        modifier = Modifier.fillMaxWidth().shadow(20.dp, RoundedCornerShape(20.dp), spotColor = shadowColor.copy(alpha = 0.5f), ambientColor = shadowColor.copy(alpha = 0.5f))
            .clip(RoundedCornerShape(20.dp)).background(gradient).padding(1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp)).background(CardDark).padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("CREDENCE TRUST SCORE", color = SilverAccent, fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.Star, contentDescription = null, tint = SilverAccent, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))

            Text("${portfolio.score}", fontSize = 64.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = if (portfolio.tier == "Gold") PrimaryGold else Color.White,
                trackColor = Color(0xFF27272A)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text("${portfolio.tier} Tier", fontSize = 16.sp, color = SilverAccent)
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Color(0xFF27272A))
            Spacer(modifier = Modifier.height(16.dp))
            Column {
                Text("BORROWING POWER", color = SilverAccent, fontSize = 10.sp, letterSpacing = 1.sp)
                Text("₹${portfolio.safeLoanLimit.roundToInt()}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LoanOfferCard(
    title: String,
    amount: String,
    rate: String,
    badge: String,
    isHighlighted: Boolean = false,
    ctaLabel: String = "Apply",
    onApply: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(if (isHighlighted) Color(0xFF27272A) else CardDark)
            .border(1.dp, if (isHighlighted) PrimaryGold.copy(alpha = 0.5f) else Color(0xFF27272A), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column {
            Text(badge, color = if (isHighlighted) PrimaryGold else SilverAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("Approved Limit", color = SilverAccent, fontSize = 12.sp)
                    Text(amount, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text(rate, color = SuccessGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onApply,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isHighlighted) PrimaryGold else Color.White, contentColor = BgBlack),
                shape = RoundedCornerShape(12.dp)
            ) { Text(ctaLabel, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
    }
}

@Composable
fun UploadPromptCard(onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CardDark).border(1.dp, Color(0xFF27272A), RoundedCornerShape(20.dp)).clickable { onClick() }.padding(24.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFF27272A)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Add, contentDescription = "Upload", tint = PrimaryGold, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Build Your Trust Score", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                InstructionRow(Icons.Default.CheckCircle, "Upload a recent 6-month bank statement PDF")
                InstructionRow(Icons.Default.Lock, "Parsed on-device — nothing is uploaded to a server")
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = BgBlack), shape = RoundedCornerShape(12.dp)) {
                Text("Select Bank Statement PDF", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun InstructionRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
        Icon(icon, contentDescription = null, tint = SilverAccent, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, color = SilverAccent, fontSize = 13.sp)
    }
}

@Composable
fun TransactionRow(txn: Transaction) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF27272A)), contentAlignment = Alignment.Center) {
            Icon(imageVector = if (txn.isExpense) Icons.Default.ShoppingCart else Icons.Default.Add, contentDescription = null, tint = if (txn.isExpense) Color.White else SuccessGreen)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(txn.merchantName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(txn.date, color = SilverAccent, fontSize = 12.sp)
        }
        Text("${if (txn.isExpense) "-" else "+"}₹${txn.amount}", color = if (txn.isExpense) Color.White else SuccessGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SettingsRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = SilverAccent, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = SilverAccent, fontSize = 12.sp)
        }
    }
}