package com.jsingh.credence.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGold,
    background = BgBlack,
    surface = CardDark,
    onPrimary = BgBlack,
    onBackground = Color.White,
    onSurface = Color.White,
    secondary = SilverAccent
)

@Composable
fun CredenceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}