package com.jsingh.credence.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BgBlack = Color(0xFF09090B)
private val CardDark = Color(0xFF18181B)
private val PrimaryGold = Color(0xFFEAB308)
private val SilverAccent = Color(0xFFA1A1AA)
private val DangerRed = Color(0xFFEF4444)
private val InfoBlue = Color(0xFF3B82F6)

@Composable
fun CardSecurityDialog(onDismiss: () -> Unit) {
    var isCardFrozen by remember { mutableStateOf(false) }
    var isOnlineEnabled by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        icon = { Icon(Icons.Default.Security, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(32.dp)) },
        title = { Text("Trust Card Security", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Manage restrictions for your NFC scheme disbursement card.", color = SilverAccent, fontSize = 13.sp, lineHeight = 18.sp)
                Spacer(modifier = Modifier.height(24.dp))

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
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = BgBlack)
            ) { Text("Done", fontWeight = FontWeight.Bold) }
        }
    )
}