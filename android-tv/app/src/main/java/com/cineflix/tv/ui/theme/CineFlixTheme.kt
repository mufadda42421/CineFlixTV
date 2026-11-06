package com.cineflix.tv.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme
import androidx.compose.runtime.Composable

object CineFlixColors {
    val ObsidianBg = Color(0xFF080808)
    val SurfaceElevated = Color(0xFF0E0E12)
    val SurfaceCard = Color(0xFF14141C)
    val SurfaceFocused = Color(0xFF1E1E28)
    val Crimson = Color(0xFFE50914)
    val CrimsonHover = Color(0xFFF40612)
    val CrimsonGlow = Color(0x66E50914)
    val TextPrimary = Color(0xFFF4F4F5)
    val TextSecondary = Color(0xFFA1A1AA)
    val TextMuted = Color(0xFF71717A)
    val EmeraldMatch = Color(0xFF34D399)
    val AmberRating = Color(0xFFFBBF24)
    val CyanFast = Color(0xFF22D3EE)
    val BorderSubtle = Color(0x1AFFFFFF)
    val BorderFocus = Color(0xFFE50914)
}

@OptIn(ExperimentalTvMaterial3Api::class)
private val CineFlixDarkColorScheme = darkColorScheme(
    primary = CineFlixColors.Crimson,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A060A),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF27272F),
    onSecondary = CineFlixColors.TextPrimary,
    secondaryContainer = CineFlixColors.SurfaceCard,
    onSecondaryContainer = CineFlixColors.TextPrimary,
    tertiary = CineFlixColors.EmeraldMatch,
    onTertiary = Color.Black,
    background = CineFlixColors.ObsidianBg,
    onBackground = CineFlixColors.TextPrimary,
    surface = CineFlixColors.SurfaceElevated,
    onSurface = CineFlixColors.TextPrimary,
    surfaceVariant = CineFlixColors.SurfaceCard,
    onSurfaceVariant = CineFlixColors.TextSecondary,
    error = Color(0xFFEF4444),
    onError = Color.White,
    border = CineFlixColors.BorderSubtle
)

@OptIn(ExperimentalTvMaterial3Api::class)
val CineFlixTvTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 44.sp,
        lineHeight = 50.sp,
        letterSpacing = (-0.5).sp,
        color = CineFlixColors.TextPrimary
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.3).sp,
        color = CineFlixColors.TextPrimary
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        color = CineFlixColors.TextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 26.sp,
        color = CineFlixColors.TextPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = CineFlixColors.TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = CineFlixColors.TextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = CineFlixColors.TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = CineFlixColors.TextSecondary
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        color = CineFlixColors.TextPrimary
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = CineFlixColors.TextSecondary
    )
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CineFlixTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CineFlixDarkColorScheme,
        typography = CineFlixTvTypography,
        content = content
    )
}
