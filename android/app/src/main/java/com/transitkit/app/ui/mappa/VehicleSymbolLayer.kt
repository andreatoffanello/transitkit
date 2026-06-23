package com.transitkit.app.ui.mappa

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.transitkit.app.data.gtfsrt.VehiclePosition
import com.transitkit.app.data.model.ScheduleRoute

// ── Stable identifiers — referenced by tap handler and layer visibility ───────

internal const val VEHICLES_SOURCE_ID = "tk_vehicles_source"
internal const val VEHICLES_LAYER_ID = "tk_vehicles_layer"
private const val VEHICLES_HALO_LAYER_ID = "tk_vehicles_halo_layer"

internal const val PROP_VEHICLE_ID = "vid"
private const val PROP_VEHICLE_ICON = "icon"        // pill bitmap id (Street)
private const val PROP_VEHICLE_DOT = "icond"        // dot bitmap id (City/Nbhd)
private const val PROP_VEHICLE_HALO = "halo"        // circle color hex
private const val PROP_VEHICLE_LIVE = "live"        // 1 = has valid position
private const val PROP_VEHICLE_SELECTED = "sel"     // 1 = currently selected

// Zoom boundary Street tier: parità con MapZoomLevels.neighborhoodMaxZoom
private const val VEH_NEAR_ZOOM = 14.0

/**
 * SymbolLayer + GeoJsonSource per i veicoli in tempo reale.
 *
 * Sostituisce `VehicleAnnotationsLayer` (ViewAnnotation Compose) eliminando
 * recomposition al pan, jank a >20 veicoli e il rischio ANR su flotte grandi.
 *
 * Architettura (parità DoVe `StopMarkerLayers.setupVehicleLayers` + transit-engine
 * `StopSymbolLayer`):
 *
 *   1. Bitmap registrati via `style.addImage(id, bitmap)` — idempotent per id.
 *      ID deterministici per colore+nome: condivisi tra veicoli stessa linea.
 *
 *   2. GeoJsonSource `tk_vehicles_source` aggiornato a ogni snapshot RT.
 *      Feature properties: `vid`, `icon`, `icond`, `halo`, `live`, `sel`.
 *
 *   3. CircleLayer `tk_vehicles_halo_layer` (sotto) — alone semi-trasparente
 *      statico (NO pulsing: parità DoVe che usa CircleLayer statico con opacity
 *      fissa 0.22, non un animator separato).
 *
 *   4. SymbolLayer `tk_vehicles_layer` (sopra) — `iconImage` step expression:
 *      - zoom < [VEH_NEAR_ZOOM] → dot piccolo
 *      - zoom ≥ [VEH_NEAR_ZOOM] → pill (badge + tail + dot)
 *      `symbolSortKey` 1.0 su veicolo selezionato → rendered sopra.
 *
 * Tap: registrato in `MappaScreen` via `MarkerTapHandler` con
 * `layerIds = [VEHICLES_LAYER_ID, STOPS_LAYER_ID]`. Property `vid` distingue
 * il feature come veicolo.
 *
 * Animator: `VehicleMarkerAnimator` interpola geometria tra snapshot (900ms
 * linear). Viene ricreato se la composable lascia la composition.
 */
