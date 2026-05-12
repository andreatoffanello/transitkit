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
import com.mapbox.maps.ImageContent
import com.mapbox.maps.ImageStretches
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMapComposable
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.image.addImage
import com.mapbox.maps.extension.style.image.image
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.IconTextFit
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.data.model.ScheduleRoute

// MARK: - Stable identifiers (single source of truth — referenced by tap handler)

internal const val STOPS_SOURCE_ID = "tk_stops_source"
internal const val STOPS_LAYER_ID = "tk_stops_layer"
internal const val STOPS_LABEL_LAYER_ID = "tk_stops_label_layer"

private const val STOP_DOT_IMAGE_ID = "tk_stop_dot"
private const val STOP_PIN_IMAGE_ID = "tk_stop_pin"
private const val STOP_PIN_METRO_IMAGE_ID = "tk_stop_pin_metro"
private const val STOP_LABEL_PILL_ID = "tk_stop_label_pill"

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

    // ─── Bitmap registration ─────────────────────────────────────────────────
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

    // Pill bg per badge label (registrato una volta — non dipende da colore).
    // Stretchable: il bitmap si scala in larghezza/altezza in modo che la zona
    // centrale assorba la variazione (bordi rounded restano intatti).
    MapEffect(Unit) { mapView ->
        mapView.mapboxMap.getStyle { style ->
            val density = ctx.resources.displayMetrics.density
            val pill = StopMarkerBitmap.labelPill(ctx)
            // Zona stretchable centrale (in px del bitmap).
            val padPx = 2f * density
            val coreW = 40f * density
            val coreH = 20f * density
            val midXa = padPx + coreW * 0.45f
            val midXb = padPx + coreW * 0.55f
            val midYa = padPx + coreH * 0.40f
            val midYb = padPx + coreH * 0.60f
            style.addImage(
                image(STOP_LABEL_PILL_ID, pill) {
                    stretchX(listOf(ImageStretches(midXa, midXb)))
                    stretchY(listOf(ImageStretches(midYa, midYb)))
                    content(ImageContent(midXa, midYa, midXb, midYb))
                }
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

                // Z-order: la fermata selezionata viene renderizzata SOPRA
                // le altre (sortKey ascending → 1 sopra 0).
                symbolSortKey(
                    Expression.fromRaw(
                        """["case", ["==", ["get", "$PROP_STOP_SELECTED"], true], 1.0, 0.0]"""
                    )
                )
            }

            // Layer 2 — Badge label sotto al pin (solo a tier Street).
            // Pattern: bitmap pill stretchable + iconTextFit(BOTH) → il pill
            // si auto-scala in larghezza/altezza per contenere il nome fermata.
            // Posizione: anchor=TOP + offset Y per stare sotto la punta tail
            // del pin. Risultato: badge solido bianco con bordo soft +
            // nome leggibile in nero, distinguibile dai POI del map style.
            if (!style.styleLayerExists(STOPS_LABEL_LAYER_ID)) {
                style.addLayer(symbolLayer(STOPS_LABEL_LAYER_ID, STOPS_SOURCE_ID) {
                    iconImage(STOP_LABEL_PILL_ID)
                    iconTextFit(IconTextFit.BOTH)
                    iconTextFitPadding(listOf(2.0, 7.0, 2.0, 7.0))
                    iconAnchor(IconAnchor.TOP)
                    // Offset Y in ems (textSize=11): 0.6em ≈ 6.6dp gap sotto pin tail.
                    iconOffset(listOf(0.0, 6.0))
                    iconAllowOverlap(true)
                    iconIgnorePlacement(true)

                    textField(
                        Expression.fromRaw(
                            """["step", ["zoom"], "", ${MapZoomLevels.neighborhoodMaxZoom}, ["get", "$PROP_STOP_NAME"]]"""
                        )
                    )
                    textSize(10.5)
                    textAnchor(TextAnchor.TOP)
                    textOffset(listOf(0.0, 6.0))
                    textOptional(false)
                    textAllowOverlap(true)
                    textIgnorePlacement(true)
                    textColor(Expression.rgb(literal(20.0), literal(20.0), literal(20.0)))
                    textPadding(1.0)
                    textMaxWidth(7.0) // wrap su 2 righe per nomi lunghi
                    // Selected label sopra le altre (parità con pin sortKey).
                    symbolSortKey(
                        Expression.fromRaw(
                            """["case", ["==", ["get", "$PROP_STOP_SELECTED"], true], 1.0, 0.0]"""
                        )
                    )
                })
            }
        }
    }
}

/** Toglie eventuale alpha dal colore — Mapbox ignora alpha < 1.0 sull'image. */
private fun forceOpaque(argb: Int): Int = (argb or 0xFF000000.toInt())
