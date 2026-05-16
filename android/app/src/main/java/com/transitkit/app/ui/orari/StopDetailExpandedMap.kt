package com.transitkit.app.ui.orari

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle
import com.transitkit.app.config.LocalTransitColors
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.ui.mappa.StopSymbolLayer
import com.transitkit.app.ui.mappa.UnifiedMapControlsPill
import com.transitkit.app.ui.mappa.UserLocationPuck
import com.transitkit.app.ui.mappa.applyTransitKitHeroStyleConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Mappa fermata espansa a fullscreen — iOS parity (item #2 + #5).
 *
 * Entry 3D (pitch 45°), pin singolo della fermata, user location puck, e
 * [UnifiedMapControlsPill] con tutti i 4 pulsanti: 2D/3D, recenter, reset
 * bearing (cond.), close.
 *
 * NON c'è bottone "Apri in mappe" qui — quella navigazione esterna resta
 * solo nel menu della toolbar di StopDetailScreen (item #2 spec).
 */
@Composable
internal fun StopDetailExpandedMap(
    stop: ResolvedStop,
    accent: androidx.compose.ui.graphics.Color,
    onDismiss: () -> Unit,
) {
    val colors = LocalTransitColors.current
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val point = Point.fromLngLat(stop.lon, stop.lat)

    val viewport = rememberMapViewportState {
        setCameraOptions(
            CameraOptions.Builder()
                .center(point)
                .zoom(16.0)
                .pitch(45.0)        // entry 3D, iOS parity
                .bearing(0.0)
                .build()
        )
    }

    // Camera state polling — pitch + bearing per il pill (uguale a MappaScreen).
    var currentPitch by remember { mutableDoubleStateOf(45.0) }
    var currentBearing by remember { mutableDoubleStateOf(0.0) }
    LaunchedEffect(Unit) {
        while (true) {
            val cs = viewport.cameraState
            if (cs != null) {
                currentPitch = cs.pitch
                currentBearing = cs.bearing
            }
            delay(200)
        }
    }
    val is3D by remember { derivedStateOf { currentPitch > 10.0 } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
        ) {
            MapboxMap(
                mapViewportState = viewport,
                style = { MapStyle(style = com.mapbox.maps.Style.STANDARD) },
                compass = {},
                scaleBar = {},
                modifier = Modifier.fillMaxSize(),
            ) {
                MapEffect(isDark) { mapView ->
                    val applied = mapView.mapboxMap.style
                    if (applied != null) {
                        applyTransitKitHeroStyleConfig(applied, isDark)
                    } else {
                        mapView.mapboxMap.subscribeStyleLoaded {
                            mapView.mapboxMap.style?.let {
                                applyTransitKitHeroStyleConfig(it, isDark)
                            }
                        }
                    }
                }
                // Stesso marker della MapTab — single source of truth.
                StopSymbolLayer(
                    stops = listOf(stop),
                    selectedStop = stop,
                    selectedRoute = null,
                    accentColor = accent,
                )
                // Puck SENZA bearing cone: con pitch 45° il cono di heading
                // veniva proiettato in prospettiva creando un "triangolo
                // sopra al dot" weird (issue segnalata). Senza cono resta
                // solo il pallino blu + accuracy ring, leggibile a qualsiasi
                // angolo di camera.
                UserLocationPuck(withBearing = false)
            }

            // Controlli mappa — pill verticale a destra centro.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(end = 16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                UnifiedMapControlsPill(
                    is3D = is3D,
                    onToggle3D = {
                        scope.launch {
                            viewport.flyTo(
                                CameraOptions.Builder()
                                    .pitch(if (is3D) 0.0 else 45.0)
                                    .build()
                            )
                        }
                    },
                    onRecenter = {
                        scope.launch {
                            viewport.flyTo(
                                CameraOptions.Builder()
                                    .center(point)
                                    .zoom(16.0)
                                    .build()
                            )
                        }
                    },
                    currentBearing = currentBearing,
                    onResetBearing = {
                        scope.launch {
                            viewport.flyTo(
                                CameraOptions.Builder()
                                    .bearing(0.0)
                                    .build()
                            )
                        }
                    },
                    expanded = true,
                    onExpandToggle = onDismiss,
                )
            }
        }
    }
}
