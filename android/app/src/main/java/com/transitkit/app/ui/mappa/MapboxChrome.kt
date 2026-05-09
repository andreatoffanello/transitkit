package com.transitkit.app.ui.mappa

import com.mapbox.bindgen.Value
import com.mapbox.maps.Style

fun applyTransitKitStandardStyleConfig(style: Style, isDark: Boolean) {
    style.setStyleImportConfigProperty("basemap", "showPointOfInterestLabels", Value.valueOf(false))
    style.setStyleImportConfigProperty("basemap", "showTransitLabels", Value.valueOf(false))
    style.setStyleImportConfigProperty("basemap", "showRoadLabels", Value.valueOf(false))
    style.setStyleImportConfigProperty("basemap", "show3dObjects", Value.valueOf(false))
    style.setStyleImportConfigProperty("basemap", "lightPreset", Value.valueOf(if (isDark) "night" else "day"))
}
