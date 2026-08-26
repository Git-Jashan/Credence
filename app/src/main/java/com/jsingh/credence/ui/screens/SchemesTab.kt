package com.jsingh.credence.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsingh.credence.domain.models.TrustPortfolio

private enum class CatalogFilter(val label: String) { ALL("All"), SCHEMES("Schemes"), LENDERS("Lenders") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemesAndLendersTab(portfolio: TrustPortfolio?, onUploadClick: () -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(CatalogFilter.ALL) }
    val applicationStage = remember { mutableStateMapOf<String, SchemeStage>() }

    val listings = remember(portfolio) { if (portfolio != null) LoanCatalog.build(portfolio) else emptyList() }
    val filtered = remember(listings, query, filter) {
        listings.filter {
            (filter == CatalogFilter.ALL || (filter == CatalogFilter.SCHEMES && it.category == ListingCategory.SCHEME) || (filter == CatalogFilter.LENDERS && it.category == ListingCategory.LENDER)) && it.title.contains(query, ignoreCase = true)
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Get a Loan", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Government schemes and private lenders matched to your Trust Score.", color = SilverAccent, fontSize = 14.sp)
        }

        if (portfolio == null) {
            item { UploadPromptCard(onUploadClick) }
            return@LazyColumn
        }

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
                items(CatalogFilter.entries.toList(), key = { it.name }) { option ->
                    FilterChip(selected = filter == option, onClick = { filter = option }, label = { Text(option.label) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryGold, selectedLabelColor = BgBlack, containerColor = CardDark, labelColor = SilverAccent))
                }
            }
        }

        if (filtered.isEmpty()) {
            item { Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) { Text("No matches for \"$query\"", color = SilverAccent, fontSize = 13.sp) } }
        } else {
            val schemes = filtered.filter { it.category == ListingCategory.SCHEME }
            val lenders = filtered.filter { it.category == ListingCategory.LENDER }

            if (schemes.isNotEmpty()) {
                item { Text("Government Schemes", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
                items(schemes, key = { it.id }) { listing ->
                    val stage = applicationStage[listing.id] ?: SchemeStage.NOT_APPLIED
                    SchemeCard(listing = listing, stage = stage, onApply = { applicationStage[listing.id] = SchemeStage.APPLIED; Toast.makeText(context, "Scheme application started.", Toast.LENGTH_LONG).show() })
                }
            }
            if (lenders.isNotEmpty()) {
                item { Text("Private Lenders", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
                items(lenders, key = { it.id }) { listing ->
                    val stage = applicationStage[listing.id] ?: SchemeStage.NOT_APPLIED
                    val pending = stage != SchemeStage.NOT_APPLIED
                    LoanOfferCard(title = listing.title, amount = formatInr(listing.amount), rate = listing.rateLabel, badge = listing.badge, ctaLabel = if (pending) "Application sent" else "Apply", onApply = { if (!pending) applicationStage[listing.id] = SchemeStage.APPLIED })
                }
            }
        }
        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

@Composable
fun SchemeCard(listing: LoanListing, stage: SchemeStage, onApply: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val (statusLabel, statusColor) = when (stage) {
        SchemeStage.NOT_APPLIED -> "Eligible" to SuccessGreen
        SchemeStage.APPLIED, SchemeStage.UNDER_VERIFICATION -> "Pending" to WarnAmber
        SchemeStage.APPROVED -> "Approved" to SuccessGreen
        SchemeStage.DISBURSED -> "Disbursed" to InfoBlue
    }

    Box(modifier = Modifier.fillMaxWidth().animateContentSize().clip(RoundedCornerShape(20.dp)).background(Color(0xFF27272A)).border(1.dp, PrimaryGold.copy(alpha = 0.4f), RoundedCornerShape(20.dp)).clickable { expanded = !expanded }.padding(20.dp)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(listing.badge, color = PrimaryGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(statusColor.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp)) { Text(statusLabel, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(listing.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(formatInr(listing.amount), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    if (stage != SchemeStage.NOT_APPLIED) {
                        val stages = listOf(SchemeStage.APPLIED, SchemeStage.UNDER_VERIFICATION, SchemeStage.APPROVED, SchemeStage.DISBURSED)
                        val currentIndex = stages.indexOf(stage).coerceAtLeast(0)
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            stages.forEachIndexed { index, _ ->
                                val reached = index <= currentIndex
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(if (reached) PrimaryGold else Color(0xFF3F3F46)))
                                if (index != stages.lastIndex) Box(modifier = Modifier.weight(1f).height(2.dp).background(if (index < currentIndex) PrimaryGold else Color(0xFF3F3F46)))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(stage.label, color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                    listing.explainer?.let { Text(it, color = SilverAccent, fontSize = 12.sp, lineHeight = 17.sp) }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onApply, enabled = stage == SchemeStage.NOT_APPLIED, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = BgBlack, disabledContainerColor = Color(0xFF3F3F46)), shape = RoundedCornerShape(12.dp)) {
                Text(if (stage == SchemeStage.NOT_APPLIED) "Apply for this scheme" else "Application in progress", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}