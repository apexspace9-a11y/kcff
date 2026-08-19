package com.kcff.safe.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF6D4AFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7DEFF),
    onPrimaryContainer = Color(0xFF21006B),
    secondary = Color(0xFF006C4C),
    tertiary = Color(0xFF8A4F00),
    background = Color(0xFFF9F7FF),
    surface = Color(0xFFF9F7FF),
    surfaceVariant = Color(0xFFE7E0EC)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFCAB8FF),
    secondary = Color(0xFF64DBAE),
    tertiary = Color(0xFFFFB86B)
)

@Composable
fun KcffTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
