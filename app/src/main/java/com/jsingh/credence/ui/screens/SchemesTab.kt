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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsingh.credence.domain.models.TrustPortfolio
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

private enum class CatalogFilter(val label: String) { ALL("All"), SCHEMES("Schemes"), LENDERS("Lenders") }
enum class TrackerSubTab(val label: String) { ACTIVE("Active Loans"), APPLICATIONS("Applications") }
private enum class SortOption(val label: String) { RECOMMENDED("Recommended"), HIGHEST_AMOUNT("Highest Amount"), LOWEST_RATE("Lowest Rate") }

private fun formatTabInr(value: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    return "\u20B9${formatter.format(value.roundToInt())}"
}

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

    var currentSection by remember(defaultTab) { mutableIntStateOf(defaultTab) }
    var trackerSubTab by remember(defaultSubTab) {
        mutableStateOf(if (defaultSubTab == 1) TrackerSubTab.APPLICATIONS else TrackerSubTab.ACTIVE)
    }

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(CatalogFilter.ALL) }
    var sortOption by remember { mutableStateOf(SortOption.RECOMMENDED) }

    // ✨ RESTORED FILTER VARIABLES
    var preApprovedOnly by remember { mutableStateOf(false) }
    var zeroCollateralOnly by remember { mutableStateOf(false) }
    val hasActiveFilters = sortOption != SortOption.RECOMMENDED || preApprovedOnly || zeroCollateralOnly

    var showFilterSheet by remember { mutableStateOf(false) }
    var showCustomLoanSheet by remember { mutableStateOf(false) }

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
                            modifier = Modifier.fillMaxWidth().clickable { sortOption = option }.padding(horizontal = 16.dp, vertical = 16.dp),
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
                            checked = preApprovedOnly, onCheckedChange = { preApprovedOnly = it },
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
                            checked = zeroCollateralOnly, onCheckedChange = { zeroCollateralOnly = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = BgBlack, checkedTrackColor = PrimaryGold, uncheckedThumbColor = SilverAccent, uncheckedTrackColor = BgBlack)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
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
                SegmentTab("Discover", currentSection == 0, Modifier.weight(1f)) { currentSection = 0 }
                SegmentTab("Track", currentSection == 1, Modifier.weight(1f)) { currentSection = 1 }
            }
        }

        if (portfolio == null) {
            item {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardDark).border(1.dp, Color(0xFF27272A), RoundedCornerShape(16.dp)).clickable { onUploadClick() }.padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Upload Bank Statement", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Link your account to view eligible capital.", color = SilverAccent, fontSize = 14.sp)
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
                            .shadow(8.dp, CircleShape, spotColor = PrimaryGold.copy(alpha=0.3f))
                            .clip(CircleShape)
                            .background(PrimaryGold)
                            .clickable { showCustomLoanSheet = true }
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = "Custom Loan", tint = BgBlack, modifier = Modifier.size(20.dp))
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
                            Box(modifier = Modifier.height(40.dp).clip(CircleShape).background(if (isSelected) PrimaryGold else CardDark).clickable { filter = option }.padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                                Text(option.label, color = if (isSelected) BgBlack else SilverAccent, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(CardDark).border(1.dp, if (hasActiveFilters) PrimaryGold else Color.Transparent, CircleShape).clickable { showFilterSheet = true }, contentAlignment = Alignment.Center) {
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
                    item { Text("Government Schemes", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp)) }
                    items(schemes, key = { it.id }) { listing ->
                        val stage = applicationStage[listing.id] ?: if (listing.id == "pm-svanidhi") LocalSchemeStage.DISBURSED else LocalSchemeStage.NOT_APPLIED
                        SchemeCard(listing = listing, stage = stage, portfolio = portfolio, onApply = { applicationStage[listing.id] = LocalSchemeStage.APPLIED; Toast.makeText(context, "Scheme application started.", Toast.LENGTH_SHORT).show() }, onOpenCard = onNavigateToCard)
                    }
                }
                if (lenders.isNotEmpty()) {
                    item { Text("Private Lenders", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp)) }
                    items(lenders, key = { it.id }) { listing ->
                        val stage = applicationStage[listing.id] ?: LocalSchemeStage.NOT_APPLIED
                        val pending = stage != LocalSchemeStage.NOT_APPLIED
                        MarketLoanCard(title = listing.title, amount = formatTabInr(listing.amount), rate = listing.rateLabel, badge = listing.badge, ctaLabel = if (pending) "Application Sent" else "Apply Now", onApply = { if (!pending) { applicationStage[listing.id] = LocalSchemeStage.APPLIED; Toast.makeText(context, "Application submitted.", Toast.LENGTH_SHORT).show() } })
                    }
                }
            }
        } else {
            // TRACK SECTION
            item {
                Row(modifier = Modifier.fillMaxWidth().background(CardDark, RoundedCornerShape(12.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TrackerSubTab.entries.forEach { subTab ->
                        val isSelected = trackerSubTab == subTab
                        SegmentTab(subTab.label, isSelected, Modifier.weight(1f)) { trackerSubTab = subTab }
                    }
                }
            }
            if (trackerSubTab == TrackerSubTab.ACTIVE) {
                item { OngoingLoanTrackerCard(title = "PM SVaNidhi — Working Capital", loanId = "LN-8849201", totalAmount = 10000.0, outstandingAmount = 6250.0, progress = 0.375f, nextEmiAmt = "₹1,250", nextEmiDate = "05 Sep", onOpenCard = onNavigateToCard, onPayEmi = { Toast.makeText(context, "Processing EMI Payment...", Toast.LENGTH_SHORT).show() }) }
            } else {
                item { PendingApplicationTrackerCard(title = "Micro-Business Working Capital", lender = "MFI Partner", appId = "APP-90211", appliedDate = "28 Aug 2026", stage = 2, onActionClick = { Toast.makeText(context, "Opening KYC Upload Portal...", Toast.LENGTH_SHORT).show() }) }
            }
        }
        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

// =====================================
// ✨ CUSTOM LOAN REQUEST POPUP
// =====================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomLoanRequestSheet(portfolio: TrustPortfolio, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    val maxLimit = portfolio.safeLoanLimit
    var amountInput by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var targetInterest by remember { mutableFloatStateOf(18f) }

    val reqAmt = amountInput.toDoubleOrNull() ?: 0.0
    val utilization = reqAmt / maxLimit.coerceAtLeast(1.0)

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
            Spacer(modifier = Modifier.height(32.dp))

            // DYNAMIC SCORE HUD
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardDark)
                    .border(1.dp, Color(0xFF27272A), RoundedCornerShape(24.dp))
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

                if (reqAmt > maxLimit) {
                    Icon(Icons.Default.Warning, contentDescription = "Over Limit", tint = DangerRed, modifier = Modifier.size(28.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // AMOUNT INPUT
            Text("LOAN AMOUNT", color = SilverAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
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

            Spacer(modifier = Modifier.height(16.dp))

            // QUICK CHIPS
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickAmountChip("₹10,000") { amountInput = "10000" }
                QuickAmountChip("₹50,000") { amountInput = "50000" }
                QuickAmountChip("Max Limit") { amountInput = maxLimit.toInt().toString() }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // INTEREST RATE
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

            OutlinedTextField(
                value = purpose, onValueChange = { purpose = it },
                label = { Text("Purpose (e.g. Buy Inventory)", color = SilverAccent) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = CardDark, unfocusedContainerColor = CardDark, focusedBorderColor = PrimaryGold, unfocusedBorderColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (reqAmt <= 0) {
                        Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                    } else {
                        onDismiss()
                        Toast.makeText(context, "Broadcasting ${formatTabInr(reqAmt)} to Lenders!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = BgBlack),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.CellTower, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Broadcast to Network", fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        }
    }
}

// =====================================
// FULLY ISOLATED MOCK CLASSES
// =====================================

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
                Row(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(statusColor.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (stage == LocalSchemeStage.NOT_APPLIED && isPreApproved) { Icon(Icons.Default.Bolt, contentDescription = null, tint = statusColor, modifier = Modifier.size(12.dp)); Spacer(modifier = Modifier.width(2.dp)) }
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
                Button(onClick = onApply, enabled = stage == LocalSchemeStage.NOT_APPLIED, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = BgBlack, disabledContainerColor = Color(0xFF3F3F46)), shape = RoundedCornerShape(12.dp)) { Text(if (stage == LocalSchemeStage.NOT_APPLIED) "Apply for this scheme" else "Application in progress", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            }
        }
    }
}

@Composable
fun MarketLoanCard(title: String, amount: String, rate: String, badge: String, isHighlighted: Boolean = false, ctaLabel: String = "Apply Now", onApply: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CardDark).border(1.dp, if (isHighlighted) PrimaryGold.copy(alpha = 0.5f) else Color(0xFF27272A), RoundedCornerShape(20.dp)).padding(20.dp)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(badge, color = if (isHighlighted) PrimaryGold else SilverAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Star, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Pre-Approved", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium) }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text(amount, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text(rate, color = SuccessGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onApply, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isHighlighted) PrimaryGold else Color(0xFF27272A), contentColor = if (isHighlighted) BgBlack else Color.White), shape = RoundedCornerShape(12.dp)) { Text(ctaLabel, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
    }
}

@Composable
fun OngoingLoanTrackerCard(title: String, loanId: String, totalAmount: Double, outstandingAmount: Double, progress: Float, nextEmiAmt: String, nextEmiDate: String, onOpenCard: () -> Unit, onPayEmi: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardDark).border(1.dp, Color(0xFF27272A), RoundedCornerShape(16.dp)).padding(20.dp)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SuccessGreen)); Spacer(modifier = Modifier.width(6.dp)); Text("ACTIVE LOAN", color = SilverAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                Text(loanId, color = SilverAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text("Outstanding", color = SilverAccent, fontSize = 12.sp); Text(formatTabInr(outstandingAmount), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold) }
                Column(horizontalAlignment = Alignment.End) { Text("Original", color = SilverAccent, fontSize = 12.sp); Text(formatTabInr(totalAmount), color = SilverAccent, fontSize = 16.sp, fontWeight = FontWeight.Medium) }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = SuccessGreen, trackColor = BgBlack)
            Spacer(modifier = Modifier.height(8.dp))
            Text("${(progress * 100).toInt()}% repaid successfully.", color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFF27272A))
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Next EMI: $nextEmiAmt", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold); Text("Due on $nextEmiDate", color = WarnAmber, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                Button(onClick = onPayEmi, colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = BgBlack), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), modifier = Modifier.height(40.dp)) { Text("Pay EMI", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onOpenCard, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), border = BorderStroke(1.dp, Color(0xFF3F3F46)), shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.Nfc, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("View Trust Card Balance", fontSize = 14.sp) }
        }
    }
}

