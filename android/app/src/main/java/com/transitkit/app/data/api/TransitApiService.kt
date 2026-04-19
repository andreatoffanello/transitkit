package com.transitkit.app.data.api

import com.transitkit.app.data.model.Departure
import com.transitkit.app.data.model.TripDetail
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TransitApiService {
    @GET("stops/{stopId}/departures")
    suspend fun getDepartures(
        @Path("stopId") stopId: String,
        @Query("date") date: String,
        @Query("limit") limit: Int = 50,
    ): List<Departure>

    @GET("trips/{tripId}")
    suspend fun getTrip(
        @Path("tripId") tripId: String,
    ): TripDetail
}
