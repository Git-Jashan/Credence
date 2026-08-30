package com.jsingh.credence.ui.dialogs

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BgBlack = Color(0xFF09090B)
private val CardDark = Color(0xFF18181B)
private val PrimaryGold = Color(0xFFEAB308)
private val SilverAccent = Color(0xFFA1A1AA)

@Composable
fun LanguageDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var selectedLang by remember { mutableStateOf("English") }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                    Toast.makeText(context, "Language set to $selectedLang", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = BgBlack)
            ) { Text("Apply", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SilverAccent) }
        }
    )
}