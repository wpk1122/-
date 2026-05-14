package com.chrona.ai.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ChronaLightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEAFE),
    onPrimaryContainer = Color(0xFF0F2F76),
    secondary = Color(0xFF0F766E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF123B37),
    tertiary = Color(0xFFF59E0B),
    onTertiary = Color(0xFF2F2100),
    tertiaryContainer = Color(0xFFFFE8B5),
    onTertiaryContainer = Color(0xFF4C3300),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF172033),
    surface = Color.White,
    onSurface = Color(0xFF172033),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8),
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
