package com.jsingh.credence.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsingh.credence.domain.engine.ScoreCalculator
import com.jsingh.credence.domain.models.TrustPortfolio
import com.jsingh.credence.domain.parser.StatementParser
import com.jsingh.credence.ui.components.PdfPasswordDialog
import kotlin.math.roundToInt


// --- PALETTE ---
val BgBlack = Color(0xFF09090B)
val CardDark = Color(0xFF18181B)
val SilverAccent = Color(0xFFA1A1AA)
val PrimaryGold = Color(0xFFEAB308)
val SuccessGreen = Color(0xFF10B981)
val InfoBlue = Color(0xFF3B82F6)
val WarnAmber = Color(0xFFF59E0B)
val DangerRed = Color(0xFFEF4444)
val SilverGradient = Brush.linearGradient(listOf(Color(0xFFE2E2E2), Color(0xFF71717A)))
val GoldGradient = Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFB8860B)))

private val TIER_ORDER = listOf("Building", "Silver", "Gold")

/** ₹ formatting kept in one place instead of scattered `.roundToInt()` string templates. */
fun formatInr(value: Double): String = "\u20B9${value.roundToInt()}"

// ---------------- DOMAIN-LITE TYPES (UI layer only — see note at bottom of file) ----------------

enum class ListingCategory { SCHEME, LENDER }

data class LoanListing(
    val id: String,
    val title: String,
    val amount: Double,
    val rateLabel: String,
    val badge: String,
    val category: ListingCategory,
    val explainer: String? = null
)

enum class SchemeStage(val label: String) {
    NOT_APPLIED("Not Applied"),
    APPLIED("Applied"),
    UNDER_VERIFICATION("Under Verification"),
    APPROVED("Approved"),
    DISBURSED("Disbursed")
}

/** Central source of truth for offers so Home and the Schemes/Lenders tab never drift apart. */
object LoanCatalog {
    fun build(portfolio: TrustPortfolio): List<LoanListing> = listOf(
        LoanListing(
            id = "pm-svanidhi",
            title = "PM SVaNidhi — Working Capital Tranche",
            amount = portfolio.safeLoanLimit,
            rateLabel = "Purpose-restricted",
            badge = "GOVERNMENT SCHEME",
            category = ListingCategory.SCHEME,
            explainer = "Disbursed against a specific purpose (e.g. cart & equipment). Verified by an NFC tap at a " +
                    "registered merchant, or by a QR scan-and-confirm if no NFC reader is available — the purchase " +
                    "itself is the proof, no photos or manual checks needed."
        ),
        LoanListing(
            id = "mfi-microloan",
            title = "Micro-Business Working Capital",
            amount = portfolio.safeLoanLimit * 0.5,
            rateLabel = "11% APR",
            badge = "MFI",
            category = ListingCategory.LENDER
        ),
        LoanListing(
            id = "nbfc-equipment",
            title = "Equipment / Inventory Loan",
            amount = portfolio.safeLoanLimit * 0.35,
            rateLabel = "14% APR",
            badge = "NBFC",
            category = ListingCategory.LENDER
        )
    )
}

private data class CardTapTransaction(
    val merchant: String,
    val category: String,
    val amount: Double,
    val approved: Boolean,
    val time: String
)

