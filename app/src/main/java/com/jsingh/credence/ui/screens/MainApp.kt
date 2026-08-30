package com.jsingh.credence.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsingh.credence.domain.engine.ScoreCalculator
import com.jsingh.credence.domain.models.TrustPortfolio
import com.jsingh.credence.domain.parser.StatementParser
import com.jsingh.credence.ui.components.PdfPasswordDialog
import com.jsingh.credence.ui.components.CredenceDrawerSheet
import com.jsingh.credence.ui.components.AlertsSheet // ✨ Using the clean Alerts Sheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.jsingh.credence.utils.SessionManager

// ==========================================
// 1. GLOBAL COLORS & UTILS
// ==========================================
val BgBlack = Color(0xFF09090B)
val CardDark = Color(0xFF18181B)
val PrimaryGold = Color(0xFFEAB308)
val SilverAccent = Color(0xFFA1A1AA)
val SuccessGreen = Color(0xFF10B981)
val DangerRed = Color(0xFFEF4444)
val InfoBlue = Color(0xFF3B82F6)
val WarnAmber = Color(0xFFF59E0B)

fun formatInr(value: Double): String = "\u20B9${value.roundToInt()}"

// ==========================================
// 2. DATA MODELS & CATALOG
// ==========================================
enum class ListingCategory { SCHEME, LENDER }
enum class SchemeStage(val label: String) { NOT_APPLIED("Not Applied"), APPLIED("Applied"), UNDER_VERIFICATION("Under Verification"), APPROVED("Approved"), DISBURSED("Disbursed") }
data class LoanListing(val id: String, val title: String, val amount: Double, val rateLabel: String, val badge: String, val category: ListingCategory, val explainer: String? = null)
data class CardTapTransaction(val merchant: String, val category: String, val amount: Double, val approved: Boolean, val time: String)

object LoanCatalog {
    fun build(portfolio: TrustPortfolio): List<LoanListing> = listOf(
        LoanListing("pm-svanidhi", "PM SVaNidhi — Working Capital", portfolio.safeLoanLimit, "Purpose-restricted", "GOVERNMENT SCHEME", ListingCategory.SCHEME, "Disbursed against a specific purpose."),
        LoanListing("mfi-microloan", "Micro-Business Working Capital", portfolio.safeLoanLimit * 0.5, "11% APR", "MFI", ListingCategory.LENDER),
        LoanListing("nbfc-equipment", "Equipment / Inventory Loan", portfolio.safeLoanLimit * 0.35, "14% APR", "NBFC", ListingCategory.LENDER)
    )
}

fun sampleCardActivity(): List<CardTapTransaction> = listOf(
    CardTapTransaction("Raju Cart & Equipment Suppliers", "Cart & Equipment", 3200.0, true, "Today, 11:42 AM"),
    CardTapTransaction("Sunrise Electronics", "Consumer Electronics", 1500.0, false, "Yesterday, 4:10 PM"),
    CardTapTransaction("Ganesh Hardware Store", "Cart & Equipment", 850.0, true, "3 days ago")
)

// ==========================================
// 3. MAIN APP ROUTER
// ==========================================
@Composable
fun MainApp(
    incomingSharedUri: Uri? = null,
    onSharedUriHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    var initialPortfolio by remember {
        mutableStateOf(
            if (sessionManager.isOnboarded()) {
                val cachedTxns = sessionManager.getCachedTransactions()
                if (cachedTxns.isNotEmpty()) ScoreCalculator.calculateScore(cachedTxns) else null
            } else null
        )
    }

    var isAppUnlocked by remember { mutableStateOf(sessionManager.isOnboarded() && initialPortfolio != null) }

    LaunchedEffect(Unit) {
        if (sessionManager.isOnboarded() && initialPortfolio == null) {
            sessionManager.clearSession()
            isAppUnlocked = false
        }
    }

    var globalParseError by remember { mutableStateOf<String?>(null) }
    var pendingOnboardingUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(incomingSharedUri) {
        if (incomingSharedUri != null && !isAppUnlocked) {
            pendingOnboardingUri = incomingSharedUri
        }
    }

    if (!isAppUnlocked) {
        OnboardingFlow(
            incomingUri = pendingOnboardingUri,
            onClearIncomingUri = { pendingOnboardingUri = null; onSharedUriHandled() },
            onFinishOnboarding = { rawText ->
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val result = StatementParser.processStatement(rawText)
                        if (result.transactions.isNotEmpty()) {
                            sessionManager.saveOnboardingSession(result)
                            initialPortfolio = ScoreCalculator.calculateScore(result.transactions)
                            isAppUnlocked = true
                        } else {
                            globalParseError = "No valid transactions recognized. Please verify the statement."
                        }
                    } catch (e: Exception) {
                        globalParseError = "Failed to parse document: ${e.message}"
                    }
                }
            }
        )
    } else {
        CredenceDashboard(
            preLoadedPortfolio = initialPortfolio,
            onResetApp = {
                sessionManager.clearSession()
                initialPortfolio = null
                isAppUnlocked = false
            }
        )
    }

    globalParseError?.let { msg ->
        AlertDialog(
            onDismissRequest = { globalParseError = null },
            confirmButton = { TextButton(onClick = { globalParseError = null }) { Text("OK", color = PrimaryGold) } },
            containerColor = CardDark,
            title = { Text("Error", color = Color.White) },
            text = { Text(msg, color = SilverAccent) }
        )
    }
}

