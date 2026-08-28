package com.lyf.composescaffold.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val lightColors = lightColorScheme(
    primary = Color(0xFFD92D32),
    onPrimary = Color.White,
    background = Color(0xFFF7F7F9),
    onBackground = Color(0xFF1B1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1B1B1F),
    onSurfaceVariant = Color(0xFF6F6F78),
    error = Color(0xFFB3261E),
)

private val darkColors = darkColorScheme(
    primary = Color(0xFFFFB3B5),
    onPrimary = Color(0xFF68000B),
    background = Color(0xFF111114),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1B1B1F),
    onSurface = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFFC9C5CC),
    error = Color(0xFFFFB4AB),
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) darkColors else lightColors,
        content = content,
    )
}
