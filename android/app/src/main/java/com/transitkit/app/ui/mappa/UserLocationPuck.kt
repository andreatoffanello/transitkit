package com.transitkit.app.ui.mappa

import androidx.compose.runtime.Composable
import com.mapbox.maps.LayerPosition
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMapComposable
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location

/**
 * Enables the Mapbox native LocationComponent puck — classic blue dot with
 * accuracy halo.
 *
 * iOS parity: il puck NON va tintato col theme operatore (su iOS era verde
 * accent → appariva quasi nero). Su Android usiamo `createDefault2DPuck()`
 * che è blu nativo Mapbox — niente override custom.
 *
 * Bearing cone disabled di default: a pitch != 0 il cono di heading viene
 * proiettato in prospettiva creando un "triangolo arrow weird" sopra il
 * dot. Sui detail hero/expanded map (3D scenico) o quando MapTab è in 3D
 * mode il puck deve restare solo "pallino + accuracy". Per workflow di
 * navigazione turn-by-turn (futuro) si può abilitare esplicitamente.
 *
 * Z-order: Mapbox renderizza il puck sul suo `mapbox-location-indicator-layer`
 * che dovrebbe stare sopra agli annotation manager. Per sicurezza, dopo
 * l'enable forziamo lo spostamento del layer in cima allo style — utile
 * quando i vehicle/stop annotation manager vengono creati DOPO il puck e
 * potrebbero altrimenti inserirsi sopra.
 *
 * Drop this inside any `MapboxMap { ... }` content scope to opt in. Requires
 * the host activity/app to have ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION
 * granted — otherwise the puck silently stays off.
 *
 * @param withBearing when true, the bearing cone (heading) is shown around the
 *        puck if a device compass / sensor signal is available. Default off.
 */
@Composable
@MapboxMapComposable
internal fun UserLocationPuck(withBearing: Boolean = false) {
    MapEffect(withBearing) { mapView ->
        mapView.location.updateSettings {
            enabled = true
            locationPuck = createDefault2DPuck(withBearing = withBearing)
            puckBearingEnabled = withBearing
            puckBearing = PuckBearing.HEADING
        }
        // Forza il layer del puck in cima allo style — defensivo contro
        // annotation manager (vehicles/stops) che venissero registrati dopo
        // e potrebbero altrimenti coprire il pallino utente.
        mapView.mapboxMap.getStyle { style ->
            runCatching {
                style.moveStyleLayer(
                    LOCATION_INDICATOR_LAYER_ID,
                    LayerPosition(null, null, null), // null,null,null = top
                )
            }
        }
    }
}

// Layer id ufficiale Mapbox Maps SDK v11 per il puck 2D.
private const val LOCATION_INDICATOR_LAYER_ID = "mapbox-location-indicator-layer"
