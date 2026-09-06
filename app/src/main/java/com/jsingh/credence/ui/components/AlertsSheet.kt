package com.jsingh.credence.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsingh.credence.domain.models.TrustPortfolio

private val BgBlack = Color(0xFF09090B)
private val CardDark = Color(0xFF18181B)
private val PrimaryGold = Color(0xFFEAB308)
private val SilverAccent = Color(0xFFA1A1AA)
private val SuccessGreen = Color(0xFF10B981)
private val WarnAmber = Color(0xFFF59E0B)
private val InfoBlue = Color(0xFF3B82F6)
private val DangerRed = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsSheet(portfolio: TrustPortfolio?, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardDark,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF27272A)) }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Alerts", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    if (portfolio != null) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(DangerRed.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("3 New", color = DangerRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (portfolio != null) {
                    Text("Mark all read", color = PrimaryGold, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { /* Handle click */ })
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            if (portfolio == null) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(Color(0xFF27272A)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = SilverAccent, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("You're all caught up", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Connect your bank statement to unlock\ncapital and receive personalized alerts.", color = SilverAccent, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 18.sp)
                }
            } else {
                AlertItem(
                    icon = Icons.Default.WarningAmber,
                    iconColor = WarnAmber,
                    title = "Action Required",
                    message = "Your Micro-Business loan is paused. Complete video e-KYC to resume disbursement.",
                    time = "10m ago",
                    isUnread = true,
                    actionLabel = "Start Video KYC",
                    onActionClick = {}
                )

                AlertItem(
                    icon = Icons.Default.Verified,
                    iconColor = SuccessGreen,
                    title = "Profile Upgraded!",
                    message = "Sustained cashflow detected in your latest statement. Your Trust Status is now Prime.",
                    time = "2h ago",
                    isUnread = true,
                    actionLabel = "View New Limits",
                    isPrimaryAction = false,
                    onActionClick = {  }
                )

                 AlertItem(
                    icon = Icons.Default.Event,
                    iconColor = InfoBlue,
                    title = "Upcoming Auto-Debit",
                    message = "Your PM SVaNidhi EMI of ₹1,250 is scheduled for 05 Sep. Keep your account funded.",
                    time = "1d ago",
                    isUnread = true,
                    actionLabel = "Manage Mandate",
                    isPrimaryAction = false,
                    onActionClick = { }
                )
            }
        }
    }
}

@Composable
private fun AlertItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    message: String,
    time: String,
    isUnread: Boolean = false,
    actionLabel: String? = null,
    isPrimaryAction: Boolean = true,
    onActionClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isUnread) iconColor.copy(alpha = 0.05f) else Color(0xFF27272A))
            .border(1.dp, if (isUnread) iconColor.copy(alpha = 0.2f) else Color(0xFF27272A), RoundedCornerShape(16.dp))
            .clickable { /* Row click */ }
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            if (isUnread) {
                Box(modifier = Modifier.padding(top = 16.dp, end = 12.dp).size(6.dp).clip(CircleShape).background(iconColor))
            } else {
                Spacer(modifier = Modifier.width(18.dp))
            }

            // Icon
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(iconColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(time, color = SilverAccent, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(message, color = SilverAccent, fontSize = 13.sp, lineHeight = 18.sp)

                // Actionable Button underneath the text
                if (actionLabel != null && onActionClick != null) {
                    Spacer(modifier = Modifier.height(14.dp))

                    if (isPrimaryAction) {
                        Button(
                            onClick = onActionClick,
                            colors = ButtonDefaults.buttonColors(containerColor = iconColor, contentColor = BgBlack),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(actionLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onActionClick,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFF3F3F46)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(actionLabel, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}