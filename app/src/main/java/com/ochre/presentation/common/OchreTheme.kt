package com.ochre.presentation.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object OchreColors {
    val Background = Color(0xFF000000)
    val Surface = Color(0xFF0D0D0D)
    val Accent = Color(0xFFE4A853)
    val TextPrimary = Color(0xFFF0EDE8)
    val TextSecondary = Color(0xFF6B6B6B)
    val Destructive = Color(0xFFCF6679)
}

private val OchreColorScheme = darkColorScheme(
    background = OchreColors.Background,
    surface = OchreColors.Surface,
    primary = OchreColors.Accent,
    onBackground = OchreColors.TextPrimary,
    onSurface = OchreColors.TextPrimary,
    onPrimary = OchreColors.Background,
)

private val OchreTypography = Typography(
    bodyLarge = TextStyle(
        color = OchreColors.TextPrimary,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        color = OchreColors.TextPrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        color = OchreColors.TextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    )
)

@Composable
fun OchreTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OchreColorScheme,
        typography = OchreTypography,
        content = content
    )
}