@Composable
fun PendingApplicationTrackerCard(title: String, lender: String, appId: String, appliedDate: String, stage: Int, onActionClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardDark).border(1.dp, WarnAmber.copy(alpha = 0.3f), RoundedCornerShape(16.dp)).padding(20.dp)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(WarnAmber)); Spacer(modifier = Modifier.width(6.dp)); Text("IN PROGRESS", color = WarnAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                Text(appId, color = SilverAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Applied $appliedDate to $lender", color = SilverAccent, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(24.dp))
            val stages = listOf("Applied", "Document Verification", "Lender Approval", "Disbursal")
            Column {
                stages.forEachIndexed { index, stageName ->
                    val isPast = index < stage - 1; val isCurrent = index == stage - 1; val isFuture = index > stage - 1
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(if (isPast || isCurrent) WarnAmber else Color(0xFF27272A)), contentAlignment = Alignment.Center) {
                                if (isPast) Icon(Icons.Default.Check, contentDescription = null, tint = BgBlack, modifier = Modifier.size(10.dp))
                                else if (isCurrent) Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(BgBlack))
                            }
                            if (index != stages.lastIndex) Box(modifier = Modifier.width(2.dp).height(32.dp).background(if (isPast) WarnAmber else Color(0xFF27272A)))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(stageName, color = if (isFuture) SilverAccent else Color.White, fontSize = 14.sp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium)
                            if (isCurrent && index == 1) {
                                Spacer(modifier = Modifier.height(8.dp)); Text("Lender requires an additional business photo.", color = SilverAccent, fontSize = 12.sp); Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = onActionClick, colors = ButtonDefaults.buttonColors(containerColor = WarnAmber, contentColor = BgBlack), shape = RoundedCornerShape(8.dp), modifier = Modifier.height(36.dp)) { Text("Upload Document", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class LocalSchemeStage(val label: String) {
    NOT_APPLIED("Not Applied"),
    APPLIED("Application Sent"),
    UNDER_VERIFICATION("Verifying Documents"),
    APPROVED("Approved"),
    DISBURSED("Disbursed")
}

enum class LocalListingCategory { SCHEME, LENDER }

data class LocalLoanListing(
    val id: String,
    val category: LocalListingCategory,
    val title: String,
    val amount: Double,
    val rateLabel: String,
    val badge: String,
    val explainer: String? = null
)

object LocalLoanCatalog {
    fun build(portfolio: TrustPortfolio): List<LocalLoanListing> {
        return listOf(
            LocalLoanListing("pm-svanidhi", LocalListingCategory.SCHEME, "PM SVaNidhi Yojana", 50000.0, "7.0% p.a.", "GOVT SCHEME", "Micro-credit facility for street vendors."),
            LocalLoanListing("mudra-shishu", LocalListingCategory.SCHEME, "PMMY Mudra (Shishu)", 50000.0, "1% / mo", "GOVT SCHEME", "Loans for micro-enterprises and startups."),
            LocalLoanListing("mfi-1", LocalListingCategory.LENDER, "KreditBee Business", 200000.0, "14% p.a.", "NBFC", "Quick working capital loans for small businesses."),
            LocalLoanListing("mfi-2", LocalListingCategory.LENDER, "Lendingkart Flexi", 100000.0, "1.5% / mo", "NBFC", "Flexible credit line based on monthly cash flow.")
        )
    }
}