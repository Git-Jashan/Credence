package com.jsingh.credence.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryTeal,
    onPrimary = BgBlack,
    secondary = PrimaryGold,
    onSecondary = BgBlack,
    background = BgBlack,
    onBackground = TextPrimary,
    surface = CardDark,
    onSurface = TextPrimary,
    surfaceVariant = CardDarkAlt,
    onSurfaceVariant = SilverAccent,
    outline = StrokeColor,
    error = DangerRed
)

@Composable
fun CredenceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
