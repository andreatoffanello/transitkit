package com.transitkit.app.ui.mappa

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Camera target proveniente dal deep link `transitkit://map?lat=&lng=&zoom=&pitch=`.
 *
 * Pattern portato da Movete (`PendingMapCamera` in AppState.kt): MainActivity
 * intercetta l'Intent in `onNewIntent` e setta il valore qui; [MappaScreen] lo
 * collega come Flow e applica `viewportState.flyTo` + consume.
 *
 * Pitch è opzionale — se null, MappaScreen mantiene il pitch corrente. Tipico:
 *  - `transitkit://map?lat=41.8902&lng=12.4922&zoom=17&pitch=55` — Roma 3D
 *  - `transitkit://map?lat=40.7580&lng=-73.9855&zoom=16` — NYC, pitch invariato
 */
data class PendingMapCamera(
    val latitude: Double,
    val longitude: Double,
    val zoom: Double,
    val pitch: Double? = null,
)

object PendingMapCameraStore {
    private val _pending = MutableStateFlow<PendingMapCamera?>(null)
    val pending: StateFlow<PendingMapCamera?> = _pending

    fun set(camera: PendingMapCamera) {
        _pending.value = camera
    }

    fun consume(): PendingMapCamera? {
        val v = _pending.value
        _pending.value = null
        return v
    }
}