// ==========================================
// 4. THE DASHBOARD UI
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredenceDashboard(
    preLoadedPortfolio: TrustPortfolio?,
    onResetApp: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    val userName = remember { sessionManager.getUserName() }
    val businessType = remember { sessionManager.getBusinessType() }
    val accountNumber = remember { sessionManager.getAccountNumber() }

    var portfolio by remember { mutableStateOf<TrustPortfolio?>(preLoadedPortfolio) }
    LaunchedEffect(preLoadedPortfolio) { portfolio = preLoadedPortfolio }

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var parseError by remember { mutableStateOf<String?>(null) }
    var showBankSelector by remember { mutableStateOf(false) }

    // ✨ Alerts Sheet State (Cleaned up from Tracker)
    var showAlertsSheet by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val pdfPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) { selectedPdfUri = uri; showDialog = true }
    }

    if (showBankSelector) {
        BankSelectionSheet(onDismiss = { showBankSelector = false }, onManualUploadClick = { pdfPickerLauncher.launch("application/pdf") })
    }

    if (showDialog && selectedPdfUri != null) {
        PdfPasswordDialog(
            context = context, pdfUri = selectedPdfUri!!, onDismiss = { showDialog = false },
            onSuccess = { rawText ->
                showDialog = false
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val result = StatementParser.processStatement(rawText)
                        if (result.transactions.isEmpty()) parseError = "No valid transactions found in this PDF."
                        else portfolio = ScoreCalculator.calculateScore(result.transactions)
                    } catch (e: Exception) { parseError = e.message }
                }
            }
        )
    }

    parseError?.let { msg ->
        AlertDialog(onDismissRequest = { parseError = null }, confirmButton = { TextButton(onClick = { parseError = null }) { Text("OK", color = PrimaryGold) } }, containerColor = CardDark, title = { Text("Error", color = Color.White) }, text = { Text(msg, color = SilverAccent) })
    }

    // ✨ Render the clean Alerts Sheet when Bell is clicked
    if (showAlertsSheet) {
        AlertsSheet(portfolio = portfolio, onDismiss = { showAlertsSheet = false })
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            CredenceDrawerSheet(userName, businessType, accountNumber, onClose = { scope.launch { drawerState.close() } }, onResetApp = onResetApp)
        }
    ) {
        Scaffold(
            containerColor = BgBlack,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Menu", tint = Color.White)
                        }
                    },
                    title = { Text("Credence", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color.White) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BgBlack),
                    actions = {

                        // ✨ Notification Bell properly wired to showAlertsSheet
                        IconButton(onClick = { showAlertsSheet = true }, modifier = Modifier.padding(end = 8.dp)) {
                            BadgedBox(badge = { if (portfolio != null) { Badge(containerColor = DangerRed, modifier = Modifier.size(10.dp)) } }) {
                                Icon(Icons.Default.Notifications, contentDescription = "Alerts", tint = SilverAccent, modifier = Modifier.size(26.dp))
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(containerColor = CardDark, contentColor = SilverAccent) {
                    NavigationBarItem(icon = { Icon(Icons.Default.Home, "Home") }, label = { Text("Home") }, selected = selectedTab == 0, onClick = { selectedTab = 0 }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, indicatorColor = Color(0xFF27272A)))
                    NavigationBarItem(icon = { Icon(Icons.Default.Search, "Loans") }, label = { Text("Loans") }, selected = selectedTab == 1, onClick = { selectedTab = 1 }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, indicatorColor = Color(0xFF27272A)))
                    NavigationBarItem(icon = { Icon(Icons.Default.Person, "Profile") }, label = { Text("Profile") }, selected = selectedTab == 2, onClick = { selectedTab = 2 }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, indicatorColor = Color(0xFF27272A)))
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (selectedTab) {
                    0 -> HomeTab(portfolio, userName, onUploadClick = { showBankSelector = true }, onNavigateToLoans = { selectedTab = 1 })
                    1 -> SchemesAndLendersTab(portfolio, onUploadClick = { showBankSelector = true }, onNavigateToCard = { selectedTab = 2 })
                    2 -> MyScoreTab(portfolio, userName, businessType, onResetData = onResetApp)
                }
            }
        }
    }
}

// ==========================================
// 5. SHARED UI COMPONENTS
// ==========================================
@Composable
fun LoanOfferCard(title: String, amount: String, rate: String, badge: String, isHighlighted: Boolean = false, ctaLabel: String = "Apply Now", onApply: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(if (isHighlighted) Color(0xFF27272A) else CardDark).border(1.dp, if (isHighlighted) PrimaryGold.copy(alpha = 0.5f) else Color(0xFF27272A), RoundedCornerShape(20.dp)).padding(20.dp)) {
        Column {
            Text(badge, color = if (isHighlighted) PrimaryGold else SilverAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column { Text("Approved Limit", color = SilverAccent, fontSize = 12.sp); Text(amount, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold) }
                Text(rate, color = SuccessGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onApply, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isHighlighted) PrimaryGold else Color.White, contentColor = BgBlack), shape = RoundedCornerShape(12.dp)) { Text(ctaLabel, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
    }
}

@Composable
fun UploadPromptCard(onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CardDark).border(1.dp, Color(0xFF27272A), RoundedCornerShape(20.dp)).clickable { onClick() }.padding(24.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFF27272A)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, contentDescription = "Upload", tint = PrimaryGold, modifier = Modifier.size(32.dp)) }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Build Your Trust Score", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SilverAccent, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(12.dp)); Text("Upload a recent 6-month bank statement PDF", color = SilverAccent, fontSize = 13.sp) }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) { Icon(Icons.Default.Lock, contentDescription = null, tint = SilverAccent, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(12.dp)); Text("Parsed on-device — nothing is uploaded to a server", color = SilverAccent, fontSize = 13.sp) }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = BgBlack), shape = RoundedCornerShape(12.dp)) { Text("Select Bank Statement PDF", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
    }
}