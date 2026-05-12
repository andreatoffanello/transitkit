package com.transitkit.app.ui.mappa

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
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.data.model.ScheduleRoute

// MARK: - Stable identifiers (single source of truth — referenced by tap handler)

internal const val STOPS_SOURCE_ID = "tk_stops_source"
internal const val STOPS_LAYER_ID = "tk_stops_layer"

private const val STOP_DOT_IMAGE_ID = "tk_stop_dot"
private const val STOP_PIN_IMAGE_ID = "tk_stop_pin"
private const val STOP_PIN_METRO_IMAGE_ID = "tk_stop_pin_metro"

internal const val PROP_STOP_ID = "id"
internal const val PROP_STOP_NAME = "name"
private const val PROP_STOP_METRO = "metro"
private const val PROP_STOP_SELECTED = "selected"

/**
 * SymbolLayer-based stop annotations. Sostituisce `ViewAnnotation` Compose —
 * elimina recomposition al pan e mantiene fluidità con N>200 fermate.
 *
 * Pattern: un solo SymbolLayer con `iconImage` espressione `step ["zoom"]`:
 *   - zoom < neighborhoodMaxZoom → dot (ARGB scelto, rounded square)
 *   - zoom ≥ neighborhoodMaxZoom → pin pieno (metro vs signpost via case)
 * `iconSize` espressione `case` su PROP_STOP_SELECTED: 1.28× quando selected
 * (parità con animateDpAsState 28→36 del vecchio StopPinView).
 *
 * Mapbox vincolo: `["zoom"]` può essere top-level input di `step`/`interpolate`,
 * non di `case`. Lo `step` decide dot vs pin, il `case` interno decide
 * metro vs signpost in base alla property.
 *
 * Tap: registrato dal chiamante via `MarkerTapHandler` con `STOPS_LAYER_ID`.
 */
@Composable
@MapboxMapComposable
internal fun StopSymbolLayer(
    stops: List<ResolvedStop>,
    selectedStop: ResolvedStop?,
    selectedRoute: ScheduleRoute?,
    accentColor: Color,
) {
    val ctx = LocalContext.current

    // Colore primario: route selezionata se presente, altrimenti accent.
    val primaryArgb = remember(selectedRoute?.color, accentColor) {
        forceOpaque(
            selectedRoute?.color?.takeIf { it.isNotBlank() }?.let { hex ->
                runCatching { android.graphics.Color.parseColor("#$hex") }.getOrNull()
            } ?: accentColor.toArgb()
        )
    }

    // ─── Bitmap registration (rare — solo a cambio colore) ───────────────────
    MapEffect(primaryArgb) { mapView ->
        mapView.mapboxMap.getStyle { style ->
            style.addImage(STOP_DOT_IMAGE_ID, StopMarkerBitmap.dot(ctx, primaryArgb))
            style.addImage(
                STOP_PIN_IMAGE_ID,
                StopMarkerBitmap.pin(
                    ctx,
                    fillArgb = primaryArgb,
                    iconRes = LucideIcons.Signpost,
                ),
            )
            style.addImage(
                STOP_PIN_METRO_IMAGE_ID,
                StopMarkerBitmap.pin(ctx, fillArgb = primaryArgb, glyph = "M"),
            )
        }
    }

    // ─── Source + layer (depends on stops + selectedStop) ────────────────────
    MapEffect(stops, selectedStop?.id) { mapView ->
        mapView.mapboxMap.getStyle { style ->
            val selectedId = selectedStop?.id
            val features = stops.map { s ->
                val isMetro = s.transitTypes.contains(1)
                Feature.fromGeometry(
                    Point.fromLngLat(s.lon, s.lat),
                    JsonObject().apply {
                        addProperty(PROP_STOP_ID, s.id)
                        addProperty(PROP_STOP_NAME, s.name)
                        addProperty(PROP_STOP_METRO, isMetro)
                        addProperty(PROP_STOP_SELECTED, s.id == selectedId)
                    },
                )
            }
            MarkerLayers.upsertSource(style, STOPS_SOURCE_ID, FeatureCollection.fromFeatures(features))

            MarkerLayers.addPinLayerIfMissing(style, STOPS_LAYER_ID, STOPS_SOURCE_ID) {
                iconImage(
                    Expression.fromRaw(
                        """["step", ["zoom"],
                          "$STOP_DOT_IMAGE_ID",
                          ${MapZoomLevels.neighborhoodMaxZoom},
                            ["case",
                              ["==", ["get", "$PROP_STOP_METRO"], true], "$STOP_PIN_METRO_IMAGE_ID",
                              "$STOP_PIN_IMAGE_ID"
                            ]
                        ]""".trimIndent()
                    )
                )
                iconAnchor(IconAnchor.BOTTOM)
                iconAllowOverlap(true)
                iconIgnorePlacement(false)
                iconSize(
                    Expression.fromRaw(
                        """["case", ["==", ["get", "$PROP_STOP_SELECTED"], true], 1.28, 1.0]"""
                    )
                )
                // Nessuna label sotto al pin per parità visiva col rendering
                // ViewAnnotation precedente — il nome fermata appare nel
                // preview card alla selezione, non sulla mappa.
            }
        }
    }
}

/** Toglie eventuale alpha dal colore — Mapbox ignora alpha < 1.0 sull'image. */
private fun forceOpaque(argb: Int): Int = (argb or 0xFF000000.toInt())
