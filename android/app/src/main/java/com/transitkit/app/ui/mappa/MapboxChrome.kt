package com.transitkit.app.ui.mappa

import com.mapbox.bindgen.Value
import com.mapbox.common.Cancelable
import com.mapbox.maps.MapView
import com.mapbox.maps.Style

/**
 * Pattern allineato a Movete/Civici (DoVe): prende il `MapView`, applica
 * subito se lo style è già caricato, e sottoscrive `styleLoaded` per
 * coprire i ricaricamenti (tema light/dark, ricomposizioni). Il `Cancelable`
 * va annullato dal chiamante (uso tipico: `awaitCancellation` dentro
 * `MapEffect`) per evitare leak di subscription multiple.
 */
fun applyTransitKitStandardStyleConfig(
    mapView: MapView,
    isDark: Boolean,
    show3D: Boolean = true,
): Cancelable {
    fun apply(style: Style) {
        runCatching {
            style.setStyleImportConfigProperty("basemap", "showPointOfInterestLabels", Value.valueOf(false))
            style.setStyleImportConfigProperty("basemap", "showTransitLabels", Value.valueOf(false))
            style.setStyleImportConfigProperty("basemap", "showRoadLabels", Value.valueOf(false))
            style.setStyleImportConfigProperty("basemap", "show3dObjects", Value.valueOf(show3D))
            style.setStyleImportConfigProperty("basemap", "lightPreset", Value.valueOf(if (isDark) "night" else "day"))
        }
    }
    mapView.mapboxMap.style?.let { apply(it) }
    return mapView.mapboxMap.subscribeStyleLoaded { mapView.mapboxMap.style?.let { apply(it) } }
}

/**
 * Style config tuned for the journey overview map. Keeps the dark/light preset
 * to match the basemap to system theme, but the polyline layer is added via
 * low-level lineLayer (not PolylineAnnotation) so the route colors are not
 * darkened by the Standard style's ambient lighting.
 */
fun applyTransitKitJourneyMapStyleConfig(style: Style, isDark: Boolean) {
    style.setStyleImportConfigProperty("basemap", "showPointOfInterestLabels", Value.valueOf(false))
    style.setStyleImportConfigProperty("basemap", "showTransitLabels", Value.valueOf(false))
    style.setStyleImportConfigProperty("basemap", "showRoadLabels", Value.valueOf(false))
    style.setStyleImportConfigProperty("basemap", "show3dObjects", Value.valueOf(false))
    style.setStyleImportConfigProperty("basemap", "lightPreset", Value.valueOf(if (isDark) "night" else "day"))
}

/**
 * Style config per le mappe HERO (dettaglio fermata / dettaglio linea).
 *
 * Differenze rispetto a [applyTransitKitStandardStyleConfig]:
 *  - **showRoadLabels = true**  → l'utente vede i nomi delle strade per
 *    orientarsi (su una mappa fissa di una singola fermata serve contesto).
 *  - **show3dObjects = true**   → edifici 3D attivi: rende la vista "scenica"
 *    quando la camera è a pitch ~50°. Stesso effetto del "Colosseo" di
 *    Movete sulla schermata dettaglio fermata.
 *
 * POI labels e transit labels restano spenti (clutter / sostituiti dai
 * nostri marker custom).
 */
fun applyTransitKitHeroStyleConfig(style: Style, isDark: Boolean) {
    style.setStyleImportConfigProperty("basemap", "showPointOfInterestLabels", Value.valueOf(false))
    style.setStyleImportConfigProperty("basemap", "showTransitLabels", Value.valueOf(false))
    style.setStyleImportConfigProperty("basemap", "showRoadLabels", Value.valueOf(true))
    style.setStyleImportConfigProperty("basemap", "show3dObjects", Value.valueOf(true))
    style.setStyleImportConfigProperty("basemap", "lightPreset", Value.valueOf(if (isDark) "night" else "day"))
}

/**
 * Opt into high-refresh rendering on devices that support it (Pixel 6+ /
 * Galaxy S2x / OnePlus 10 — 90/120Hz). Mapbox defaults the render loop to
 * 60Hz; on a ProMotion-class display this leaves visible cadence on the
 * table during pan/zoom. Safe no-op on 60Hz devices and on the emulator.
 */
fun MapView.applyMaximumFps(target: Int = 120) {
    runCatching { setMaximumFps(target) }
}
