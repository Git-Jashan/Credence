package com.jsingh.credence.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsingh.credence.domain.models.TrustPortfolio
import kotlinx.coroutines.launch
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlin.math.roundToInt

private enum class CatalogFilter(val label: String) { ALL("All"), SCHEMES("Schemes"), LENDERS("Lenders") }
enum class TrackerSubTab(val label: String) { ACTIVE("Active Loans"), APPLICATIONS("Applications") }
private enum class SortOption(val label: String) { RECOMMENDED("Recommended"), HIGHEST_AMOUNT("Highest Amount"), LOWEST_RATE("Lowest Rate") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemesAndLendersTab(
    portfolio: TrustPortfolio?,
    defaultTab: Int = 0,
    defaultSubTab: Int = 0,
    onUploadClick: () -> Unit,
    onNavigateToCard: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var currentSection by remember(defaultTab) { mutableIntStateOf(defaultTab) }
    var trackerSubTab by remember(defaultSubTab) { mutableStateOf(if (defaultSubTab == 1) TrackerSubTab.APPLICATIONS else TrackerSubTab.ACTIVE) }

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(CatalogFilter.ALL) }
    var sortOption by remember { mutableStateOf(SortOption.RECOMMENDED) }

    var preApprovedOnly by remember { mutableStateOf(false) }
    var zeroCollateralOnly by remember { mutableStateOf(false) }
    val hasActiveFilters = sortOption != SortOption.RECOMMENDED || preApprovedOnly || zeroCollateralOnly

    var showFilterSheet by remember { mutableStateOf(false) }
    var showCustomLoanSheet by remember { mutableStateOf(false) }
    var selectedListingForReview by remember { mutableStateOf<LocalLoanListing?>(null) }

    val applicationStage = remember { mutableStateMapOf<String, LocalSchemeStage>() }
    val listings = remember(portfolio) { if (portfolio != null) LocalLoanCatalog.build(portfolio) else emptyList() }

    val filtered = remember(listings, query, filter, sortOption, preApprovedOnly, zeroCollateralOnly) {
        var base = listings.filter {
            (filter == CatalogFilter.ALL ||
                    (filter == CatalogFilter.SCHEMES && it.category == LocalListingCategory.SCHEME) ||
                    (filter == CatalogFilter.LENDERS && it.category == LocalListingCategory.LENDER)) &&
                    it.title.contains(query, ignoreCase = true)
        }

        if (preApprovedOnly && portfolio != null) {
            base = base.filter { it.amount <= portfolio.safeLoanLimit }
        }

        when (sortOption) {
            SortOption.RECOMMENDED -> base
            SortOption.HIGHEST_AMOUNT -> base.sortedByDescending { it.amount }
            SortOption.LOWEST_RATE -> base.sortedBy { it.rateLabel.length }
        }
    }

    if (showCustomLoanSheet && portfolio != null) {
        CustomLoanRequestSheet(portfolio = portfolio, onDismiss = { showCustomLoanSheet = false })
    }

