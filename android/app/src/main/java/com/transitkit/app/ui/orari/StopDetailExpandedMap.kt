package com.transitkit.app.ui.orari

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import kotlinx.coroutines.awaitCancellation
import com.transitkit.app.config.LocalTransitColors
import com.transitkit.app.data.model.ResolvedDeparture
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.ui.mappa.MapLineFocusViewModel
import com.transitkit.app.ui.mappa.MapZoomLevels
import com.transitkit.app.ui.mappa.RoutePolylineLayer
import com.transitkit.app.ui.mappa.StopSymbolLayer
import com.transitkit.app.ui.mappa.UnifiedMapControlsPill
import com.transitkit.app.ui.mappa.UserLocationPuck
import com.transitkit.app.ui.mappa.VehicleSymbolLayer
import com.transitkit.app.ui.mappa.applyTransitKitHeroStyleConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Mappa fermata espansa a fullscreen — iOS parity (item #2 + #5).
 *
 * Entry 3D (pitch 45° a zoom fermata), pin singolo della fermata, user
 * location puck, e
 * [UnifiedMapControlsPill] con tutti i 4 pulsanti: 2D/3D, recenter, reset
 * bearing (cond.), close.
 *
 * Vista linea: track di badge in alto ([ExpandedMapLineTrack]) — tap su un
 * badge attiva polilinea + fermate tiered + mezzi live della linea (stessi
 * primitivi della mappa principale, dati da [MapLineFocusViewModel]) con
 * fit camera sull'intera linea; ri-tap deseleziona e riporta la camera
 * sulla fermata. I layer restano SEMPRE composti — al deselect ricevono
 * liste vuote (un early-return lascerebbe il source Mapbox sporco).
 *
 * NON c'è bottone "Apri in mappe" qui — quella navigazione esterna resta
 * solo nel menu della toolbar di StopDetailScreen (item #2 spec).
 */
@Composable
internal fun StopDetailExpandedMap(
    stop: ResolvedStop,
    accent: androidx.compose.ui.graphics.Color,
    availableRoutes: List<ResolvedDeparture>,
    onDismiss: () -> Unit,
) {
    val colors = LocalTransitColors.current
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val point = Point.fromLngLat(stop.lon, stop.lat)

    val lineFocus: MapLineFocusViewModel = hiltViewModel()
    val selectedRouteId by lineFocus.selectedRouteId.collectAsStateWithLifecycle()
    val selectedRoute by lineFocus.selectedRoute.collectAsStateWithLifecycle()
    val routePolylines by lineFocus.routePolylines.collectAsStateWithLifecycle()
    val lineStops by lineFocus.lineStops.collectAsStateWithLifecycle()
    val vehiclesWithColor by lineFocus.vehiclesWithColor.collectAsStateWithLifecycle()
    val routes by lineFocus.routes.collectAsStateWithLifecycle()

    // Chiusura del fullscreen → reset del selettore: la mappa compatta
    // sottostante torna alla sola fermata.
    DisposableEffect(Unit) {
        onDispose { lineFocus.clear() }
    }

    val selectedLineColor = remember(selectedRoute?.color) {
        selectedRoute?.color?.takeIf { it.isNotBlank() }?.let { hex ->
            com.transitkit.app.ui.components.parseHexColor(hex, fallback = Color.Transparent)
                .takeIf { it != Color.Transparent }
        }
    }

    val viewport = rememberMapViewportState {
        setCameraOptions(
            CameraOptions.Builder()
                .center(point)
                .zoom(MapZoomLevels.stopFocus)
                .pitch(MapZoomLevels.pitch3D)   // entry 3D, iOS parity
                .bearing(0.0)
                .build()
        )
    }

    // Camera state polling — pitch, bearing e zoom per il pill (uguale a MappaScreen).
    var currentPitch by remember { mutableDoubleStateOf(MapZoomLevels.pitch3D) }
    var currentBearing by remember { mutableDoubleStateOf(0.0) }
    var currentZoom by remember { mutableDoubleStateOf(MapZoomLevels.stopFocus) }
    LaunchedEffect(Unit) {
        while (true) {
            val cs = viewport.cameraState
            if (cs != null) {
                currentPitch = cs.pitch
                currentBearing = cs.bearing
                currentZoom = cs.zoom
            }
            delay(200)
        }
    }
    // Preferenza dell'utente: sopravvive ai cambi di zoom (l'overview di linea
    // porta la camera piatta senza che l'utente abbia chiesto la 2D).
    var intent3D by remember { mutableStateOf(true) }
    // Stato EFFETTIVO — è quello che il pill mostra: senza il gate sullo zoom
    // l'etichetta direbbe "3D" sopra una mappa piatta.
    val is3D by remember {
        derivedStateOf { currentPitch > 10.0 && MapZoomLevels.allows3D(currentZoom) }
    }

    // True solo dopo che una linea è stata selezionata almeno una volta —
    // distingue il deselect (camera back to stop) dalla composizione iniziale.
    var hadLineSelection by remember { mutableStateOf(false) }

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
                MapEffect(Unit) { mapView ->
                    // Cap di zoom-out (app cittadina) — token condiviso con iOS.
                    mapView.mapboxMap.setBounds(
                        com.mapbox.maps.CameraBoundsOptions.Builder()
                            .minZoom(MapZoomLevels.minUserZoom)
                            .build()
                    )
                }

                // Correttivo pitch↔zoom — stesso di MappaScreen: senza, una
                // pinch sotto `pitch3DMinZoom` lascia la camera inclinata su
                // una vista d'insieme mentre il pill dice già "2D".
                MapEffect(Unit) { mapView ->
                    var wasAllowed3D = MapZoomLevels.allows3D(mapView.mapboxMap.cameraState.zoom)
                    var lastDrivenPitch = mapView.mapboxMap.cameraState.pitch
                    val cancelable = mapView.mapboxMap.subscribeMapIdle {
                        val camera = mapView.mapboxMap.cameraState
                        val allows = MapZoomLevels.allows3D(camera.zoom)
                        val tilted = camera.pitch > 10.0
                        val correction = MapAnimationOptions.mapAnimationOptions { duration(250) }
                        when {
                            !allows && camera.pitch > 1.0 -> {
                                lastDrivenPitch = 0.0
                                viewport.easeTo(
                                    CameraOptions.Builder().pitch(0.0).build(),
                                    correction,
                                )
                            }
                            allows && !wasAllowed3D && intent3D && !tilted -> {
                                lastDrivenPitch = MapZoomLevels.pitch3D
                                viewport.easeTo(
                                    CameraOptions.Builder().pitch(MapZoomLevels.pitch3D).build(),
                                    correction,
                                )
                            }
                            // Tilt cambiato a due dita da vicino: l'intent segue
                            // il dito, altrimenti il primo fly-to successivo
                            // rimette il tilt appena tolto.
                            allows && tilted != intent3D &&
                                kotlin.math.abs(camera.pitch - lastDrivenPitch) > 2.0 ->
                                intent3D = tilted
                        }
                        wasAllowed3D = allows
                    }
                    try {
                        awaitCancellation()
                    } finally {
                        cancelable.cancel()
                    }
                }
                // Keyed anche su is3D: senza, gli edifici estrusi restavano
                // accesi in "2D" e il pill cambiava solo il pitch.
                MapEffect(isDark, is3D) { mapView ->
                    mapView.mapboxMap.style?.let { applyTransitKitHeroStyleConfig(it, isDark, show3D = is3D) }
                    val cancelable = mapView.mapboxMap.subscribeStyleLoaded {
                        mapView.mapboxMap.style?.let { applyTransitKitHeroStyleConfig(it, isDark, show3D = is3D) }
                    }
                    try {
                        awaitCancellation()
                    } finally {
                        cancelable.cancel()
                    }
                }

                // Polilinea linea selezionata — stesso layer della mappa principale.
                RoutePolylineLayer(
                    selectedRoute = selectedRoute,
                    routePolylines = routePolylines,
                    selectedLineColor = selectedLineColor,
                )

                // Fermate via SymbolLayer — SEMPRE composto (source unico):
                // senza selezione mostra solo la fermata corrente; con la
                // vista linea attiva il source viene ricostruito con tutte
                // le fermate della linea (il marker base è così sostituito,
                // non duplicato) e la fermata corrente resta evidenziata.
                StopSymbolLayer(
                    stops = if (selectedRouteId != null) lineStops else listOf(stop),
                    selectedStop = stop,
                    selectedRoute = selectedRoute,
                    accentColor = accent,
                )

                // Mezzi live della linea — SymbolLayer nativo (lista vuota quando
                // nessuna selezione). Non interattivi qui: nessun tap handler.
                VehicleSymbolLayer(
                    vehiclesWithColor = vehiclesWithColor,
                    selectedVehicle = null,
                    routes = routes,
                )

                // Puck SENZA bearing cone: con pitch 45° il cono di heading
                // veniva proiettato in prospettiva creando un "triangolo
                // sopra al dot" weird (issue segnalata). Senza cono resta
                // solo il pallino blu + accuracy ring, leggibile a qualsiasi
                // angolo di camera.
                UserLocationPuck()

                // Camera della vista linea — DEVE vivere dentro lo scope mappa:
                // emessa da fuori (callback del tap) il viewport la scarta.
                // Fit dell'intera linea quando le polilinee sono pronte (fetch async).
                MapEffect(selectedRouteId, routePolylines) { _ ->
                    if (selectedRouteId == null) return@MapEffect
                    val all = routePolylines.flatten()
                    if (all.size < 2) return@MapEffect
                    hadLineSelection = true
                    val minLat = all.minOf { it.latitude() }
                    val maxLat = all.maxOf { it.latitude() }
                    val minLon = all.minOf { it.longitude() }
                    val maxLon = all.maxOf { it.longitude() }
                    viewport.flyTo(
                        CameraOptions.Builder()
                            .center(Point.fromLngLat((minLon + maxLon) / 2, (minLat + maxLat) / 2))
                            .zoom(MapZoomLevels.lineOverview)
                            // Overview → piatta per regola di zoom, non per una
                            // modalità nascosta: l'intent 3D resta intatto.
                            .pitch(MapZoomLevels.pitchFor(MapZoomLevels.lineOverview, intent3D))
                            .bearing(0.0)
                            .build()
                    )
                }

                // Reset camera al deselect — keyed SOLO su selectedRouteId:
                // se fosse keyed anche sulle polilinee, il loro svuotamento
                // riavvierebbe l'effect cancellando il delay prima del
                // setCameraOptions finale. Deselect: flyTo da solo viene
                // scartato dal viewport dopo il fit-linea, setCameraOptions
                // da solo idem — la coppia (anima, poi fissa lo stato a fine
                // animazione) tiene.
                MapEffect(selectedRouteId) { _ ->
                    if (selectedRouteId != null || !hadLineSelection) return@MapEffect
                    hadLineSelection = false
                    val stopCamera = CameraOptions.Builder()
                        .center(point)
                        .zoom(MapZoomLevels.stopFocus)
                        .pitch(MapZoomLevels.pitchFor(MapZoomLevels.stopFocus, intent3D))
                        .bearing(0.0)
                        .build()
                    viewport.flyTo(stopCamera)
                    delay(650)
                    viewport.setCameraOptions(stopCamera)
                }
            }

            // Vignetta theme-aware in alto — sfuma verso il colore di sfondo
            // dell'app (scuro in dark, chiaro in light), solo leggibilità.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            0f to colors.background.copy(alpha = 0.88f),
                            1f to Color.Transparent,
                        )
                    )
            )

            // Scrim status bar — le icone di sistema del Dialog sono chiare e NON
            // controllabili via window API (la Dialog window le resetta dopo
            // l'attach). Garantiamo il contrasto con uno scrim scuro dietro lo
            // status bar, sopra la vignetta chiara (pattern maps immersive: icone
            // bianche su velatura scura). Solo l'altezza dello status bar → non
            // intacca il line track sottostante.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.45f),
                            1f to Color.Black.copy(alpha = 0.12f),
                        )
                    )
            )

            // Track badge linee — trasparente, edge-to-edge sopra la mappa.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 8.dp),
            ) {
                ExpandedMapLineTrack(
                    routes = availableRoutes,
                    selectedRouteId = selectedRouteId,
                    onRouteTap = lineFocus::toggleRoute,
                )
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
                        val enabling = !is3D
                        intent3D = enabling
                        scope.launch {
                            val target = CameraOptions.Builder()
                                .pitch(if (enabling) MapZoomLevels.pitch3D else 0.0)
                            // Inclinare da lontano darebbe una mappa piatta storta:
                            // il 3D si accende avvicinandosi.
                            if (enabling && !MapZoomLevels.allows3D(currentZoom)) {
                                target.zoom(MapZoomLevels.pitch3DEntryZoom)
                            }
                            viewport.flyTo(target.build())
                        }
                    },
                    onRecenter = {
                        scope.launch {
                            viewport.flyTo(
                                CameraOptions.Builder()
                                    .center(point)
                                    .zoom(MapZoomLevels.stopFocus)
                                    // Un builder senza .pitch() lascia il tilt
                                    // com'era: qui va detto, non ereditato.
                                    .pitch(MapZoomLevels.pitchFor(MapZoomLevels.stopFocus, intent3D))
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
