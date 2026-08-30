package com.jsingh.credence.ui.dialogs

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CardDark = Color(0xFF18181B)
private val SilverAccent = Color(0xFFA1A1AA)
private val DangerRed = Color(0xFFEF4444)

@Composable
fun WipeDataConfirmDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(32.dp)) },
        title = { Text("Wipe Financial Data?", color = Color.White, fontWeight = FontWeight.Bold) },
        text = { Text("This will instantly delete all parsed statement data and reset your Trust Score. This action cannot be undone.", color = SilverAccent, fontSize = 13.sp) },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onConfirm()
                },
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White)
            ) { Text("Wipe Data", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SilverAccent) }
        }
    )
}