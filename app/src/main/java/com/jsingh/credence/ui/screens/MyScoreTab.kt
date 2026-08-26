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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsingh.credence.domain.models.TrustPortfolio
import kotlin.random.Random

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
            item { UploadPromptCard { } }
        } else {
            item {
                Text("Factor Breakdown", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(24.dp))

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
                            Column { Text("Projected Score", color = SilverAccent, fontSize = 11.sp); Text("$projectedScore", color = if (consistencySlider > 0) SuccessGreen else Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold) }
                            Column(horizontalAlignment = Alignment.End) { Text("New Loan Limit", color = SilverAccent, fontSize = 11.sp); Text(formatInr(projectedLimit), color = if (consistencySlider > 0) SuccessGreen else Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold) }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(value = consistencySlider, onValueChange = { consistencySlider = it }, valueRange = 0f..1f, colors = SliderDefaults.colors(thumbColor = InfoBlue, activeTrackColor = InfoBlue, inactiveTrackColor = CardDark))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                FactorBar("Income Consistency", (portfolio.vitals.incomeConsistency.toFloat() / 100f).coerceIn(0f, 1f), "${portfolio.vitals.incomeConsistency}%", SuccessGreen)
                Spacer(modifier = Modifier.height(10.dp))
                FactorBar("Transaction Frequency", (portfolio.vitals.transactionFrequency.toFloat() / 60f).coerceIn(0f, 1f), "${portfolio.vitals.transactionFrequency}/mo", InfoBlue)
                Spacer(modifier = Modifier.height(10.dp))
                FactorBar("Payer Diversity", (portfolio.vitals.payerDiversity.toFloat() / 10f).coerceIn(0f, 1f), "${portfolio.vitals.payerDiversity} payers", Color(0xFFA855F7))
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Text("Trust Card", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Tap at a registered merchant — spending is auto-restricted to your approved category.", color = SilverAccent, fontSize = 12.sp, lineHeight = 17.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF27272A)).clickable { showCardSheet = true }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("APPROVED CATEGORY", color = SilverAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("Cart & Equipment Suppliers", color = PrimaryGold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text("View card →", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val sendIntent = Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, "I have a Credence Trust Score of ${portfolio.score} (${portfolio.tier} tier), pre-approved for ${formatInr(portfolio.safeLoanLimit)}."); type = "text/plain" }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Trust Profile"))
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = BgBlack), shape = RoundedCornerShape(12.dp)
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
            Row(modifier = Modifier.fillMaxWidth().clickable { onResetData() }.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = SilverAccent, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column { Text("Wipe Local Data", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium); Text("Deletes your parsed statement data", color = SilverAccent, fontSize = 12.sp) }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun FactorBar(label: String, value: Float, display: String, color: Color) {
    val animatedProgress by animateFloatAsState(targetValue = value.coerceIn(0f, 1f), animationSpec = tween(1200, easing = LinearOutSlowInEasing), label = "factorBar")
    val alphaPulse by rememberInfiniteTransition(label = "").animateFloat(initialValue = 0.6f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "")

    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(CardDark).border(1.dp, Color(0xFF27272A), RoundedCornerShape(14.dp)).padding(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = SilverAccent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(display, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(BgBlack)) {
            Box(modifier = Modifier.fillMaxWidth(animatedProgress).height(8.dp).clip(RoundedCornerShape(4.dp)).background(if (value > 0.75f) color.copy(alpha = alphaPulse) else color))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrustCardSheet(limit: Double, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = CardDark) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text("Trust Card", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Purpose-restricted scheme disbursement", color = SilverAccent, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(24.dp))
            FlippableTrustCard(balance = limit)
            Spacer(modifier = Modifier.height(32.dp))
            Text("Recent Activity", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            sampleCardActivity().forEach { txn ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF27272A)), contentAlignment = Alignment.Center) {
                        Icon(if (txn.approved) Icons.Default.CheckCircle else Icons.Default.Close, contentDescription = null, tint = if (txn.approved) SuccessGreen else DangerRed, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) { Text(txn.merchant, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1); Text("${txn.category} · ${txn.time}", color = SilverAccent, fontSize = 11.sp) }
                    Column(horizontalAlignment = Alignment.End) { Text(formatInr(txn.amount), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold); Text(if (txn.approved) "Approved" else "Declined", color = if (txn.approved) SuccessGreen else DangerRed, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
fun FlippableTrustCard(balance: Double) {
    var flipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (flipped) 180f else 0f, animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing), label = "")

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp).clickable { flipped = !flipped }.graphicsLayer { rotationY = rotation; cameraDistance = 12f * density }) {
            if (rotation <= 90f) {
                Box(modifier = Modifier.fillMaxSize().shadow(16.dp, RoundedCornerShape(20.dp), spotColor = PrimaryGold.copy(alpha = 0.4f)).clip(RoundedCornerShape(20.dp)).background(GoldGradient).padding(1.dp)) {
                    Column(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(19.dp)).background(BgBlack).padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("CREDENCE TRUST CARD", color = SilverAccent, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.CheckCircle, contentDescription = "NFC", tint = SuccessGreen, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text("•••• •••• •••• 4821", color = Color.White, fontSize = 20.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            Column { Text("RESTRICTED TO", color = SilverAccent, fontSize = 9.sp); Text("Cart & Equipment", color = PrimaryGold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                            Column(horizontalAlignment = Alignment.End) { Text("BALANCE", color = SilverAccent, fontSize = 9.sp); Text(formatInr(balance), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }.clip(RoundedCornerShape(20.dp)).background(CardDark).border(1.dp, Color(0xFF27272A), RoundedCornerShape(20.dp)).padding(20.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                            val rng = remember { Random(42) }
                            Canvas(modifier = Modifier.size(80.dp)) {
                                val cell = size.width / 7
                                for (r in 0 until 7) {
                                    for (c in 0 until 7) {
                                        if ((r < 3 && c < 3) || (r < 3 && c >= 4) || (r >= 4 && c < 3) || rng.nextBoolean()) {
                                            drawRect(color = DangerRed, topLeft = Offset(c * cell, r * cell), size = Size(cell * 0.9f, cell * 0.9f))
                                        }
                                    }
                                }
                            }
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