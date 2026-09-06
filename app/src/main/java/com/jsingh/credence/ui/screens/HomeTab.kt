package com.jsingh.credence.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HomeTab(
    portfolio: TrustPortfolio?,
    userName: String,
    onUploadClick: () -> Unit,
    onNavigateToMarket: () -> Unit,
    onNavigateToActiveLoans: () -> Unit,
    onNavigateToApplications: () -> Unit
) {
    var showLinkedAccountsSheet by remember { mutableStateOf(false) }

    if (showLinkedAccountsSheet && portfolio != null) {
        val syncDate = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()) }
        HomeLinkedAccountsSheet(
            portfolio = portfolio,
            accountNumber = "•••• 4821",
            syncDate = syncDate,
            onDismiss = { showLinkedAccountsSheet = false },
            onUploadClick = { showLinkedAccountsSheet = false; onUploadClick() }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BgBlack).padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Dashboard", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
        }

        item {
            if (portfolio == null) {
                HomeUploadPromptCard(onUploadClick)
            } else {
                val maxLimit = portfolio.safeLoanLimit.toFloat().coerceAtLeast(5000f)
                var sliderValue by remember { mutableFloatStateOf(maxLimit * 0.25f) }

                val reqAmt = sliderValue.toDouble()
                val utilization = reqAmt / maxLimit.toDouble()

                val penaltyRaw = utilization * 50.0
                val riskPenalty = if (penaltyRaw.isNaN()) 0 else penaltyRaw.roundToInt()
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

                    QuickActionsRow(
                        onNavigateToMarket = onNavigateToMarket,
                        onNavigateToApplications = onNavigateToApplications,
                        onNavigateToActiveLoans = onNavigateToActiveLoans,
                        onOpenManageAccounts = { showLinkedAccountsSheet = true }
                    )
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
                    Text("See All", color = PrimaryGold, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { onNavigateToActiveLoans() })
                }
                Spacer(modifier = Modifier.height(16.dp))
                PortfolioSnapshotCard(
                    onNavigateToActiveLoans = onNavigateToActiveLoans,
                    onNavigateToApplications = onNavigateToApplications
                )
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

                HomeLoanOfferCard(
                    title = "PM SVaNidhi — Working Capital",
                    amount = formatInr(portfolio.safeLoanLimit),
                    rate = "Purpose-restricted",
                    badge = "GOVERNMENT SCHEME",
                    isHighlighted = true,
                    ctaLabel = "Review Offer",
                    onApply = onNavigateToMarket
                )
            }

            item {
                DataFreshnessStrip(portfolio = portfolio, onUpdateClick = { showLinkedAccountsSheet = true })
            }
        }
        item { Spacer(modifier = Modifier.height(2.dp)) }
    }
}

// ==========================================
// ACTION BUTTONS
// ==========================================
@Composable
fun QuickActionsRow(
    onNavigateToMarket: () -> Unit,
    onNavigateToApplications: () -> Unit,
    onNavigateToActiveLoans: () -> Unit,
    onOpenManageAccounts: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        QuickActionButton("Market", Icons.Default.Storefront, onNavigateToMarket)
        QuickActionButton("Tracker", Icons.Default.DataUsage, onNavigateToApplications)
        QuickActionButton("History", Icons.Default.History, onNavigateToActiveLoans)
        QuickActionButton("Update", Icons.Default.Sync, onOpenManageAccounts)
    }
}

