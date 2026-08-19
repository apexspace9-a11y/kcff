package com.kcff.safe.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val KcCyan = Color(0xFF00E5FF)
val KcBlue = Color(0xFF078BFF)
val KcBlueDeep = Color(0xFF064CCB)
val KcViolet = Color(0xFF9C63FF)
val KcGold = Color(0xFFFFC400)
val KcGoldHot = Color(0xFFFF9800)
val KcGreen = Color(0xFF32F28B)
val KcRed = Color(0xFFFF3D5A)
val KcNavy = Color(0xFF030711)
val KcSurface = Color(0xFF06101D)
val KcSurfaceRaised = Color(0xFF09172A)
val KcSurfaceBlue = Color(0xFF09244A)
val KcTextMuted = Color(0xFF8FA7C2)

private val KcffColors = darkColorScheme(
    primary = KcCyan,
    onPrimary = Color(0xFF001018),
    primaryContainer = KcSurfaceBlue,
    onPrimaryContainer = Color(0xFFD7F9FF),
    secondary = KcGold,
    onSecondary = Color(0xFF211700),
    secondaryContainer = Color(0xFF3B2D00),
    onSecondaryContainer = Color(0xFFFFE386),
    tertiary = KcViolet,
    onTertiary = Color(0xFF160B31),
    background = KcNavy,
    onBackground = Color(0xFFF4F8FF),
    surface = KcSurface,
    onSurface = Color(0xFFF4F8FF),
    surfaceVariant = KcSurfaceRaised,
    onSurfaceVariant = KcTextMuted,
    outline = Color(0xFF174C76),
    outlineVariant = Color(0xFF0E2D4C),
    error = KcRed,
    onError = Color.White
)

@Composable
fun KcffTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KcffColors,
        content = content
    )
}
