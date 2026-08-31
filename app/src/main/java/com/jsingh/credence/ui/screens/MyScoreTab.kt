package com.jsingh.credence.ui.screens

import android.content.Intent
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsingh.credence.domain.models.TrustPortfolio
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.random.Random

private fun formatProfileInr(value: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    return "\u20B9${formatter.format(value.roundToInt())}"
}

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

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Trust Profile", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("Your certified financial identity.", color = SilverAccent, fontSize = 14.sp)
                }
                Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(CardDark).border(1.dp, Color(0xFF27272A), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (portfolio == null) {
            item { UploadPromptCard(onClick = onUploadClick) }
        } else {
            // 1. DIGITAL PASSPORT
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(20.dp), spotColor = PrimaryGold.copy(alpha = 0.1f)).clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(CardDark, CardDark, PrimaryGold.copy(alpha = 0.05f)), Offset(0f, 0f), Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)))
                        .border(1.dp, Color(0xFF27272A), RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(top = 20.dp, start = 20.dp, end = 20.dp, bottom = 16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
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
                                    Text(userName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Default.Verified, contentDescription = "Verified", tint = InfoBlue, modifier = Modifier.size(18.dp))
                                }
                                Text(businessType, color = PrimaryGold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("ID: $credenceId", color = SilverAccent, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BgBlack).padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 2. ACCOUNT MANAGEMENT ROW
            item {
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardDark).border(1.dp, Color(0xFF27272A), RoundedCornerShape(16.dp)).clickable { showLinkedAccountsSheet = true }.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(SuccessGreen.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp)) }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column { Text("Manage Bank Accounts", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold); Text("1 Linked • Active", color = SilverAccent, fontSize = 13.sp) }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SilverAccent)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // 3. KEY METRICS GRID
            item {
                Text("Underwriting Summary", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(CardDark).border(1.dp, Color(0xFF27272A), RoundedCornerShape(12.dp)).padding(16.dp)) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Speed, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(6.dp)); Text("Trust Score", color = SilverAccent, fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("${portfolio.score}/100", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Measures business reliability.", color = SilverAccent, fontSize = 10.sp, lineHeight = 14.sp)
                        }
                    }
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(CardDark).border(1.dp, Color(0xFF27272A), RoundedCornerShape(12.dp)).padding(16.dp)) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(6.dp)); Text("Safe Limit", color = SilverAccent, fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(formatProfileInr(portfolio.safeLoanLimit), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Max capacity via cashflow.", color = SilverAccent, fontSize = 10.sp, lineHeight = 14.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val ratio = portfolio.vitals.inflowOutflowRatio
                    val ratioColor = if (ratio >= 1.2) SuccessGreen else if (ratio >= 1.0) WarnAmber else DangerRed
                    MetricGridCard(modifier = Modifier.weight(1f), title = "Cashflow Ratio", value = String.format(Locale.US, "%.2fx", ratio), icon = Icons.Default.SwapVert, color = ratioColor)
                    MetricGridCard(modifier = Modifier.weight(1f), title = "Data Depth", value = "${portfolio.statementMonths} Mos", icon = Icons.Default.History, color = SilverAccent)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // 4. DIAGNOSTICS
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Diagnostic Risk Report", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("0 Hard Inquiries", color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(20.dp))
                FactorBar("Income Consistency", (portfolio.vitals.incomeConsistency.toFloat() / 100f).coerceIn(0f, 1f), "${portfolio.vitals.incomeConsistency}%", "Stable month-over-month revenue lowers default risk.", SuccessGreen)
                Spacer(modifier = Modifier.height(12.dp))
                FactorBar("Customer Diversity", (portfolio.vitals.payerDiversity.toFloat() / 15f).coerceIn(0f, 1f), "${portfolio.vitals.payerDiversity} Payers", "Distinct payers means lower dependency risk.", Color(0xFFA855F7))
                Spacer(modifier = Modifier.height(12.dp))
                FactorBar("Transaction Velocity", (portfolio.vitals.transactionFrequency.toFloat() / 60f).coerceIn(0f, 1f), "${portfolio.vitals.transactionFrequency}/mo", "Proves the business is highly active.", InfoBlue)
                Spacer(modifier = Modifier.height(32.dp))
            }

            // 5. EXPORT CENTER
            item {
                Text("Disbursement & Export", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF27272A)).clickable { showCardSheet = true }.padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Nfc, contentDescription = null, tint = SilverAccent, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(6.dp)); Text("RESTRICTED TRUST CARD", color = SilverAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("View Virtual NFC Card", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(CardDark), contentAlignment = Alignment.Center) { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = PrimaryGold) }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardDark).border(1.dp, PrimaryGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp)).padding(20.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(PrimaryGold.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(20.dp)) }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column { Text("Certified Financial Report", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold); Text("Send to MFI Agents or Lenders", color = SilverAccent, fontSize = 12.sp) }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Generates a highly secure, 1-page document summarizing your verified limit. Ready for WhatsApp.", color = SilverAccent, fontSize = 13.sp, lineHeight = 18.sp)
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(onClick = { /* Export Logic */ }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = BgBlack), shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Share Certified Report", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
//... (Remaining helper functions like LinkedAccountsSheet, FactorBar remain the same)
// ✨ POP-UP SHEET FOR MANAGING ACCOUNTS
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedAccountsSheet(
    portfolio: TrustPortfolio,
    accountNumber: String,
    syncDate: String,
    onDismiss: () -> Unit,
    onUploadClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = CardDark) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
            Text("Linked Financial Data", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Manage your connected bank accounts and statements.", color = SilverAccent, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(24.dp))

            // Active Bank Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgBlack)
                    .border(1.dp, SuccessGreen.copy(alpha=0.3f), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(SuccessGreen.copy(alpha=0.15f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Primary Account", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                val maskedAcc = if (accountNumber.length > 4) accountNumber.takeLast(4) else accountNumber
                                Text("${portfolio.bankName} •••• $maskedAcc", color = SilverAccent, fontSize = 13.sp)
                            }
                        }
                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(SuccessGreen.copy(alpha=0.15f)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text("ACTIVE", color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFF27272A))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("LAST SYNCED", color = SilverAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(syncDate, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryGold.copy(alpha = 0.1f))
                                .clickable { onUploadClick() }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Update", color = PrimaryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF27272A).copy(alpha=0.4f))
                    .clickable { onUploadClick() }
                    .border(1.dp, Color(0xFF3F3F46), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Link Another Bank Account", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// Helper for the 2x2 Grid
@Composable
fun MetricGridCard(modifier: Modifier = Modifier, title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Box(modifier = modifier.clip(RoundedCornerShape(12.dp)).background(CardDark).border(1.dp, Color(0xFF27272A), RoundedCornerShape(12.dp)).padding(16.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, color = SilverAccent, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// Factor Bar for Diagnostics
@Composable
fun FactorBar(label: String, value: Float, display: String, description: String, color: Color) {
    val animatedProgress by animateFloatAsState(targetValue = value.coerceIn(0f, 1f), animationSpec = tween(1200, easing = LinearOutSlowInEasing), label = "factorBar")

    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(CardDark).border(1.dp, Color(0xFF27272A), RoundedCornerShape(14.dp)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(display, color = color, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(description, color = SilverAccent, fontSize = 12.sp, lineHeight = 16.sp)

        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(BgBlack)) {
            Box(modifier = Modifier.fillMaxWidth(animatedProgress).height(6.dp).clip(RoundedCornerShape(3.dp)).background(color))
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

            val mockTxns = listOf(
                Triple("Raju Cart & Equipment", 3200.0, true),
                Triple("Sunrise Electronics", 1500.0, false),
                Triple("Ganesh Hardware Store", 850.0, true)
            )

            mockTxns.forEach { (merchant, amount, approved) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(if(approved) SuccessGreen.copy(alpha=0.15f) else DangerRed.copy(alpha=0.15f)), contentAlignment = Alignment.Center) {
                        Icon(if (approved) Icons.Default.Check else Icons.Default.Close, contentDescription = null, tint = if (approved) SuccessGreen else DangerRed, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(merchant, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        Text(if (approved) "Cart & Equipment" else "Consumer Electronics", color = SilverAccent, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatProfileInr(amount), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(if (approved) "Approved" else "Category Mismatch", color = if (approved) SuccessGreen else DangerRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FlippableTrustCard(balance: Double, status: String) {
    var flipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (flipped) 180f else 0f, animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing), label = "")

    val cardTint = when(status.lowercase()) {
        "prime", "gold" -> PrimaryGold
        "trusted", "silver" -> SuccessGreen
        else -> InfoBlue
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth().height(210.dp).clickable { flipped = !flipped }.graphicsLayer { rotationY = rotation; cameraDistance = 12f * density }) {
            if (rotation <= 90f) {
                Box(modifier = Modifier.fillMaxSize().shadow(16.dp, RoundedCornerShape(20.dp), spotColor = cardTint.copy(alpha = 0.4f)).clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(cardTint.copy(alpha=0.8f), cardTint.copy(alpha=0.4f)))).padding(1.dp)) {
                    Column(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(19.dp)).background(BgBlack).padding(24.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Column {
                                Text("CREDENCE", color = Color.White, fontSize = 14.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Black)
                                Text("TRUST CARD", color = cardTint, fontSize = 9.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.Default.Contactless, contentDescription = "NFC", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text("••••  ••••  ••••  4821", color = Color.White, fontSize = 22.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            Column { Text("RESTRICTED TO", color = SilverAccent, fontSize = 9.sp, letterSpacing = 0.5.sp); Text("Cart & Equipment", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                            Column(horizontalAlignment = Alignment.End) { Text("BALANCE", color = SilverAccent, fontSize = 9.sp, letterSpacing = 0.5.sp); Text(formatProfileInr(balance), color = cardTint, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }.clip(RoundedCornerShape(20.dp)).background(Color.White).padding(20.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(110.dp).background(Color.White), contentAlignment = Alignment.Center) {
                            val rng = remember { Random(42) }
                            Canvas(modifier = Modifier.size(100.dp)) {
                                val cell = size.width / 11
                                val drawAlignmentSquare = { x: Float, y: Float ->
                                    drawRoundRect(color = Color.Black, topLeft = Offset(x, y), size = Size(cell*3, cell*3), cornerRadius = CornerRadius(4f))
                                    drawRoundRect(color = Color.White, topLeft = Offset(x+cell*0.5f, y+cell*0.5f), size = Size(cell*2, cell*2), cornerRadius = CornerRadius(2f))
                                    drawRoundRect(color = Color.Black, topLeft = Offset(x+cell, y+cell), size = Size(cell, cell), cornerRadius = CornerRadius(2f))
                                }
                                drawAlignmentSquare(0f, 0f)
                                drawAlignmentSquare(size.width - cell*3, 0f)
                                drawAlignmentSquare(0f, size.height - cell*3)

                                for (r in 0 until 11) {
                                    for (c in 0 until 11) {
                                        val inTopLeft = r < 4 && c < 4
                                        val inTopRight = r < 4 && c > 6
                                        val inBottomLeft = r > 6 && c < 4
                                        if (!inTopLeft && !inTopRight && !inBottomLeft && rng.nextBoolean()) {
                                            drawRoundRect(color = Color.Black, topLeft = Offset(c * cell, r * cell), size = Size(cell * 0.8f, cell * 0.8f), cornerRadius = CornerRadius(2f))
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Scan to pay restricted merchant", color = BgBlack, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = SilverAccent, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (rotation <= 90f) "Tap card to flip to QR code" else "Tap card to flip to NFC", color = SilverAccent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}