package com.jsingh.credence.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsingh.credence.domain.models.TrustPortfolio
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScoreTab(
    portfolio: TrustPortfolio?,
    userName: String,
    businessType: String,
    accountNumber: String,
    onResetData: () -> Unit,
    onUploadClick: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var showCardSheet by remember { mutableStateOf(false) }
    var showLinkedAccountsSheet by remember { mutableStateOf(false) }
    var showQrSheet by remember { mutableStateOf(false) }

    val credenceId = remember { "CRD-" + UUID.randomUUID().toString().take(8).uppercase() }
    val syncDate = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()) }

    if (showCardSheet) {
        TrustCardSheet(limit = portfolio?.safeLoanLimit ?: 0.0, status = portfolio?.tier ?: "Building", onDismiss = { showCardSheet = false })
    }

    if (showLinkedAccountsSheet && portfolio != null) {
        LinkedAccountsSheet(
            portfolio = portfolio, accountNumber = accountNumber, syncDate = syncDate, onDismiss = { showLinkedAccountsSheet = false },
            onUploadClick = { showLinkedAccountsSheet = false; onUploadClick() }
        )
    }

    if (showQrSheet && portfolio != null) {
        DigitalIdQrSheet(
            userName = userName,
            credenceId = credenceId,
            portfolio = portfolio,
            syncDate = syncDate,
            onDismiss = { showQrSheet = false }
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(BgBlack).padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Portfolio", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CardDark)
                        .border(1.dp, Color(0xFF27272A), CircleShape)
                        .clickable { showQrSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR", tint = PrimaryGold, modifier = Modifier.size(22.dp))
                }
            }
        }

        if (portfolio == null) {
            item { ScoreUploadPromptCard(onClick = onUploadClick) }
        } else {

            // 1. DIGITAL PASSPORT
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = PrimaryGold.copy(alpha = 0.1f))
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF18181B), CardDark, PrimaryGold.copy(alpha = 0.05f)), Offset.Zero, Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)))
                        .border(1.dp, Color(0xFF27272A), RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CREDENCE DIGITAL ID", color = SilverAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SuccessGreen).shadow(4.dp, spotColor = SuccessGreen))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ACTIVE", color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(PrimaryGold.copy(alpha = 0.15f)).border(1.dp, PrimaryGold.copy(alpha = 0.3f), CircleShape), contentAlignment = Alignment.Center) {
                                Text(if (userName.isNotBlank()) userName.take(1).uppercase() else "U", color = PrimaryGold, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(verticalArrangement = Arrangement.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(userName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                                }
                                Text(businessType, color = PrimaryGold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

                                Text("ID: $credenceId", color = SilverAccent, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BgBlack).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("DATA SOURCE", color = SilverAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("${portfolio.bankName} (${portfolio.statementMonths}Mos)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Box(modifier = Modifier.height(24.dp).width(1.dp).background(Color(0xFF27272A)))
                            Column(horizontalAlignment = Alignment.End) {
                                Text("LAST SYNCED", color = SilverAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(syncDate, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // 2. ACCOUNT MANAGEMENT ROW
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardDark).clickable { showLinkedAccountsSheet = true }.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(SuccessGreen.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Manage Bank Accounts", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("1 Linked • Active", color = SilverAccent, fontSize = 13.sp)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SilverAccent)
                }
            }

            // 3. KEY METRICS GRID
            item {
                Text("Underwriting Summary", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MetricGridCard(
                        modifier = Modifier.weight(1f),
                        title = "Trust Score",
                        value = "${portfolio.score}/100",
                        subtitle = "Tier: ${portfolio.tier.uppercase()}",
                        icon = Icons.Default.Speed,
                        color = InfoBlue
                    )
                    MetricGridCard(
                        modifier = Modifier.weight(1f),
                        title = "Safe Limit",
                        value = formatInr(portfolio.safeLoanLimit), // ✨ using global formatter
                        subtitle = "${portfolio.statementMonths} Mos Data",
                        icon = Icons.Default.AccountBalanceWallet,
                        color = PrimaryGold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val ratio = portfolio.vitals.inflowOutflowRatio
                    val ratioColor = if (ratio >= 1.2) SuccessGreen else if (ratio >= 1.0) WarnAmber else DangerRed
                    MetricGridCard(
                        modifier = Modifier.weight(1f),
                        title = "Cashflow",
                        value = String.format(Locale.US, "%.2fx", ratio),
                        subtitle = "Inflow vs Outflow",
                        icon = Icons.Default.SwapVert,
                        color = ratioColor
                    )

                    val dti = portfolio.vitals.debtToIncomeRatio
                    val dtiColor = if (dti < 0.3) SuccessGreen else if (dti < 0.5) WarnAmber else DangerRed
                    MetricGridCard(
                        modifier = Modifier.weight(1f),
                        title = "Debt Burden",
                        value = "${(dti * 100).roundToInt()}%",
                        subtitle = "Est. EMI: ${formatInr(portfolio.vitals.estimatedEMI)}", // ✨ using global formatter
                        icon = Icons.Default.TrendingDown,
                        color = dtiColor
                    )
                }
            }

            // 4. FULLY MAPPED DIAGNOSTICS
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Risk Diagnostics", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Powered by Credence AI", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(16.dp))

                val consistencyProgress = (portfolio.vitals.incomeConsistency.toFloat() / 100f).coerceIn(0f, 1f)
                FactorBar("Income Consistency", consistencyProgress, "${portfolio.vitals.incomeConsistency}%", "Low monthly volatility confirms predictable cashflow for lenders.", SuccessGreen)
                Spacer(modifier = Modifier.height(12.dp))

                val diversityProgress = (portfolio.vitals.payerDiversity.toFloat() / 15f).coerceIn(0f, 1f)
                FactorBar("Customer Diversity", diversityProgress, "${portfolio.vitals.payerDiversity} Payers", "Distinct payer handles reduces single-client dependency risk.", Color(0xFFA855F7))
                Spacer(modifier = Modifier.height(12.dp))

                val velocityProgress = (portfolio.vitals.transactionFrequency.toFloat() / 60f).coerceIn(0f, 1f)
                FactorBar("Transaction Velocity", velocityProgress, "${portfolio.vitals.transactionFrequency}/mo", "High monthly transaction volume proves the business is actively trading.", InfoBlue)
                Spacer(modifier = Modifier.height(12.dp))

                val bounceCount = portfolio.vitals.bounceCount
                val integrityProgress = if (bounceCount == 0) 1f else (1f - (bounceCount * 0.33f)).coerceIn(0f, 1f)
                FactorBar(
                    label = "Repayment Integrity",
                    value = integrityProgress,
                    display = if (bounceCount == 0) "Zero Bounces" else "$bounceCount Penalties",
                    description = if (bounceCount == 0) "Zero ECS/NACH defaults or bank penalty charges detected." else "Penalties detected on statement; heavily affects risk score.",
                    color = if (bounceCount == 0) SuccessGreen else DangerRed
                )
            }

            // 5. EXPORT CENTER
            item {
                Text("Disbursement", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardDark).clickable { showCardSheet = true }.padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Nfc, contentDescription = null, tint = SilverAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("RESTRICTED TRUST CARD", color = SilverAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("View Virtual NFC Card", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(BgBlack), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = PrimaryGold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 6. VERIFICATION LEDGER
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF3F3F46), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Credence Zero-Knowledge Engine v1.0.0", color = Color(0xFF3F3F46), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Text("Data parsed locally. Never stored on external servers.", color = Color(0xFF3F3F46), fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ==========================================
// LOCAL ISOLATED COMPONENTS
// ==========================================

@Composable
private fun ScoreUploadPromptCard(onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CardDark).border(1.dp, Color(0xFF27272A), RoundedCornerShape(20.dp)).clickable { onClick() }.padding(24.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFF27272A)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, contentDescription = "Upload", tint = PrimaryGold, modifier = Modifier.size(32.dp)) }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Build Your Trust Score", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SilverAccent, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(12.dp)); Text("Upload a recent 6-month bank statement PDF", color = SilverAccent, fontSize = 13.sp) }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = BgBlack), shape = RoundedCornerShape(12.dp)) { Text("Select Bank Statement PDF", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalIdQrSheet(userName: String, credenceId: String, portfolio: TrustPortfolio, syncDate: String, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var isGeneratingQr by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1500)
        isGeneratingQr = false
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    var isGeneratingPdf by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = CardDark) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Verified Identity QR", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Lenders can scan this to instantly verify your profile.", color = SilverAccent, fontSize = 13.sp, textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(24.dp))

            Box(modifier = Modifier.size(240.dp).clip(RoundedCornerShape(20.dp)).background(Color.White).padding(16.dp), contentAlignment = Alignment.Center) {
                if (isGeneratingQr) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = BgBlack, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Generating Secure\nZero-Knowledge Hash...", color = BgBlack, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                } else {
                    // Frame tightly hugs the massive edge-to-edge QR icon
                    Box(modifier = Modifier.fillMaxSize().border(2.dp, Color(0xFFE4E4E7), RoundedCornerShape(12.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.QrCode2, contentDescription = "QR Code", tint = BgBlack, modifier = Modifier.fillMaxSize())
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = {
                        if (!isGeneratingPdf) {
                            scope.launch {
                                isGeneratingPdf = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                kotlinx.coroutines.delay(1800) // Fake PDF Build Time

                                try {
                                    val pdfDoc = android.graphics.pdf.PdfDocument()
                                    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(400, 600, 1).create()
                                    val page = pdfDoc.startPage(pageInfo)
                                    val paint = android.graphics.Paint().apply { color = android.graphics.Color.BLACK; textSize = 16f }
                                    page.canvas.drawText("CREDENCE CERTIFIED REPORT", 40f, 50f, paint)
                                    page.canvas.drawText("ID: $credenceId", 40f, 90f, paint)
                                    page.canvas.drawText("Name: $userName", 40f, 120f, paint)
                                    page.canvas.drawText("Trust Score: ${portfolio.score}/100", 40f, 150f, paint)
                                    pdfDoc.finishPage(page)
                                    val file = java.io.File(context.cacheDir, "Credence_Report_$credenceId.pdf")
                                    pdfDoc.writeTo(java.io.FileOutputStream(file))
                                    pdfDoc.close()
                                } catch (e: Exception) { /* Silently fail if cache access is denied */ }

                                isGeneratingPdf = false

                                val report = "📄 *CREDENCE CERTIFIED REPORT*\nReport ID: $credenceId\nApplicant: $userName\nVerified Limit: ${formatInr(portfolio.safeLoanLimit)}\nTrust Score: ${portfolio.score}/100\nDate: $syncDate\n\n[System Note: Full PDF document verified on device]"
                                val sendIntent = Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, report); type = "text/plain" }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Certified Report"))
                            }
                        }
                    },
                    enabled = !isGeneratingQr,
                    modifier = Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = BgBlack, disabledContainerColor = Color(0xFF3F3F46)), shape = RoundedCornerShape(14.dp)
                ) {
                    if (isGeneratingPdf) {
                        CircularProgressIndicator(color = BgBlack, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compiling PDF...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    } else {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share PDF", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        clipboardManager.setText(AnnotatedString(credenceId))
                        Toast.makeText(context, "Credence ID Copied!", Toast.LENGTH_SHORT).show()
                    },
                    enabled = !isGeneratingQr,
                    modifier = Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), border = BorderStroke(1.dp, Color(0xFF27272A)), shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Copy ID", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun MetricGridCard(modifier: Modifier = Modifier, title: String, value: String, subtitle: String? = null, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Box(modifier = modifier.clip(RoundedCornerShape(16.dp)).background(CardDark).padding(16.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(6.dp)); Text(title, color = SilverAccent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, color = SilverAccent, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun FactorBar(label: String, value: Float, display: String, description: String, color: Color) {
    val animatedProgress by animateFloatAsState(targetValue = value.coerceIn(0f, 1f), animationSpec = tween(1200, easing = LinearOutSlowInEasing), label = "factorBar")

    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardDark).padding(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(display, color = color, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(description, color = SilverAccent, fontSize = 13.sp, lineHeight = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(BgBlack)) {
            Box(modifier = Modifier.fillMaxWidth(animatedProgress).height(6.dp).clip(RoundedCornerShape(3.dp)).background(color))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedAccountsSheet(portfolio: TrustPortfolio, accountNumber: String, syncDate: String, onDismiss: () -> Unit, onUploadClick: () -> Unit) {
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
                            Column { Text("Primary Account", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold); val maskedAcc = if (accountNumber.length > 4) accountNumber.takeLast(4) else accountNumber; Text("${portfolio.bankName} •••• $maskedAcc", color = SilverAccent, fontSize = 13.sp) }
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
                                    kotlinx.coroutines.delay(2200) // Fake Handshake
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
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Link Another Bank Account", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrustCardSheet(limit: Double, status: String, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = CardDark) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text("Credence Trust Card", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Purpose-restricted scheme disbursement", color = SilverAccent, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(24.dp))
            FlippableTrustCard(balance = limit, status = status)
            Spacer(modifier = Modifier.height(32.dp))
            Text("Recent Auth Activity", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            val mockTxns = listOf(Triple("Raju Cart & Equipment", 3200.0, true), Triple("Sunrise Electronics", 1500.0, false), Triple("Ganesh Hardware Store", 850.0, true))
            mockTxns.forEach { (merchant, amount, approved) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(if(approved) SuccessGreen.copy(alpha=0.15f) else DangerRed.copy(alpha=0.15f)), contentAlignment = Alignment.Center) {
                        Icon(if (approved) Icons.Default.Check else Icons.Default.Close, contentDescription = null, tint = if (approved) SuccessGreen else DangerRed, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) { Text(merchant, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1); Text(if (approved) "Cart & Equipment" else "Consumer Electronics", color = SilverAccent, fontSize = 12.sp) }
                    Column(horizontalAlignment = Alignment.End) { Text(formatInr(amount), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold); Text(if (approved) "Approved" else "Category Mismatch", color = if (approved) SuccessGreen else DangerRed, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
fun FlippableTrustCard(balance: Double, status: String) {
    var flipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (flipped) 180f else 0f, animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing), label = "")
    val cardTint = when(status.lowercase()) { "prime", "gold" -> PrimaryGold; "trusted", "silver" -> SuccessGreen; else -> InfoBlue }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth().height(210.dp).clickable { flipped = !flipped }.graphicsLayer { rotationY = rotation; cameraDistance = 12f * density }) {
            if (rotation <= 90f) {
                Box(modifier = Modifier.fillMaxSize().shadow(16.dp, RoundedCornerShape(20.dp), spotColor = cardTint.copy(alpha = 0.4f)).clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(cardTint.copy(alpha=0.8f), cardTint.copy(alpha=0.4f)))).padding(1.dp)) {
                    Column(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(19.dp)).background(BgBlack).padding(24.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) { Column { Text("CREDENCE", color = Color.White, fontSize = 14.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Black); Text("TRUST CARD", color = cardTint, fontSize = 9.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold) }; Icon(Icons.Default.Contactless, contentDescription = "NFC", tint = Color.White, modifier = Modifier.size(24.dp)) }
                        Spacer(modifier = Modifier.weight(1f))
                        Text("••••  ••••  ••••  4821", color = Color.White, fontSize = 22.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) { Column { Text("RESTRICTED TO", color = SilverAccent, fontSize = 9.sp, letterSpacing = 0.5.sp); Text("Cart & Equipment", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }; Column(horizontalAlignment = Alignment.End) { Text("BALANCE", color = SilverAccent, fontSize = 9.sp, letterSpacing = 0.5.sp); Text(formatInr(balance), color = cardTint, fontSize = 18.sp, fontWeight = FontWeight.Bold) } }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }.clip(RoundedCornerShape(20.dp)).background(Color.White).padding(20.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(110.dp).background(Color.White), contentAlignment = Alignment.Center) {
                            val rng = remember { Random(42) }
                            Canvas(modifier = Modifier.size(100.dp)) {
                                val cell = size.width / 11
                                val drawAlignmentSquare = { x: Float, y: Float -> drawRoundRect(color = Color.Black, topLeft = Offset(x, y), size = Size(cell*3, cell*3), cornerRadius = CornerRadius(4f)); drawRoundRect(color = Color.White, topLeft = Offset(x+cell*0.5f, y+cell*0.5f), size = Size(cell*2, cell*2), cornerRadius = CornerRadius(2f)); drawRoundRect(color = Color.Black, topLeft = Offset(x+cell, y+cell), size = Size(cell, cell), cornerRadius = CornerRadius(2f)) }
                                drawAlignmentSquare(0f, 0f); drawAlignmentSquare(size.width - cell*3, 0f); drawAlignmentSquare(0f, size.height - cell*3)
                                for (r in 0 until 11) for (c in 0 until 11) { val inTopLeft = r < 4 && c < 4; val inTopRight = r < 4 && c > 6; val inBottomLeft = r > 6 && c < 4; if (!inTopLeft && !inTopRight && !inBottomLeft && rng.nextBoolean()) drawRoundRect(color = Color.Black, topLeft = Offset(c * cell, r * cell), size = Size(cell * 0.8f, cell * 0.8f), cornerRadius = CornerRadius(2f)) }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Scan to pay restricted merchant", color = BgBlack, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = SilverAccent, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(6.dp)); Text(if (rotation <= 90f) "Tap card to flip to QR code" else "Tap card to flip to NFC", color = SilverAccent, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
    }
}