    if (selectedListingForReview != null && portfolio != null) {
        LoanOfferReviewSheet(
            listing = selectedListingForReview!!,
            portfolio = portfolio,
            onDismiss = { selectedListingForReview = null },
            onAccept = { listing ->
                applicationStage[listing.id] = LocalSchemeStage.APPLIED
                selectedListingForReview = null
                Toast.makeText(context, "Application securely signed and submitted.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showFilterSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }, sheetState = sheetState, containerColor = BgBlack) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Refine Marketplace", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    if (hasActiveFilters) {
                        Text(
                            text = "Reset", color = PrimaryGold, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                sortOption = SortOption.RECOMMENDED
                                preApprovedOnly = false
                                zeroCollateralOnly = false
                            }.padding(8.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Text("SORT BY", color = SilverAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardDark)) {
                    SortOption.entries.forEachIndexed { index, option ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                sortOption = option
                            }.padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(option.label, color = if (sortOption == option) PrimaryGold else Color.White, fontSize = 16.sp, fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Medium)
                            if (sortOption == option) Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(20.dp))
                        }
                        if (index < SortOption.entries.size - 1) HorizontalDivider(color = BgBlack, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("SMART FILTERS", color = SilverAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardDark)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Pre-Approved Only", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Hide loans above your limit", color = SilverAccent, fontSize = 12.sp)
                        }
                        Switch(
                            checked = preApprovedOnly, onCheckedChange = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); preApprovedOnly = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = BgBlack, checkedTrackColor = PrimaryGold, uncheckedThumbColor = SilverAccent, uncheckedTrackColor = BgBlack)
                        )
                    }
                    HorizontalDivider(color = BgBlack, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Zero Collateral", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("No asset pledging required", color = SilverAccent, fontSize = 12.sp)
                        }
                        Switch(
                            checked = zeroCollateralOnly, onCheckedChange = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); zeroCollateralOnly = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = BgBlack, checkedTrackColor = PrimaryGold, uncheckedThumbColor = SilverAccent, uncheckedTrackColor = BgBlack)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = BgBlack),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Apply Filters", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(BgBlack).padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Capital Market", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth().background(CardDark, RoundedCornerShape(16.dp)).padding(4.dp)) {
                SegmentTab("Discover", currentSection == 0, Modifier.weight(1f)) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); currentSection = 0 }
                SegmentTab("Track", currentSection == 1, Modifier.weight(1f)) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); currentSection = 1 }
            }
        }

        if (portfolio == null) {
            item {
                var isStarting by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CardDark).border(1.dp, Color(0xFF27272A), RoundedCornerShape(20.dp)).clickable {
                    if (!isStarting) {
                        scope.launch { isStarting = true; kotlinx.coroutines.delay(1000); isStarting = false; onUploadClick() }
                    }
                }.padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isStarting) {
                            CircularProgressIndicator(color = PrimaryGold, strokeWidth = 3.dp, modifier = Modifier.size(48.dp))
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(48.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(if (isStarting) "Initializing Secure Vault..." else "Upload Bank Statement", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Link your account to view eligible capital.", color = SilverAccent, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                }
            }
            return@LazyColumn
        }

        if (currentSection == 0) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it }, placeholder = { Text("Search lenders...", color = SilverAccent) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SilverAccent) }, singleLine = true,
                        modifier = Modifier.weight(1f).height(54.dp), shape = CircleShape,
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = CardDark, unfocusedContainerColor = CardDark, focusedBorderColor = PrimaryGold, unfocusedBorderColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Row(
                        modifier = Modifier
                            .height(54.dp)
                            .shadow(12.dp, CircleShape, spotColor = PrimaryGold.copy(alpha=0.2f))
                            .clip(CircleShape)
                            .background(PrimaryGold)
                            .clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); showCustomLoanSheet = true }
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = "Custom", tint = BgBlack, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Custom", color = BgBlack, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                        items(CatalogFilter.entries.toTypedArray(), key = { it.name }) { option ->
                            val isSelected = filter == option
                            Box(modifier = Modifier.height(40.dp).clip(CircleShape).background(if (isSelected) PrimaryGold else CardDark).clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); filter = option }.padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                                Text(option.label, color = if (isSelected) BgBlack else SilverAccent, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(CardDark).border(1.dp, if (hasActiveFilters) PrimaryGold else Color.Transparent, CircleShape).clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); showFilterSheet = true }, contentAlignment = Alignment.Center) {
                        if (hasActiveFilters) Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(8.dp).clip(CircleShape).background(PrimaryGold))
                        Icon(Icons.Default.Tune, contentDescription = "Filter", tint = if (hasActiveFilters) PrimaryGold else SilverAccent, modifier = Modifier.size(20.dp))
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.SearchOff, contentDescription = null, tint = Color(0xFF27272A), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No matching capital found.", color = SilverAccent, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                val schemes = filtered.filter { it.category == LocalListingCategory.SCHEME }
                val lenders = filtered.filter { it.category == LocalListingCategory.LENDER }

                if (schemes.isNotEmpty()) {
                    item { Text("Government Schemes", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
                    items(schemes, key = { it.id }) { listing ->
                        val stage = applicationStage[listing.id] ?: if (listing.id == "pm-svanidhi") LocalSchemeStage.DISBURSED else LocalSchemeStage.NOT_APPLIED
                        SchemeCard(
                            listing = listing,
                            stage = stage,
                            portfolio = portfolio,
                            onApply = { selectedListingForReview = listing },
                            onOpenCard = onNavigateToCard
                        )
                    }
                }
                if (lenders.isNotEmpty()) {
                    item { Text("Private Lenders", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
                    items(lenders, key = { it.id }) { listing ->
                        val stage = applicationStage[listing.id] ?: LocalSchemeStage.NOT_APPLIED
                        val pending = stage != LocalSchemeStage.NOT_APPLIED
                        MarketLoanCard(
                            listing = listing,
                            isHighlighted = listing.id == "mfi-2",
                            ctaLabel = if (pending) "Application Sent" else "Apply Now",
                            onApply = { if (!pending) selectedListingForReview = listing }
                        )
                    }
                }
            }
        } else {
            // TRACK SECTION
            item {
                Row(modifier = Modifier.fillMaxWidth().background(CardDark, RoundedCornerShape(12.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TrackerSubTab.entries.forEach { subTab ->
                        val isSelected = trackerSubTab == subTab
                        SegmentTab(subTab.label, isSelected, Modifier.weight(1f)) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); trackerSubTab = subTab }
                    }
                }
            }
            if (trackerSubTab == TrackerSubTab.ACTIVE) {
                item {
                    OngoingLoanTrackerCard(
                        title = "PM SVaNidhi — Working Capital",
                        loanId = "LN-8849201",
                        totalAmount = 50000.0,
                        outstandingAmount = 31250.0,
                        progress = 0.375f,
                        nextEmiAmt = "₹4,166",
                        nextEmiDate = "05 Sep",
                        onOpenCard = onNavigateToCard,
                        onPayEmi = { Toast.makeText(context, "EMI Cleared for LN-8849201", Toast.LENGTH_SHORT).show() }
                    )
                }
            } else {
                item {
                    PendingApplicationTrackerCard(
                        title = "Micro-Business Working Capital",
                        lender = "MFI Partner",
                        appId = "APP-90211",
                        appliedDate = "28 Aug 2026",
                        stage = 2,
                        onActionClick = { Toast.makeText(context, "Secure Portal Loaded", Toast.LENGTH_SHORT).show() }
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// =====================================
// ✨ THE TERM SHEET REVIEWER
// =====================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanOfferReviewSheet(listing: LocalLoanListing, portfolio: TrustPortfolio, onDismiss: () -> Unit, onAccept: (LocalLoanListing) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var isAnalyzing by remember { mutableStateOf(true) }
    var isSigning by remember { mutableStateOf(false) }
    var selectedTenure by remember { mutableFloatStateOf(12f) }

    LaunchedEffect(listing) {
        isAnalyzing = true
        kotlinx.coroutines.delay(1200)
        isAnalyzing = false
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val loanAmount = listing.amount
    val processingFeePercent = listing.procFee.replace("%", "").toDoubleOrNull() ?: 1.5
    val processingFeeAmount = loanAmount * (processingFeePercent / 100.0)
    val netDisbursal = loanAmount - processingFeeAmount

    val yearlyInterestRate = listing.rateLabel.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 14.0
    val totalInterest = loanAmount * (yearlyInterestRate / 100.0) * (selectedTenure / 12.0)
    val monthlyEmi = (loanAmount + totalInterest) / selectedTenure

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = BgBlack) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {

            if (isAnalyzing) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryGold, strokeWidth = 3.dp, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Fetching Live Term Sheet...", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Negotiating with ${listing.category.name} network", color = SilverAccent, fontSize = 13.sp)
                }
            } else {
                Text("Review Offer Terms", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(listing.title, color = SilverAccent, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(24.dp))

                // ✨ The Receipt Ledger
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CardDark).padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Gross Loan Amount", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text(formatInr(loanAmount), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Processing Fee (${listing.procFee})", color = SilverAccent, fontSize = 15.sp)
                        Text("- ${formatInr(processingFeeAmount)}", color = DangerRed, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    DashedDivider()
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Net Disbursal", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(formatInr(netDisbursal), color = SuccessGreen, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text("Adjust Tenure", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("${selectedTenure.roundToInt()} Months", color = PrimaryGold, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = selectedTenure,
                    onValueChange = { selectedTenure = it },
                    valueRange = 6f..36f,
                    steps = 5,
                    modifier = Modifier.height(20.dp),
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = PrimaryGold, inactiveTrackColor = Color(0xFF27272A))
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ✨ Glowing EMI Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = PrimaryGold.copy(alpha=0.15f))
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardDark)
                        .border(1.dp, PrimaryGold.copy(alpha=0.4f), RoundedCornerShape(16.dp))
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EventRepeat, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Estimated EMI", color = SilverAccent, fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Starts next month", color = Color(0xFF71717A), fontSize = 11.sp)
                        }
                    }
                    Text(formatInr(monthlyEmi), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (!isSigning) {
                            scope.launch {
                                isSigning = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                kotlinx.coroutines.delay(1800)
                                isSigning = false
                                onAccept(listing)
                            }
                        }
                    },
                    enabled = !isSigning,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = BgBlack, disabledContainerColor = Color(0xFF27272A), disabledContentColor = SilverAccent),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isSigning) {
                        CircularProgressIndicator(color = BgBlack, strokeWidth = 3.dp, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Executing e-Sign...", fontWeight = FontWeight.Black, fontSize = 16.sp)
                    } else {
                        Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("e-Sign & Submit Application", fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

// =====================================
// CUSTOM LOAN REQUEST POPUP
// =====================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomLoanRequestSheet(portfolio: TrustPortfolio, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var isSubmitting by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    val maxLimit = portfolio.safeLoanLimit
    var amountInput by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var targetInterest by remember { mutableFloatStateOf(18f) }
    var acceptTerms by remember { mutableStateOf(false) }

    val reqAmt = amountInput.toDoubleOrNull() ?: 0.0
    val utilization = reqAmt / maxLimit.coerceAtLeast(1.0)

    val estimatedMonthlyEmi = if (reqAmt > 0) {
        val yearlyInterest = reqAmt * (targetInterest / 100.0)
        (reqAmt + yearlyInterest) / 12.0
    } else 0.0

    val amtPenalty = if (utilization > 1.0) {
        ((utilization * 40.0) + ((utilization - 1.0) * 80.0)).roundToInt()
    } else {
        (utilization * 40.0).roundToInt()
    }

    val interestEffect = ((targetInterest - 18f) * 1.5f).roundToInt()
    val dynamicScore = (portfolio.score - amtPenalty + interestEffect).coerceIn(0, 100)

    val (oddsText, oddsColor) = when {
        dynamicScore >= 70 -> "High Approval" to SuccessGreen
        dynamicScore >= 45 -> "Medium Match" to WarnAmber
        else -> "Low Approval" to DangerRed
    }

    val animatedProgress by animateFloatAsState(targetValue = dynamicScore / 100f, animationSpec = tween(600, easing = FastOutSlowInEasing), label = "ring")

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = BgBlack) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {

            Text("Broadcast Request", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Negotiate terms directly with our lender network.", color = SilverAccent, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardDark)
                    .border(1.dp, Color(0xFF27272A), RoundedCornerShape(20.dp))
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 6.dp.toPx()
                        val arcSize = Size(size.width - stroke, size.height - stroke)
                        val topLeft = Offset(stroke / 2, stroke / 2)

                        drawArc(color = Color(0xFF27272A), startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(width = stroke, cap = StrokeCap.Round), size = arcSize, topLeft = topLeft)
                        drawArc(color = oddsColor, startAngle = -90f, sweepAngle = 360f * animatedProgress, useCenter = false, style = Stroke(width = stroke, cap = StrokeCap.Round), size = arcSize, topLeft = topLeft)
                    }
                    Text("$dynamicScore", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text("PROJECTED ODDS", color = SilverAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(oddsText, color = oddsColor, fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("REQUESTED AMOUNT", color = SilverAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            OutlinedTextField(
                value = amountInput,
                onValueChange = { if (it.all { char -> char.isDigit() } && it.length < 9) amountInput = it },
                textStyle = TextStyle(fontSize = 54.sp, fontWeight = FontWeight.Black, color = PrimaryGold, textAlign = TextAlign.Center, letterSpacing = (-2).sp),
                placeholder = { Text("₹0", fontSize = 54.sp, fontWeight = FontWeight.Black, color = Color(0xFF27272A), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent
                )
            )

            AnimatedVisibility(visible = reqAmt > 0) {
                Text("Est. EMI: ${formatInr(estimatedMonthlyEmi)} for 12 mos", color = SuccessGreen, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickAmountChip("₹10k") { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); amountInput = "10000" }
                QuickAmountChip("₹50k") { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); amountInput = "50000" }
                QuickAmountChip("Max") { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); amountInput = maxLimit.toInt().toString() }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text("Target Interest Rate", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("${targetInterest.roundToInt()}%", color = PrimaryGold, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(value = targetInterest, onValueChange = { targetInterest = it }, valueRange = 8f..36f, modifier = Modifier.height(20.dp), colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = PrimaryGold, inactiveTrackColor = Color(0xFF27272A)))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("8% (Harder)", color = SilverAccent, fontSize = 11.sp)
                Text("36% (Easier)", color = SilverAccent, fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth().clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); acceptTerms = !acceptTerms }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(if (acceptTerms) PrimaryGold else CardDark).border(1.dp, if (acceptTerms) PrimaryGold else SilverAccent, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                    if (acceptTerms) Icon(Icons.Default.Check, contentDescription = null, tint = BgBlack, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("I authorize Credence to share my Zero-Knowledge Trust Profile with verified lenders.", color = SilverAccent, fontSize = 12.sp, lineHeight = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (reqAmt <= 0) {
                        Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                    } else if (!isSubmitting && !isSuccess) {
                        scope.launch {
                            isSubmitting = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            kotlinx.coroutines.delay(1800)
                            isSubmitting = false
                            isSuccess = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            kotlinx.coroutines.delay(1000)
                            onDismiss()
                        }
                    }
                },
                enabled = acceptTerms && reqAmt > 0,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isSuccess) SuccessGreen else PrimaryGold, contentColor = BgBlack, disabledContainerColor = Color(0xFF27272A), disabledContentColor = SilverAccent),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = BgBlack, strokeWidth = 3.dp, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Encrypting Payload...", fontWeight = FontWeight.Black, fontSize = 16.sp)
                } else if (isSuccess) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Broadcasted to Network!", fontWeight = FontWeight.Black, fontSize = 16.sp)
                } else {
                    Icon(Icons.Default.CellTower, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Broadcast Request", fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }
        }
    }
}

// =====================================
// UTILS & MOCK CLASSES
// =====================================

@Composable
fun DashedDivider(color: Color = Color(0xFF3F3F46), thickness: Float = 3f, dashWidth: Float = 15f, dashGap: Float = 10f) {
    Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = thickness,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashWidth, dashGap), 0f)
        )
    }
}

