package com.aegis.cloak.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Tactical Military OLED Palette
val TacticalDarkBg = Color(0xFF080C10)
val TacticalSurface = Color(0xFF0D1117)
val TacticalCardBg = Color(0xFF161B22)
val TacticalBorder = Color(0xFF30363D)
val TacticalCyan = Color(0xFF00F0FF)
val TacticalCyanGlow = Color(0x3300F0FF)
val TacticalAmber = Color(0xFFFFB000)
val TacticalAmberGlow = Color(0x33FFB000)
val TacticalCrimson = Color(0xFFFF3B30)
val TacticalCrimsonGlow = Color(0x33FF3B30)
val TacticalGreen = Color(0xFF00E676)
val TacticalGreenGlow = Color(0x3300E676)
val TacticalTextPrimary = Color(0xFFF0F6FC)
val TacticalTextSecondary = Color(0xFF8B949E)
val TacticalTextMuted = Color(0xFF484F58)

private val TacticalColorScheme = darkColorScheme(
    primary = TacticalCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF0A2230),
    onPrimaryContainer = TacticalCyan,
    secondary = TacticalAmber,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF2C1F05),
    onSecondaryContainer = TacticalAmber,
    tertiary = TacticalGreen,
    background = TacticalDarkBg,
    onBackground = TacticalTextPrimary,
    surface = TacticalSurface,
    onSurface = TacticalTextPrimary,
    surfaceVariant = TacticalCardBg,
    onSurfaceVariant = TacticalTextSecondary,
    outline = TacticalBorder,
    error = TacticalCrimson,
    onError = Color.White
)

val TacticalTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = 1.5.sp,
        color = TacticalTextPrimary
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = 1.2.sp,
        color = TacticalTextPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        letterSpacing = 1.0.sp,
        color = TacticalTextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.8.sp,
        color = TacticalTextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp,
        color = TacticalTextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp,
        color = TacticalTextSecondary
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 1.2.sp,
        color = TacticalCyan
    )
)

@Composable
fun AegisCloakTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TacticalColorScheme,
        typography = TacticalTypography,
        content = content
    )
}
