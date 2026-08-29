package com.jsingh.credence.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsingh.credence.domain.models.TrustPortfolio
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt



private enum class CatalogFilter(val label: String) { ALL("All"), SCHEMES("Schemes"), LENDERS("Lenders") }
private enum class TrackerSubTab(val label: String) { ACTIVE("Active Loans"), APPLICATIONS("Applications") }

private fun formatTabInr(value: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    return "\u20B9${formatter.format(value.roundToInt())}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemesAndLendersTab(
    portfolio: TrustPortfolio?,
    onUploadClick: () -> Unit,
    onNavigateToCard: () -> Unit = {}
) {
    val context = LocalContext.current
    var currentSection by remember { mutableIntStateOf(0) } // 0 = Discover, 1 = Track
    var trackerSubTab by remember { mutableStateOf(TrackerSubTab.ACTIVE) } // Sub-toggle inside Track

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(CatalogFilter.ALL) }

    val applicationStage = remember { mutableStateMapOf<String, SchemeStage>() }
    val listings = remember(portfolio) { if (portfolio != null) LoanCatalog.build(portfolio) else emptyList() }
    val filtered = remember(listings, query, filter) {
        listings.filter {
            (filter == CatalogFilter.ALL ||
                    (filter == CatalogFilter.SCHEMES && it.category == ListingCategory.SCHEME) ||
                    (filter == CatalogFilter.LENDERS && it.category == ListingCategory.LENDER)) &&
                    it.title.contains(query, ignoreCase = true)
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Capital Hub", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Discover schemes or manage your portfolio.", color = SilverAccent, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(20.dp))

            // ✨ Primary Master Toggle: Discover vs Track
            Row(modifier = Modifier.fillMaxWidth().background(CardDark, RoundedCornerShape(12.dp)).padding(4.dp)) {
                SegmentTab("Discover", currentSection == 0, Modifier.weight(1f)) { currentSection = 0 }
                SegmentTab("Track", currentSection == 1, Modifier.weight(1f)) { currentSection = 1 }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // ✨ Secondary Sub-Toggle: Only renders if "Track" is selected
            AnimatedVisibility(visible = currentSection == 1) {
                Row(modifier = Modifier.fillMaxWidth().background(BgBlack, RoundedCornerShape(10.dp)).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TrackerSubTab.entries.forEach { subTab ->
                        val isSelected = trackerSubTab == subTab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF27272A) else Color.Transparent)
                                .clickable { trackerSubTab = subTab }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(subTab.label, color = if (isSelected) Color.White else SilverAccent, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                        }
                    }
                }
            }
        }

        if (portfolio == null) {
            item { UploadPromptCard(onUploadClick) }
            return@LazyColumn
        }

        // =====================================
        // SECTION 0: DISCOVER
        // =====================================
        if (currentSection == 0) {
            item {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, placeholder = { Text("Search schemes or lenders", color = SilverAccent) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SilverAccent) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = CardDark, unfocusedContainerColor = CardDark, focusedBorderColor = PrimaryGold, unfocusedBorderColor = Color(0xFF27272A), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CatalogFilter.entries.toTypedArray(), key = { it.name }) { option ->
                        FilterChip(
                            selected = filter == option, onClick = { filter = option },
                            label = { Text(option.label, fontWeight = if (filter == option) FontWeight.Bold else FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryGold, selectedLabelColor = BgBlack, containerColor = CardDark, labelColor = SilverAccent),
                            border = FilterChipDefaults.filterChipBorder(enabled = true, selected = filter == option, borderColor = Color(0xFF27272A), selectedBorderColor = PrimaryGold)
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.SearchOff, contentDescription = null, tint = Color(0xFF27272A), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No matches found.", color = SilverAccent, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                val schemes = filtered.filter { it.category == ListingCategory.SCHEME }
                val lenders = filtered.filter { it.category == ListingCategory.LENDER }

                if (schemes.isNotEmpty()) {
                    item { Text("Government Schemes", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp)) }
                    items(schemes, key = { it.id }) { listing ->
                        val defaultStage = if (listing.id == "pm-svanidhi") SchemeStage.DISBURSED else SchemeStage.NOT_APPLIED
                        val stage = applicationStage[listing.id] ?: defaultStage

                        SchemeCard(
                            listing = listing, stage = stage,
                            onApply = { applicationStage[listing.id] = SchemeStage.APPLIED; Toast.makeText(context, "Scheme application started.", Toast.LENGTH_SHORT).show() },
                            onOpenCard = onNavigateToCard
                        )
                    }
                }
                if (lenders.isNotEmpty()) {
                    item { Text("Private Lenders", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp)) }
                    items(lenders, key = { it.id }) { listing ->
                        val stage = applicationStage[listing.id] ?: SchemeStage.NOT_APPLIED
                        val pending = stage != SchemeStage.NOT_APPLIED
                        LoanOfferCard(
                            title = listing.title, amount = formatTabInr(listing.amount), rate = listing.rateLabel, badge = listing.badge,
                            ctaLabel = if (pending) "Application Sent" else "Apply Now",
                            onApply = {
                                if (!pending) {
                                    applicationStage[listing.id] = SchemeStage.APPLIED
                                    Toast.makeText(context, "Application submitted to ${listing.title}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
        // =====================================
        // SECTION 1: TRACK (SPLIT INTO ACTIVE vs APPLICATIONS)
        // =====================================
        else {
            if (trackerSubTab == TrackerSubTab.ACTIVE) {
                item { Text("Active Disbursments", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
                item {
                    OngoingLoanTrackerCard(
                        title = "PM SVaNidhi — Working Capital",
                        loanId = "LN-8849201",
                        totalAmount = 10000.0,
                        outstandingAmount = 6250.0,
                        progress = 0.375f,
                        nextEmiAmt = "₹1,250",
                        nextEmiDate = "05 Sep",
                        onOpenCard = onNavigateToCard,
                        onPayEmi = { Toast.makeText(context, "Processing EMI Payment...", Toast.LENGTH_SHORT).show() }
                    )
                }
            } else {
                item { Text("Pending Applications", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
                item {
                    PendingApplicationTrackerCard(
                        title = "Micro-Business Working Capital",
                        lender = "MFI Partner",
                        appId = "APP-90211",
                        appliedDate = "28 Aug 2026",
                        stage = 2, // 1: Applied, 2: Verification, 3: Approved
                        onActionClick = { Toast.makeText(context, "Opening KYC Upload Portal...", Toast.LENGTH_SHORT).show() }
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

// Master Segment Tab
@Composable
private fun SegmentTab(text: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(if (isSelected) Color(0xFF27272A) else Color.Transparent).clickable { onClick() }.padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (isSelected) Color.White else SilverAccent, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
    }
}

// Discover Schemes Card
@Composable
fun SchemeCard(listing: LoanListing, stage: SchemeStage, onApply: () -> Unit, onOpenCard: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val (statusLabel, statusColor) = when (stage) {
        SchemeStage.NOT_APPLIED -> "Eligible" to SuccessGreen
        SchemeStage.APPLIED, SchemeStage.UNDER_VERIFICATION -> "Pending" to WarnAmber
        SchemeStage.APPROVED -> "Approved" to PrimaryGold
        SchemeStage.DISBURSED -> "Funds Ready" to InfoBlue
    }

    Box(
        modifier = Modifier.fillMaxWidth().animateContentSize().clip(RoundedCornerShape(20.dp)).background(Color(0xFF27272A))
            .border(1.dp, if (stage == SchemeStage.DISBURSED) InfoBlue.copy(alpha = 0.5f) else PrimaryGold.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .clickable { expanded = !expanded }.padding(20.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(listing.badge, color = PrimaryGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(statusColor.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(statusLabel, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(listing.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(formatTabInr(listing.amount), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    if (stage != SchemeStage.NOT_APPLIED) {
                        val stages = listOf(SchemeStage.APPLIED, SchemeStage.UNDER_VERIFICATION, SchemeStage.APPROVED, SchemeStage.DISBURSED)
                        val currentIndex = stages.indexOf(stage).coerceAtLeast(0)

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            stages.forEachIndexed { index, _ ->
                                val reached = index <= currentIndex
                                val active = index == currentIndex
                                Box(modifier = Modifier.size(if (active) 14.dp else 10.dp).clip(CircleShape).background(if (reached) statusColor else Color(0xFF3F3F46)).border(2.dp, if (active) Color(0xFF18181B) else Color.Transparent, CircleShape))
                                if (index != stages.lastIndex) {
                                    Box(modifier = Modifier.weight(1f).height(2.dp).background(if (index < currentIndex) statusColor else Color(0xFF3F3F46)))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stage.label, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    listing.explainer?.let { Text(it, color = SilverAccent, fontSize = 13.sp, lineHeight = 18.sp) }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            if (stage == SchemeStage.DISBURSED) {
                Button(onClick = onOpenCard, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = InfoBlue, contentColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Nfc, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Trust Card", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            } else {
                Button(onClick = onApply, enabled = stage == SchemeStage.NOT_APPLIED, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = BgBlack, disabledContainerColor = Color(0xFF3F3F46)), shape = RoundedCornerShape(12.dp)) {
                    Text(if (stage == SchemeStage.NOT_APPLIED) "Apply for this scheme" else "Application in progress", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

// Active Loans Tracker Card
@Composable
private fun OngoingLoanTrackerCard(
    title: String, loanId: String, totalAmount: Double, outstandingAmount: Double,
    progress: Float, nextEmiAmt: String, nextEmiDate: String,
    onOpenCard: () -> Unit, onPayEmi: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardDark).border(1.dp, Color(0xFF27272A), RoundedCornerShape(16.dp)).padding(20.dp)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SuccessGreen))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ACTIVE LOAN", color = SilverAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                Text(loanId, color = SilverAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Outstanding Balance", color = SilverAccent, fontSize = 12.sp)
                    Text(formatTabInr(outstandingAmount), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Original Loan", color = SilverAccent, fontSize = 12.sp)
                    Text(formatTabInr(totalAmount), color = SilverAccent, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = SuccessGreen, trackColor = BgBlack)
            Spacer(modifier = Modifier.height(8.dp))
            Text("${(progress * 100).toInt()}% repaid successfully.", color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFF27272A))
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Next EMI: $nextEmiAmt", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Due on $nextEmiDate", color = WarnAmber, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Button(onClick = onPayEmi, colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = BgBlack), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), modifier = Modifier.height(40.dp)) {
                    Text("Pay EMI", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onOpenCard, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), border = BorderStroke(1.dp, Color(0xFF3F3F46)), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Nfc, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Trust Card Balance", fontSize = 14.sp)
            }
        }
    }
}

// Applications Tracker Card
@Composable
private fun PendingApplicationTrackerCard(title: String, lender: String, appId: String, appliedDate: String, stage: Int, onActionClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardDark).border(1.dp, WarnAmber.copy(alpha = 0.3f), RoundedCornerShape(16.dp)).padding(20.dp)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(WarnAmber))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("IN PROGRESS", color = WarnAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                Text(appId, color = SilverAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Applied $appliedDate to $lender", color = SilverAccent, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(24.dp))

            val stages = listOf("Applied", "Document Verification", "Lender Approval", "Disbursal")
            Column {
                stages.forEachIndexed { index, stageName ->
                    val isPast = index < stage - 1
                    val isCurrent = index == stage - 1
                    val isFuture = index > stage - 1

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(if (isPast || isCurrent) WarnAmber else Color(0xFF27272A)), contentAlignment = Alignment.Center) {
                                if (isPast) Icon(Icons.Default.Check, contentDescription = null, tint = BgBlack, modifier = Modifier.size(10.dp))
                                else if (isCurrent) Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(BgBlack))
                            }
                            if (index != stages.lastIndex) {
                                Box(modifier = Modifier.width(2.dp).height(32.dp).background(if (isPast) WarnAmber else Color(0xFF27272A)))
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(stageName, color = if (isFuture) SilverAccent else Color.White, fontSize = 14.sp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium)
                            if (isCurrent && index == 1) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Lender requires an additional business photo.", color = SilverAccent, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = onActionClick, colors = ButtonDefaults.buttonColors(containerColor = WarnAmber, contentColor = BgBlack), shape = RoundedCornerShape(8.dp), modifier = Modifier.height(36.dp)) {
                                    Text("Upload Document", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}