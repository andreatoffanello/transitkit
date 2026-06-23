package com.transitkit.app.ui.mappa

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMapComposable
import com.mapbox.maps.extension.style.layers.properties.generated.IconPitchAlignment
import com.mapbox.maps.extension.style.layers.properties.generated.IconRotationAlignment
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.locationcomponent.location

// ── Stable identifiers ────────────────────────────────────────────────────────

private const val USER_PUCK_SOURCE_ID = "tk_user_puck_source"
private const val USER_PUCK_LAYER_ID  = "tk_user_puck_layer"
private const val USER_PUCK_IMAGE_ID  = "tk_user_puck_dot"

/**
 * Puck posizione utente come SymbolLayer billboarded — PIATTO a qualunque pitch.
 *
 * ## Perché non `createDefault2DPuck()`
 * Il layer nativo `mapbox-location-indicator-layer` è **ground-projected**: a
 * pitch != 0 il dot si inclina con il piano terreno, dando l'effetto "ellisse
 * in prospettiva" su mappe 3D. Parità con i vehicle/stop SymbolLayer che usano
 * `iconPitchAlignment=VIEWPORT` (= camera-facing = sempre upright).
 *
 * ## Architettura
 *  1. `location.enabled = true` con una `LocationPuck2D` completamente trasparente
 *     — lasciamo a Mapbox il tracking GPS ma nascondiamo il layer ground-projected.
 *  2. `addOnIndicatorPositionChangedListener` → aggiorna un `GeoJsonSource`
 *     (singolo Point) con la posizione live.
 *  3. `SymbolLayer` con `iconPitchAlignment=VIEWPORT` + `iconRotationAlignment=VIEWPORT`:
 *     il dot rimane PIATTO/upright indipendentemente dal pitch della camera.
 *  4. Layer posizionato in cima allo style via `moveStyleLayer` (parità con la
 *     vecchia implementazione nativa — il puck utente deve stare sopra tutto).
 *
 * ## Bitmap
 * Cerchio blu Mapbox-standard (#1E88E5) + anello bianco + drop shadow leggero.
 * Disegnato con Canvas — nessun pathData SVG inventato.
 * Stateless / no cache interna: viene ricalcolato solo al primo attach dello style.
 *
 * Drop-in replacement di `UserLocationPuck()` senza modifiche ai caller.
 */
