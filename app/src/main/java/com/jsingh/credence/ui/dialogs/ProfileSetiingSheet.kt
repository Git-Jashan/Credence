package com.jsingh.credence.ui.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BgBlack = Color(0xFF09090B)
private val CardDark = Color(0xFF18181B)
private val SilverAccent = Color(0xFFA1A1AA)
private val DangerRed = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsSheet(
    userName: String,
    businessType: String,
    onDismiss: () -> Unit,
    onSignOutClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = CardDark) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text("Account Settings", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            // Account Info Readout
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BgBlack).border(1.dp, Color(0xFF27272A), RoundedCornerShape(12.dp)).padding(16.dp)) {
                Column {
                    Text("ACCOUNT OWNER", color = SilverAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(userName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("BUSINESS TYPE", color = SilverAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(businessType, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Sign Out / Wipe Button
            Button(
                onClick = {
                    onDismiss() // Close sheet first
                    onSignOutClick() // Then trigger the dialog
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed.copy(alpha = 0.15f), contentColor = DangerRed),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.3f))
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out & Clear Data", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}