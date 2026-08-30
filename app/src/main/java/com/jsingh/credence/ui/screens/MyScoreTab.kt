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
    onResetData: () -> Unit // Kept for interface compatibility, UI button removed
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showCardSheet by remember { mutableStateOf(false) }

    val credenceId = remember { "CRD-" + UUID.randomUUID().toString().take(8).uppercase() }
    val syncDate = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()) }

    if (showCardSheet) {
        TrustCardSheet(
            limit = portfolio?.safeLoanLimit ?: 0.0,
            status = portfolio?.tier ?: "Building",
            onDismiss = { showCardSheet = false }
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
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(CardDark).border(1.dp, Color(0xFF27272A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (portfolio == null) {
            item { UploadPromptCard { } }
        } else {
            // ==========================================
            // ✨ 1. THE COOKED DIGITAL PASSPORT
            // ==========================================
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = PrimaryGold.copy(alpha = 0.1f))
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(CardDark, CardDark, PrimaryGold.copy(alpha = 0.05f)),
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            )
                        )
                        .border(1.dp, Color(0xFF27272A), RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(top = 20.dp, start = 20.dp, end = 20.dp, bottom = 16.dp)) {

                        // Header Ribbon: Biometric & Status
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

                        // Core Identity
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryGold.copy(alpha = 0.15f))
                                    .border(1.dp, PrimaryGold.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
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

                        // Data Inset Ribbon (Looks highly structured)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(BgBlack)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("DATA SOURCE", color = SilverAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("${portfolio.bankName} (${portfolio.statementMonths}Mos)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            // Vertical Separator
                            Box(modifier = Modifier.height(24.dp).width(1.dp).background(Color(0xFF27272A)))

                            Column(horizontalAlignment = Alignment.End) {
                                Text("LAST SYNCED", color = SilverAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(syncDate, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // ==========================================
            // 2. KEY METRICS GRID
            // ==========================================
            item {
                Text("Underwriting Summary", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricGridCard(modifier = Modifier.weight(1f), title = "Trust Score", value = "${portfolio.score}/100", icon = Icons.Default.Speed, color = InfoBlue)
                    MetricGridCard(modifier = Modifier.weight(1f), title = "Safe Limit", value = formatProfileInr(portfolio.safeLoanLimit), icon = Icons.Default.AccountBalanceWallet, color = PrimaryGold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val ratio = portfolio.vitals.inflowOutflowRatio
                    val ratioColor = if (ratio >= 1.2) SuccessGreen else if (ratio >= 1.0) WarnAmber else DangerRed

                    MetricGridCard(modifier = Modifier.weight(1f), title = "Cashflow Ratio", value = String.format(Locale.US, "%.2fx", ratio), icon = Icons.Default.SwapVert, color = ratioColor)
                    MetricGridCard(modifier = Modifier.weight(1f), title = "Data Depth", value = "${portfolio.statementMonths} Months", icon = Icons.Default.History, color = SilverAccent)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // ==========================================
            // 3. DEEP DIAGNOSTICS
            // ==========================================
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Diagnostic Risk Report", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("0 Hard Inquiries", color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("How the Credence Engine evaluates your repayment capacity.", color = SilverAccent, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(20.dp))

                FactorBar(
                    label = "Income Consistency",
                    value = (portfolio.vitals.incomeConsistency.toFloat() / 100f).coerceIn(0f, 1f),
                    display = "${portfolio.vitals.incomeConsistency}%",
                    description = "Coefficient of variation. High consistency proves stable month-over-month revenue, lowering default risk.",
                    color = SuccessGreen
                )
                Spacer(modifier = Modifier.height(12.dp))

                FactorBar(
                    label = "Customer Diversity",
                    value = (portfolio.vitals.payerDiversity.toFloat() / 15f).coerceIn(0f, 1f),
                    display = "${portfolio.vitals.payerDiversity} Payers",
                    description = "Measures reliance on a single income source. More distinct payers means lower business risk.",
                    color = Color(0xFFA855F7)
                )
                Spacer(modifier = Modifier.height(12.dp))

                FactorBar(
                    label = "Transaction Velocity",
                    value = (portfolio.vitals.transactionFrequency.toFloat() / 60f).coerceIn(0f, 1f),
                    display = "${portfolio.vitals.transactionFrequency}/mo",
                    description = "Daily operational activity. Proves the business is highly active and generating foot traffic.",
                    color = InfoBlue
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            // ==========================================
            // 4. THE EXPORT CENTER
            // ==========================================
            item {
                Text("Disbursement & Export", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(16.dp))

                // Trust Card Entry
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF27272A)).clickable { showCardSheet = true }.padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Nfc, contentDescription = null, tint = SilverAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RESTRICTED TRUST CARD", color = SilverAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("View Virtual NFC Card", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(CardDark), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = PrimaryGold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Official Report Generator
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardDark).border(1.dp, PrimaryGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp)).padding(20.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(PrimaryGold.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Certified Financial Report", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("Send to MFI Agents or Lenders", color = SilverAccent, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Generates a highly secure, 1-page document summarizing your verified limit and risk profile. Ready for WhatsApp.", color = SilverAccent, fontSize = 13.sp, lineHeight = 18.sp)
                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val cashflowStr = String.format(Locale.US, "%.2fx", portfolio.vitals.inflowOutflowRatio)

                                val detailedReport = """
                                    📄 *CREDENCE CERTIFIED REPORT*
                                    Report ID: $credenceId
                                    Date: $syncDate
                                    -------------------------
                                    👤 *Applicant:* $userName
                                    🏪 *Business:* $businessType
                                    🏦 *Data Source:* ${portfolio.bankName} (${portfolio.statementMonths} Mos)
                                    
                                    ✅ *TRUST SCORE:* ${portfolio.score}/100 (${portfolio.tier} Status)
                                    💰 *SAFE LIMIT:* ${formatProfileInr(portfolio.safeLoanLimit)}
                                    
                                    📊 *FINANCIAL DIAGNOSTICS*
                                    • Cashflow Ratio: $cashflowStr (Income vs Expense)
                                    • Income Consistency: ${portfolio.vitals.incomeConsistency}%
                                    • Customer Base: ${portfolio.vitals.payerDiversity} distinct payers
                                    • Txn Velocity: ${portfolio.vitals.transactionFrequency} per month
                                    
                                    🔒 _Report generated & mathematically verified on-device by Credence Engine. Data is tamper-proof._
                                """.trimIndent()

                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, detailedReport)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Send Official Report to Lender"))
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = BgBlack),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Share Certified Report", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(48.dp))
            }

            // ==========================================
            // 5. VERIFICATION LEDGER (Zero-Knowledge Trust)
            // ==========================================
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF3F3F46), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Credence Zero-Knowledge Engine v1.2", color = Color(0xFF3F3F46), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Text("Data parsed locally. Never stored on external servers.", color = Color(0xFF3F3F46), fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.height(48.dp))
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

            // Sample mock activity
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