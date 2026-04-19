package com.transitkit.app.data.model

import androidx.compose.runtime.Immutable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Stop(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val routes: List<String>? = null,
)

@JsonClass(generateAdapter = true)
data class Route(
    val id: String,
    @Json(name = "short_name") val shortName: String,
    @Json(name = "long_name") val longName: String? = null,
    val color: String? = null,
    @Json(name = "text_color") val textColor: String? = null,
)

@Immutable
@JsonClass(generateAdapter = true)
data class Departure(
    @Json(name = "trip_id") val tripId: String,
    @Json(name = "route_id") val routeId: String,
    @Json(name = "route_short_name") val routeShortName: String,
    val headsign: String,
    @Json(name = "departure_time") val departureTime: String,
    @Json(name = "realtime_departure_time") val realtimeDepartureTime: String? = null,
    val delay: Int? = null,
    @Json(name = "is_realtime") val isRealtime: Boolean = false,
    @Json(name = "route_color") val routeColor: String? = null,
    @Json(name = "route_text_color") val routeTextColor: String? = null,
    val transitType: Int = 3,
)

@JsonClass(generateAdapter = true)
data class TripDetail(
    @Json(name = "trip_id") val tripId: String,
    val headsign: String? = null,
    val stops: List<StopTime>,
)

@JsonClass(generateAdapter = true)
data class StopTime(
    @Json(name = "stop_id") val stopId: String,
    @Json(name = "stop_name") val stopName: String,
    @Json(name = "departure_time") val departureTime: String,
    @Json(name = "sequence_number") val sequenceNumber: Int,
)

// --- CDN Schedule models ---

@JsonClass(generateAdapter = true)
data class ScheduleResponse(
    val operator: OperatorInfo,
    val lastUpdated: String,
    val routes: List<ScheduleRoute>,
    val stops: List<ScheduleStop>,
)

@JsonClass(generateAdapter = true)
data class OperatorInfo(val id: String, val name: String)

@Immutable
@JsonClass(generateAdapter = true)
data class ScheduleRoute(
    val id: String,
    val name: String,
    val longName: String,
    val color: String,
    val textColor: String,
    val transitType: Int,
    val directions: List<RouteDirection>,
)

@Immutable
@JsonClass(generateAdapter = true)
data class RouteDirection(
    val directionId: Int,
    val headsign: String,
    val stopIds: List<String>,
    val shape: List<List<Double>>,
)

@Immutable
@JsonClass(generateAdapter = true)
data class ScheduleStop(
    val id: String,
    val name: String,
    val lat: Double,
    @Json(name = "lng") val lon: Double,
    val departures: List<ScheduleDeparture>,
    /// Original GTFS stop_ids aggregated under this station (from backend).
    /// Used to match GTFS-RT VehiclePosition.stop_id back to our synthetic id.
    val gtfsStopIds: List<String>? = null,
)

@JsonClass(generateAdapter = true)
data class ScheduleDeparture(
    val routeId: String,
    val routeName: String,
    val tripId: String,
    val serviceDays: List<String>,
    val departureTime: String,  // "HH:MM:SS"
    val headsign: String = "",
)

// UI-ready stop (resolved from schedule)
@Immutable
data class ResolvedStop(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val routeNames: List<String>,
    val routeIds: List<String>,
    val routeColors: List<String> = emptyList(),
    val transitTypes: List<Int> = emptyList(),
    /// Original GTFS stop_ids aggregated under this station.
    val gtfsStopIds: List<String> = emptyList(),
)

// UI-ready departure (resolved, time-aware)
@Immutable
data class ResolvedDeparture(
    val routeId: String,
    val routeName: String,
    val tripId: String,
    val headsign: String,
    val departureTime: String,     // "HH:MM" display
    val minutesFromMidnight: Int,
    val routeColor: String,
    val routeTextColor: String = "",
    val transitType: Int = 3,
    val serviceDays: List<String> = emptyList(),
)

fun ResolvedDeparture.toDeparture() = Departure(
    tripId = tripId,
    routeId = routeId,
    routeShortName = routeName,
    headsign = headsign,
    departureTime = departureTime,
    realtimeDepartureTime = null,
    delay = null,
    isRealtime = false,
    routeColor = routeColor,
    routeTextColor = routeTextColor,
    transitType = transitType,
)