/** Placeholder demo activity until the Trust Card is backed by real tap/scan events. */
private fun sampleCardActivity(): List<CardTapTransaction> = listOf(
    CardTapTransaction("Raju Cart & Equipment Suppliers", "Cart & Equipment", 3200.0, approved = true, time = "Today, 11:42 AM"),
    CardTapTransaction("Sunrise Electronics", "Consumer Electronics", 1500.0, approved = false, time = "Yesterday, 4:10 PM"),
    CardTapTransaction("Ganesh Hardware Store", "Cart & Equipment", 850.0, approved = true, time = "3 days ago")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val context = LocalContext.current
    var portfolio by remember { mutableStateOf<TrustPortfolio?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var parseError by remember { mutableStateOf<String?>(null) }

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
                try {
                    val transactions = StatementParser.processStatement(rawText)
                    if (transactions.isEmpty()) {
                        parseError = "We couldn't find any transactions in that PDF. Try a different statement."
                    } else {
                        portfolio = ScoreCalculator.calculateScore(transactions)
                    }
                } catch (e: Exception) {
                    parseError = "Something went wrong reading that statement: ${e.message ?: "unknown error"}"
                }
            }
        )
    }

    parseError?.let { message ->
        AlertDialog(
            onDismissRequest = { parseError = null },
            confirmButton = { TextButton(onClick = { parseError = null }) { Text("OK", color = PrimaryGold) } },
            containerColor = CardDark,
            title = { Text("Couldn't read statement", color = Color.White) },
            text = { Text(message, color = SilverAccent) }
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
                    val p = portfolio
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp).clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF27272A)).padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(8.dp).clip(CircleShape)
                                .background(if (p != null) SuccessGreen else SilverAccent)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (p != null) "${p.tier} tier" else "No statement yet",
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
                    icon = { Icon(Icons.Default.Search, "Get a Loan") }, label = { Text("Get a Loan") },
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
                0 -> HomeTab(portfolio, onUploadClick = { pdfPickerLauncher.launch("application/pdf") }, onNavigateToLoans = { selectedTab = 1 })
                1 -> SchemesAndLendersTab(portfolio) { pdfPickerLauncher.launch("application/pdf") }
                2 -> MyScoreTab(portfolio) { portfolio = null }
            }
        }
    }
}

// ---------------- HOME ----------------

@Composable
fun HomeTab(portfolio: TrustPortfolio?, onUploadClick: () -> Unit, onNavigateToLoans: () -> Unit) {
    val listings = remember(portfolio) {
        if (portfolio != null) LoanCatalog.build(portfolio) else emptyList()
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Overview", color = SilverAccent, fontSize = 14.sp, letterSpacing = 1.sp)
            Text("Trust Dashboard", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }

        item {
            if (portfolio == null) UploadPromptCard(onUploadClick)
            else TrustScoreGaugeCard(portfolio)
        }

        if (portfolio != null) {
            item { TierRoadmap(currentTier = portfolio.tier) }
            item { ScoreExplainerCard(portfolio) }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Eligible right now", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = onNavigateToLoans) { Text("See all", color = PrimaryGold, fontSize = 13.sp) }
                }
                Spacer(modifier = Modifier.height(12.dp))
                EligibilityCard(listings, onNavigateToLoans)
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
    val shadowColor = ringColor

    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animationPlayed = true }

    val animatedFraction by animateFloatAsState(
        targetValue = if (animationPlayed) (portfolio.score.toFloat() / 900f).coerceIn(0f, 1f) else 0f,
        animationSpec = tween(1400, easing = FastOutSlowInEasing), label = "gaugeAnim"
    )

    Box(
        modifier = Modifier.fillMaxWidth()
            .shadow(20.dp, RoundedCornerShape(24.dp), spotColor = shadowColor.copy(alpha = 0.45f), ambientColor = shadowColor.copy(alpha = 0.45f))
            .clip(RoundedCornerShape(24.dp))
            .background(if (isGold) GoldGradient else SilverGradient)
            .padding(1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp)).background(CardDark).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("CREDENCE TRUST SCORE", color = SilverAccent, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ringColor.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("${portfolio.tier} tier", color = ringColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 14.dp.toPx()
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    val topLeft = Offset(stroke / 2, stroke / 2)
                    drawArc(
                        color = Color(0xFF27272A),
                        startAngle = -215f, sweepAngle = 250f, useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                        size = arcSize, topLeft = topLeft
                    )
                    drawArc(
                        color = ringColor,
                        startAngle = -215f, sweepAngle = 250f * animatedFraction, useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                        size = arcSize, topLeft = topLeft
                    )
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
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape)
                                .background(if (reached) PrimaryGold else Color(0xFF27272A))
                                .border(1.dp, if (index == currentIndex) Color.White else Color.Transparent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (reached) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BgBlack, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(tier, color = if (reached) Color.White else SilverAccent, fontSize = 11.sp, fontWeight = if (index == currentIndex) FontWeight.Bold else FontWeight.Normal)
                    }
                    if (index != TIER_ORDER.lastIndex) {
                        Box(modifier = Modifier.weight(0.6f).height(2.dp).background(if (index < currentIndex) PrimaryGold else Color(0xFF27272A)))
                    }
                }
            }
            if (currentIndex < TIER_ORDER.lastIndex) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Keep income consistency and payer diversity steady to move up to ${TIER_ORDER[currentIndex + 1]}.",
                    color = SilverAccent, fontSize = 12.sp, lineHeight = 16.sp
                )
            }
        }
    }
}

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
                            "transactions/month across ${portfolio.vitals.payerDiversity} distinct payers. Keeping this steady is what moves your score — " +
                            "not a single big deposit.",
                    color = Color.White, fontSize = 13.sp, lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun EligibilityCard(listings: List<LoanListing>, onNavigateToLoans: () -> Unit) {
    val topScheme = listings.firstOrNull { it.category == ListingCategory.SCHEME }
    val topLender = listings.firstOrNull { it.category == ListingCategory.LENDER }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        topScheme?.let {
            LoanOfferCard(
                title = it.title, amount = formatInr(it.amount), rate = it.rateLabel, badge = it.badge,
                isHighlighted = true, ctaLabel = "View scheme details", onApply = onNavigateToLoans
            )
        }
        topLender?.let {
            LoanOfferCard(
                title = it.title, amount = formatInr(it.amount), rate = it.rateLabel, badge = it.badge,
                ctaLabel = "View lender offers", onApply = onNavigateToLoans
            )
        }
    }
}

