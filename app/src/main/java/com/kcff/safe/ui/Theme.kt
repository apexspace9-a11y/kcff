package com.kcff.safe.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val KcCyan = Color(0xFF39D9FF)
val KcBlue = Color(0xFF4B7DFF)
val KcViolet = Color(0xFF8E72FF)
val KcGold = Color(0xFFFFC928)
val KcGreen = Color(0xFF4DE5A7)
val KcRed = Color(0xFFFF5B76)
val KcNavy = Color(0xFF030814)
val KcSurface = Color(0xFF07111F)
val KcSurfaceRaised = Color(0xFF0D192A)

private val KcffColors = darkColorScheme(
    primary = KcCyan,
    onPrimary = Color(0xFF001018),
    primaryContainer = Color(0xFF0C3245),
    onPrimaryContainer = Color(0xFFC6F3FF),
    secondary = KcGold,
    onSecondary = Color(0xFF251A00),
    secondaryContainer = Color(0xFF4D3900),
    onSecondaryContainer = Color(0xFFFFE28A),
    tertiary = KcViolet,
    onTertiary = Color(0xFF130C36),
    background = KcNavy,
    onBackground = Color(0xFFF0F7FF),
    surface = KcSurface,
    onSurface = Color(0xFFF0F7FF),
    surfaceVariant = KcSurfaceRaised,
    onSurfaceVariant = Color(0xFFA9BDD1),
    outline = Color(0xFF31506B),
    outlineVariant = Color(0xFF1B3148),
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
