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

    /** Cap di zoom-out (min zoom Mapbox) — app cittadina, oltre l'overview
     *  regionale la mappa è solo rumore. Allineato a `maxCameraDistance`
     *  su iOS (MapZoomLevels.swift); quando aggiorni uno, aggiorna l'altro. */
    const val minUserZoom = 8.0

    // ── Pitch (3D) ───────────────────────────────────────────────────────────
    //
    // Il 3D è una feature dello zoom vicino, non una modalità globale: tilt e
    // panning prospettico hanno senso solo dove Mapbox estrude gli edifici
    // (fill-extrusion dal LOD ~15.5-16). Sotto questa soglia la camera torna
    // piatta, così la vista d'insieme resta leggibile. La preferenza utente NON
    // viene persa: è intent, e il pitch effettivo è `intent && zoom lo consente`.
    //
    // Allineato a iOS `MapZoomLevels.pitch3DMaxDelta = 0.009`
    // (zoom 15.5 ≈ latDelta 0.009); quando aggiorni uno, aggiorna l'altro.

    /** Zoom ≥ questo valore → il 3D è consentito. Sotto → camera piatta. */
    const val pitch3DMinZoom = 15.5

    /** Zoom a cui portarsi quando l'utente chiede il 3D da troppo lontano:
     *  entrare in 3D senza avvicinarsi darebbe una mappa piatta inclinata. */
    const val pitch3DEntryZoom = 16.0

    /** Angolo di tilt delle mappe interattive. Uno solo, ovunque. */
    const val pitch3D = 45.0

    /** Angolo di tilt delle hero map non interattive (header dettaglio fermata):
     *  più cinematico perché non ci si naviga dentro. Parità con iOS. */
    const val pitch3DHero = 50.0

    // ── Tier resolver ────────────────────────────────────────────────────────

    fun tier(zoom: Double): MapZoomTier = when {
        zoom < cityMaxZoom         -> MapZoomTier.City
        zoom < neighborhoodMaxZoom -> MapZoomTier.Neighborhood
        else                       -> MapZoomTier.Street
    }

    /** True se a questo zoom il 3D è consentito (per etichette e stato dei controlli). */
    fun allows3D(zoom: Double): Boolean = zoom >= pitch3DMinZoom

    /**
     * Pitch effettivo per uno zoom dato l'intent dell'utente.
     * Unico punto in cui si decide se la camera è inclinata o piatta.
     *
     * GOTCHA Mapbox: un `CameraOptions.Builder` che NON chiama `.pitch(...)`
     * lascia il pitch INVARIATO. Ogni fly-to che può atterrare a zoom cittadino
     * deve passare da qui, altrimenti eredita silenziosamente il tilt.
     */
    fun pitchFor(zoom: Double, intent3D: Boolean = true, angle: Double = pitch3D): Double =
        if (intent3D && allows3D(zoom)) angle else 0.0
}
