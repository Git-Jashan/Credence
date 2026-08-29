package com.jsingh.credence.ui.components

import android.content.Intent
import android.net.Uri
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

// 100% Local Colors
private val BgBlack = Color(0xFF09090B)
private val CardDark = Color(0xFF18181B)
private val PrimaryGold = Color(0xFFEAB308)
private val SilverAccent = Color(0xFFA1A1AA)
private val DangerRed = Color(0xFFEF4444)
private val SuccessGreen = Color(0xFF10B981)
private val InfoBlue = Color(0xFF3B82F6)

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

    // Action States
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showCardSecurityDialog by remember { mutableStateOf(false) }
    var isGeneratingPdf by remember { mutableStateOf(false) }

    ModalDrawerSheet(
        drawerContainerColor = CardDark,
        modifier = Modifier.width(320.dp) // Slightly wider for better text layout
    ) {
        // ==================================================
        // 1. VERIFIED HEADER
        // ==================================================
        Box(modifier = Modifier.fillMaxWidth().background(BgBlack).padding(24.dp)) {
            Column(modifier = Modifier.padding(top = 24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier.size(60.dp).clip(RoundedCornerShape(14.dp)).background(PrimaryGold.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val initial = if (userName.isNotBlank()) userName.take(1).uppercase() else "U"
                        Text(initial, color = PrimaryGold, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                    // Verified Badge
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(SuccessGreen.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("VERIFIED ID", color = SuccessGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(userName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(businessType, color = PrimaryGold, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))
                val maskedAcc = if (accountNumber.length > 4) accountNumber.takeLast(4) else accountNumber
                Text("Linked A/C: •••• $maskedAcc", color = SilverAccent, fontSize = 12.sp)
            }
        }

        // ==================================================
        // 2. ACTIONABLE MENU ITEMS
        // ==================================================
        Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {

            Text("DATA & EXPORT", color = SilverAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))

            // Generate PDF Action
            ActionMenuItem(
                icon = Icons.Default.PictureAsPdf,
                title = "Export Trust Certificate",
                subtitle = "Download PDF for offline agents",
                isLoading = isGeneratingPdf,
                onClick = {
                    isGeneratingPdf = true
                    coroutineScope.launch {
                        delay(1500) // Simulate PDF generation delay
                        isGeneratingPdf = false
                        Toast.makeText(context, "Certificate saved to Downloads folder", Toast.LENGTH_LONG).show()
                        onClose()
                    }
                }
            )

            // Data Sync Action
            ActionMenuItem(
                icon = Icons.Default.Sync,
                title = "Update Bank Statement",
                subtitle = "Sync latest month to boost score",
                onClick = {
                    Toast.makeText(context, "Please use the 'Wipe Data' option to start a fresh upload.", Toast.LENGTH_LONG).show()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("CARD & PREFERENCES", color = SilverAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))

            // Card Security Action
            ActionMenuItem(
                icon = Icons.Default.CreditCardOff,
                title = "Trust Card Security",
                subtitle = "Freeze card or view limits",
                onClick = { showCardSecurityDialog = true }
            )

            // Language Switcher Action
            ActionMenuItem(
                icon = Icons.Default.Language,
                title = "App Language",
                subtitle = "English (Change to हिन्दी, etc.)",
                onClick = { showLanguageDialog = true }
            )

            // Support Action
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

            // Danger Zone
            Box(
                modifier = Modifier.clickable { onResetApp() }.fillMaxWidth().padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "Wipe Data", tint = DangerRed)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Wipe Local Data", color = DangerRed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Delete statement & reset app", color = SilverAccent, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    // ==================================================
    // INTERACTIVE DIALOGS
    // ==================================================

    // 1. Language Selection Dialog
    if (showLanguageDialog) {
        var selectedLang by remember { mutableStateOf("English") }
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            containerColor = CardDark,
            title = { Text("Select Language", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf("English", "हिन्दी (Hindi)", "मराठी (Marathi)", "தமிழ் (Tamil)").forEach { lang ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { selectedLang = lang }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedLang == lang,
                                onClick = { selectedLang = lang },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryGold, unselectedColor = SilverAccent)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(lang, color = if (selectedLang == lang) Color.White else SilverAccent, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLanguageDialog = false
                        Toast.makeText(context, "Language set to $selectedLang", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = BgBlack)
                ) { Text("Apply", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text("Cancel", color = SilverAccent) }
            }
        )
    }

    // 2. Card Security Dialog (Functional Toggles)
    if (showCardSecurityDialog) {
        var isCardFrozen by remember { mutableStateOf(false) }
        var isOnlineEnabled by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showCardSecurityDialog = false },
            containerColor = CardDark,
            icon = { Icon(Icons.Default.Security, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(32.dp)) },
            title = { Text("Trust Card Security", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Manage restrictions for your NFC scheme disbursement card.", color = SilverAccent, fontSize = 13.sp, lineHeight = 18.sp)
                    Spacer(modifier = Modifier.height(24.dp))

                    // Freeze Toggle
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Freeze Card", color = if (isCardFrozen) DangerRed else Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Temporarily block all NFC and QR payments.", color = SilverAccent, fontSize = 12.sp)
                        }
                        Switch(
                            checked = isCardFrozen,
                            onCheckedChange = { isCardFrozen = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = DangerRed, uncheckedThumbColor = SilverAccent, uncheckedTrackColor = Color(0xFF27272A))
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFF27272A))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Online Payments Toggle
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Online Transactions", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Allow card usage on verified vendor websites.", color = SilverAccent, fontSize = 12.sp)
                        }
                        Switch(
                            checked = isOnlineEnabled,
                            onCheckedChange = { isOnlineEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = BgBlack, checkedTrackColor = PrimaryGold, uncheckedThumbColor = SilverAccent, uncheckedTrackColor = Color(0xFF27272A))
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showCardSecurityDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = BgBlack)
                ) { Text("Done", fontWeight = FontWeight.Bold) }
            }
        )
    }
}

// ✨ Custom Menu Item component with Subtitles and Loading States
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
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF27272A)),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = PrimaryGold, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                Icon(icon, contentDescription = title, tint = PrimaryGold, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = SilverAccent, fontSize = 11.sp)
        }
    }
}