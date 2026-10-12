package com.transitkit.app.config

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
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
// WCAG contrast + accent legibility
// ---------------------------------------------------------------------------
//
// `accentColor` è config di operatore ed è validato SOLO contro il bianco
// (`textOnPrimary`). Usato grezzo come *foreground* sul dark quasi-nero fallisce:
// appalcart #06845C sta a 4.15:1, charlottesville #0066CC a 3.51:1. Un nuovo
// operatore può stare peggio, quindi la correzione va calcolata, non scelta a mano.

private fun linearize(c: Float): Double {
    val d = c.toDouble()
    return if (d <= 0.04045) d / 12.92 else Math.pow((d + 0.055) / 1.055, 2.4)
}

private fun Color.relativeLuminance(): Double =
    0.2126 * linearize(red) + 0.7152 * linearize(green) + 0.0722 * linearize(blue)

fun contrastRatio(a: Color, b: Color): Double {
    val la = a.relativeLuminance()
    val lb = b.relativeLuminance()
    return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
}

/**
 * Schiarisce [this] in HSL finché non supera [minRatio] contro [against].
 * Ritorna l'originale se già passa. Parità con iOS `readableOnDark()`.
 *
 * HSL e non HSV di proposito: alzare la V di un blu scuro saturo (#000080) si
 * ferma a #0000FF, che contro il dark sta ancora a 2.2:1 e il loop scadrebbe.
 * La L di HSL converge verso il bianco, quindi termina sempre con un colore che
 * conserva l'hue del brand.
 *
 * [minRatio] è 5.2 e non 4.5 perché [against] è la superficie più scura che
 * esista: quasi nulla è disegnato direttamente su #080C18. Le card sono
 * bgSecondary (bianco 5% sopra) e le card sopra la mappa sono più chiare ancora.
 * Misurato: convergere esatti a 4.5 sul background lascia appalcart a 4.26:1 e
 * charlottesville a 4.30:1 sulla card — sotto AA proprio dove il colore è
 * dipinto davvero. A 5.2 diventano 4.81 e 4.90. Stessa soglia su iOS
 * (`readableOnDark` in ColorUtils.swift); quando aggiorni una, aggiorna l'altra.
 */
fun Color.readableOnDark(
    against: Color = Color(0xFF080C18),
    minRatio: Double = 5.2,
    maxSteps: Int = 50,
): Color {
    if (contrastRatio(this, against) >= minRatio) return this

    val (h, s0, l0) = rgbToHsl(red, green, blue)
    // Un accent completamente desaturato smette di leggersi come brand: tieni un
    // pavimento sulla saturazione mentre sali di luminosità, così l'hue sopravvive.
    val s = maxOf(s0, 0.25f)
    var l = l0
    repeat(maxSteps) {
        l = minOf(1f, l + 0.02f)
        val candidate = hslToColor(h, s, l)
        if (contrastRatio(candidate, against) >= minRatio) return candidate
    }
    return Color(0xFFF0F0F0)
}

/**
 * Specchio di [readableOnDark] per il tema chiaro: scurisce [this] in HSL finché
 * non supera [minRatio] contro [against].
 *
 * Serve la stessa protezione del ramo dark, e per la stessa ragione: l'accent
 * arriva dal config dell'operatore. Un verde chiaro (#4CAF50) come label di un
 * bottone secondario sul proprio fill tonale sta a 2.5:1 — di giorno il bottone
 * sparisce. Scendere di L conserva l'hue del brand dove alzarla lo laverebbe.
 */
fun Color.readableOnLight(
    against: Color = Color(0xFFFFFFFF),
    minRatio: Double = 4.5,
    maxSteps: Int = 50,
): Color {
    if (contrastRatio(this, against) >= minRatio) return this

    val (h, s0, l0) = rgbToHsl(red, green, blue)
    val s = maxOf(s0, 0.25f)
    var l = l0
    repeat(maxSteps) {
        l = maxOf(0f, l - 0.02f)
        val candidate = hslToColor(h, s, l)
        if (contrastRatio(candidate, against) >= minRatio) return candidate
    }
    return Color(0xFF111827)
}

