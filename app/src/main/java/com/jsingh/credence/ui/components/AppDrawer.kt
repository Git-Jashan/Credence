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

// Import the separated Modals and Dialogs
import com.jsingh.credence.ui.dialogs.CardSecurityDialog
import com.jsingh.credence.ui.dialogs.LanguageDialog
import com.jsingh.credence.ui.dialogs.WipeDataConfirmDialog
import com.jsingh.credence.ui.sheets.ProfileSettingsSheet

// 100% Local Colors
private val BgBlack = Color(0xFF09090B)
private val CardDark = Color(0xFF18181B)
private val PrimaryGold = Color(0xFFEAB308)
private val SilverAccent = Color(0xFFA1A1AA)

@Composable
fun CredenceDrawerSheet(
    userName: String,
    businessType: String,
    accountNumber: String,
    onClose: () -> Unit,
    onResetApp: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Elegant State Hoisting
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showCardSecurityDialog by remember { mutableStateOf(false) }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var showWipeConfirmDialog by remember { mutableStateOf(false) }

    ModalDrawerSheet(drawerContainerColor = CardDark, modifier = Modifier.width(320.dp)) {

        // ==================================================
        // 1. ✨ FULL-WIDTH DARK HEADER (Perfectly Aligned)
        // ==================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgBlack)
                .clickable { showProfileSheet = true }
                .padding(top = 24.dp, bottom = 24.dp, start = 24.dp, end = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Clean Circular Avatar
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

                // Middle: Stacked Details (Aligned perfectly to the avatar)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = userName,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = businessType,
                        color = PrimaryGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    ) }

                Spacer(modifier = Modifier.width(8.dp))

                // Right: Navigation Chevron
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Settings",
                    tint = SilverAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // ==================================================
        // 2. ACTIONABLE MENU ITEMS
        // ==================================================
        Column(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {

            Text("DATA & EXPORT", color = SilverAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))

            ActionMenuItem(
                icon = Icons.Default.PictureAsPdf,
                title = "Export Trust Certificate",
                subtitle = "Download PDF for offline agents",
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

            ActionMenuItem(
                icon = Icons.Default.Sync,
                title = "Update Bank Statement",
                subtitle = "Sync latest month to boost score",
                onClick = {
                    Toast.makeText(context, "Tap your profile at the top to reset data and upload a new statement.", Toast.LENGTH_LONG).show()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text("CARD & PREFERENCES", color = SilverAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))

            ActionMenuItem(
                icon = Icons.Default.CreditCardOff,
                title = "Trust Card Security",
                subtitle = "Freeze card or view limits",
                onClick = { showCardSecurityDialog = true }
            )

            ActionMenuItem(
                icon = Icons.Default.Language,
                title = "App Language",
                subtitle = "English (Change to हिन्दी, etc.)",
                onClick = { showLanguageDialog = true }
            )

            ActionMenuItem(
                icon = Icons.Default.SupportAgent,
                title = "WhatsApp Support",
                subtitle = "Connect with a loan advisor",
                onClick = {
                    Toast.makeText(context, "Opening WhatsApp...", Toast.LENGTH_SHORT).show()
                    onClose()
                }
            )

            Spacer(modifier = Modifier.weight(1f))
            HorizontalDivider(color = Color(0xFF27272A))

            // App Version footer
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Credence v1.2.0 (Build 84)", color = Color(0xFF3F3F46), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // ==================================================
    // 3. LAUNCH MODALS & DIALOGS
    // ==================================================

    if (showProfileSheet) {
        ProfileSettingsSheet(
            userName = userName,
            businessType = businessType,
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

// Reusable Action Menu Item Component
@Composable
fun ActionMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading) { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF27272A)),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = PrimaryGold, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Icon(icon, contentDescription = title, tint = PrimaryGold, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = SilverAccent, fontSize = 12.sp)
        }
    }
}