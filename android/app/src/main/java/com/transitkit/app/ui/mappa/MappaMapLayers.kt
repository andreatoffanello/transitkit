package com.transitkit.app.ui.mappa

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.gson.JsonObject
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMapComposable
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.transitkit.app.data.model.ScheduleRoute

internal const val ROUTE_LINE_SOURCE_ID = "tk_route_line_source"
private const val ROUTE_LINE_CASING_LAYER_ID = "tk_route_line_casing"
private const val ROUTE_LINE_LAYER_ID = "tk_route_line"
private const val PROP_LINE_COLOR = "color"

/**
 * Polilinea rotta selezionata: doppio stroke (casing bianco sotto + tratto
 * colorato sopra) reso con `LineLayer` NATIVO inserito SOTTO le fermate
 * ([STOPS_LAYER_ID]) — così fermate e mezzi restano sempre leggibili sopra
 * la linea.
 *
 * Era basato su `PolylineAnnotation` (annotation plugin), che Mapbox disegna
 * SOPRA tutti i layer di stile → la linea copriva i marker. Il colore è una
 * property del feature (`color`) così cambia con la linea senza ricreare il
 * layer. Source svuotato quando nessuna linea è selezionata.
 */
@Composable
@MapboxMapComposable
internal fun RoutePolylineLayer(
    selectedRoute: ScheduleRoute?,
    routePolylines: List<List<Point>>,
    selectedLineColor: Color?,
) {
    MapEffect(selectedRoute?.id, routePolylines, selectedLineColor) { mapView ->
        mapView.mapboxMap.getStyle { style ->
            val colorHex = selectedLineColor?.let { argbToHex(it.toArgb()) }
            val features = if (selectedRoute == null || colorHex == null) {
                emptyList()
            } else {
                routePolylines
                    .filter { it.size >= 2 }
                    .map { pts ->
                        Feature.fromGeometry(
                            LineString.fromLngLats(pts),
                            JsonObject().apply { addProperty(PROP_LINE_COLOR, colorHex) },
                        )
                    }
            }
            MarkerLayers.upsertSource(style, ROUTE_LINE_SOURCE_ID, FeatureCollection.fromFeatures(features))

            // slot("top") via PLAIN addLayer (NOT addLayerBelow): Mapbox Standard
            // v3 darkens layers outside the top slot via "night" ambient lighting,
            // turning the polyline near-black. An explicit LayerPosition (below the
            // stops layer) makes Mapbox IGNORE the slot → the line falls back into
            // the darkened mid-stack. Z-order is instead handled by add/composition
            // order in the shared "top" slot: this layer is added BEFORE the stop
            // and vehicle SymbolLayers (also slot "top"), so markers stay above the
            // line. Mirrors JourneyMapView, which renders colored in dark mode.
            MarkerLayers.addLineLayerIfMissing(
                style, ROUTE_LINE_CASING_LAYER_ID, ROUTE_LINE_SOURCE_ID,
            ) {
                slot("top")
                lineColor("#FFFFFF")
                lineWidth(7.0)
                lineOpacity(0.7)
                // emissiveStrength(1) = the line emits its own color and is NOT
                // modulated by the Standard "night" ambient lighting that was
                // darkening the polyline to near-black in dark mode. This — not
                // the slot — is the real fix for the dark polyline.
                lineEmissiveStrength(1.0)
                lineCap(LineCap.ROUND)
                lineJoin(LineJoin.ROUND)
            }
            MarkerLayers.addLineLayerIfMissing(
                style, ROUTE_LINE_LAYER_ID, ROUTE_LINE_SOURCE_ID,
            ) {
                slot("top")
                lineColor(Expression.get(PROP_LINE_COLOR))
                lineWidth(4.0)
                lineOpacity(0.95)
                lineEmissiveStrength(1.0)
                lineCap(LineCap.ROUND)
                lineJoin(LineJoin.ROUND)
            }
        }
    }
}

/** ARGB int → "#RRGGBB" per le property colore del feature. */
private fun argbToHex(argb: Int): String = String.format("#%06X", 0xFFFFFF and argb)

// MARK: - StopAnnotationsLayer rimosso —
// fermate ora rese via `StopSymbolLayer` (SymbolLayer nativo Mapbox).
// MARK: - VehicleAnnotationsLayer rimosso —
// veicoli ora resi via `VehicleSymbolLayer` (SymbolLayer nativo Mapbox).
