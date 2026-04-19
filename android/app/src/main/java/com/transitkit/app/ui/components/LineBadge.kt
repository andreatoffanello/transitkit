package com.transitkit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.data.model.ScheduleRoute
import com.transitkit.app.data.model.ResolvedDeparture

// -----------------------------------------------------------------------------
// LineBadge — the only component that renders a GTFS line pill in the app.
// -----------------------------------------------------------------------------
// Same design as the iOS sibling. Scope: departure rows, line lists, stop
// coincidences, filter chips, sheet headers, vehicle/trip cards.
//
// Map annotations (VehicleAnnotationView, StopAnnotationView) are NOT in
// scope: they use custom shapes (dot + halo, pin + tail) and stay as-is.
//
// Single-operator-per-app product → no "operator logo" chip variant.
// `showTransitIcon` toggles an optional left-side modal icon (bus / tram /
// rail / ferry) for multi-modal agencies.
// -----------------------------------------------------------------------------

enum class LineBadgeSize { Small, Medium, Large }

private data class LineBadgeDims(
    val fontSize: TextUnit,
    val iconSize: Dp,
    val padH: Dp,
    val padV: Dp,
    val minWidth: Dp,
    val radius: Dp,
    val spacing: Dp,
)

private fun LineBadgeSize.dims(): LineBadgeDims = when (this) {
    LineBadgeSize.Small -> LineBadgeDims(11.sp, 12.dp, 6.dp, 3.dp, 24.dp, 4.dp, 3.dp)
    LineBadgeSize.Medium -> LineBadgeDims(13.sp, 14.dp, 8.dp, 4.dp, 32.dp, 6.dp, 5.dp)
    LineBadgeSize.Large -> LineBadgeDims(15.sp, 16.dp, 10.dp, 5.dp, 40.dp, 6.dp, 6.dp)
}

/**
 * Convenience overload from a [ScheduleRoute]. Defaults to [LineBadgeSize.Large]
 * (primary context: lines list, line detail header, departure rows).
 */
@Composable
fun LineBadge(
    route: ScheduleRoute,
    size: LineBadgeSize = LineBadgeSize.Large,
    showTransitIcon: Boolean = false,
    modifier: Modifier = Modifier,
) {
    LineBadge(
        name = route.name,
        colorHex = route.color,
        textColorHex = route.textColor,
        transitType = route.transitType,
        size = size,
        showTransitIcon = showTransitIcon,
        modifier = modifier,
    )
}

/**
 * Convenience overload from a [ResolvedDeparture]. Defaults to [LineBadgeSize.Large].
 */
@Composable
fun LineBadge(
    departure: ResolvedDeparture,
    size: LineBadgeSize = LineBadgeSize.Large,
    showTransitIcon: Boolean = false,
    modifier: Modifier = Modifier,
) {
    LineBadge(
        name = departure.routeName,
        colorHex = departure.routeColor,
        textColorHex = departure.routeTextColor,
        transitType = departure.transitType,
        size = size,
        showTransitIcon = showTransitIcon,
        modifier = modifier,
    )
}

/**
 * Low-level badge — expects hex color strings. Pass [textColorHex] = null to
 * auto-derive by WCAG luminance.
 */
@Composable
fun LineBadge(
    name: String,
    colorHex: String?,
    textColorHex: String? = null,
    transitType: Int? = null,
    size: LineBadgeSize = LineBadgeSize.Medium,
    showTransitIcon: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val d = size.dims()
    val bg = parseHexColor(colorHex, fallback = Color(0xFF3B82F6))
    val fg = resolveTextColor(textColorHex, background = bg)
    val cd = "Linea $name"

    Row(
        modifier = modifier
            .defaultMinSize(minWidth = d.minWidth)
            .background(bg, RoundedCornerShape(d.radius))
            .padding(horizontal = d.padH, vertical = d.padV)
            .semantics { contentDescription = cd },
        horizontalArrangement = Arrangement.spacedBy(d.spacing, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showTransitIcon) {
            transitIcon(transitType)?.let { iconRes ->
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(d.iconSize),
                )
            }
        }
        androidx.compose.material3.Text(
            text = name,
            color = fg,
            fontSize = d.fontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

// -----------------------------------------------------------------------------
// Color helpers
// -----------------------------------------------------------------------------

/**
 * Parse a GTFS hex color ("#AABBCC" or "AABBCC"). Falls back to [fallback]
 * on any parse failure — feeds sometimes publish blank strings.
 */
fun parseHexColor(hex: String?, fallback: Color = Color(0xFF3B82F6)): Color {
    val s = hex?.trim()?.removePrefix("#") ?: return fallback
    if (s.length != 6 && s.length != 8) return fallback
    return runCatching {
        val argb = if (s.length == 6) "FF$s" else s
        Color(argb.toLong(16).toInt())
    }.getOrDefault(fallback)
}

/**
 * WCAG 2.1 relative luminance of a color.
 */
private fun Color.luminance(): Double {
    fun linearize(c: Float): Double {
        val v = c.toDouble()
        return if (v <= 0.03928) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
    }
    return 0.2126 * linearize(red) + 0.7152 * linearize(green) + 0.0722 * linearize(blue)
}

/**
 * Returns a text color with WCAG-compliant contrast:
 * - Null / sentinel ("000000"/"FFFFFF") → auto-derive from background luminance.
 * - Provided value kept only if it passes 4.5:1 ratio; otherwise overridden.
 */
private fun resolveTextColor(textColorHex: String?, background: Color): Color {
    val sentinels = setOf("", "000000", "FFFFFF", "000", "FFF")
    val cleaned = textColorHex?.trim()?.removePrefix("#")?.uppercase()
    if (cleaned.isNullOrEmpty() || cleaned in sentinels) {
        return if (background.luminance() > 0.5) Color.Black else Color.White
    }
    val candidate = parseHexColor(cleaned, fallback = Color.White)
    val lBg = background.luminance()
    val lFg = candidate.luminance()
    val ratio = if (lBg > lFg) (lBg + 0.05) / (lFg + 0.05) else (lFg + 0.05) / (lBg + 0.05)
    return if (ratio >= 4.5) candidate
        else if (lBg > 0.5) Color.Black else Color.White
}

/**
 * Optional transit modal icon. Returns null when [transitType] is missing or
 * unmapped — the badge will just render as a color pill without icon.
 * GTFS route_type mapping per spec.
 */
private fun transitIcon(transitType: Int?): Int? = when (transitType) {
    0 -> LucideIcons.Tram          // 0 = Tram, Streetcar, Light rail
    1, 2, 12 -> LucideIcons.Train  // 1 = Subway/Metro, 2 = Rail, 12 = Monorail
    3, 11 -> LucideIcons.BusFront       // 3 = Bus, 11 = Trolleybus
    4 -> LucideIcons.Ship          // 4 = Ferry
    else -> null
}
