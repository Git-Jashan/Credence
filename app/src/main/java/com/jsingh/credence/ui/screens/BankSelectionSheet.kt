package com.jsingh.credence.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BankConfig(val name: String, val waNumber: String, val waMsg: String, val brandColor: Color)

val SupportedBanks = listOf(
    BankConfig("HDFC Bank", "917070022222", "Hi", Color(0xFF004C8F)),
    BankConfig("SBI", "919022690226", "Hi", Color(0xFF0077B5)),
    BankConfig("ICICI Bank", "918640086400", "Hi", Color(0xFFF1592A)),
    BankConfig("Axis Bank", "917036165000", "Hi", Color(0xFF97144D))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankSelectionSheet(
    onDismiss: () -> Unit,
    onManualUploadClick: () -> Unit
) {
    val context = LocalContext.current
    var selectedBank by remember { mutableStateOf<BankConfig?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF18181B), // CardDark
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF27272A)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("Select your Bank", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("How would you like to fetch your statement?", color = Color(0xFFA1A1AA), fontSize = 14.sp)

            Spacer(modifier = Modifier.height(24.dp))

            if (selectedBank == null) {
                // STEP 1: CHOOSE BANK
                SupportedBanks.forEach { bank ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedBank = bank }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(bank.brandColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = bank.brandColor)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(bank.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    HorizontalDivider(color = Color(0xFF27272A))
                }
            } else {
                // STEP 2: CHOOSE METHOD (WhatsApp vs Manual)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(selectedBank!!.brandColor))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(selectedBank!!.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val uri = Uri.parse("https://wa.me/${selectedBank!!.waNumber}?text=${selectedBank!!.waMsg}")
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366), contentColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "WhatsApp")
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Fetch via WhatsApp", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onManualUploadClick()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF27272A)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = "Upload", tint = Color(0xFFA1A1AA))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Upload PDF Manually", color = Color(0xFFA1A1AA), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}