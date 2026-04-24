package com.transitkit.app.ui.orari

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Transit type helper
// ---------------------------------------------------------------------------

internal fun transitTypeDisplayName(transitType: Int): String = when (transitType) {
    0 -> "Tram"
    1 -> "Metro"
    2 -> "Treno"
    4 -> "Ferry"
    else -> "Bus"
}

/** WCAG relative-luminance-based foreground picker for GTFS route badge backgrounds. */
internal fun routeBadgeContrast(bg: Color): Color {
    fun lin(c: Float): Float = if (c <= 0.03928f) c / 12.92f else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    val l = 0.2126f * lin(bg.red) + 0.7152f * lin(bg.green) + 0.0722f * lin(bg.blue)
    return if (l < 0.5f) Color.White else Color(0xFF111827)
}
