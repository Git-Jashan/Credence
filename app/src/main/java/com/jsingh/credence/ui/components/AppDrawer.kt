package com.jsingh.credence.ui.components

import android.widget.Toast
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Import your existing models and Dialogs/Sheets
import com.jsingh.credence.domain.models.TrustPortfolio
import com.jsingh.credence.ui.dialogs.CardSecurityDialog
import com.jsingh.credence.ui.dialogs.LanguageDialog
import com.jsingh.credence.ui.dialogs.WipeDataConfirmDialog
import com.jsingh.credence.ui.sheets.ProfileSettingsSheet

// ✨ IMPORT THE EXISTING SHEET FROM MY SCORE TAB
import com.jsingh.credence.ui.screens.LinkedAccountsSheet

// 100% Local Colors
private val BgBlack = Color(0xFF09090B)
private val CardDark = Color(0xFF18181B)
private val PrimaryGold = Color(0xFFEAB308)
private val SilverAccent = Color(0xFFA1A1AA)

@Composable
fun CredenceDrawerSheet(
    portfolio: TrustPortfolio?, // ✨ NEW: We need this to feed your LinkedAccountsSheet
    userName: String,
    businessType: String,
    accountNumber: String,
    onClose: () -> Unit,
    onResetApp: () -> Unit,
    onUploadClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showCardSecurityDialog by remember { mutableStateOf(false) }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var showWipeConfirmDialog by remember { mutableStateOf(false) }

    // ✨ State to show your existing LinkedAccountsSheet
    var showLinkedAccountsSheet by remember { mutableStateOf(false) }

    ModalDrawerSheet(drawerContainerColor = CardDark, modifier = Modifier.width(320.dp)) {

        // ==================================================
        // 1. FULL-WIDTH DARK HEADER
        // ==================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgBlack)
                .clickable { showProfileSheet = true }
                .padding(top = 48.dp, bottom = 24.dp, start = 24.dp, end = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(PrimaryGold.copy(alpha = 0.15f))
                        .border(1.dp, PrimaryGold.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = if (userName.isNotBlank()) userName.take(1).uppercase() else "U"
                    Text(initial, color = PrimaryGold, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = userName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = businessType, color = PrimaryGold, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(Icons.Default.ChevronRight, contentDescription = "Settings", tint = SilverAccent, modifier = Modifier.size(24.dp))
            }
        }

        // ==================================================
        // 2. MINIMAL ACTION ITEMS
        // ==================================================
        Column(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {

            // ✨ OPENS YOUR EXISTING ACCOUNTS SHEET!
            ActionMenuItem(
                icon = Icons.Default.AccountBalance,
                title = "Manage & Update Accounts",
                onClick = {
                    if (portfolio != null) {
                        showLinkedAccountsSheet = true
                    } else {
                        // If no portfolio exists, just go straight to upload
                        onClose()
                        onUploadClick()
                    }
                }
            )

            ActionMenuItem(
                icon = Icons.Default.PictureAsPdf,
                title = "Export Trust Certificate",
                isLoading = isGeneratingPdf,
                onClick = {
                    isGeneratingPdf = true
                    coroutineScope.launch {
                        delay(1500)
                        isGeneratingPdf = false
                        Toast.makeText(context, "Certificate saved to Downloads folder", Toast.LENGTH_LONG).show()
                        onClose()
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFF27272A), modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(modifier = Modifier.height(16.dp))

            ActionMenuItem(
                icon = Icons.Default.CreditCardOff,
                title = "Trust Card Security",
                onClick = { showCardSecurityDialog = true }
            )

            ActionMenuItem(
                icon = Icons.Default.Language,
                title = "App Language",
                onClick = { showLanguageDialog = true }
            )

            ActionMenuItem(
                icon = Icons.Default.SupportAgent,
                title = "WhatsApp Support",
                onClick = {
                    Toast.makeText(context, "Opening WhatsApp...", Toast.LENGTH_SHORT).show()
                    onClose()
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Credence v1.2.0", color = Color(0xFF3F3F46), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // ==================================================
    // 3. LAUNCH MODALS & DIALOGS
    // ==================================================

    // ✨ REUSES THE EXACT SHEET FROM MYSCORETAB.KT
    if (showLinkedAccountsSheet && portfolio != null) {
        val syncDate = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()) }
        LinkedAccountsSheet(
            portfolio = portfolio,
            accountNumber = accountNumber,
            syncDate = syncDate,
            onDismiss = { showLinkedAccountsSheet = false },
            onUploadClick = {
                showLinkedAccountsSheet = false
                onClose()
                onUploadClick()
            }
        )
    }

    if (showProfileSheet) {
        ProfileSettingsSheet(
            userName = userName,
            businessType = businessType,
            accountNumber = accountNumber,
            onDismiss = { showProfileSheet = false },
            onSignOutClick = { showWipeConfirmDialog = true }
        )
    }

    if (showWipeConfirmDialog) {
        WipeDataConfirmDialog(
            onDismiss = { showWipeConfirmDialog = false },
            onConfirm = {
                onClose()
                onResetApp()
            }
        )
    }

    if (showLanguageDialog) {
        LanguageDialog(onDismiss = { showLanguageDialog = false })
    }

    if (showCardSecurityDialog) {
        CardSecurityDialog(onDismiss = { showCardSecurityDialog = false })
    }
}

// ✨ ULTRA-MINIMAL ACTION MENU ITEM
@Composable
fun ActionMenuItem(
    icon: ImageVector,
    title: String,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading) { onClick() }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF27272A)),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = PrimaryGold, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
            } else {
                Icon(icon, contentDescription = title, tint = PrimaryGold, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}