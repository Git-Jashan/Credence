package com.jsingh.credence.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
import com.jsingh.credence.ui.components.AlertsSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.jsingh.credence.utils.SessionManager
import java.text.DecimalFormat

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

// ✨ GUARANTEED INDIAN COMMA FORMATTER (Lakhs & Crores)
fun formatInr(value: Double): String {
    val formatter = DecimalFormat("##,##,##0")
    return "₹${formatter.format(value.roundToInt())}"
}
// ==========================================
// 2. MAIN APP ROUTER
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
// 3. THE DASHBOARD UI
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

    // ✨ DEEP LINKING STATES FOR SCHEMES TAB
    var schemesDefaultTab by remember { mutableIntStateOf(0) } // 0 = Discover, 1 = Track
    var schemesDefaultSubTab by remember { mutableIntStateOf(0) } // 0 = Active, 1 = Apps

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

    if (showAlertsSheet) {
        AlertsSheet(portfolio = portfolio, onDismiss = { showAlertsSheet = false })
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            CredenceDrawerSheet(
                portfolio = portfolio,
                userName = userName,
                businessType = businessType,
                accountNumber = accountNumber,
                onClose = { scope.launch { drawerState.close() } },
                onResetApp = onResetApp,
                onUploadClick = {
                    scope.launch { drawerState.close() }
                    showBankSelector = true
                }
            )
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
                    // ✨ 1. PERFECTLY WIRED HOME TAB ROUTING
                    0 -> HomeTab(
                        portfolio = portfolio,
                        userName = userName,
                        onUploadClick = { showBankSelector = true },
                        // 👇 REMOVED THE OLD "onNavigateToLoans" SO IT COMPILES
                        onNavigateToMarket = {
                            schemesDefaultTab = 0 // Discover Tab
                            selectedTab = 1
                        },
                        onNavigateToActiveLoans = {
                            schemesDefaultTab = 1 // Track Tab
                            schemesDefaultSubTab = 0 // Active Sub-Tab
                            selectedTab = 1
                        },
                        onNavigateToApplications = {
                            schemesDefaultTab = 1 // Track Tab
                            schemesDefaultSubTab = 1 // Apps Sub-Tab
                            selectedTab = 1
                        }
                    )
                    // ✨ 2. SCHEMES TAB CATCHES THE STATES
                    1 -> SchemesAndLendersTab(
                        portfolio = portfolio,
                        defaultTab = schemesDefaultTab,
                        defaultSubTab = schemesDefaultSubTab,
                        onUploadClick = { showBankSelector = true },
                        onNavigateToCard = { selectedTab = 2 }
                    )
                    2 -> MyScoreTab(
                        portfolio = portfolio,
                        userName = userName,
                        businessType = businessType,
                        accountNumber = accountNumber,
                        onResetData = onResetApp,
                        onUploadClick = { showBankSelector = true }
                    )
                }
            }
        }
    }
}