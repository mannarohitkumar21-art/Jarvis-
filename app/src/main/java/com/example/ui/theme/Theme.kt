package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisDarkColorScheme = darkColorScheme(
    primary = JarvisCyan,
    onPrimary = Color(0xFF001B22),
    primaryContainer = Color(0xFF004D5A),
    onPrimaryContainer = Color(0xFF9DF0FF),
    secondary = JarvisCyanDark,
    onSecondary = Color(0xFF001F29),
    secondaryContainer = Color(0xFF0A3A54),
    onSecondaryContainer = Color(0xFFBCE9FF),
    tertiary = JarvisNeonPurple,
    onTertiary = Color.White,
    background = JarvisObsidianBg,
    onBackground = JarvisTextWhite,
    surface = JarvisSurfaceCard,
    onSurface = JarvisTextWhite,
    surfaceVariant = JarvisSurfaceCardHigh,
    onSurfaceVariant = JarvisTextDim,
    outline = JarvisSurfaceBorder,
    error = JarvisAlertRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to cyber dark for JARVIS AI
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisDarkColorScheme,
        typography = Typography,
        content = content
    )
}
