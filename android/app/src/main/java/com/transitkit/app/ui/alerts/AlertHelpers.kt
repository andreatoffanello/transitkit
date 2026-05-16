package com.transitkit.app.ui.alerts

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.data.model.AlertCause
import com.transitkit.app.data.model.AlertEffect
import com.transitkit.app.data.model.ServiceAlert

/** Localized cause label. `null` for unknown/other (signals "no signifier label"). */
@Composable
fun causeName(cause: AlertCause): String? = when (cause) {
    AlertCause.UNKNOWN_CAUSE, AlertCause.OTHER_CAUSE -> null
    AlertCause.TECHNICAL_PROBLEM -> stringResource(R.string.alert_cause_technical)
    AlertCause.STRIKE -> stringResource(R.string.alert_cause_strike)
    AlertCause.DEMONSTRATION -> stringResource(R.string.alert_cause_demonstration)
    AlertCause.ACCIDENT -> stringResource(R.string.alert_cause_accident)
    AlertCause.HOLIDAY -> stringResource(R.string.alert_cause_holiday)
    AlertCause.WEATHER -> stringResource(R.string.alert_cause_weather)
    AlertCause.MAINTENANCE -> stringResource(R.string.alert_cause_maintenance)
    AlertCause.CONSTRUCTION -> stringResource(R.string.alert_cause_construction)
    AlertCause.POLICE_ACTIVITY -> stringResource(R.string.alert_cause_police)
    AlertCause.MEDICAL_EMERGENCY -> stringResource(R.string.alert_cause_medical)
}

/** Localized effect label. `null` for unknown/no-effect/other. */
@Composable
fun effectName(effect: AlertEffect): String? = when (effect) {
    AlertEffect.NO_SERVICE -> stringResource(R.string.alert_effect_no_service)
    AlertEffect.REDUCED_SERVICE -> stringResource(R.string.alert_effect_reduced_service)
    AlertEffect.SIGNIFICANT_DELAYS -> stringResource(R.string.alert_effect_delays)
    AlertEffect.DETOUR -> stringResource(R.string.alert_effect_detour)
    AlertEffect.ADDITIONAL_SERVICE -> stringResource(R.string.alert_effect_additional)
    AlertEffect.MODIFIED_SERVICE -> stringResource(R.string.alert_effect_modified)
    AlertEffect.STOP_MOVED -> stringResource(R.string.alert_effect_stop_moved)
    AlertEffect.ACCESSIBILITY_ISSUE -> stringResource(R.string.alert_effect_accessibility)
    AlertEffect.NO_EFFECT, AlertEffect.OTHER_EFFECT, AlertEffect.UNKNOWN_EFFECT -> null
}

/**
 * Actionable title built from cause + effect — replaces the bureaucratic
 * header text in the primary UI role. Mirrors the Movete pattern.
 */
@Composable
fun displayTitle(alert: ServiceAlert): String {
    val c = causeName(alert.cause)
    val e = effectName(alert.effect)
    return when {
        c != null && e != null -> "$c · $e"
        c != null -> c
        e != null -> e
        else -> stringResource(R.string.alert_default_title)
    }
}

/**
 * Signifier icon for a GTFS-RT cause. Falls back to `AlertTriangle` when no
 * dedicated icon exists in the asset catalog yet (wrench, megaphone, cone,
 * cloud-rain, calendar — to be added).
 */
@DrawableRes
fun alertCauseIcon(cause: AlertCause): Int = when (cause) {
    AlertCause.STRIKE, AlertCause.DEMONSTRATION -> LucideIcons.Users
    AlertCause.HOLIDAY -> LucideIcons.Bell
    AlertCause.POLICE_ACTIVITY -> LucideIcons.Shield
    else -> LucideIcons.AlertTriangle
}
