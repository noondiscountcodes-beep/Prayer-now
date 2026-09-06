package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PolishColorScheme = lightColorScheme(
    primary = ProfessionalEmerald,
    onPrimary = Color.White,
    primaryContainer = ProfessionalEmeraldTint,
    onPrimaryContainer = ProfessionalEmeraldDark,
    secondary = PolishGold,
    onSecondary = Color.White,
    background = PolishCanvasBg,
    onBackground = PolishTextPrimary,
    surface = PolishSurface,
    onSurface = PolishTextPrimary,
    surfaceVariant = PolishIconBox,
    onSurfaceVariant = PolishTextSecondary,
    outline = PolishBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = ProfessionalEmeraldLight,
    onPrimary = Color.White,
    primaryContainer = ProfessionalEmeraldDark,
    onPrimaryContainer = Color.White,
    secondary = PolishGold,
    onSecondary = Color.Black,
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else PolishColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
