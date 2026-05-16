package com.transitkit.app.data.model

import androidx.compose.runtime.Immutable

/**
 * Origin or destination chosen by the user in the planner — unified across three
 * sources (GTFS stop, GPS position, free position on the map / search result).
 * Mirrors Movete's PlannerLocation. Routing snaps Place / CurrentLocation kinds
 * to the nearest GTFS stop internally; the picked lat/lon and name are preserved
 * for display.
 */
@Immutable
data class PlannerLocation(
    val kind: Kind,
    val name: String,
    val lat: Double,
    val lon: Double,
    /** Original GTFS stop id when kind == Stop, else null. */
    val stopId: String? = null,
    /** Route names to display when kind == Stop (Place / CurrentLocation: empty). */
    val routeNames: List<String> = emptyList(),
    val transitTypes: List<Int> = emptyList(),
) {
    enum class Kind { Stop, Place, CurrentLocation }

    companion object {
        fun fromStop(stop: ResolvedStop): PlannerLocation = PlannerLocation(
            kind = Kind.Stop,
            name = stop.name,
            lat = stop.lat,
            lon = stop.lon,
            stopId = stop.id,
            routeNames = stop.routeNames,
            transitTypes = stop.transitTypes,
        )
    }
}
