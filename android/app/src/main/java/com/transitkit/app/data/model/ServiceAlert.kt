package com.transitkit.app.data.model

/** GTFS-RT SeverityLevel enum. Default UNKNOWN when feed omits it. */
enum class AlertSeverity(val raw: Int) {
    UNKNOWN(1), INFO(2), WARNING(3), SEVERE(4);

    companion object {
        fun fromRaw(v: Int): AlertSeverity = values().firstOrNull { it.raw == v } ?: UNKNOWN
    }
}

/** GTFS-RT Effect enum. */
enum class AlertEffect(val raw: Int) {
    NO_SERVICE(1),
    REDUCED_SERVICE(2),
    SIGNIFICANT_DELAYS(3),
    DETOUR(4),
    ADDITIONAL_SERVICE(5),
    MODIFIED_SERVICE(6),
    OTHER_EFFECT(7),
    UNKNOWN_EFFECT(8),
    STOP_MOVED(9),
    NO_EFFECT(10),
    ACCESSIBILITY_ISSUE(11);

    companion object {
        fun fromRaw(v: Int): AlertEffect = values().firstOrNull { it.raw == v } ?: UNKNOWN_EFFECT
    }
}

/**
 * One active window during which an alert applies. Either endpoint may be null
 * (±infinity). Values are POSIX epoch seconds.
 */
data class AlertTimeRange(val start: Long?, val end: Long?) {
    fun contains(epoch: Long): Boolean {
        if (start != null && epoch < start) return false
        if (end != null && epoch >= end) return false
        return true
    }
}

/**
 * Decoded GTFS-RT service alert. `headerText` and `descriptionText` are
 * language-code → text maps (BCP-47 primary tag, lowercase). Consumers resolve
 * via `LocalizedText.resolved()`.
 */
data class ServiceAlert(
    val id: String,
    val activePeriods: List<AlertTimeRange>,
    val severity: AlertSeverity,
    val effect: AlertEffect,
    val headerText: Map<String, String>,
    val descriptionText: Map<String, String>,
    val affectedStopIds: Set<String>,
    val affectedRouteIds: Set<String>,
    val url: String?,
) {
    /** True if any active period covers the given epoch, or if none is set. */
    fun isActive(epoch: Long): Boolean {
        if (activePeriods.isEmpty()) return true
        return activePeriods.any { it.contains(epoch) }
    }
}
