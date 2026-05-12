package com.transitkit.app.ui.mappa

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalHapticFeedback
import com.mapbox.geojson.Point
import com.mapbox.maps.ViewAnnotationAnchor
import com.mapbox.maps.extension.compose.MapboxMapComposable
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.viewannotation.annotationAnchor
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import com.transitkit.app.config.LocalTransitColors
import com.transitkit.app.data.gtfsrt.VehiclePosition
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.data.model.ScheduleRoute
import java.util.concurrent.atomic.AtomicLong

/**
 * Doppio stroke polyline (bianco sotto + colore linea sopra) — enfasi
 * visiva sulla rotta selezionata sopra lo style Mapbox.
 */
@Composable
@MapboxMapComposable
internal fun RoutePolylineLayer(
    selectedRoute: ScheduleRoute?,
    routePolylines: List<List<Point>>,
    selectedLineColor: Color?,
) {
    if (selectedRoute == null || routePolylines.isEmpty() || selectedLineColor == null) return
    routePolylines.forEachIndexed { idx, linePoints ->
        val points = linePoints
        if (points.size >= 2) {
            key("poly_w_${selectedRoute.id}_$idx") {
                PolylineAnnotation(points = points) {
                    lineColor = Color.White
                    lineWidth = 7.0
                    lineOpacity = 0.7
                }
            }
            key("poly_c_${selectedRoute.id}_$idx") {
                PolylineAnnotation(points = points) {
                    lineColor = selectedLineColor
                    lineWidth = 4.0
                    lineOpacity = 0.95
                }
            }
        }
    }
}

// MARK: - StopAnnotationsLayer rimosso —
// fermate ora rese via `StopSymbolLayer` (SymbolLayer nativo Mapbox).

/**
 * Vehicle annotations. Route-selected: tutti i veicoli di quella linea.
 * Unselected: viewport-filtered è fatto a monte dal VM.
 * Render selected vehicle last per z-order corretto.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
@MapboxMapComposable
internal fun VehicleAnnotationsLayer(
    vehiclesWithColor: List<Pair<VehiclePosition, Color>>,
    selectedVehicle: Pair<VehiclePosition, Color>?,
    selectedRoute: ScheduleRoute?,
    routes: List<ScheduleRoute>,
    tier: MapZoomTier,
    haloAlpha: Float,
    lastAnnotationTapMs: AtomicLong,
    onVehicleTap: (VehiclePosition, Color) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val visibleVehicles = when {
        selectedRoute != null -> vehiclesWithColor.filter { it.first.routeId == selectedRoute.id }
        tier == MapZoomTier.Street -> vehiclesWithColor
        else -> vehiclesWithColor  // a city/neighborhood mostra comunque dot (movete 1:1)
    }
    val (selectedVehicles, otherVehicles) = visibleVehicles.partition {
        it.first.vehicleId == selectedVehicle?.first?.vehicleId
    }
    (otherVehicles + selectedVehicles).forEach { (vehicle, routeColor) ->
        val route = routes.firstOrNull { it.id == vehicle.routeId }
        val routeName = route?.name?.take(4) ?: ""

        key(vehicle.vehicleId) {
            ViewAnnotation(
                options = viewAnnotationOptions {
                    geometry(Point.fromLngLat(vehicle.lon, vehicle.lat))
                    annotationAnchor { anchor(ViewAnnotationAnchor.CENTER) }
                    allowOverlap(true)
                }
            ) {
                Box(
                    modifier = Modifier.pointerInteropFilter { event ->
                        if (event.action == android.view.MotionEvent.ACTION_UP) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            lastAnnotationTapMs.set(System.currentTimeMillis())
                            onVehicleTap(vehicle, routeColor)
                        }
                        true
                    }
                ) {
                    VehicleAnnotationView(
                        lineColor = routeColor,
                        routeName = routeName,
                        tier = tier,
                        haloAlpha = haloAlpha,
                    )
                }
            }
        }
    }
}
