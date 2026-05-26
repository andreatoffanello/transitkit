package com.transitkit.app.ui.planner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.google.gson.JsonObject
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMapComposable
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.data.model.IntermediateStop
import com.transitkit.app.ui.mappa.MapZoomLevels
import com.transitkit.app.ui.mappa.MarkerLayers
import com.transitkit.app.ui.mappa.StopMarkerBitmap

/**
 * Renderizza le fermate intermedie di un transit leg riusando la stessa
 * pipeline di [com.transitkit.app.ui.mappa.StopSymbolLayer] della mappa
 * principale: bitmap dot (zoom < neighborhoodMaxZoom) → bitmap pin signpost
 * (zoom ≥ neighborhoodMaxZoom). Niente più `CircleAnnotation` artigianali —
 * stessi componenti, stessi tier zoom della mappa Mappa.
 *
 * IDs source/layer/image **devono** essere unici per leg (route color diverso →
 * bitmap diversi). Il chiamante passa una stringa `legKey` stabile.
 */
@Composable
@MapboxMapComposable
internal fun JourneyIntermediateStopsLayer(
    legKey: String,
    stops: List<IntermediateStop>,
    routeColor: Color,
) {
    val ctx = LocalContext.current

    val argb = remember(routeColor) { (routeColor.toArgb() or 0xFF000000.toInt()) }
    val sourceId = "tk_journey_inter_${legKey}_src"
    val layerId = "tk_journey_inter_${legKey}_layer"
    val dotImageId = "tk_journey_inter_${legKey}_dot"
    val pinImageId = "tk_journey_inter_${legKey}_pin"

    MapEffect(argb, dotImageId, pinImageId) { mapView ->
        mapView.mapboxMap.getStyle { style ->
            style.addImage(dotImageId, StopMarkerBitmap.dot(ctx, argb))
            style.addImage(
                pinImageId,
                StopMarkerBitmap.pin(ctx, fillArgb = argb, iconRes = LucideIcons.Signpost),
            )
        }
    }

    MapEffect(stops, sourceId, layerId) { mapView ->
        mapView.mapboxMap.getStyle { style ->
            val features = stops
                .filter { !(it.lat == 0.0 && it.lon == 0.0) }
                .map { s ->
                    Feature.fromGeometry(
                        Point.fromLngLat(s.lon, s.lat),
                        JsonObject().apply {
                            addProperty("id", s.id)
                            addProperty("name", s.name)
                        },
                    )
                }
            MarkerLayers.upsertSource(style, sourceId, FeatureCollection.fromFeatures(features))

            MarkerLayers.addPinLayerIfMissing(style, layerId, sourceId) {
                iconImage(
                    Expression.fromRaw(
                        """["step", ["zoom"],
                          "$dotImageId",
                          ${MapZoomLevels.neighborhoodMaxZoom},
                          "$pinImageId"
                        ]""".trimIndent()
                    )
                )
                iconAnchor(IconAnchor.BOTTOM)
                iconAllowOverlap(true)
                iconIgnorePlacement(false)
            }
        }
    }
}
