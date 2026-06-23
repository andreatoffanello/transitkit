package com.transitkit.app.ui.mappa

import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import com.google.gson.JsonObject
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxStyleManager

/**
 * Glide morbido dei marker veicoli tra snapshot GTFS-RT.
 *
 * I VehiclePosition arrivano ~ogni 15s (poll GtfsRtFetcher): assegnare la geometria diretta al
 * `GeoJsonSource` fa "teletrasportare" i pin. Qui ogni veicolo interpola la
 * propria posizione mostrata verso quella nuova con un glide LINEARE di 900ms,
 * ricostruendo la FeatureCollection a ogni frame del [ValueAnimator].
 *
 * Porta fedele di `VehicleMarkerAnimator` da DoVe (civici) — stessa durata,
 * stesso interpolatore, stesso pattern di handover morbido.
 *
 * Stato e animator toccati SOLO dal main thread (MapEffect + ValueAnimator
 * callbacks). Parità iOS `AnimatedVehicleMarker`.
 */
internal class VehicleMarkerAnimator {

    /** Posizione attualmente mostrata per vehicleId: [lng, lat]. */
    private val displayed = HashMap<String, DoubleArray>()
    private var animator: ValueAnimator? = null

    /** Destinazione di un veicolo: props GeoJSON + coordinata target. */
    data class Target(
        val id: String,
        val props: JsonObject,
        val lng: Double,
        val lat: Double,
    )

    /**
     * Posizione di partenza del glide per [id]: l'ultima mostrata se il
     * veicolo era già a schermo, altrimenti la destinazione stessa (veicolo
     * nuovo → appare direttamente in posizione, niente glide da [0,0]).
     */
    fun startFor(id: String, destLng: Double, destLat: Double): DoubleArray =
        displayed[id]?.let { doubleArrayOf(it[0], it[1]) } ?: doubleArrayOf(destLng, destLat)

    /**
     * Avvia il glide di tutti i [targets] dalla loro posizione corrente alla
     * nuova. Cancella un glide in corso (handover morbido: riparte dalla
     * posizione raggiunta in quell'istante).
     */
    fun animateTo(style: MapboxStyleManager, sourceId: String, targets: List<Target>) {
        animator?.cancel()

        // Snapshot delle partenze PRIMA di mutare `displayed` durante i tick.
        val starts = HashMap<String, DoubleArray>(targets.size)
        for (t in targets) starts[t.id] = startFor(t.id, t.lng, t.lat)
        // Dimentica i veicoli spariti da questo snapshot.
        displayed.keys.retainAll(targets.map { it.id }.toSet())

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = GLIDE_MS
            interpolator = LinearInterpolator()
            addUpdateListener { va ->
                val f = va.animatedValue as Float
                val feats = ArrayList<Feature>(targets.size)
                for (t in targets) {
                    val s = starts.getValue(t.id)
                    val lng = s[0] + (t.lng - s[0]) * f
                    val lat = s[1] + (t.lat - s[1]) * f
                    val cur = displayed.getOrPut(t.id) { DoubleArray(2) }
                    cur[0] = lng
                    cur[1] = lat
                    feats.add(Feature.fromGeometry(Point.fromLngLat(lng, lat), t.props))
                }
                MarkerLayers.upsertSource(style, sourceId, FeatureCollection.fromFeatures(feats))
            }
            start()
        }
    }

    /** Ferma il glide (onDispose del MapEffect). */
    fun cancel() {
        animator?.cancel()
        animator = null
    }

    companion object {
        /** Durata glide: rapido ma morbido. Parità DoVe + iOS AnimatedVehicleMarker. */
        private const val GLIDE_MS = 900L
    }
}
