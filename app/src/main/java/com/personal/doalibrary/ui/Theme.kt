package com.personal.doalibrary.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@Composable
fun AppTheme(mode: String, content: @Composable () -> Unit) {
    val dark = when (mode) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    val scheme = if (dark) darkColorScheme(
        primary = Color(0xFF5FC3A0), onPrimary = Color(0xFF0C1D17),
        background = Color(0xFF14181A), onBackground = Color(0xFFE8E6E1),
        surface = Color(0xFF1C2225), onSurface = Color(0xFFE8E6E1),
        surfaceVariant = Color(0xFF263033), onSurfaceVariant = Color(0xFF9AA3A0)
    ) else lightColorScheme(
        primary = Color(0xFF1F6B52), onPrimary = Color.White,
        background = Color(0xFFF6F1E7), onBackground = Color(0xFF2B2A26),
        surface = Color(0xFFFFFDF8), onSurface = Color(0xFF2B2A26),
        surfaceVariant = Color(0xFFEAE3D2), onSurfaceVariant = Color(0xFF77716A)
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun arabColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFF0E6C8) else Color(0xFF1B3A2F)