@Composable
fun QuickActionButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    val scope = rememberCoroutineScope()
    var isRouting by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
        if (!isRouting) {
            scope.launch {
                isRouting = true
                kotlinx.coroutines.delay(500) // ✨ Fake network fetch
                isRouting = false
                onClick()
            }
        }
    }) {
        Box(
            modifier = Modifier.size(52.dp).clip(CircleShape).background(CardDark),
            contentAlignment = Alignment.Center
        ) {
            if (isRouting) {
                CircularProgressIndicator(color = PrimaryGold, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                Icon(icon, contentDescription = label, tint = PrimaryGold, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = SilverAccent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreditSimulatorWidget(dynamicScore: Int, requestedAmount: Double, maxLimit: Float, sliderValue: Float, onSliderChange: (Float) -> Unit) {
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
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = PrimaryGold,
                    inactiveTrackColor = Color(0xFF27272A)
                ),
                track = { sliderState ->
                    SliderDefaults.Track(
                        colors = SliderDefaults.colors(
                            activeTrackColor = PrimaryGold,
                            inactiveTrackColor = Color(0xFF27272A)
                        ),
                        sliderState = sliderState,
                        drawStopIndicator = null
                    )
                }
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
private fun PortfolioSnapshotCard(onNavigateToActiveLoans: () -> Unit, onNavigateToApplications: () -> Unit) {
    val scope = rememberCoroutineScope()
    var isSyncing by remember { mutableStateOf(false) }
    var activeRoute by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(CardDark).padding(20.dp)) {

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (isSyncing) WarnAmber else SuccessGreen))
                Spacer(modifier = Modifier.width(6.dp))
                Text("LENDER NETWORK", color = SilverAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            if (isSyncing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = PrimaryGold, strokeWidth = 2.dp, modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Syncing...", color = PrimaryGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                    scope.launch { isSyncing = true; kotlinx.coroutines.delay(1500); isSyncing = false }
                }) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = SilverAccent, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Refresh", color = SilverAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Row 1: Active Loan
        Row(modifier = Modifier.fillMaxWidth().clickable {
            if (activeRoute == null) {
                scope.launch { activeRoute = "loans"; kotlinx.coroutines.delay(700); activeRoute = null; onNavigateToActiveLoans() }
            }
        }, verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(InfoBlue.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                if (activeRoute == "loans") CircularProgressIndicator(color = InfoBlue, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                else Icon(Icons.Default.Payments, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(20.dp))
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

        // Row 2: Application
        Row(modifier = Modifier.fillMaxWidth().clickable {
            if (activeRoute == null) {
                scope.launch { activeRoute = "apps"; kotlinx.coroutines.delay(700); activeRoute = null; onNavigateToApplications() }
            }
        }, verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(PrimaryGold.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                if (activeRoute == "apps") CircularProgressIndicator(color = PrimaryGold, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                else Icon(Icons.Default.Autorenew, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(20.dp))
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
private fun DataFreshnessStrip(portfolio: TrustPortfolio, onUpdateClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(100.dp)).background(CardDark).clickable { onUpdateClick() }.padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text("Verified via ${portfolio.bankName} • ${portfolio.statementMonths} Mos", color = SilverAccent, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SilverAccent, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun HomeUploadPromptCard(onClick: () -> Unit) {
    val scope = rememberCoroutineScope()
    var isStarting by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CardDark).border(1.dp, Color(0xFF27272A), RoundedCornerShape(20.dp)).clickable {
        if (!isStarting) {
            scope.launch { isStarting = true; kotlinx.coroutines.delay(1000); isStarting = false; onClick() }
        }
    }.padding(24.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFF27272A)), contentAlignment = Alignment.Center) {
                if (isStarting) CircularProgressIndicator(color = PrimaryGold, strokeWidth = 3.dp, modifier = Modifier.size(24.dp))
                else Icon(Icons.Default.Add, contentDescription = "Upload", tint = PrimaryGold, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Build Your Trust Score", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SilverAccent, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(12.dp)); Text("Upload a recent 6-month bank statement", color = SilverAccent, fontSize = 13.sp) }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (!isStarting) {
                        scope.launch { isStarting = true; kotlinx.coroutines.delay(1000); isStarting = false; onClick() }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = BgBlack),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isStarting) "Initializing Secure Vault..." else "Select Statement PDF", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun HomeLoanOfferCard(title: String, amount: String, rate: String, badge: String, isHighlighted: Boolean = false, ctaLabel: String = "Apply Now", onApply: () -> Unit) {
    val scope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(if (isHighlighted) Color(0xFF111113) else CardDark).border(1.dp, if (isHighlighted) PrimaryGold.copy(alpha = 0.5f) else Color(0xFF27272A), RoundedCornerShape(20.dp)).padding(20.dp)) {
        Column {
            Text(badge, color = if (isHighlighted) PrimaryGold else SilverAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column { Text("Approved Limit", color = SilverAccent, fontSize = 12.sp); Text(amount, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold) }
                Text(rate, color = SuccessGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    scope.launch {
                        isChecking = true
                        kotlinx.coroutines.delay(1400) // Fake API negotiation
                        isChecking = false
                        onApply()
                    }
                },
                enabled = !isChecking,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isHighlighted) PrimaryGold else Color.White, contentColor = BgBlack),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isChecking) {
                    CircularProgressIndicator(color = BgBlack, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Pinging Lenders...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                } else {
                    Text(ctaLabel, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeLinkedAccountsSheet(
    portfolio: TrustPortfolio,
    accountNumber: String,
    syncDate: String,
    onDismiss: () -> Unit,
    onUploadClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var isSyncing by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = CardDark) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
            Text("Linked Financial Data", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Manage your connected bank accounts and statements.", color = SilverAccent, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(24.dp))

            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BgBlack).border(1.dp, SuccessGreen.copy(alpha=0.3f), RoundedCornerShape(16.dp)).padding(20.dp)) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(SuccessGreen.copy(alpha=0.15f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp)) }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Primary Account", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                val maskedAcc = if (accountNumber.length > 4) accountNumber.takeLast(4) else accountNumber
                                Text("${portfolio.bankName} •••• $maskedAcc", color = SilverAccent, fontSize = 13.sp)
                            }
                        }
                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(SuccessGreen.copy(alpha=0.15f)).padding(horizontal = 10.dp, vertical = 6.dp)) { Text("ACTIVE", color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFF27272A))
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column { Text("LAST SYNCED", color = SilverAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp); Spacer(modifier = Modifier.height(2.dp)); Text(if (isSuccess) "Just now" else syncDate, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium) }

                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (isSuccess) SuccessGreen.copy(alpha = 0.1f) else PrimaryGold.copy(alpha = 0.1f)).clickable {
                            if (!isSyncing && !isSuccess) {
                                scope.launch {
                                    isSyncing = true
                                    kotlinx.coroutines.delay(2200)
                                    isSyncing = false
                                    isSuccess = true
                                    kotlinx.coroutines.delay(1000)
                                    onUploadClick()
                                }
                            }
                        }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSyncing) {
                                    CircularProgressIndicator(color = PrimaryGold, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Syncing...", color = PrimaryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                } else if (isSuccess) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Synced", color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.Sync, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Update", color = PrimaryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF27272A).copy(alpha=0.4f)).clickable { onUploadClick() }.border(1.dp, Color(0xFF3F3F46), RoundedCornerShape(16.dp)).padding(16.dp), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Link Another Bank Account", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}