@Composable
@MapboxMapComposable
internal fun VehicleSymbolLayer(
    vehiclesWithColor: List<Pair<VehiclePosition, Color>>,
    selectedVehicle: Pair<VehiclePosition, Color>?,
    routes: List<ScheduleRoute>,
) {
    val ctx = LocalContext.current

    val animator = remember { VehicleMarkerAnimator() }
    DisposableEffect(Unit) { onDispose { animator.cancel() } }

    MapEffect(vehiclesWithColor, selectedVehicle?.first?.vehicleId) { mapView ->
        mapView.mapboxMap.getStyle { style ->
            val selectedId = selectedVehicle?.first?.vehicleId
            val routesById = routes.associateBy { it.id }

            // ── 1. Register bitmaps (idempotent: skip if already present) ───
            val registeredImages = mutableSetOf<String>()
            for ((vehicle, color) in vehiclesWithColor) {
                val argb = forceOpaqueVehicle(color.toArgb())
                val routeName = routesById[vehicle.routeId]?.name?.take(4) ?: ""

                val dotId = VehicleMarkerBitmap.dotImageId(argb)
                if (dotId !in registeredImages && style.getStyleImage(dotId) == null) {
                    style.addImage(dotId, VehicleMarkerBitmap.dot(ctx, argb))
                }
                registeredImages += dotId

                val pillId = VehicleMarkerBitmap.pillImageId(argb, routeName)
                if (pillId !in registeredImages && style.getStyleImage(pillId) == null) {
                    style.addImage(pillId, VehicleMarkerBitmap.pill(ctx, argb, routeName))
                }
                registeredImages += pillId
            }

            // ── 2. Build animation targets ───────────────────────────────────
            val targets = ArrayList<VehicleMarkerAnimator.Target>(vehiclesWithColor.size)
            for ((vehicle, color) in vehiclesWithColor) {
                val argb = forceOpaqueVehicle(color.toArgb())
                val routeName = routesById[vehicle.routeId]?.name?.take(4) ?: ""
                val haloHex = argbToHex(argb)

                val props = JsonObject().apply {
                    addProperty(PROP_VEHICLE_ID, vehicle.vehicleId)
                    addProperty(PROP_VEHICLE_ICON, VehicleMarkerBitmap.pillImageId(argb, routeName))
                    addProperty(PROP_VEHICLE_DOT, VehicleMarkerBitmap.dotImageId(argb))
                    addProperty(PROP_VEHICLE_HALO, haloHex)
                    addProperty(PROP_VEHICLE_LIVE, 1)
                    addProperty(PROP_VEHICLE_SELECTED, if (vehicle.vehicleId == selectedId) 1 else 0)
                }
                targets.add(VehicleMarkerAnimator.Target(vehicle.vehicleId, props, vehicle.lon, vehicle.lat))
            }

            // ── 3. Seed source at START positions (before glide begins) ──────
            val seedFeats = ArrayList<Feature>(targets.size)
            for (t in targets) {
                val s = animator.startFor(t.id, t.lng, t.lat)
                seedFeats.add(Feature.fromGeometry(Point.fromLngLat(s[0], s[1]), t.props))
            }
            MarkerLayers.upsertSource(style, VEHICLES_SOURCE_ID, FeatureCollection.fromFeatures(seedFeats))

            // ── 4. Halo CircleLayer (static, under symbol) ───────────────────
            // Parità DoVe: opacity fissa 0.22 a zoom ≥ VEH_NEAR_ZOOM, 0 prima.
            // Non pulsante — parità DoVe che usa step expression, non animator.
            MarkerLayers.addCircleLayerIfMissing(style, VEHICLES_HALO_LAYER_ID, VEHICLES_SOURCE_ID) {
                filter(Expression.fromRaw("""["==", ["get", "$PROP_VEHICLE_LIVE"], 1]"""))
                circleColor(Expression.fromRaw("""["get", "$PROP_VEHICLE_HALO"]"""))
                circleRadius(13.0)
                circleBlur(0.35)
                circleOpacity(
                    Expression.fromRaw(
                        """["step", ["zoom"], 0.0, $VEH_NEAR_ZOOM, 0.22]"""
                    )
                )
            }

            // ── 5. SymbolLayer (above halo) ───────────────────────────────────
            MarkerLayers.addPinLayerIfMissing(style, VEHICLES_LAYER_ID, VEHICLES_SOURCE_ID) {
                iconImage(
                    Expression.fromRaw(
                        """["step", ["zoom"],
                          ["get", "$PROP_VEHICLE_DOT"],
                          $VEH_NEAR_ZOOM,
                          ["get", "$PROP_VEHICLE_ICON"]
                        ]"""
                    )
                )
                // iconAnchor=CENTER per entrambe le varianti.
                // Il bitmap pill è costruito con il dot center al CENTRO verticale
                // del bitmap (VehicleMarkerBitmap.pill) — parità DoVe vehiclePill.
                iconAnchor(IconAnchor.CENTER)
                iconAllowOverlap(true)
                iconIgnorePlacement(true)
                // Selected vehicle renders on top of others (sort key ascending).
                symbolSortKey(
                    Expression.fromRaw(
                        """["case", ["==", ["get", "$PROP_VEHICLE_SELECTED"], 1], 1.0, 0.0]"""
                    )
                )
            }

            // ── 6. Start position glide ──────────────────────────────────────
            animator.animateTo(style, VEHICLES_SOURCE_ID, targets)
        }
    }
}

/** Forza alpha=FF sul colore per Mapbox che ignora alpha parziale sulle immagini. */
private fun forceOpaqueVehicle(argb: Int): Int = argb or 0xFF000000.toInt()

/** Converte ARGB int in stringa hex "#RRGGBB" per circleColor expression. */
private fun argbToHex(argb: Int): String =
    String.format("#%06X", 0xFFFFFF and argb)
