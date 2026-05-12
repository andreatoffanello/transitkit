package com.transitkit.app.ui.mappa

/**
 * Tier discreti di zoom mappa. Sorgente unica per:
 *  - rendering filter (SymbolLayer expressions / Composable visibility)
 *  - selezione marker style (dot vs pin)
 *  - parità visiva con iOS (vedi MapZoomLevels.swift)
 *
 * I bucket sono volutamente pochi (3) per evitare flicker su transizioni —
 * ogni soglia coincide con un cambio visivo netto.
 */
enum class MapZoomTier {
    /** zoom < cityMaxZoom → vista cittadina, dot piccolo. */
    City,
    /** cityMaxZoom ≤ zoom < neighborhoodMaxZoom → dot. */
    Neighborhood,
    /** zoom ≥ neighborhoodMaxZoom → pin pieno + icona / lettera. */
    Street,
}

/**
 * Single source of truth per soglie e zoom preferenziali della mappa Mapbox.
 * Convenzione zoom Mapbox: 0..22, più alto = più zoomato (parità web/Google).
 */
object MapZoomLevels {

    // ── Soglie tier (confine SUPERIORE — strict less than) ───────────────────

    /** Zoom < questo valore → [MapZoomTier.City]. Sopra → Neighborhood. */
    const val cityMaxZoom = 12.0

    /** Zoom < questo valore → [MapZoomTier.Neighborhood]. ≥ → [MapZoomTier.Street]. */
    const val neighborhoodMaxZoom = 14.0

    // ── Preferred zoom per use-case ──────────────────────────────────────────

    /** Zoom default all'apertura della tab Mappa quando NON c'è posizione utente. */
    const val cityDefaultEntry = 12.5

    /** Zoom default all'apertura della tab Mappa quando c'è posizione utente. */
    const val userDefaultEntry = 15.0

    /** Zoom per "Vedi linea su mappa" — overview di tutta la linea selezionata. */
    const val lineOverview = 12.5

    /** Zoom per follow veicolo (auto-focus quando seleziono un veicolo). */
    const val vehicleFocus = 16.5

    /** Zoom per focus su singola fermata (auto-zoom su tap se zoom inferiore). */
    const val stopFocus = 16.0

    // ── Tier resolver ────────────────────────────────────────────────────────

    fun tier(zoom: Double): MapZoomTier = when {
        zoom < cityMaxZoom         -> MapZoomTier.City
        zoom < neighborhoodMaxZoom -> MapZoomTier.Neighborhood
        else                       -> MapZoomTier.Street
    }
}
