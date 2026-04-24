package com.transitkit.app.ui.mappa

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.transitkit.app.config.LucideIcons

// ---------------------------------------------------------------------------
// Utilities condivise tra componenti di MappaScreen.
// ---------------------------------------------------------------------------

internal fun contrastingTextColor(bg: Color): Color {
    fun channel(c: Float): Double {
        val v = c.toDouble()
        return if (v <= 0.03928) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
    }
    val l = 0.2126 * channel(bg.red) + 0.7152 * channel(bg.green) + 0.0722 * channel(bg.blue)
    return if (l < 0.5) Color.White else Color.Black
}

@DrawableRes
internal fun iconForTransitType(type: Int) = when (type) {
    0 -> LucideIcons.Tram
    1 -> LucideIcons.Train
    2 -> LucideIcons.Train
    4 -> LucideIcons.Ship
    else -> LucideIcons.BusFront
}

@DrawableRes
internal fun stopPinIcon(transitTypes: Set<Int>): Int {
    val busTypes = setOf(3, 11)
    val hasBus = transitTypes.any { it in busTypes } || transitTypes.isEmpty()
    if (hasBus) return LucideIcons.Signpost
    val priority = listOf(4, 0, 1, 2, 6, 7, 5, 12)
    for (type in priority) if (type in transitTypes) return iconForTransitType(type)
    return LucideIcons.Signpost
}