@Composable
fun StatementSummaryCard(portfolio: TrustPortfolio) {
    val total = portfolio.recentTransactions.size
    val expenseCount = portfolio.recentTransactions.count { it.isExpense }
    val incomeCount = total - expenseCount

    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardDark).border(1.dp, Color(0xFF27272A), RoundedCornerShape(16.dp)).padding(16.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = SilverAccent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Statement parsed on-device — nothing leaves your phone", color = SilverAccent, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryStat(label = "Transactions found", value = "$total")
                SummaryStat(label = "Income entries", value = "$incomeCount", color = SuccessGreen)
                SummaryStat(label = "Expense entries", value = "$expenseCount", color = Color.White)
            }
        }
    }
}

@Composable
fun SummaryStat(label: String, value: String, color: Color = Color.White) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = SilverAccent, fontSize = 11.sp)
    }
}

// ---------------- SCHEMES & LENDERS ----------------

private enum class CatalogFilter(val label: String) { ALL("All"), SCHEMES("Schemes"), LENDERS("Lenders") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemesAndLendersTab(portfolio: TrustPortfolio?, onUploadClick: () -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(CatalogFilter.ALL) }
    val applicationStage = remember { mutableStateMapOf<String, SchemeStage>() }

    val listings = remember(portfolio) {
        if (portfolio != null) LoanCatalog.build(portfolio) else emptyList()
    }

    val filtered = remember(listings, query, filter) {
        listings.filter {
            (filter == CatalogFilter.ALL ||
                    (filter == CatalogFilter.SCHEMES && it.category == ListingCategory.SCHEME) ||
                    (filter == CatalogFilter.LENDERS && it.category == ListingCategory.LENDER)) &&
                    it.title.contains(query, ignoreCase = true)
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Get a Loan", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Government schemes and private lenders matched to your Trust Score.", color = SilverAccent, fontSize = 14.sp)
        }

