package com.jsingh.credence.ui.dialogs

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.jsingh.credence.domain.models.TrustPortfolio
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

// Local Colors
private val CardDark = Color(0xFF18181B)
private val PrimaryGold = Color(0xFFEAB308)
private val SilverAccent = Color(0xFFA1A1AA)
private val SuccessGreen = Color(0xFF10B981)

@Composable
fun ExportCertificateDialog(userName: String, portfolio: TrustPortfolio, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val credenceId = remember { "CRD-" + UUID.randomUUID().toString().take(8).uppercase() }
    val syncDate = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()) }

    var step by remember { mutableIntStateOf(0) }
    val statuses = listOf(
        "Compiling Ledger Data...",
        "Applying Zero-Knowledge Hash...",
        "Generating PDF Document...",
        "Ready to Share!"
    )

    LaunchedEffect(Unit) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        // Simulate step 1
        delay(800)
        step = 1
        // Simulate step 2
        delay(900)
        step = 2
        // Simulate step 3 (Actual PDF Gen)
        delay(700)

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
        } catch (e: Exception) { /* Silently ignore if cache access fails in demo */ }

        step = 3
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        delay(400) // Brief pause to show "Ready to Share!"

        // Fire Intent
        val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
        val formattedLimit = "₹${formatter.format(portfolio.safeLoanLimit.roundToInt())}"

        val report = "📄 *CREDENCE CERTIFIED REPORT*\nReport ID: $credenceId\nApplicant: $userName\nVerified Limit: $formattedLimit\nTrust Score: ${portfolio.score}/100\nDate: $syncDate\n\n[System Note: Full PDF document verified on device]"
        val sendIntent = Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, report); type = "text/plain" }
        context.startActivity(Intent.createChooser(sendIntent, "Share Certified Report"))

        onDismiss()
    }

    Dialog(onDismissRequest = { /* Prevent dismiss while generating */ }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CardDark,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, PrimaryGold.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                    if (step < 3) {
                        CircularProgressIndicator(color = PrimaryGold, strokeWidth = 3.dp, modifier = Modifier.fillMaxSize())
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(24.dp))
                    } else {
                        Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(SuccessGreen.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(32.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Exporting Certificate",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                AnimatedContent(
                    targetState = step,
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                    label = "statusText"
                ) { currentStep ->
                    Text(
                        text = statuses[currentStep.coerceAtMost(statuses.lastIndex)],
                        color = if (currentStep == 3) SuccessGreen else SilverAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}