package com.chrona.ai.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ChronaLightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF1D4ED8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0ECFF),
    onPrimaryContainer = Color(0xFF0B2A6F),
    secondary = Color(0xFF0891B2),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF0E3B4A),
    tertiary = Color(0xFFD99A18),
    onTertiary = Color(0xFF2A1D00),
    tertiaryContainer = Color(0xFFFFEDBF),
    onTertiaryContainer = Color(0xFF4C3300),
    background = Color(0xFFF7FBFF),
    onBackground = Color(0xFF111827),
    surface = Color.White,
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFE8F1FF),
    onSurfaceVariant = Color(0xFF4B5D75),
    outline = Color(0xFF9DB4D0),
    error = Color(0xFFB91C1C),
    onError = Color.White
)

@Composable
fun ChronaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ChronaLightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
