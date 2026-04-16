package com.fibonacci.fibohealth.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary            = Indigo,
    secondary          = Cyan,
    background         = DarkBg,
    surface            = DarkSurface,
    onBackground       = Color.White,
    onSurface          = Color.White,
    onSurfaceVariant   = Color(0xFF94A3B8),
    outline            = DarkBorder
)

private val LightColors = lightColorScheme(
    primary            = Indigo,
    secondary          = Cyan,
    background         = LightBg,
    surface            = Color.White,
    onBackground       = Color(0xFF0F172A),
    onSurface          = Color(0xFF0F172A),
    onSurfaceVariant   = Color(0xFF64748B),
    outline            = Color(0xFFE2E8F0)
)

@Composable
fun FiboHealthTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography  = FiboTypography,
        content     = content
    )
}
