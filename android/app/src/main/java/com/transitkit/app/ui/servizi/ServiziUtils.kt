package com.transitkit.app.ui.servizi

import android.net.Uri
import androidx.annotation.DrawableRes
import com.transitkit.app.config.LucideIcons

/**
 * Resolve a Lucide icon key (as used in config.json, e.g. "bus", "map-pin")
 * to the corresponding drawable resource id. Falls back to the info icon when unknown.
 */
@DrawableRes
internal fun iconDrawableFor(key: String): Int = when (key) {
    "accessibility" -> LucideIcons.Accessibility
    "bus" -> LucideIcons.BusFront
    "bus-front" -> LucideIcons.BusFront
    "clock" -> LucideIcons.Clock
    "compass" -> LucideIcons.Compass
    "external-link" -> LucideIcons.ExternalLink
    "globe" -> LucideIcons.Globe
    "headphones" -> LucideIcons.Headphones
    "info" -> LucideIcons.Info
    "list-ordered" -> LucideIcons.ListOrdered
    "mail" -> LucideIcons.Mail
    "map" -> LucideIcons.Map
    "map-pin" -> LucideIcons.MapPin
    "phone" -> LucideIcons.Phone
    "route" -> LucideIcons.Route
    "ticket" -> LucideIcons.Ticket
    "users" -> LucideIcons.Users
    else -> LucideIcons.Info
}

/** Strip everything except digits and leading '+'. */
internal fun String.phoneDigits(): String {
    val plus = if (startsWith("+")) "+" else ""
    return plus + filter { it.isDigit() }
}

/**
 * Dispatches `transitkit://` CTA values through in-app navigation callbacks
 * rather than external Intent.ACTION_VIEW. Returns true if handled.
 */
internal fun handleInternalCtaUri(
    value: String,
    onNavigateToMappa: () -> Unit,
): Boolean {
    val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return false
    if (uri.scheme != "transitkit") return false
    return when (uri.host) {
        "map" -> { onNavigateToMappa(); true }
        else -> false
    }
}
