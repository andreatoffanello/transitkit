package com.transitkit.app.ui.mappa

import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.MapboxStyleManager
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.extension.style.layers.generated.CircleLayerDsl
import com.mapbox.maps.extension.style.layers.generated.LineLayerDsl
import com.mapbox.maps.extension.style.layers.generated.SymbolLayerDsl
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs

/**
 * Helpers `GeoJsonSource` + `SymbolLayer` (Mapbox 11.x). Astrae il flusso
 * ripetuto:
 *  1. al primo passaggio aggiungi source + 1-N layer
 *  2. ai successivi aggiorna solo la `featureCollection` del source
 *
 * Tutte le funzioni sono idempotenti: chiamare due volte non rompe lo stato.
 */
internal object MarkerLayers {

    fun upsertSource(
        style: MapboxStyleManager,
        sourceId: String,
        fc: FeatureCollection,
    ) {
        val src = style.getSourceAs<GeoJsonSource>(sourceId)
        if (src == null) {
            style.addSource(geoJsonSource(sourceId) { featureCollection(fc) })
        } else {
            src.featureCollection(fc)
        }
    }

    fun addPinLayerIfMissing(
        style: MapboxStyleManager,
        layerId: String,
        sourceId: String,
        config: SymbolLayerDsl.() -> Unit,
    ) {
        if (style.styleLayerExists(layerId)) return
        style.addLayer(symbolLayer(layerId, sourceId, config))
    }

    /** Aggiunge un `CircleLayer` se non esiste già (es. alone/halo dei mezzi live). */
    fun addCircleLayerIfMissing(
        style: MapboxStyleManager,
        layerId: String,
        sourceId: String,
        config: CircleLayerDsl.() -> Unit,
    ) {
        if (style.styleLayerExists(layerId)) return
        style.addLayer(circleLayer(layerId, sourceId, config))
    }

    /**
     * Aggiunge un `LineLayer` SOTTO [belowLayerId] (es. la polilinea della rotta
     * sotto fermate e mezzi). Se il layer di riferimento non esiste ancora,
     * fallback ad `addLayer` (in coda allo stack corrente) — sull'ordine di
     * composizione la polilinea viene comunque aggiunta prima dei marker, quindi
     * resta sotto. Idempotente.
     */
    fun addLineLayerBelowIfMissing(
        style: MapboxStyleManager,
        layerId: String,
        sourceId: String,
        belowLayerId: String,
        config: LineLayerDsl.() -> Unit,
    ) {
        if (style.styleLayerExists(layerId)) return
        val layer = lineLayer(layerId, sourceId, config)
        if (style.styleLayerExists(belowLayerId)) {
            style.addLayerBelow(layer, belowLayerId)
        } else {
            style.addLayer(layer)
        }
    }
}
