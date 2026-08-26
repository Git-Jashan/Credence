package com.jsingh.credence.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ---------- Core surfaces (deep navy fintech dark, not flat black) ----------
val BgBlack = Color(0xFF0A0E16)
val BgBlackElevated = Color(0xFF0F1420)
val CardDark = Color(0xFF141B28)
val CardDarkAlt = Color(0xFF1B2432)
val StrokeColor = Color(0xFF242E3D)

// ---------- Text ----------
val TextPrimary = Color(0xFFF4F6FA)
val SilverAccent = Color(0xFF8B96A8)
val TextMuted = Color(0xFF5A6579)

// ---------- Brand accents ----------
val PrimaryTeal = Color(0xFF22D6AE)      // primary CTA / brand - "trust" green-teal
val PrimaryTealDeep = Color(0xFF0E9E80)
val PrimaryGold = Color(0xFFF0B94A)      // Gold tier accent
val GoldDeep = Color(0xFFC4881E)
val AccentViolet = Color(0xFFA78BFA)

val SuccessGreen = Color(0xFF2ED47A)
val InfoBlue = Color(0xFF4C8DFF)
val WarnAmber = Color(0xFFFFB020)
val DangerRed = Color(0xFFFF5C6C)

// ---------- Gradients ----------
val SilverGradient = Brush.linearGradient(listOf(Color(0xFFE8ECF3), Color(0xFF8B96A8)))
val GoldGradient = Brush.linearGradient(listOf(Color(0xFFFCE38A), Color(0xFFC4881E)))
val TealGradient = Brush.linearGradient(listOf(Color(0xFF3FF0C4), Color(0xFF0E9E80)))
val HeroGradient = Brush.linearGradient(listOf(Color(0xFF17324A), Color(0xFF0A0E16)))
val VioletGradient = Brush.linearGradient(listOf(Color(0xFFC4B5FD), Color(0xFF7C3AED)))