@Composable
fun RowScope.QuickAmountChip(label: String, onClick: () -> Unit) {
    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(CardDark).clickable { onClick() }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SegmentTab(text: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier = modifier.clip(RoundedCornerShape(12.dp)).background(if (isSelected) Color(0xFF27272A) else Color.Transparent).clickable { onClick() }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(text, color = if (isSelected) Color.White else SilverAccent, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
    }
}

// ✨ SchemeCard
@Composable
fun SchemeCard(listing: LocalLoanListing, stage: LocalSchemeStage, portfolio: TrustPortfolio, onApply: () -> Unit, onOpenCard: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    val isPreApproved = listing.amount <= portfolio.safeLoanLimit
    val (statusLabel, statusColor) = when (stage) {
        LocalSchemeStage.NOT_APPLIED -> if (isPreApproved) "Pre-Approved" to SuccessGreen else "Checking Match" to WarnAmber
        LocalSchemeStage.APPLIED, LocalSchemeStage.UNDER_VERIFICATION -> "Pending" to WarnAmber
        LocalSchemeStage.APPROVED -> "Approved" to PrimaryGold
        LocalSchemeStage.DISBURSED -> "Funds Ready" to InfoBlue
    }

    Box(modifier = Modifier.fillMaxWidth().animateContentSize().clip(RoundedCornerShape(20.dp)).background(CardDark).border(1.dp, if (stage == LocalSchemeStage.DISBURSED) InfoBlue.copy(alpha = 0.5f) else Color(0xFF27272A), RoundedCornerShape(20.dp)).clickable { expanded = !expanded }.padding(20.dp)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(listing.badge, color = PrimaryGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Row(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(statusColor.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (stage == LocalSchemeStage.NOT_APPLIED && isPreApproved) { Icon(Icons.Default.Bolt, contentDescription = null, tint = statusColor, modifier = Modifier.size(10.dp)); Spacer(modifier = Modifier.width(4.dp)) }
                    Text(statusLabel, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(listing.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(formatInr(listing.amount), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)

            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text("Tenure", color = SilverAccent, fontSize = 11.sp); Text(listing.tenure, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                Column(horizontalAlignment = Alignment.End) { Text("Interest / Proc. Fee", color = SilverAccent, fontSize = 11.sp); Text("${listing.rateLabel} • ${listing.procFee}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(20.dp))
                    DashedDivider(color = Color(0xFF27272A))
                    Spacer(modifier = Modifier.height(16.dp))
                    if (stage != LocalSchemeStage.NOT_APPLIED) {
                        val stages = listOf(LocalSchemeStage.APPLIED, LocalSchemeStage.UNDER_VERIFICATION, LocalSchemeStage.APPROVED, LocalSchemeStage.DISBURSED)
                        val currentIndex = stages.indexOf(stage).coerceAtLeast(0)
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            stages.forEachIndexed { index, _ ->
                                val reached = index <= currentIndex; val active = index == currentIndex
                                Box(modifier = Modifier.size(if (active) 14.dp else 10.dp).clip(CircleShape).background(if (reached) statusColor else Color(0xFF3F3F46)).border(2.dp, if (active) CardDark else Color.Transparent, CircleShape))
                                if (index != stages.lastIndex) Box(modifier = Modifier.weight(1f).height(2.dp).background(if (index < currentIndex) statusColor else Color(0xFF3F3F46)))
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
            if (stage == LocalSchemeStage.DISBURSED) {
                Button(onClick = onOpenCard, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = InfoBlue, contentColor = Color.White), shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.Nfc, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Open Trust Card", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            } else {
                Button(
                    onClick = onApply,
                    enabled = stage == LocalSchemeStage.NOT_APPLIED,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = BgBlack, disabledContainerColor = Color(0xFF27272A), disabledContentColor = SilverAccent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (stage == LocalSchemeStage.NOT_APPLIED) "Review & Apply" else "Application in progress", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun MarketLoanCard(listing: LocalLoanListing, isHighlighted: Boolean = false, ctaLabel: String = "Apply Now", onApply: () -> Unit) {
    val isPending = ctaLabel != "Apply Now"

    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CardDark).border(1.dp, if (isHighlighted) PrimaryGold.copy(alpha = 0.5f) else Color(0xFF27272A), RoundedCornerShape(20.dp)).padding(20.dp)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(listing.badge, color = if (isHighlighted) PrimaryGold else SilverAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Star, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Pre-Approved", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium) }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(listing.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(formatInr(listing.amount), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)

            Spacer(modifier = Modifier.height(16.dp))
            DashedDivider(color = Color(0xFF27272A))
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text("Tenure", color = SilverAccent, fontSize = 11.sp); Text(listing.tenure, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                Column(horizontalAlignment = Alignment.End) { Text("Interest / Proc. Fee", color = SilverAccent, fontSize = 11.sp); Text("${listing.rateLabel} • ${listing.procFee}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF18181B)).border(1.dp, Color(0xFF27272A), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) { Text("⚡ Fast Disbursal", color = SilverAccent, fontSize = 10.sp, fontWeight = FontWeight.Medium) }
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF18181B)).border(1.dp, Color(0xFF27272A), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) { Text("📄 Minimal Docs", color = SilverAccent, fontSize = 10.sp, fontWeight = FontWeight.Medium) }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onApply,
                enabled = !isPending,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isHighlighted) PrimaryGold else Color(0xFF27272A), contentColor = if (isHighlighted) BgBlack else Color.White, disabledContainerColor = Color(0xFF27272A), disabledContentColor = SilverAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(ctaLabel, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun OngoingLoanTrackerCard(title: String, loanId: String, totalAmount: Double, outstandingAmount: Double, progress: Float, nextEmiAmt: String, nextEmiDate: String, onOpenCard: () -> Unit, onPayEmi: () -> Unit) {
    val scope = rememberCoroutineScope()
    var isPaying by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CardDark).border(1.dp, Color(0xFF27272A), RoundedCornerShape(20.dp)).padding(24.dp)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SuccessGreen)); Spacer(modifier = Modifier.width(8.dp)); Text("ACTIVE LOAN", color = SilverAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                Text(loanId, color = SilverAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(SuccessGreen.copy(alpha=0.1f)).border(1.dp, SuccessGreen.copy(alpha=0.2f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                Icon(Icons.Default.Autorenew, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Auto-Pay (NACH) Active", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text("Outstanding", color = SilverAccent, fontSize = 13.sp); Spacer(modifier = Modifier.height(4.dp)); Text(formatInr(outstandingAmount), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold) }
                Column(horizontalAlignment = Alignment.End) { Text("Original", color = SilverAccent, fontSize = 13.sp); Spacer(modifier = Modifier.height(4.dp)); Text(formatInr(totalAmount), color = SilverAccent, fontSize = 16.sp, fontWeight = FontWeight.Medium) }
            }
            Spacer(modifier = Modifier.height(20.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)), color = SuccessGreen, trackColor = BgBlack, strokeCap = StrokeCap.Round)
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${(progress * 100).toInt()}% repaid successfully", color = SuccessGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text("${formatInr(totalAmount - outstandingAmount)} paid", color = SilverAccent, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
            DashedDivider(color = Color(0xFF27272A))
            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Next EMI: $nextEmiAmt", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(2.dp)); Text("Due on $nextEmiDate", color = WarnAmber, fontSize = 13.sp, fontWeight = FontWeight.Medium) }

                Button(
                    onClick = {
                        if(!isPaying) {
                            scope.launch {
                                isPaying = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                kotlinx.coroutines.delay(2000)
                                isPaying = false
                                onPayEmi()
                            }
                        }
                    },
                    enabled = !isPaying,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = BgBlack, disabledContainerColor = PrimaryGold),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    if (isPaying) {
                        CircularProgressIndicator(color = BgBlack, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Processing...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    } else {
                        Text("Pay Early", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            OutlinedButton(onClick = onOpenCard, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), border = BorderStroke(1.dp, Color(0xFF3F3F46)), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.Nfc, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("View Trust Card Balance", fontSize = 15.sp, fontWeight = FontWeight.Medium) }

            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                    if(!isDownloading) {
                        scope.launch {
                            isDownloading = true
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            kotlinx.coroutines.delay(1500)
                            isDownloading = false
                            Toast.makeText(context, "Statement downloaded to device securely.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.padding(8.dp)) {
                    if (isDownloading) {
                        CircularProgressIndicator(color = SilverAccent, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generating PDF...", color = SilverAccent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    } else {
                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = SilverAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download Statement (SOA)", color = SilverAccent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
@Composable
fun PendingApplicationTrackerCard(title: String, lender: String, appId: String, appliedDate: String, stage: Int, onActionClick: () -> Unit) {
    val scope = rememberCoroutineScope()
    var isRouting by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardDark)
            .border(1.dp, WarnAmber.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(24.dp)
    ) {
        Column {
            // HEADER
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(WarnAmber.copy(alpha = 0.1f))
                        .border(1.dp, WarnAmber.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(WarnAmber))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ACTION REQUIRED", color = WarnAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                Text(appId, color = SilverAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // TITLE & SUBTITLE
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Lender: $lender • Applied $appliedDate", color = SilverAccent, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(28.dp))

            val stages = listOf(
                Triple("Application Submitted", "28 Aug, 10:45 AM", true),
                Triple("Document Verification", "Pending Upload", false),
                Triple("Final Underwriting", "Estimated 1-2 Days", false),
                Triple("Bank Disbursal", "Pending", false)
            )

            // TIMELINE LEDGER
            Column {
                stages.forEachIndexed { index, (stageName, stageSub, isCompleted) ->
                    val isCurrent = index == stage - 1
                    val isFuture = index > stage - 1

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(20.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(if (isCompleted) SuccessGreen else if (isCurrent) WarnAmber else Color(0xFF27272A)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCompleted) Icon(Icons.Default.Check, contentDescription = null, tint = BgBlack, modifier = Modifier.size(12.dp))
                                else if (isCurrent) Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(BgBlack))
                            }
                            if (index != stages.lastIndex) {
                                val height = if (isCurrent) 124.dp else 44.dp
                                Box(modifier = Modifier.width(2.dp).height(height).background(if (isCompleted) SuccessGreen else Color(0xFF27272A)))
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f).padding(bottom = if (isCurrent) 0.dp else 24.dp)) {
                            Text(stageName, color = if (isFuture) SilverAccent else Color.White, fontSize = 16.sp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(stageSub, color = if (isCurrent) WarnAmber else SilverAccent, fontSize = 13.sp)

                            if (isCurrent) {
                                Spacer(modifier = Modifier.height(16.dp))

                                // DROP-ZONE ACTION BOX
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF18181B).copy(alpha = 0.5f))
                                        .border(1.dp, WarnAmber.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(WarnAmber.copy(alpha=0.15f)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = WarnAmber, modifier = Modifier.size(18.dp))
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text("Shop Frontage", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("JPG or PNG, max 5MB", color = SilverAccent, fontSize = 11.sp, maxLines = 1)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Button(
                                        onClick = {
                                            if(!isRouting){
                                                scope.launch {
                                                    isRouting = true
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    kotlinx.coroutines.delay(1500)
                                                    isRouting = false
                                                    onActionClick()
                                                }
                                            }
                                        },
                                        enabled = !isRouting,
                                        colors = ButtonDefaults.buttonColors(containerColor = WarnAmber, contentColor = BgBlack, disabledContainerColor = WarnAmber.copy(alpha=0.5f)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.height(36.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                                    ) {
                                        if(isRouting) {
                                            CircularProgressIndicator(color = BgBlack, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                        } else {
                                            Text("Upload", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            DashedDivider(color = Color(0xFF27272A))
            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {

                // Withdraw App Chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(DangerRed.copy(alpha = 0.1f))
                        .border(1.dp, DangerRed.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            Toast.makeText(context, "Application Withdrawn Successfully.", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {Icon(Icons.Default.Close, contentDescription = null, tint = DangerRed, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Withdraw", color = DangerRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(20.dp))
                // Contact Lender Chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(PrimaryGold.copy(alpha = 0.1f))
                        .border(1.dp, PrimaryGold.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            Toast.makeText(context, "Connecting to Support...", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Contact", color = PrimaryGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
enum class LocalSchemeStage(val label: String) { NOT_APPLIED("Not Applied"), APPLIED("Application Sent"), UNDER_VERIFICATION("Verifying Documents"), APPROVED("Approved"), DISBURSED("Disbursed") }
enum class LocalListingCategory { SCHEME, LENDER }

data class LocalLoanListing(
    val id: String,
    val category: LocalListingCategory,
    val title: String,
    val amount: Double,
    val rateLabel: String,
    val badge: String,
    val tenure: String = "12-36 Mos",
    val procFee: String = "1.5%",
    val explainer: String? = null
)

object LocalLoanCatalog {
    fun build(portfolio: TrustPortfolio): List<LocalLoanListing> {
        return listOf(
            LocalLoanListing("pm-svanidhi", LocalListingCategory.SCHEME, "PM SVaNidhi Yojana", 50000.0, "7.0% p.a.", "GOVT SCHEME", "12 Mos", "₹0", "Micro-credit facility for street vendors."),
            LocalLoanListing("mudra-shishu", LocalListingCategory.SCHEME, "PMMY Mudra (Shishu)", 50000.0, "1% / mo", "GOVT SCHEME", "Up to 60 Mos", "₹0", "Loans for micro-enterprises and startups."),
            LocalLoanListing("mfi-1", LocalListingCategory.LENDER, "KreditBee Business", 200000.0, "14% p.a.", "NBFC", "12-24 Mos", "2.0%"),
            LocalLoanListing("mfi-2", LocalListingCategory.LENDER, "Lendingkart Flexi", 100000.0, "1.5% / mo", "NBFC", "6-36 Mos", "1.5%")
        )
    }
}