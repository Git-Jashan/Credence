package com.jsingh.credence.ui.dialogs

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Translate
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch

// 100% Local Colors
private val BgBlack = Color(0xFF09090B)
private val CardDark = Color(0xFF18181B)
private val PrimaryGold = Color(0xFFEAB308)
private val SilverAccent = Color(0xFFA1A1AA)

@Composable
fun LanguageDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var selectedLang by remember { mutableStateOf("English") }

    // ✨ FAKE LOCALIZATION ENGINE STATE
    var isApplying by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!isApplying) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CardDark,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF27272A), RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // ✨ PREMIUM HEADER
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(PrimaryGold.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Translate, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("App Language", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Select interface language", color = SilverAccent, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ✨ SELECTABLE LANGUAGE CARDS
                val languages = listOf(
                    Pair("English", "English"),
                    Pair("हिन्दी", "Hindi")
                )

                languages.forEach { (nativeName, englishName) ->
                    val isSelected = selectedLang == nativeName
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) PrimaryGold.copy(alpha = 0.1f) else BgBlack)
                            .border(1.dp, if (isSelected) PrimaryGold.copy(alpha = 0.5f) else Color(0xFF27272A), RoundedCornerShape(16.dp))
                            .clickable {
                                if (!isApplying) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedLang = nativeName
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(nativeName, color = if (isSelected) PrimaryGold else Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(englishName, color = SilverAccent, fontSize = 12.sp)
                        }

                        // Custom Radio Checkmark
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(22.dp))
                        } else {
                            Box(modifier = Modifier.size(20.dp).clip(CircleShape).border(2.dp, Color(0xFF3F3F46), CircleShape))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ✨ ACTION BUTTONS WITH FAKE LOADING
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isApplying,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Cancel", color = SilverAccent, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = {
                            if (!isApplying) {
                                scope.launch {
                                    isApplying = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    kotlinx.coroutines.delay(1200) // ✨ Fake UI reload for language pack
                                    isApplying = false

                                    val toastMsg = if (selectedLang == "हिन्दी") "भाषा हिन्दी में बदल दी गई है" else "Language set to English"
                                    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            }
                        },
                        enabled = !isApplying,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = BgBlack, disabledContainerColor = PrimaryGold),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        if (isApplying) {
                            CircularProgressIndicator(color = BgBlack, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            // Dynamic loading text!
                            Text(if (selectedLang == "हिन्दी") "लागू हो रहा है..." else "Applying...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        } else {
                            Text(if (selectedLang == "हिन्दी") "लागू करें (Apply)" else "Apply", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}