package com.transitkit.app.ui.planner

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.viewannotation.annotationAnchor
import com.mapbox.maps.viewannotation.geometry
import androidx.compose.ui.unit.dp
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.api.PolylineDecoder
import com.transitkit.app.data.model.Journey
import com.transitkit.app.data.model.JourneyLeg
import com.transitkit.app.data.model.PlannerStop
import com.transitkit.app.data.model.TransitLeg
import com.transitkit.app.data.model.WalkingLeg
import com.transitkit.app.ui.components.parseHexColor
import com.transitkit.app.ui.mappa.applyTransitKitStandardStyleConfig

/**
 * Map overview of a journey. Shows ONLY the stops involved in the route — board,
 * alight, intermediates per transit leg — plus start/end and user puck. The
 * general GTFS stop catalogue is intentionally NOT rendered here: this is a
 * route summary, not the main map. Polylines (transit colored per route,
 * walking dashed) sit below the stop markers; user puck sits on top.
 */
@Composable
fun JourneyMapView(
    journey: Journey,
    userLocation: Pair<Double, Double>?,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val colors = TransitTheme.colors

    val decodedLegs = remember(journey) {
        journey.legs.map { leg -> leg to coordsForLeg(leg) }
    }
    val allPoints = remember(decodedLegs) {
        decodedLegs.flatMap { it.second }
    }

    val walkingFeatures = remember(decodedLegs) {
        FeatureCollection.fromFeatures(
            decodedLegs.mapNotNull { (leg, coords) ->
                if (leg is WalkingLeg && coords.size >= 2)
                    Feature.fromGeometry(LineString.fromLngLats(coords))
                else null
            }
        )
    }
    val transitFeatures = remember(decodedLegs, isDark) {
        FeatureCollection.fromFeatures(
            decodedLegs.mapNotNull { (leg, coords) ->
                if (leg !is TransitLeg || coords.size < 2) return@mapNotNull null
                val color = safeRouteColor(
                    parseHexColor(leg.routeColor, fallback = accentColor),
                    isDark,
                )
                val hex = "#%02X%02X%02X".format(
                    (color.red * 255).toInt(),
                    (color.green * 255).toInt(),
                    (color.blue * 255).toInt(),
                )
                Feature.fromGeometry(LineString.fromLngLats(coords)).apply {
                    addStringProperty("route_color", hex)
                }
            }
        )
    }

    val viewportState = rememberMapViewportState {
        if (allPoints.size >= 2) {
            val lats = allPoints.map { it.latitude() }
            val lngs = allPoints.map { it.longitude() }
            val centerLat = (lats.min() + lats.max()) / 2
            val centerLng = (lngs.min() + lngs.max()) / 2
            val latDelta = (lats.max() - lats.min()).coerceAtLeast(0.005)
            val lngDelta = (lngs.max() - lngs.min()).coerceAtLeast(0.005)
            val maxDelta = maxOf(latDelta, lngDelta) * 1.4
            val zoom = (kotlin.math.log2(360.0 / maxDelta) - 0.4).coerceIn(10.0, 17.0)
            setCameraOptions(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(centerLng, centerLat))
                    .zoom(zoom)
                    .pitch(0.0)
                    .bearing(0.0)
                    .build()
            )
        }
    }

    MapboxMap(
        modifier = modifier,
        mapViewportState = viewportState,
        style = { MapStyle(style = Style.STANDARD) },
        compass = {},
        scaleBar = {},
    ) {
        // Native blue puck — always rendered above every other layer.
        com.transitkit.app.ui.mappa.UserLocationPuck()

        // Match the main map's basemap configuration (no POI/road labels, no
        // 3D buildings, lightPreset bound to theme).
        MapEffect(isDark) { mapView ->
            val s = mapView.mapboxMap.style
            if (s != null) applyTransitKitStandardStyleConfig(s, isDark)
            else mapView.mapboxMap.subscribeStyleLoaded {
                mapView.mapboxMap.style?.let { applyTransitKitStandardStyleConfig(it, isDark) }
            }
        }

        // Walking dashed — theme-adapted color so it stays legible against the
        // basemap whether dark or light.
        MapEffect(walkingFeatures, isDark) { mapView ->
            mapView.mapboxMap.getStyle { style ->
                val src = style.getSourceAs<GeoJsonSource>(WALKING_SOURCE_ID)
                if (src == null) {
                    style.addSource(geoJsonSource(WALKING_SOURCE_ID) {
                        featureCollection(walkingFeatures)
                    })
                } else {
                    src.featureCollection(walkingFeatures)
                }
                val walkColor = if (isDark) "#CBD5E1" else "#374151"
                if (style.styleLayerExists(WALKING_LAYER_ID)) {
                    style.removeStyleLayer(WALKING_LAYER_ID)
                }
                style.addLayer(
                    lineLayer(WALKING_LAYER_ID, WALKING_SOURCE_ID) {
                        slot("top")
                        lineColor(android.graphics.Color.parseColor(walkColor))
                        lineWidth(3.5)
                        lineOpacity(0.95)
                        lineCap(LineCap.ROUND)
                        lineJoin(LineJoin.ROUND)
                        lineDasharray(listOf(1.4, 1.6))
                    }
                )
            }
        }

        // Transit colored polylines. `slot("top")` keeps them above the basemap
        // labels and bypasses the Standard ambient-lighting darkening that hits
        // mid-stack layers.
        MapEffect(transitFeatures) { mapView ->
            mapView.mapboxMap.getStyle { style ->
                val src = style.getSourceAs<GeoJsonSource>(TRANSIT_SOURCE_ID)
                if (src == null) {
                    style.addSource(geoJsonSource(TRANSIT_SOURCE_ID) {
                        featureCollection(transitFeatures)
                    })
                } else {
                    src.featureCollection(transitFeatures)
                }
                if (style.styleLayerExists(TRANSIT_LAYER_ID)) {
                    style.removeStyleLayer(TRANSIT_LAYER_ID)
                }
                style.addLayer(
                    lineLayer(TRANSIT_LAYER_ID, TRANSIT_SOURCE_ID) {
                        slot("top")
                        lineColor(
                            com.mapbox.maps.extension.style.expressions.dsl.generated.get(
                                "route_color",
                            )
                        )
                        lineWidth(6.0)
                        lineOpacity(1.0)
                        lineCap(LineCap.ROUND)
                        lineJoin(LineJoin.ROUND)
                    }
                )
            }
        }

        // Intermediate stop markers — small white-center, route-color ring.
        // Coords come directly from MOTIS legGeometry intermediate points.
        decodedLegs.forEach { (leg, _) ->
            if (leg !is TransitLeg) return@forEach
            val color = safeRouteColor(parseHexColor(leg.routeColor, fallback = accentColor), isDark)
            leg.intermediateStops.forEach { stop ->
                if (stop.lat == 0.0 && stop.lon == 0.0) return@forEach
                CircleAnnotation(point = Point.fromLngLat(stop.lon, stop.lat)) {
                    circleColor = Color.White
                    circleRadius = 3.5
                    circleStrokeColor = color
                    circleStrokeWidth = 2.0
                }
            }
        }

        // Board / alight pins — colored dot with white stroke.
        decodedLegs.forEach { (leg, _) ->
            if (leg is TransitLeg) {
                val color = safeRouteColor(parseHexColor(leg.routeColor, fallback = accentColor), isDark)
                CircleAnnotation(point = leg.boardStop.toPoint()) {
                    circleColor = color
                    circleRadius = 5.5
                    circleStrokeColor = Color.White
                    circleStrokeWidth = 2.0
                }
                CircleAnnotation(point = leg.alightStop.toPoint()) {
                    circleColor = color
                    circleRadius = 5.5
                    circleStrokeColor = Color.White
                    circleStrokeWidth = 2.0
                }
            }
        }

        // Start / end pins.
        allPoints.firstOrNull()?.let { start ->
            CircleAnnotation(point = start) {
                circleColor = colors.accent
                circleRadius = 9.0
                circleStrokeColor = Color.White
                circleStrokeWidth = 2.5
            }
        }
        allPoints.lastOrNull()?.let { end ->
            CircleAnnotation(point = end) {
                circleColor = Color(0xFFE53935)
                circleRadius = 9.0
                circleStrokeColor = Color.White
                circleStrokeWidth = 2.5
            }
        }

        // User puck handled by `UserLocationPuck` (native Mapbox plugin) at
        // the top of this MapboxMap content — managed z-order, no manual
        // overlay needed.
    }
}

