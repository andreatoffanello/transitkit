package com.transitkit.app.data.model

data class PlannerStop(val id: String, val name: String, val lat: Double, val lng: Double)

data class Journey(
    val id: String,
    val departureTime: Long,  // epoch millis
    val arrivalTime: Long,    // epoch millis
    val legs: List<JourneyLeg>,
)

val Journey.transfers: Int get() = (legs.filterIsInstance<TransitLeg>().size - 1).coerceAtLeast(0)

val Journey.durationSeconds: Long get() = ((arrivalTime - departureTime) / 1_000L).coerceAtLeast(0L)

val Journey.durationMinutes: Long get() = durationSeconds / 60L

sealed class JourneyLeg

data class TransitLeg(
    val boardStop: PlannerStop,
    val alightStop: PlannerStop,
    val departureTime: Long,   // epoch millis
    val arrivalTime: Long,     // epoch millis
    val routeName: String,
    val routeColor: String,    // hex without #, e.g. "2D7F3C"
    val routeTextColor: String,
    val tripId: String,
    val intermediateStops: List<IntermediateStop>,
    val headsign: String = "",
    /** Encoded polyline (Google Algorithm, precision 7 per MOTIS) — null when unavailable. */
    val polyline: String? = null,
) : JourneyLeg()

data class WalkingLeg(
    val fromStop: PlannerStop,
    val toStop: PlannerStop,
    val walkSeconds: Int,
    val walkMeters: Int = 0,
    /** Encoded polyline (Google Algorithm, precision 7 per MOTIS) — null when unavailable. */
    val polyline: String? = null,
) : JourneyLeg()

/** Total walking duration across all walking legs (seconds). */
val Journey.totalWalkSeconds: Int
    get() = legs.filterIsInstance<WalkingLeg>().sumOf { it.walkSeconds }

data class IntermediateStop(
    val id: String,
    val name: String,
    val time: String,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
)
