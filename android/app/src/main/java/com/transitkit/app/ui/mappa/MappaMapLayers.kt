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

            MarkerLayers.addLineLayerBelowIfMissing(
                style, ROUTE_LINE_CASING_LAYER_ID, ROUTE_LINE_SOURCE_ID, STOPS_LAYER_ID,
            ) {
                lineColor("#FFFFFF")
                lineWidth(7.0)
                lineOpacity(0.7)
                lineCap(LineCap.ROUND)
                lineJoin(LineJoin.ROUND)
            }
            MarkerLayers.addLineLayerBelowIfMissing(
                style, ROUTE_LINE_LAYER_ID, ROUTE_LINE_SOURCE_ID, STOPS_LAYER_ID,
            ) {
                lineColor(Expression.get(PROP_LINE_COLOR))
                lineWidth(4.0)
                lineOpacity(0.95)
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
