package com.jsingh.credence.ui.components

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PdfPasswordDialog(
    context: Context,
    pdfUri: Uri,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    var passwordInput by remember { mutableStateOf("") }
    var isExtracting by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isExtracting) onDismiss() },
        title = { Text("Encrypted Statement") },
        text = {
            Column {
                Text("Enter password (usually DOB or Account Number):")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it; isError = false },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    isError = isError
                )
                if (isError) Text("Wrong password", color = MaterialTheme.colorScheme.error)
                if (isExtracting) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
            }
        },
        confirmButton = {
            TextButton(
                enabled = passwordInput.isNotEmpty() && !isExtracting,
                onClick = {
                    isExtracting = true
                    coroutineScope.launch {
                        val resultText = extractTextFromPdf(context, pdfUri, passwordInput.trim())
                        isExtracting = false
                        when (resultText) {
                            "ERROR_WRONG_PASSWORD" -> isError = true
                            "ERROR_UNKNOWN", "ERROR_FILE_NOT_FOUND" -> {
                                Toast.makeText(context, "Failed to read PDF", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                            else -> {
                                Toast.makeText(context, "Statement Unlocked!", Toast.LENGTH_SHORT).show()
                                onSuccess(resultText)
                                onDismiss()
                            }
                        }
                    }
                }
            ) { Text("Unlock") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isExtracting) { Text("Cancel") }
        }
    )
}

private suspend fun extractTextFromPdf(context: Context, uri: Uri, password: String): String {
    return withContext(Dispatchers.IO) {
        try {
            // Break it into simple steps for the Kotlin compiler
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext "ERROR_FILE_NOT_FOUND"

            inputStream.use { stream ->
                PDDocument.load(stream, password).use { document ->
                    val stripper = PDFTextStripper()
                    stripper.sortByPosition = true
                    // Explicitly return the text from the document
                    stripper.getText(document)
                }
            }
        } catch (e: InvalidPasswordException) {
            "ERROR_WRONG_PASSWORD"
        } catch (e: Exception) {
            Log.e("PDF_ERROR", "Parse Error", e)
            "ERROR_UNKNOWN"
        }
    }
}