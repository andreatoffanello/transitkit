package com.transitkit.app.data.model

data class PlannerStop(val id: String, val name: String, val lat: Double, val lng: Double)

data class Journey(
    val id: String,
    val departureTime: Long,  // epoch millis
    val arrivalTime: Long,    // epoch millis
    val legs: List<JourneyLeg>,
)

val Journey.transfers: Int get() = (legs.filterIsInstance<TransitLeg>().size - 1).coerceAtLeast(0)

val Journey.durationMinutes: Long get() = (arrivalTime - departureTime) / 60_000L

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
) : JourneyLeg()

data class WalkingLeg(
    val fromStop: PlannerStop,
    val walkSeconds: Int,
) : JourneyLeg()

data class IntermediateStop(val id: String, val name: String, val time: String)
