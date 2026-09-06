package com.jsingh.credence.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsingh.credence.ui.components.PdfPasswordDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnboardingFlow(
    incomingUri: Uri?,
    onClearIncomingUri: () -> Unit,
    onFinishOnboarding: (String) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Phase 1: Mobile & OTP State (Name Removed)
    var phoneNumber by remember { mutableStateOf("") }
    var businessType by remember { mutableStateOf("") }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val businessOptions = listOf(
        "Retail & Kirana Store", "Food & Beverage (Cafe/Vendor)",
        "Services, Salon & Repair", "Freelance, Gig & Digital",
        "Wholesale & Distributor", "Manufacturing & Production",
        "Agri-business & Farming", "Transport & Logistics"
    )

    var otpSent by remember { mutableStateOf(false) }
    var otp by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Phase 2: Bank Selection State
    val context = LocalContext.current
    var selectedBank by remember { mutableStateOf<BankConfig?>(null) }
    var uploadMethod by remember { mutableStateOf("whatsapp") }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedBank) {
        uploadMethod = "whatsapp" // Default to WhatsApp when a new bank is selected
    }

    LaunchedEffect(incomingUri) {
        if (incomingUri != null) {
            selectedPdfUri = incomingUri
            showPasswordDialog = true
            step = 2
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedPdfUri = uri
            showPasswordDialog = true
        }
    }

    if (showPasswordDialog && selectedPdfUri != null) {
        PdfPasswordDialog(
            context = context,
            pdfUri = selectedPdfUri!!,
            onDismiss = { showPasswordDialog = false; onClearIncomingUri() },
            onSuccess = { rawText ->
                showPasswordDialog = false
                onClearIncomingUri()
                onFinishOnboarding(rawText)
            }
        )
    }

    Scaffold(containerColor = Color(0xFF09090B)) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Progress Bar
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(2) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (step >= index + 1) Color(0xFFEAB308) else Color(0xFF27272A))
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    slideInHorizontally(animationSpec = tween(500), initialOffsetX = { fullWidth -> fullWidth }) togetherWith
                            slideOutHorizontally(animationSpec = tween(500), targetOffsetX = { fullWidth -> -fullWidth })
                },
                label = "onboarding_flow"
            ) { currentStep ->
                when (currentStep) {
                    1 -> {
                        // ==========================================
                        // PHASE 1: MOBILE & BUSINESS FLOW
                        // ==========================================
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            Text("Welcome to Credence", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Let's secure your account and link your business.", color = Color(0xFFA1A1AA), fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(32.dp))

                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { if (it.length <= 10) phoneNumber = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                placeholder = { Text("Mobile Number", color = Color.DarkGray) },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Gray) },
                                prefix = { Text("+91  ", color = if(otpSent) Color.Gray else Color.White, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = !otpSent,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFEAB308),
                                    unfocusedBorderColor = Color(0xFF27272A),
                                    disabledBorderColor = Color(0xFF27272A),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    disabledTextColor = Color.Gray
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            ExposedDropdownMenuBox(
                                expanded = isDropdownExpanded,
                                onExpandedChange = { if(!otpSent) isDropdownExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = businessType.ifEmpty { "Select Primary Business" },
                                    onValueChange = {},
                                    readOnly = true,
                                    leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, tint = Color.Gray) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    enabled = !otpSent,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFEAB308),
                                        unfocusedBorderColor = Color(0xFF27272A),
                                        disabledBorderColor = Color(0xFF27272A),
                                        focusedTextColor = if (businessType.isEmpty()) Color.DarkGray else Color.White,
                                        disabledTextColor = Color.Gray
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = isDropdownExpanded,
                                    onDismissRequest = { isDropdownExpanded = false },
                                    modifier = Modifier.background(Color(0xFF18181B))
                                ) {
                                    businessOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option, color = Color.White) },
                                            onClick = { businessType = option; isDropdownExpanded = false }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))

                            AnimatedVisibility(
                                visible = otpSent,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column {
                                    HorizontalDivider(color = Color(0xFF27272A))
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text("Enter OTP sent via SMS", color = Color(0xFFA1A1AA), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = otp,
                                        onValueChange = { if (it.length <= 4) otp = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        placeholder = { Text("0000", color = Color.DarkGray) },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 24.sp, letterSpacing = 16.sp, fontWeight = FontWeight.Bold),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFFEAB308),
                                            unfocusedBorderColor = Color(0xFF27272A),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    Spacer(modifier = Modifier.height(32.dp))
                                }
                            }

                            if (!otpSent) {
                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        isLoading = true
                                        coroutineScope.launch { delay(600); isLoading = false; otpSent = true }
                                    },
                                    enabled = phoneNumber.length == 10 && businessType.isNotBlank() && !isLoading,
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB308), disabledContainerColor = Color(0xFF27272A)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    if (isLoading) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                                    else Text("Send Secure OTP", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        isLoading = true
                                        coroutineScope.launch { delay(600); isLoading = false; step = 2 }
                                    },
                                    enabled = otp.length == 4 && !isLoading,
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB308), disabledContainerColor = Color(0xFF27272A)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    if (isLoading) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                                    else Text("Verify & Continue", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                    2 -> {
                        // ==========================================
                        // PHASE 2: BANK IMPORT FLOW
                        // ==========================================
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            Text("Link Primary Account", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Select the primary bank you use for your business.", color = Color(0xFFA1A1AA), fontSize = 14.sp, lineHeight = 20.sp)
                            Spacer(modifier = Modifier.height(32.dp))

                            if (selectedBank == null) {
                                // Upgraded Bank List UI (Cards instead of flat list)
                                SupportedBanks.forEach { bank ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xFF18181B)) // Subtle card background
                                            .clickable { selectedBank = bank }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.size(48.dp).clip(CircleShape).background(bank.brandColor.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = bank.brandColor, modifier = Modifier.size(24.dp))
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(bank.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.weight(1f))
                                        Icon(Icons.Default.ChevronRight, contentDescription = "Select", tint = Color.DarkGray)
                                    }
                                }
                            } else {
                                // Bank Selected Header
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF18181B)).padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(selectedBank!!.brandColor))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(selectedBank!!.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(24.dp))

                                // THE METHOD TOGGLE (WhatsApp vs Manual)
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF18181B)).padding(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (uploadMethod == "whatsapp") Color(0xFF27272A) else Color.Transparent).clickable { uploadMethod = "whatsapp" }.padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) { Text("WhatsApp", color = if (uploadMethod == "whatsapp") Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp) }

                                    Box(
                                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (uploadMethod == "manual") Color(0xFF27272A) else Color.Transparent).clickable { uploadMethod = "manual" }.padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) { Text("Manual PDF", color = if (uploadMethod == "manual") Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // DYNAMIC INSTRUCTIONS & BUTTONS
                                AnimatedContent(targetState = uploadMethod, label = "method_switch") { method ->
                                    if (method == "whatsapp") {
                                        // WHATSAPP UI
                                        Column {
                                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF25D366).copy(alpha = 0.08f)).border(1.dp, Color(0xFF25D366).copy(alpha = 0.3f), RoundedCornerShape(16.dp)).padding(16.dp)) {
                                                Column {
                                                    Text("How WhatsApp fetch works:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    InstructionStep("1", "Tap below to open a pre-typed chat with ${selectedBank!!.name}'s verified bot.", Color(0xFF25D366))
                                                    InstructionStep("2", "Send the message to request your latest 6-month statement.", Color(0xFF25D366))
                                                    InstructionStep("3", "Once received, tap 'Share' on the PDF in WhatsApp and select Credence.", Color(0xFF25D366))
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(24.dp))
                                            Button(
                                                onClick = {
                                                    val uri = Uri.parse("https://wa.me/${selectedBank!!.waNumber}?text=${selectedBank!!.waMsg}")
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                                },
                                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366), contentColor = Color.White),
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Icon(Icons.Default.Chat, contentDescription = "WhatsApp")
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text("Open WhatsApp", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            }
                                        }
                                    } else {
                                        // MANUAL PDF UI
                                        Column {
                                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFEAB308).copy(alpha = 0.08f)).border(1.dp, Color(0xFFEAB308).copy(alpha = 0.3f), RoundedCornerShape(16.dp)).padding(16.dp)) {
                                                Column {
                                                    Text("How to get your PDF:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    InstructionStep("1", "Log into your ${selectedBank!!.name} mobile app or internet banking.", Color(0xFFEAB308))
                                                    InstructionStep("2", "Navigate to 'Services' -> 'Account Statements'.", Color(0xFFEAB308))
                                                    InstructionStep("3", "Download a 6-month statement as a PDF, then tap below to upload it.", Color(0xFFEAB308))
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(24.dp))
                                            Button(
                                                onClick = { pdfPickerLauncher.launch("application/pdf") },
                                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB308), contentColor = Color.Black),
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Icon(Icons.Default.UploadFile, contentDescription = "Upload")
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text("Browse Files", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                                TextButton(onClick = { selectedBank = null }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Change Bank", color = Color.Gray, fontWeight = FontWeight.Medium)
                                }
                                Spacer(modifier = Modifier.height(40.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InstructionStep(number: String, text: String, color: Color) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(bottom = 12.dp)) {
        Box(
            modifier = Modifier.size(22.dp).clip(CircleShape).background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(number, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, color = Color(0xFFA1A1AA), fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 2.dp))
    }
}