private fun rgbToHsl(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
    val maxV = maxOf(r, g, b)
    val minV = minOf(r, g, b)
    val l = (maxV + minV) / 2f
    if (maxV == minV) return Triple(0f, 0f, l)
    val d = maxV - minV
    val s = if (l > 0.5f) d / (2f - maxV - minV) else d / (maxV + minV)
    val h = when (maxV) {
        r -> (g - b) / d + (if (g < b) 6f else 0f)
        g -> (b - r) / d + 2f
        else -> (r - g) / d + 4f
    }
    return Triple(h / 6f, s, l)
}

private fun hslToColor(h: Float, s: Float, l: Float): Color {
    if (s == 0f) return Color(l, l, l)
    fun hue2rgb(p: Float, q: Float, t0: Float): Float {
        var t = t0
        if (t < 0f) t += 1f
        if (t > 1f) t -= 1f
        return when {
            t < 1f / 6f -> p + (q - p) * 6f * t
            t < 1f / 2f -> q
            t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
            else -> p
        }
    }
    val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
    val p = 2f * l - q
    return Color(hue2rgb(p, q, h + 1f / 3f), hue2rgb(p, q, h), hue2rgb(p, q, h - 1f / 3f))
}

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
    /** Accent utilizzabile come FOREGROUND anche in dark (schiarito se serve).
     *  `accent` resta corretto per fill pieni e stroke con bianco sopra. */
    val accentReadable: Color,
    /** Label + icona di un bottone secondario/ghost/outline. In dark è bianco:
     *  su #080C18 nessun accent della flotta regge 4.5:1, e su una card mappa
     *  scendono sotto 3:1. L'identità brand resta al fill dei bottoni primari.
     *  In light resta l'accent, scurito quanto serve per reggere 4.5:1 sul
     *  proprio fill tonale (vedi [readableOnLight]). */
    val secondaryFg: Color,
    /** Fill tonale sotto un bottone secondario. Suggerisce l'area tappabile,
     *  non porta contrasto: quello lo fa [secondaryFg]. */
    val secondaryFill: Color,
    /** Bordo di un bottone outline. Mai `accent.copy(alpha=…)` in dark. */
    val secondaryBorder: Color,
    val realtimeGreen: Color,
    val realtimeRed: Color,
    val realtimeOrange: Color,
    val tabBarBg: Color,
    val tabInactive: Color,
)

val LocalTransitColors = staticCompositionLocalOf<TransitColors> {
    error("No TransitColors provided — wrap your app with TransitKitTheme")
}

val LocalOperatorConfig = staticCompositionLocalOf<OperatorConfig> {
    error("No OperatorConfig provided — wrap your app with TransitKitTheme")
}

// ---------------------------------------------------------------------------
// Factories
// ---------------------------------------------------------------------------

fun transitColorsLight(accent: Color, primary: Color): TransitColors {
    // Il fill tonale è il fondo PIÙ SCURO su cui secondaryFg finisce (più della
    // card bianca e più di #F5F7FA): per un foreground scuro è lì che il
    // contrasto è minimo, quindi è lì che va misurato.
    val secondaryFillSolid = accent.copy(alpha = 0.10f).compositeOver(Color(0xFFF5F7FA))
    val readable = accent.readableOnLight(against = secondaryFillSolid)
    return TransitColors(
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
        accentReadable = readable,
        secondaryFg    = readable,
        secondaryFill  = accent.copy(alpha = 0.10f),
        secondaryBorder = accent.copy(alpha = 0.30f),
        realtimeGreen  = Color(0xFF22C55E),
        realtimeRed    = Color(0xFFEF4444),
        realtimeOrange = Color(0xFFF97316),
        tabBarBg       = Color.White.copy(alpha = 0.90f),
        tabInactive   = Color(0xFF9CA3AF),
    )
}

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
    accentReadable = accent.readableOnDark(),
    secondaryFg    = Color(0xFFF0F0F0),
    secondaryFill  = Color.White.copy(alpha = 0.10f),
    secondaryBorder = Color.White.copy(alpha = 0.18f),
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
        // `primary` è il primaryColor dell'operatore, un colore pensato per fondi
        // CHIARI (appalcart #165F9C): come ruolo M3 `primary` finisce sotto ogni
        // TextButton/OutlinedButton/Checkbox/Switch non stilizzato e in dark è
        // illeggibile. Il default M3 va reso sicuro per costruzione, invece di
        // sperare che ogni call site si ricordi di sovrascriverlo.
        primary          = primary.readableOnDark(),
        onPrimary        = Color(0xFF080C18),
        primaryContainer = primary.copy(alpha = 0.20f),
        secondary        = accent.readableOnDark(),
        onSecondary      = Color(0xFF080C18),
        background       = Color(0xFF080C18),
        surface          = Color(0xFF0F1424),
        onBackground     = Color(0xFFF0F0F0),
        onSurface        = Color(0xFFF0F0F0),
    )

