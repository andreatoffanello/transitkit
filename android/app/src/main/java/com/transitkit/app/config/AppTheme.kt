package com.transitkit.app.config

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.transitkit.app.R

// ---------------------------------------------------------------------------
// Hex parsing
// ---------------------------------------------------------------------------

fun String.toColor(): Color {
    val hex = this.trimStart('#')
    return when (hex.length) {
        6 -> {
            val v = hex.toLong(16)
            Color(red = ((v shr 16) and 0xFF) / 255f,
                  green = ((v shr 8) and 0xFF) / 255f,
                  blue = (v and 0xFF) / 255f)
        }
        8 -> {
            val v = hex.toLong(16)
            Color(alpha = ((v shr 24) and 0xFF) / 255f,
                  red = ((v shr 16) and 0xFF) / 255f,
                  green = ((v shr 8) and 0xFF) / 255f,
                  blue = (v and 0xFF) / 255f)
        }
        else -> throw IllegalArgumentException("Invalid color hex: '$this'")
    }
}

private fun String?.toColorOrFallback(fallback: Color): Color =
    if (this != null && (length == 7 || length == 9) && startsWith('#')) toColor() else fallback

// ---------------------------------------------------------------------------
// Semantic color tokens
// ---------------------------------------------------------------------------

@Immutable
data class TransitColors(
    val background: Color,
    val bgSecondary: Color,
    val glassFill: Color,
    val glassBorder: Color,
    val separator: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val secondary: Color,
    val primary: Color,
    val realtimeGreen: Color,
    val realtimeRed: Color,
    val realtimeOrange: Color,
    val tabBarBg: Color,
    val tabInactive: Color,
)

val LocalTransitColors = staticCompositionLocalOf<TransitColors> {
    error("No TransitColors provided — wrap your app with TransitKitTheme")
}

// ---------------------------------------------------------------------------
// Factories
// ---------------------------------------------------------------------------

fun transitColorsLight(accent: Color, primary: Color) = TransitColors(
    background    = Color(0xFFF5F7FA),
    bgSecondary   = Color.White,
    glassFill     = Color.White.copy(alpha = 0.80f),
    glassBorder   = Color.Black.copy(alpha = 0.05f),
    separator     = Color.Black.copy(alpha = 0.04f),
    textPrimary   = Color(0xFF111827),
    textSecondary = Color(0xFF666666),
    textTertiary  = Color(0xFF999999),
    accent        = accent,
    secondary     = accent.copy(alpha = 0.7f),
    primary       = primary,
    realtimeGreen  = Color(0xFF22C55E),
    realtimeRed    = Color(0xFFEF4444),
    realtimeOrange = Color(0xFFF97316),
    tabBarBg       = Color.White.copy(alpha = 0.90f),
    tabInactive   = Color(0xFF9CA3AF),
)

fun transitColorsDark(accent: Color, primary: Color) = TransitColors(
    background    = Color(0xFF080C18),
    bgSecondary   = Color.White.copy(alpha = 0.05f),
    glassFill     = Color.White.copy(alpha = 0x14 / 255f),
    glassBorder   = Color.White.copy(alpha = 0.06f),
    separator     = Color.White.copy(alpha = 0.04f),
    textPrimary   = Color(0xFFF0F0F0),
    textSecondary = Color.White.copy(alpha = 0.50f),
    textTertiary  = Color.White.copy(alpha = 0.35f),
    accent        = accent,
    secondary     = accent.copy(alpha = 0.7f),
    primary       = primary,
    realtimeGreen  = Color(0xFF22C55E),
    realtimeRed    = Color(0xFFEF4444),
    realtimeOrange = Color(0xFFF97316),
    tabBarBg       = Color(0xFF131729).copy(alpha = 0.90f),
    tabInactive   = Color(0xFF555B6E),
)

// ---------------------------------------------------------------------------
// Material3 ColorScheme derivation
// ---------------------------------------------------------------------------

private fun buildLightColorScheme(accent: Color, primary: Color): ColorScheme =
    lightColorScheme(
        primary          = primary,
        onPrimary        = Color.White,
        primaryContainer = primary.copy(alpha = 0.12f),
        secondary        = accent,
        onSecondary      = Color.White,
        background       = Color(0xFFF5F7FA),
        surface          = Color.White,
        onBackground     = Color(0xFF111827),
        onSurface        = Color(0xFF111827),
    )

private fun buildDarkColorScheme(accent: Color, primary: Color): ColorScheme =
    darkColorScheme(
        primary          = primary,
        onPrimary        = Color.White,
        primaryContainer = primary.copy(alpha = 0.20f),
        secondary        = accent,
        onSecondary      = Color.White,
        background       = Color(0xFF080C18),
        surface          = Color(0xFF0F1424),
        onBackground     = Color(0xFFF0F0F0),
        onSurface        = Color(0xFFF0F0F0),
    )

// ---------------------------------------------------------------------------
// Typography — Inter via Google Fonts
// ---------------------------------------------------------------------------

private val fontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)
private val interFont = GoogleFont("Inter")
val InterFontFamily = FontFamily(
    Font(googleFont = interFont, fontProvider = fontsProvider, weight = FontWeight.Light),
    Font(googleFont = interFont, fontProvider = fontsProvider, weight = FontWeight.Normal),
    Font(googleFont = interFont, fontProvider = fontsProvider, weight = FontWeight.Medium),
    Font(googleFont = interFont, fontProvider = fontsProvider, weight = FontWeight.SemiBold),
    Font(googleFont = interFont, fontProvider = fontsProvider, weight = FontWeight.Bold),
)

private val transitTypography = Typography(
    displayLarge  = TextStyle(fontFamily = InterFontFamily, fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontFamily = InterFontFamily, fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = 0.sp),
    displaySmall  = TextStyle(fontFamily = InterFontFamily, fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp),
    headlineLarge  = TextStyle(fontFamily = InterFontFamily, fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp),
    headlineMedium = TextStyle(fontFamily = InterFontFamily, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp),
    headlineSmall  = TextStyle(fontFamily = InterFontFamily, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp),
    titleLarge  = TextStyle(fontFamily = InterFontFamily, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontFamily = InterFontFamily, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall  = TextStyle(fontFamily = InterFontFamily, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge  = TextStyle(fontFamily = InterFontFamily, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = InterFontFamily, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall  = TextStyle(fontFamily = InterFontFamily, fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = 0.4.sp),
    labelLarge  = TextStyle(fontFamily = InterFontFamily, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = InterFontFamily, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall  = TextStyle(fontFamily = InterFontFamily, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)

// ---------------------------------------------------------------------------
// Entry point composable
// ---------------------------------------------------------------------------

@Composable
fun TransitKitTheme(
    config: OperatorConfig,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val accentColor  = config.theme.accentColor.toColorOrFallback(Color(0xFF06845C))
    val primaryColor = config.theme.primaryColor.toColorOrFallback(Color(0xFF165F9C))

    val transitColors = if (darkTheme) {
        transitColorsDark(accent = accentColor, primary = primaryColor)
    } else {
        transitColorsLight(accent = accentColor, primary = primaryColor)
    }

    val colorScheme = if (darkTheme) buildDarkColorScheme(accentColor, primaryColor)
    else buildLightColorScheme(accentColor, primaryColor)

    CompositionLocalProvider(LocalTransitColors provides transitColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = transitTypography,
            content = content,
        )
    }
}

// ---------------------------------------------------------------------------
// Accessor shortcut
// ---------------------------------------------------------------------------

object TransitTheme {
    val colors: TransitColors
        @Composable get() = LocalTransitColors.current
}
