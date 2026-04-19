package com.transitkit.app.ui.components

import androidx.annotation.DrawableRes
import com.transitkit.app.config.LucideIcons

// -----------------------------------------------------------------------------
// Stop icon resolver — design system helper.
// -----------------------------------------------------------------------------
// In the TransitKit design system, a *stop* is visually represented by a
// signpost ("the sign at the corner"), not by the vehicle icon. The exception
// is stops that exclusively serve non-bus modes (ferry-only, rail-only,
// tram-only), where the mode icon carries more information.
//
// Use `stopIcon(transitTypes)` whenever rendering the leading glyph for a
// stop entity (favorite rows, home nearby list, line detail stops, full
// stop detail header). For a route/vehicle icon — stay with `routeIcon()`
// (bus → BusFront, tram → Train, …).
// -----------------------------------------------------------------------------

/** GTFS route_type → single-mode icon. Bus routes resolve to the vehicle
 *  icon (BusFront) here — this is the *route/vehicle* icon resolver. */
@DrawableRes
fun routeIcon(type: Int): Int = when (type) {
    0 -> LucideIcons.Tram            // Tram, Streetcar, Light rail
    1, 2, 12 -> LucideIcons.Train    // Subway, Rail, Monorail
    4 -> LucideIcons.Ship            // Ferry
    3, 11 -> LucideIcons.BusFront    // Bus, Trolleybus
    else -> LucideIcons.BusFront
}

/** A stop is a signpost, unless it exclusively serves a non-bus mode in
 *  which case the mode icon is more informative (e.g. a ferry-only pier
 *  renders the ferry icon). */
@DrawableRes
fun stopIcon(transitTypes: Collection<Int>): Int {
    val busTypes = setOf(3, 11)
    val hasBusOrUnknown = transitTypes.isEmpty() || transitTypes.any { it in busTypes }
    if (hasBusOrUnknown) return LucideIcons.Signpost
    // Non-bus-exclusive stop — pick the most prominent mode by preference:
    // ferry > tram > subway > rail > cable/gondola/funicular > monorail.
    val priority = listOf(4, 0, 1, 2, 6, 7, 5, 12)
    for (type in priority) if (type in transitTypes) return routeIcon(type)
    return LucideIcons.Signpost
}