// ---------------------------------------------------------------------------
// Typography — Inter (bundled static fonts in res/font/)
// ---------------------------------------------------------------------------
// Bundling Inter avoids the downloadable-font runtime races and the silent
// fallback to Roboto when Google Play Services / its fonts provider isn't
// reachable (typical on `google_apis` AVDs without Play Store).

val InterFontFamily = FontFamily(
    androidx.compose.ui.text.font.Font(R.font.inter_light,    FontWeight.Light),
    androidx.compose.ui.text.font.Font(R.font.inter_regular,  FontWeight.Normal),
    androidx.compose.ui.text.font.Font(R.font.inter_medium,   FontWeight.Medium),
    androidx.compose.ui.text.font.Font(R.font.inter_semibold, FontWeight.SemiBold),
    androidx.compose.ui.text.font.Font(R.font.inter_bold,     FontWeight.Bold),
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

/**
 * Superficie OPACA per card/pill flottanti SULLA MAPPA. In dark `bgSecondary`
 * è glass (White 5%): sopra al `background` delle schermate funziona, sopra
 * la mappa diventa trasparente (si vedono le strade attraverso la card).
 * Compositandolo sul background si ottiene lo stesso colore percepito, opaco.
 * Light: White su quasi-bianco → invariato.
 */
val TransitColors.surfaceOverMap: Color
    get() {
        // Card OPACA e leggibile, ma NON un grigio piatto: in dark prende una
        // leggera velatura del colore brand (accent) così resta coerente con lo
        // shader teal/verde della home invece di sembrare un blocco grigio morto.
        // È esattamente il "vetro che pescava il teal dello sfondo" del look
        // originale, reso però opaco. In light resta pulita (bianca). UNICA
        // superficie per card / input / blocchi / controlli mappa — un solo
        // materiale, coerente ovunque.
        val base = bgSecondary.compositeOver(bgSecondary.compositeOver(background))
        return if (background.red > 0.5f) base
        else accent.copy(alpha = 0.12f).compositeOver(base)
    }

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

    // Icone delle system bar coerenti col tema, aggiornate a ogni cambio
    // light/dark. I COLORI delle fasce non passano più da statusBarColor /
    // navigationBarColor (ignorati da API 35 in edge-to-edge): li disegna
    // SystemBarsInsetFrame qui sotto.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalTransitColors provides transitColors,
        LocalOperatorConfig provides config,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = transitTypography,
        ) {
            // CRUCIALE: MaterialTheme NON propaga automaticamente LocalTextStyle.
            // Senza questo, ogni Text() che usa solo (fontSize / fontWeight)
            // senza `style = MaterialTheme.typography.X` ricade su Roboto di
            // sistema invece che su Inter. Forziamo LocalTextStyle = bodyLarge
            // (Inter Regular 16sp) come default; le typography variants restano
            // comunque accessibili via `style = MaterialTheme.typography.X`.
            CompositionLocalProvider(
                androidx.compose.material3.LocalTextStyle provides transitTypography.bodyLarge.copy(
                    fontFamily = InterFontFamily,
                ),
            ) {
                content()
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Accessor shortcut
// ---------------------------------------------------------------------------

object TransitTheme {
    val colors: TransitColors
        @Composable get() = LocalTransitColors.current

    val config: OperatorConfig
        @Composable get() = LocalOperatorConfig.current
}
