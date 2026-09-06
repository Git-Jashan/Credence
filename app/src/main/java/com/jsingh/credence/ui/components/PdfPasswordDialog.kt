package com.jsingh.credence.ui.components

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val CardDark = Color(0xFF18181B)
private val SilverAccent = Color(0xFFA1A1AA)
private val PrimaryNeon = Color(0xFFEAB308)
private val ErrorRed = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPasswordDialog(
    context: Context,
    pdfUri: Uri,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    val prefs = context.getSharedPreferences("credence_prefs", Context.MODE_PRIVATE)
    val savedPassword = prefs.getString("last_pdf_pass", "") ?: ""

    var passwordInput by remember { mutableStateOf(savedPassword) }
    var rememberPassword by remember { mutableStateOf(savedPassword.isNotEmpty()) }
    var isExtracting by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {
        }
    }

    Dialog(onDismissRequest = { if (!isExtracting) onDismiss() }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(CardDark)
                .border(1.dp, Color(0xFF27272A), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(PrimaryNeon.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Security", tint = PrimaryNeon, modifier = Modifier.size(28.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Encrypted Statement", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Enter your PDF password (usually DOB as DDMMYYYY or PAN)", color = SilverAccent, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it; isError = false },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    isError = isError,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryNeon,
                        unfocusedBorderColor = Color(0xFF3F3F46),
                        cursorColor = PrimaryNeon,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        errorBorderColor = ErrorRed,
                        errorTextColor = ErrorRed
                    ),
                    placeholder = { Text("Password", color = SilverAccent.copy(alpha = 0.5f)) }
                )

                if (isError) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Incorrect password. Try again.", color = ErrorRed, fontSize = 12.sp)
                }

                // REMEMBER PASSWORD CHECKBOX
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberPassword,
                        onCheckedChange = { rememberPassword = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = PrimaryNeon,
                            checkmarkColor = Color.Black,
                            uncheckedColor = SilverAccent
                        )
                    )
                    Text("Remember password for next time", color = SilverAccent, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isExtracting) {
                    CircularProgressIndicator(color = PrimaryNeon, modifier = Modifier.size(40.dp))
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel", color = SilverAccent, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            enabled = passwordInput.isNotEmpty(),
                            onClick = {
                                isExtracting = true

                                if (rememberPassword) {
                                    prefs.edit().putString("last_pdf_pass", passwordInput).apply()
                                } else {
                                    prefs.edit().remove("last_pdf_pass").apply()
                                }

                                coroutineScope.launch {
                                    val resultText = extractTextFromPdf(context, pdfUri, passwordInput.trim())
                                    isExtracting = false
                                    when (resultText) {
                                        "ERROR_WRONG_PASSWORD" -> isError = true
                                        "ERROR_UNKNOWN", "ERROR_FILE_NOT_FOUND" -> {
                                            Toast.makeText(context, "Failed to read PDF file.", Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        }
                                        else -> {
                                            onSuccess(resultText)
                                            onDismiss()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Unlock", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

private suspend fun extractTextFromPdf(context: Context, uri: Uri, password: String): String {
    return withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext "ERROR_FILE_NOT_FOUND"

            inputStream.use { stream ->
                PDDocument.load(stream, password).use { document ->
                    val stripper = PDFTextStripper()
                    stripper.sortByPosition = true
                    stripper.getText(document)
                }
            }
        } catch (e: InvalidPasswordException) {
            "ERROR_WRONG_PASSWORD"
        } catch (e: Exception) {
            Log.e("PDF_ERROR", "Parse Error: ${e.message}", e)
            "ERROR_UNKNOWN"
        }
    }
}