@Composable
@MapboxMapComposable
internal fun UserLocationPuck() {
    val ctx = LocalContext.current

    MapEffect(Unit) { mapView ->
        // ── 1. Enable Mapbox location tracking with invisible native puck ────
        // `LocationPuck2D(opacity = 0f)` → Mapbox tiene il tracking GPS e il
        // position flow attivo, ma il layer ground-projected è completamente
        // invisibile. opacity=0 è il modo più pulito: evita di manipolare
        // ImageHolder bitmap trasparenti.
        mapView.location.updateSettings {
            enabled = true
            locationPuck = LocationPuck2D(opacity = 0f)
            puckBearingEnabled = false
        }

        // ── 2. Seed the GeoJson source at (0,0) — immediately overwritten by
        //       the position listener below, so it's never visible at this loc.
        mapView.mapboxMap.getStyle { style ->
            val seed = FeatureCollection.fromFeatures(emptyList<Feature>())
            MarkerLayers.upsertSource(style, USER_PUCK_SOURCE_ID, seed)

            // Register bitmap (idempotent — skip if already present from a
            // prior style load, e.g. style reload on dark/light toggle).
            if (style.getStyleImage(USER_PUCK_IMAGE_ID) == null) {
                style.addImage(USER_PUCK_IMAGE_ID, buildPuckBitmap(ctx))
            }

            // ── 3. SymbolLayer — billboarded (VIEWPORT pitch + rotation) ────
            MarkerLayers.addPinLayerIfMissing(style, USER_PUCK_LAYER_ID, USER_PUCK_SOURCE_ID) {
                iconImage(USER_PUCK_IMAGE_ID)
                iconAllowOverlap(true)
                iconIgnorePlacement(true)
                // VIEWPORT = camera-facing: il dot NON si inclina col pitch.
                iconPitchAlignment(IconPitchAlignment.VIEWPORT)
                iconRotationAlignment(IconRotationAlignment.VIEWPORT)
            }

            // Move user puck layer to top of style (above vehicles/stops).
            runCatching {
                style.moveStyleLayer(USER_PUCK_LAYER_ID, com.mapbox.maps.LayerPosition(null, null, null))
            }
        }

        // ── 4. Live position updates via indicator listener ──────────────────
        // addOnIndicatorPositionChangedListener fires every time the SDK gets
        // a new GPS fix — drives the GeoJsonSource with a single Point feature.
        //
        // Re-registers bitmap/layer/source idempotently on every position update:
        // covers style reload (dark/light toggle, basemap reload) where custom
        // layers are wiped. addImage + addPinLayerIfMissing are both idempotent.
        mapView.location.addOnIndicatorPositionChangedListener { point ->
            mapView.mapboxMap.getStyle { style ->
                // Bitmap: re-add if missing (style reload wipes registered images)
                if (style.getStyleImage(USER_PUCK_IMAGE_ID) == null) {
                    style.addImage(USER_PUCK_IMAGE_ID, buildPuckBitmap(ctx))
                }

                // Source + layer (idempotent helpers — skip if already present)
                val fc = FeatureCollection.fromFeature(Feature.fromGeometry(point))
                MarkerLayers.upsertSource(style, USER_PUCK_SOURCE_ID, fc)

                MarkerLayers.addPinLayerIfMissing(style, USER_PUCK_LAYER_ID, USER_PUCK_SOURCE_ID) {
                    iconImage(USER_PUCK_IMAGE_ID)
                    iconAllowOverlap(true)
                    iconIgnorePlacement(true)
                    iconPitchAlignment(IconPitchAlignment.VIEWPORT)
                    iconRotationAlignment(IconRotationAlignment.VIEWPORT)
                }

                // Move to top (above vehicles/stops). Best-effort.
                runCatching {
                    style.moveStyleLayer(USER_PUCK_LAYER_ID, com.mapbox.maps.LayerPosition(null, null, null))
                }
            }
        }
    }
}

// ── Puck bitmap ───────────────────────────────────────────────────────────────

/**
 * Disegna un cerchio blu + anello bianco + drop shadow leggero.
 * Dimensioni: 22 dp dot + 3 dp ring + 3 dp shadow pad = ~ 28 dp totali.
 * Colore: Mapbox system blue #1E88E5.
 */
private fun buildPuckBitmap(
    ctx: android.content.Context,
    dotDp: Float = 14f,
    ringWidthDp: Float = 3f,
    shadowRadiusDp: Float = 3f,
): Bitmap {
    val density = ctx.resources.displayMetrics.density
    val dotPx   = dotDp * density
    val ringPx  = ringWidthDp * density
    val padPx   = (shadowRadiusDp * density).toInt()

    val dotR  = dotPx / 2f
    val ringR = dotR + ringPx / 2f
    val total = (dotPx + ringPx + padPx * 2).toInt()

    val bmp = Bitmap.createBitmap(total, total, Bitmap.Config.ARGB_8888)
    val c   = Canvas(bmp)
    val cx  = total / 2f
    val cy  = total / 2f

    // Drop shadow (sotto il ring)
    c.drawCircle(cx, cy + 1.2f * density, ringR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x50000000
        style = Paint.Style.FILL
    })

    // Ring bianco
    c.drawCircle(cx, cy, ringR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AColor.WHITE
        style = Paint.Style.FILL
    })

    // Dot blu Mapbox (#1E88E5)
    c.drawCircle(cx, cy, dotR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1E88E5.toInt()
        style = Paint.Style.FILL
    })

    return bmp
}