private const val WALKING_SOURCE_ID = "transitkit-journey-walking-src"
private const val WALKING_LAYER_ID = "transitkit-journey-walking-line"
private const val TRANSIT_SOURCE_ID = "transitkit-journey-transit-src"
private const val TRANSIT_LAYER_ID = "transitkit-journey-transit-line"

private fun coordsForLeg(leg: JourneyLeg): List<Point> = when (leg) {
    is TransitLeg -> {
        leg.polyline?.let { PolylineDecoder.decode(it) }?.takeIf { it.size >= 2 }
            ?.let { decoded ->
                val board = leg.boardStop.toPoint()
                val alight = leg.alightStop.toPoint()
                buildList(decoded.size + 2) {
                    if (!sameCoord(decoded.first(), board)) add(board)
                    addAll(decoded)
                    if (!sameCoord(decoded.last(), alight)) add(alight)
                }
            }
            ?: listOf(leg.boardStop.toPoint(), leg.alightStop.toPoint())
    }
    is WalkingLeg -> {
        val from = leg.fromStop.toPoint()
        val to = leg.toStop.toPoint()
        leg.polyline?.let { PolylineDecoder.decode(it) }?.takeIf { it.size >= 2 }
            ?.let { decoded ->
                buildList(decoded.size + 2) {
                    if (!sameCoord(decoded.first(), from)) add(from)
                    addAll(decoded)
                    if (!sameCoord(decoded.last(), to)) add(to)
                }
            }
            ?: listOf(from, to)
    }
}

private fun sameCoord(a: Point, b: Point): Boolean =
    Math.abs(a.latitude() - b.latitude()) < 0.000009 &&
        Math.abs(a.longitude() - b.longitude()) < 0.000009

private fun PlannerStop.toPoint(): Point = Point.fromLngLat(lng, lat)

/**
 * Brighten dark route hexes on dark basemap while preserving hue/saturation.
 */
private fun safeRouteColor(base: Color, isDark: Boolean): Color {
    if (!isDark) return base
    val maxC = maxOf(base.red, base.green, base.blue)
    val floor = 0.92f
    if (maxC >= floor) return base
    val scale = floor / maxC.coerceAtLeast(0.001f)
    return Color(
        red = (base.red * scale).coerceAtMost(1f),
        green = (base.green * scale).coerceAtMost(1f),
        blue = (base.blue * scale).coerceAtMost(1f),
        alpha = 1f,
    )
}