        if (portfolio == null) {
            item { UploadPromptCard(onUploadClick) }
            return@LazyColumn
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search schemes or lenders", color = SilverAccent) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SilverAccent) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardDark, unfocusedContainerColor = CardDark,
                    focusedBorderColor = PrimaryGold, unfocusedBorderColor = Color(0xFF27272A),
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White
                )
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CatalogFilter.entries.toList(), key = { it.name }) { option ->
                    FilterChip(
                        selected = filter == option, onClick = { filter = option }, label = { Text(option.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryGold, selectedLabelColor = BgBlack,
                            containerColor = CardDark, labelColor = SilverAccent
                        )
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Text("No matches for \"$query\"", color = SilverAccent, fontSize = 13.sp)
                }
            }
        } else {
            val schemes = filtered.filter { it.category == ListingCategory.SCHEME }
            val lenders = filtered.filter { it.category == ListingCategory.LENDER }

            if (schemes.isNotEmpty()) {
                item { SectionHeader("Government Schemes") }
                items(schemes, key = { it.id }) { listing ->
                    val stage = applicationStage[listing.id] ?: SchemeStage.NOT_APPLIED
                    SchemeCard(
                        listing = listing, stage = stage,
                        onApply = {
                            applicationStage[listing.id] = SchemeStage.APPLIED
                            Toast.makeText(context, "Scheme application started — you'll be notified as it moves through verification.", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
            if (lenders.isNotEmpty()) {
                item { SectionHeader("Private Lenders") }
                items(lenders, key = { it.id }) { listing ->
                    val stage = applicationStage[listing.id] ?: SchemeStage.NOT_APPLIED
                    val pending = stage != SchemeStage.NOT_APPLIED
                    LoanOfferCard(
                        title = listing.title, amount = formatInr(listing.amount), rate = listing.rateLabel, badge = listing.badge,
                        ctaLabel = if (pending) "Application sent" else "Apply",
                        onApply = {
                            if (!pending) {
                                applicationStage[listing.id] = SchemeStage.APPLIED
                                Toast.makeText(context, "Application sent to lender — check status under My Score.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
fun SchemeCard(listing: LoanListing, stage: SchemeStage, onApply: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.fillMaxWidth().animateContentSize().clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF27272A))
            .border(1.dp, PrimaryGold.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .clickable { expanded = !expanded }
            .padding(20.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(listing.badge, color = PrimaryGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                StatusPill(stage)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(listing.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(formatInr(listing.amount), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    if (stage != SchemeStage.NOT_APPLIED) {
                        SchemeStageStepper(stage)
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                    listing.explainer?.let {
                        Text(it, color = SilverAccent, fontSize = 12.sp, lineHeight = 17.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(if (expanded) "Tap to collapse" else "Tap to see how disbursement works", color = SilverAccent.copy(alpha = 0.7f), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onApply,
                enabled = stage == SchemeStage.NOT_APPLIED,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = BgBlack, disabledContainerColor = Color(0xFF3F3F46)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (stage == SchemeStage.NOT_APPLIED) "Apply for this scheme" else "Application in progress", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun SchemeStageStepper(stage: SchemeStage) {
    val stages = listOf(SchemeStage.APPLIED, SchemeStage.UNDER_VERIFICATION, SchemeStage.APPROVED, SchemeStage.DISBURSED)
    val currentIndex = stages.indexOf(stage).coerceAtLeast(0)
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        stages.forEachIndexed { index, s ->
            val reached = index <= currentIndex
            Box(
                modifier = Modifier.size(10.dp).clip(CircleShape).background(if (reached) PrimaryGold else Color(0xFF3F3F46))
            )
            if (index != stages.lastIndex) {
                Box(modifier = Modifier.weight(1f).height(2.dp).background(if (index < currentIndex) PrimaryGold else Color(0xFF3F3F46)))
            }
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(stage.label, color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
}

@Composable
fun StatusPill(stage: SchemeStage) {
    val (label, color) = when (stage) {
        SchemeStage.NOT_APPLIED -> "Eligible" to SuccessGreen
        SchemeStage.APPLIED, SchemeStage.UNDER_VERIFICATION -> "Pending" to WarnAmber
        SchemeStage.APPROVED -> "Approved" to SuccessGreen
        SchemeStage.DISBURSED -> "Disbursed" to InfoBlue
    }
    Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// ---------------- MY SCORE & DATA ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScoreTab(portfolio: TrustPortfolio?, onResetData: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showCardSheet by remember { mutableStateOf(false) }

    if (showCardSheet) {
        TrustCardSheet(limit = portfolio?.safeLoanLimit ?: 0.0, onDismiss = { showCardSheet = false })
    }

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
                Spacer(modifier = Modifier.height(4.dp))
                Text("All 6 factors behind your score — fully rule-based, nothing hidden.", color = SilverAccent, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(24.dp))

                // ---------------------------------------------------------
                // SCORE SIMULATOR
                // ---------------------------------------------------------
                var consistencySlider by remember { mutableFloatStateOf(0f) }
                val projectedScore = portfolio.score + (consistencySlider * 120).toInt()
                val projectedLimit = portfolio.safeLoanLimit * (1.0 + (consistencySlider * 0.5))

                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF27272A).copy(alpha = 0.5f)).border(1.dp, InfoBlue.copy(alpha = 0.3f), RoundedCornerShape(16.dp)).padding(20.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, tint = InfoBlue, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Score Simulator", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("See what happens if you increase your income consistency.", color = SilverAccent, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Projected Score", color = SilverAccent, fontSize = 11.sp)
                                Text("$projectedScore", color = if (consistencySlider > 0) SuccessGreen else Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("New Loan Limit", color = SilverAccent, fontSize = 11.sp)
                                Text(formatInr(projectedLimit), color = if (consistencySlider > 0) SuccessGreen else Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = consistencySlider,
                            onValueChange = { consistencySlider = it },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(thumbColor = InfoBlue, activeTrackColor = InfoBlue, inactiveTrackColor = CardDark)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Current", color = SilverAccent, fontSize = 10.sp)
                            Text("+ Perfect Consistency", color = InfoBlue, fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                // ---------------------------------------------------------

                // ALL FACTOR BARS
                FactorBar(
                    label = "Income Consistency",
                    value = (portfolio.vitals.incomeConsistency.toFloat() / 100f).coerceIn(0f, 1f),
                    display = "${portfolio.vitals.incomeConsistency}%",
                    color = SuccessGreen
                )
                Spacer(modifier = Modifier.height(10.dp))
                FactorBar(
                    label = "Transaction Frequency",
                    value = (portfolio.vitals.transactionFrequency.toFloat() / 60f).coerceIn(0f, 1f),
                    display = "${portfolio.vitals.transactionFrequency}/mo",
                    color = InfoBlue
                )
                Spacer(modifier = Modifier.height(10.dp))
                FactorBar(
                    label = "Payer Diversity",
                    value = (portfolio.vitals.payerDiversity.toFloat() / 10f).coerceIn(0f, 1f),
                    display = "${portfolio.vitals.payerDiversity} payers",
                    color = Color(0xFFA855F7)
                )

                Spacer(modifier = Modifier.height(10.dp))
                val ratio = inflowOutflowRatio(portfolio)
                if (ratio != null) {
                    FactorBar(
                        label = "Inflow / Outflow Ratio",
                        value = (ratio.toFloat() / 2f).coerceIn(0f, 1f),
                        display = "%.2fx".format(ratio),
                        color = if (ratio >= 1.0) SuccessGreen else WarnAmber
                    )
                } else {
                    FactorPlaceholder(label = "Inflow / Outflow Ratio")
                }

                Spacer(modifier = Modifier.height(10.dp))
                FactorPlaceholder(label = "Account Longevity", note = "Needs 12+ months of statement history")
                Spacer(modifier = Modifier.height(10.dp))
                FactorPlaceholder(label = "Lean-Period Resilience", note = "Needs 12+ months of statement history")
                Spacer(modifier = Modifier.height(24.dp))
            } // END OF FACTOR BREAKDOWN BLOCK

            item {
                Text("Connected Data Sources", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Broader coverage means a harder-to-game score.", color = SilverAccent, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(12.dp))
                DataSourceRow(label = "Bank Statement (PDF)", connected = true)
                DataSourceRow(label = "SMS Transaction Alerts", connected = false)
                DataSourceRow(label = "Gmail Bank Mail", connected = false)
                DataSourceRow(label = "UPI History", connected = false)
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
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF27272A))
                        .clickable { showCardSheet = true }.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("APPROVED CATEGORY", color = SilverAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Cart & Equipment Suppliers", color = PrimaryGold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text("View card →", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
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
                                        "pre-approved for ${formatInr(portfolio.safeLoanLimit)}. Verified from my own bank statement, parsed on-device."
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

private fun inflowOutflowRatio(portfolio: TrustPortfolio): Double? {
    var inflow = 0.0
    var outflow = 0.0
    portfolio.recentTransactions.forEach { txn ->
        val value = txn.amount.toString().toDoubleOrNull() ?: return@forEach
        if (txn.isExpense) outflow += value else inflow += value
    }
    return if (outflow > 0.0) inflow / outflow else null
}

@Composable
fun FactorBar(label: String, value: Float, display: String, color: Color) {
    val animatedProgress by animateFloatAsState(
        targetValue = value.coerceIn(0f, 1f),
        animationSpec = tween(1200, easing = LinearOutSlowInEasing),
        label = "factorBar"
    )

    // Continuous pulse animation for healthy scores (> 75%)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse),
        label = "alphaPulse"
    )

    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(CardDark).border(1.dp, Color(0xFF27272A), RoundedCornerShape(14.dp)).padding(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = SilverAccent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(display, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(BgBlack)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (value > 0.75f) color.copy(alpha = alphaPulse) else color
                    )
            )
        }
    }
}

@Composable
fun FactorPlaceholder(label: String, note: String = "Not enough data yet") {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(CardDark).border(1.dp, Color(0xFF27272A), RoundedCornerShape(14.dp)).padding(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = SilverAccent, fontSize = 12.sp)
            Text(note, color = SilverAccent.copy(alpha = 0.6f), fontSize = 11.sp)
        }
    }
}

@Composable
fun DataSourceRow(label: String, connected: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (connected) Icons.Default.CheckCircle else Icons.Default.Lock,
            contentDescription = null,
            tint = if (connected) SuccessGreen else SilverAccent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(
            if (connected) "Connected" else "Coming soon",
            color = if (connected) SuccessGreen else SilverAccent,
            fontSize = 12.sp, fontWeight = FontWeight.Medium
        )
    }
}

// ---------------- TRUST CARD (NFC / QR — Section 5) ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrustCardSheet(limit: Double, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    val activity = remember { sampleCardActivity() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = CardDark) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text("Trust Card", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Purpose-restricted scheme disbursement", color = SilverAccent, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(24.dp))

            FlippableTrustCard(balance = limit)

            Spacer(modifier = Modifier.height(32.dp))
            Text("Recent Activity", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            activity.forEach { CardActivityRow(it) }
        }
    }
}

@Composable
fun FlippableTrustCard(balance: Double) {
    var flipped by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "cardFlip"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clickable { flipped = !flipped }
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
        ) {
            if (rotation <= 90f) {
                // FRONT OF CARD
                Box(
                    modifier = Modifier.fillMaxSize().shadow(16.dp, RoundedCornerShape(20.dp), spotColor = PrimaryGold.copy(alpha = 0.4f)).clip(RoundedCornerShape(20.dp)).background(GoldGradient).padding(1.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(19.dp)).background(BgBlack).padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("CREDENCE TRUST CARD", color = SilverAccent, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.Wifi, contentDescription = "NFC", tint = SuccessGreen, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text("•••• •••• •••• 4821", color = Color.White, fontSize = 20.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            Column {
                                Text("RESTRICTED TO", color = SilverAccent, fontSize = 9.sp)
                                Text("Cart & Equipment", color = PrimaryGold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("BALANCE", color = SilverAccent, fontSize = 9.sp)
                                Text(formatInr(balance), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // BACK OF CARD
                Box(
                    modifier = Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }.clip(RoundedCornerShape(20.dp)).background(CardDark).border(1.dp, Color(0xFF27272A), RoundedCornerShape(20.dp)).padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.QrCode, contentDescription = "QR", tint = BgBlack, modifier = Modifier.size(80.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Scan at checkout to pay directly", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(if (rotation <= 90f) "Tap card to flip to QR code" else "Tap card to flip to NFC", color = SilverAccent.copy(alpha = 0.7f), fontSize = 11.sp)
    }
}

@Composable
private fun CardActivityRow(txn: CardTapTransaction) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF27272A)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (txn.approved) Icons.Default.CheckCircle else Icons.Default.Close,
                contentDescription = if (txn.approved) "Approved" else "Declined",
                tint = if (txn.approved) SuccessGreen else DangerRed,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(txn.merchant, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text("${txn.category} · ${txn.time}", color = SilverAccent, fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatInr(txn.amount), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(if (txn.approved) "Approved" else "Declined", color = if (txn.approved) SuccessGreen else DangerRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ---------------- SHARED COMPONENTS ----------------

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