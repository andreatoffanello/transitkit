package com.transitkit.app.config

import androidx.compose.runtime.Immutable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

typealias LocalizedText = Map<String, String>

fun LocalizedText.resolved(): String {
    val code = java.util.Locale.getDefault().language
    return this[code] ?: this["en"] ?: values.firstOrNull().orEmpty()
}

@Immutable
@JsonClass(generateAdapter = true)
data class FareType(
    val name: String = "",
    val price: String = "",
    val notes: String? = null,
)

@Immutable
@JsonClass(generateAdapter = true)
data class FareInfo(
    val types: List<FareType> = emptyList(),
    @Json(name = "purchaseUrl") val purchaseUrl: String? = null,
    val notes: String? = null,
)

@Immutable
@JsonClass(generateAdapter = true)
data class PointOfSale(
    val name: String = "",
    val address: String? = null,
    val hours: String? = null,
)

@Immutable
@JsonClass(generateAdapter = true)
data class ContactConfig(
    val phone: String? = null,
    val email: String? = null,
    val tdd: String? = null,
    val address: String? = null,
    val hours: LocalizedText? = null,
)

@Immutable
@JsonClass(generateAdapter = true)
data class ServiceCta(
    val type: String,      // "phone" | "url"
    val label: LocalizedText,
    val value: String,
)

@Immutable
@JsonClass(generateAdapter = true)
data class ServiceLink(
    val label: LocalizedText,
    val url: String,
)

@Immutable
@JsonClass(generateAdapter = true)
data class ServiceInfo(
    val id: String,
    val icon: String,
    val title: LocalizedText,
    val subtitle: LocalizedText,
    val description: LocalizedText,
    val audience: LocalizedText? = null,
    val steps: List<LocalizedText>? = null,
    val hours: LocalizedText? = null,
    val fare: LocalizedText? = null,
    @Json(name = "serviceArea") val serviceArea: LocalizedText? = null,
    val notes: List<LocalizedText>? = null,
    val cta: ServiceCta? = null,
    val links: List<ServiceLink>? = null,
)

@Immutable
@JsonClass(generateAdapter = true)
data class AccessibilityInfo(
    val title: LocalizedText,
    val description: LocalizedText,
    val bullets: List<LocalizedText>,
    @Json(name = "moreUrl") val moreUrl: String? = null,
)

@Immutable
@JsonClass(generateAdapter = true)
data class OperatorConfig(
    val id: String,
    val name: String,
    @Json(name = "fullName") val fullName: String,
    val url: String,
    @Json(name = "cdnUrl") val cdnUrl: String? = null,
    val region: String? = null,
    val country: String,
    val timezone: String,
    val locale: List<String>,
    val theme: ThemeConfig,
    val store: StoreConfig,
    val map: MapConfig,
    val features: FeaturesConfig,
    @Json(name = "routing_endpoint") val routingEndpoint: String? = null,
    /** Base URL of the transitkit-console CMS used to register test devices,
     *  e.g. `https://console.transitkit.app`. Optional — when missing the
     *  developer-mode "Register" button surfaces an error rather than firing. */
    @Json(name = "console_api_url") val consoleApiUrl: String? = null,
    @Json(name = "gtfs_rt") val gtfsRt: GtfsRtConfig? = null,
    @Json(name = "headsign_map") val headsignMap: Map<String, String>? = null,
    val contact: ContactConfig? = null,
    val fares: FareInfo? = null,
    @Json(name = "pointsOfSale") val pointsOfSale: List<PointOfSale>? = null,
    @Json(name = "privacyUrl") val privacyUrl: String? = null,
    val services: List<ServiceInfo>? = null,
    val accessibility: AccessibilityInfo? = null,
) {
    @Immutable
    @JsonClass(generateAdapter = true)
    data class ThemeConfig(
        @Json(name = "primaryColor") val primaryColor: String,
        @Json(name = "accentColor") val accentColor: String,
        @Json(name = "textOnPrimary") val textOnPrimary: String,
        @Json(name = "secondaryColor") val secondaryColor: String? = null,
    )

    @Immutable
    @JsonClass(generateAdapter = true)
    data class StoreConfig(
        val title: String,
        val subtitle: String,
        val keywords: String,
    )

    @Immutable
    @JsonClass(generateAdapter = true)
    data class MapConfig(
        @Json(name = "centerLat") val centerLat: Double,
        @Json(name = "centerLng") val centerLng: Double,
        @Json(name = "defaultZoom") val defaultZoom: Double,
    )

    @Immutable
    @JsonClass(generateAdapter = true)
    data class FeaturesConfig(
        @Json(name = "enableMap") val enableMap: Boolean = true,
        @Json(name = "enableGeolocation") val enableGeolocation: Boolean = false,
        @Json(name = "enableFavorites") val enableFavorites: Boolean = true,
        @Json(name = "enableNotifications") val enableNotifications: Boolean = false,
        @Json(name = "useRemoteEngine") val useRemoteEngine: Boolean = false,
    )

    @Immutable
    @JsonClass(generateAdapter = true)
    data class GtfsRtConfig(
        @Json(name = "vehicle_positions") val vehiclePositionsUrl: String? = null,
        @Json(name = "trip_updates") val tripUpdatesUrl: String? = null,
        @Json(name = "alerts") val serviceAlertsUrl: String? = null,
    )
}
