package com.transitkit.app.ui.mappa

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.ScreenBox
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures

/**
 * Click listener su MapView con hit-test via `queryRenderedFeatures`.
 * Da usare dentro `MapboxMap { ... }` content scope.
 *
 * Hit-test:
 *  - converte le coord lat/lon del tap in pixel screen
 *  - costruisce una `ScreenBox` di ±[hitPaddingDp] dp attorno al tap
 *  - interroga i [layerIds] e prende la feature più vicina al pixel del tap
 *
 * Il box è necessario perché i bitmap pin hanno aree alpha=0: un point query
 * col solo pixel del tap fallisce se l'utente tappa "vicino" al pin invece
 * che esattamente sul fill.
 *
 * Listener registrato UNA sola volta (dipendenza `Unit`); i callback sono
 * letti via `rememberUpdatedState` — niente accumulo di handler stale.
 */
@OptIn(MapboxExperimental::class)
@Composable
internal fun MarkerTapHandler(
    layerIds: List<String>,
    onPinTap: (Feature, Point) -> Unit,
    onMissed: ((Point) -> Unit)? = null,
    hitPaddingDp: Float = 8f,
) {
    val onPinRef = rememberUpdatedState(onPinTap)
    val onMissedRef = rememberUpdatedState(onMissed)
    val layerIdsRef = rememberUpdatedState(layerIds)

    MapEffect(Unit) { mapView ->
        val density = mapView.context.resources.displayMetrics.density
        val padPx = hitPaddingDp * density
        val listener = OnMapClickListener { point ->
            val s = mapView.mapboxMap.pixelForCoordinate(point)
            val box = ScreenBox(
                ScreenCoordinate(s.x - padPx, s.y - padPx),
                ScreenCoordinate(s.x + padPx, s.y + padPx),
            )
            mapView.mapboxMap.queryRenderedFeatures(
                RenderedQueryGeometry(box),
                RenderedQueryOptions(layerIdsRef.value, null),
            ) { result ->
                val candidates = result.value.orEmpty()
                if (candidates.isEmpty()) {
                    onMissedRef.value?.invoke(point)
                    return@queryRenderedFeatures
                }
                val feature = candidates.minByOrNull { fc ->
                    val pt = fc.queriedFeature.feature.geometry() as? Point
                        ?: return@minByOrNull Double.MAX_VALUE
                    val pinScreen = mapView.mapboxMap.pixelForCoordinate(pt)
                    val dx = pinScreen.x - s.x
                    val dy = pinScreen.y - s.y
                    dx * dx + dy * dy
                }?.queriedFeature?.feature ?: return@queryRenderedFeatures
                val pt = feature.geometry() as? Point ?: return@queryRenderedFeatures
                onPinRef.value(feature, pt)
            }
            true
        }
        mapView.gestures.addOnMapClickListener(listener)
    }
}
