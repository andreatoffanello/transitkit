package com.transitkit.app.ui.mappa

import androidx.annotation.DrawableRes
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
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor

/**
 * Pin singolo Mapbox per mappe "focus" (es. StopDetail). Riusa
 * [StopMarkerBitmap.pin] + [MarkerLayers] per condividere la pipeline di
 * rendering con [StopSymbolLayer] — single source of truth pin shape.
 *
 * Setup atomico in un solo `MapEffect`: bitmap registrato PRIMA del layer
 * (ordine garantito), così il layer trova subito la sua iconImage senza
 * race con un secondo MapEffect.
 *
 * Da usare dentro `MapboxMap { ... }` content scope.
 */
@Composable
@MapboxMapComposable
internal fun SingleStopMarker(
    point: Point,
    color: Color,
    @DrawableRes iconRes: Int? = null,
    glyph: String? = null,
) {
    val ctx = LocalContext.current
    val argb = remember(color) { (color.toArgb() or 0xFF000000.toInt()) }

    MapEffect(point, argb, iconRes, glyph) { mapView ->
        mapView.mapboxMap.getStyle { style ->
            // 1) bitmap (overwrites se già presente con stesso id)
            style.addImage(
                SINGLE_STOP_IMAGE_ID,
                StopMarkerBitmap.pin(ctx, fillArgb = argb, iconRes = iconRes, glyph = glyph),
            )
            // 2) source — 1 feature
            val feature = Feature.fromGeometry(point, JsonObject())
            MarkerLayers.upsertSource(
                style,
                SINGLE_STOP_SOURCE_ID,
                FeatureCollection.fromFeatures(listOf(feature)),
            )
            // 3) layer (idempotente)
            MarkerLayers.addPinLayerIfMissing(
                style,
                SINGLE_STOP_LAYER_ID,
                SINGLE_STOP_SOURCE_ID,
            ) {
                iconImage(SINGLE_STOP_IMAGE_ID)
                iconAnchor(IconAnchor.BOTTOM)
                iconAllowOverlap(true)
                iconIgnorePlacement(true)
            }
        }
    }
}

private const val SINGLE_STOP_SOURCE_ID = "tk_single_stop_source"
private const val SINGLE_STOP_LAYER_ID = "tk_single_stop_layer"
private const val SINGLE_STOP_IMAGE_ID = "tk_single_stop_marker"
