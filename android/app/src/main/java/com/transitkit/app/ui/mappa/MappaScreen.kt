package com.transitkit.app.ui.mappa

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.ui.platform.LocalDensity
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle
import com.transitkit.app.config.LocalTransitColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalPermissionsApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
)
@Composable
fun MappaScreen(
    onNavigateToStop: (stopId: String) -> Unit = {},
    onOpenTripDetail: (tripId: String, fromStopId: String, routeName: String, routeColor: String) -> Unit = { _, _, _, _ -> },
    initialRouteId: String? = null,
    initialVehicleId: String? = null,
    initialPreviewStopId: String? = null,
    viewModel: MappaViewModel = hiltViewModel(),
) {
    val vehiclesWithColor by viewModel.vehiclesWithColor.collectAsStateWithLifecycle()
    val mapCenter by viewModel.mapCenter.collectAsStateWithLifecycle()
    val defaultZoom by viewModel.defaultZoom.collectAsStateWithLifecycle()
    val selectedStop by viewModel.selectedStop.collectAsStateWithLifecycle()
    val stops by viewModel.stops.collectAsStateWithLifecycle()
    val stopDepartures by viewModel.stopDepartures.collectAsStateWithLifecycle()
    val isDeparturesLoading by viewModel.isDeparturesLoading.collectAsStateWithLifecycle()
    val routes by viewModel.routes.collectAsStateWithLifecycle()
    val selectedRouteId by viewModel.selectedRouteId.collectAsStateWithLifecycle()
    val selectedVehicle by viewModel.selectedVehicle.collectAsStateWithLifecycle()
    val tripDelays by viewModel.tripDelays.collectAsStateWithLifecycle()
    val liveCountByRouteId by viewModel.liveCountByRouteId.collectAsStateWithLifecycle()
    val routePolylines by viewModel.routePolylines.collectAsStateWithLifecycle()

    // Location permission
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    LaunchedEffect(Unit) { locationPermission.launchPermissionRequest() }

    // Pre-select route when navigating from LineDetail
    LaunchedEffect(initialRouteId) {
        if (initialRouteId != null) viewModel.selectRoute(initialRouteId)
    }

    // Deeplink: open a specific vehicle preview card.
    // Wait up to 15s for the feed to contain the requested id (cold-start path
    // — first fetch + parse of GTFS-RT feed can take several seconds over slow
    // networks). Poll the UNFILTERED list so a stale route filter from
    // SavedStateHandle doesn't hide the target vehicle.
    LaunchedEffect(initialVehicleId) {
        val vid = initialVehicleId ?: return@LaunchedEffect
        // Poll the unfiltered vehicle list — a stale route filter from saved
        // state could hide the target otherwise. 15s covers cold-start where
        // the first GTFS-RT fetch + parse races the first compose frame.
        repeat(150) {
            val match = viewModel.vehicles.value.firstOrNull { it.vehicleId == vid }
            if (match != null) {
                val route = viewModel.routes.value.firstOrNull { it.id == match.routeId }
                val color = route?.color?.takeIf { it.isNotBlank() }?.let { hex ->
                    runCatching { Color(android.graphics.Color.parseColor("#$hex")) }.getOrNull()
                } ?: Color(0xFF06845C)
                viewModel.clearSelectedStop()
                viewModel.selectVehicle(match, color)
                return@LaunchedEffect
            }
            kotlinx.coroutines.delay(100)
        }
    }

    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    // Deeplink: `transitkit://map/stop/<stopId>` — apre la Mappa con la
    // fermata selezionata + camera fly-to a stopFocus zoom (Street tier).
    // Re-runs quando arrivano gli stops (l'iniziale load dal CDN può
    // impiegare diversi secondi); guard `applied` evita double-selection.
    var previewApplied by remember { mutableStateOf(false) }
    LaunchedEffect(initialPreviewStopId, stops) {
        val sid = initialPreviewStopId ?: return@LaunchedEffect
        if (previewApplied) return@LaunchedEffect
        val match = stops.firstOrNull { it.id == sid } ?: return@LaunchedEffect
        viewModel.clearSelectedVehicle()
        viewModel.selectStop(match)
        previewApplied = true
    }

    // Camera state (viewport) — movete parity
    val viewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(mapCenter.longitude(), mapCenter.latitude()))
            zoom(defaultZoom.toDouble())
        }
    }

    // AtomicLong avoids Compose stale-closure: onMapClickListener captures the ref once
    // at composition but always reads the latest timestamp via .get().
    val lastAnnotationTapMs = remember { java.util.concurrent.atomic.AtomicLong(0L) }

    // Camera snapshot polled 300ms (viewportState doesn't expose Compose state).
    // Tracks zoom/bearing/pitch + bounding box for viewport-filtering.
    // FOLLOW-UP: polling `while(true) delay(300)` — audit segnalato. Non fixato qui.
    var currentZoom by remember { mutableStateOf(defaultZoom.toDouble()) }
    var currentBearing by remember { mutableStateOf(0.0) }
    var currentPitch by remember { mutableStateOf(0.0) }

    LaunchedEffect(Unit) {
        var lastLat = 0.0
        var lastLon = 0.0
        var lastZoom = 0.0
        while (true) {
            val camera = viewportState.cameraState
            if (camera != null) {
                val lat = camera.center.latitude()
                val lon = camera.center.longitude()
                currentZoom = camera.zoom
                currentBearing = camera.bearing
                currentPitch = camera.pitch
                val moved =
                    Math.abs(lat - lastLat) > 0.0002 ||
                    Math.abs(lon - lastLon) > 0.0002 ||
                    Math.abs(camera.zoom - lastZoom) > 0.15
                if (moved) {
                    lastLat = lat
                    lastLon = lon
                    lastZoom = camera.zoom
                }
            }
            delay(300)
        }
    }

    val tier by remember { derivedStateOf { MapZoomLevels.tier(currentZoom) } }
    val is3D by remember { derivedStateOf { currentPitch > 10.0 } }

    // Follow selected vehicle — camera segue le posizioni aggiornate.
    var isFollowingVehicle by remember { mutableStateOf(false) }
    LaunchedEffect(vehiclesWithColor, selectedVehicle, isFollowingVehicle) {
        if (!isFollowingVehicle || selectedVehicle == null) return@LaunchedEffect
        val id = selectedVehicle!!.first.vehicleId
        val updated = vehiclesWithColor.firstOrNull { it.first.vehicleId == id } ?: return@LaunchedEffect
        viewportState.flyTo(
            CameraOptions.Builder()
                .center(Point.fromLngLat(updated.first.lon, updated.first.lat))
                .build()
        )
    }

    var showLinePicker by remember { mutableStateOf(false) }

    // Pulse halo condiviso tra tutti i veicoli — 1 animazione, N letture
    val pulseTransition = rememberInfiniteTransition(label = "vehicle_pulse")
    val haloAlpha by pulseTransition.animateFloat(
        initialValue = 0.42f,
        targetValue = 0.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "halo_alpha",
    )

    val selectedRoute = selectedRouteId?.let { id -> routes.firstOrNull { it.id == id } }
    val selectedLineColor = remember(selectedRoute?.color) {
        selectedRoute?.color?.takeIf { it.isNotBlank() }?.let { hex ->
            runCatching { Color(android.graphics.Color.parseColor("#$hex")) }.getOrNull()
        }
    }

    // Fly-to su fermata selezionata (sia da tap che da deeplink map/stop/<id>):
    // porta la camera alla coordinata stop a `stopFocus` zoom — fa scattare
    // il tier Street, mostrando pin pieno + label sotto.
    //
    // Padding bottom: la preview card occupa circa metà schermo. Senza
    // padding la fermata viene centrata sotto la card. Con `EdgeInsets.bottom`
    // = altezza card stimata, Mapbox considera quel pixel range come
    // "occupato" e centra la fermata nell'area visibile sopra la card.
    val density = LocalDensity.current
    val previewCardHeightPx = remember(density) {
        with(density) { 360.dp.toPx().toDouble() }
    }
    LaunchedEffect(selectedStop?.id) {
        val s = selectedStop
        if (s == null) {
            // Resetta il padding al deselect — altrimenti rimane sticky.
            viewportState.flyTo(
                CameraOptions.Builder()
                    .padding(EdgeInsets(0.0, 0.0, 0.0, 0.0))
                    .build()
            )
            return@LaunchedEffect
        }
        viewportState.flyTo(
            CameraOptions.Builder()
                .center(Point.fromLngLat(s.lon, s.lat))
                .zoom(maxOf(currentZoom, MapZoomLevels.stopFocus))
                .padding(EdgeInsets(0.0, 0.0, previewCardHeightPx, 0.0))
                .build()
        )
    }

    // Fit camera ai punti polilinea quando cambia la rotta selezionata
    LaunchedEffect(routePolylines) {
        if (routePolylines.isEmpty()) return@LaunchedEffect
        val all = routePolylines.flatten()
        if (all.size < 2) return@LaunchedEffect
        val minLat = all.minOf { it.latitude() }
        val maxLat = all.maxOf { it.latitude() }
        val minLon = all.minOf { it.longitude() }
        val maxLon = all.maxOf { it.longitude() }
        val center = Point.fromLngLat((minLon + maxLon) / 2, (minLat + maxLat) / 2)
        viewportState.flyTo(
            CameraOptions.Builder()
                .center(center)
                .zoom(MapZoomLevels.lineOverview)
                .build()
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {

        MapboxMap(
            modifier = Modifier
                .fillMaxSize()
                .semantics { testTag = "mappa_tab" },
            mapViewportState = viewportState,
            style = { MapStyle(style = Style.STANDARD) },
            compass = {},
            scaleBar = {},
            onMapClickListener = { _ ->
                val elapsed = System.currentTimeMillis() - lastAnnotationTapMs.get()
                if (elapsed > 400L) {
                    viewModel.clearSelectedStop()
                    viewModel.clearSelectedVehicle()
                    isFollowingVehicle = false
                }
                false
            },
        ) {
            RoutePolylineLayer(
                selectedRoute = selectedRoute,
                routePolylines = routePolylines,
                selectedLineColor = selectedLineColor,
            )

            // Stops via SymbolLayer nativo (no ViewAnnotation Compose).
            StopSymbolLayer(
                stops = stops,
                selectedStop = selectedStop,
                selectedRoute = selectedRoute,
                accentColor = LocalTransitColors.current.accent,
            )

            // Tap handler combinato fermate + (in futuro) veicoli SymbolLayer.
            // Per i veicoli oggi ancora ViewAnnotation: il loro tap è dentro
            // VehicleAnnotationsLayer. Qui solo le fermate.
            MarkerTapHandler(
                layerIds = listOf(STOPS_LAYER_ID),
                onPinTap = { feature, _ ->
                    val stopId = feature.getStringProperty(PROP_STOP_ID) ?: return@MarkerTapHandler
                    val stop = stops.firstOrNull { it.id == stopId } ?: return@MarkerTapHandler
                    lastAnnotationTapMs.set(System.currentTimeMillis())
                    isFollowingVehicle = false
                    viewModel.clearSelectedVehicle()
                    viewModel.selectStop(stop)
                },
            )

            VehicleAnnotationsLayer(
                vehiclesWithColor = vehiclesWithColor,
                selectedVehicle = selectedVehicle,
                selectedRoute = selectedRoute,
                routes = routes,
                tier = tier,
                haloAlpha = haloAlpha,
                lastAnnotationTapMs = lastAnnotationTapMs,
                onVehicleTap = { vehicle, routeColor ->
                    viewModel.clearSelectedStop()
                    viewModel.selectVehicle(vehicle, routeColor)
                    isFollowingVehicle = true
                    scope.launch {
                        viewportState.flyTo(
                            CameraOptions.Builder()
                                .center(Point.fromLngLat(vehicle.lon, vehicle.lat))
                                .zoom(maxOf(currentZoom, MapZoomLevels.vehicleFocus))
                                .build()
                        )
                    }
                },
            )

            MapEffect(Unit) { mapView ->
                mapView.mapboxMap.setPrefetchZoomDelta(4)
            }

            MapEffect(isDark) { mapView ->
                val s = mapView.mapboxMap.style
                if (s != null) {
                    applyTransitKitStandardStyleConfig(s, isDark)
                } else {
                    mapView.mapboxMap.subscribeStyleLoaded {
                        mapView.mapboxMap.style?.let { loaded ->
                            applyTransitKitStandardStyleConfig(loaded, isDark)
                        }
                    }
                }
            }
        }

        // -----------------------------------------------------------------------
        // Top-start: search pill o dismiss chip
        // -----------------------------------------------------------------------

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 16.dp, top = 12.dp),
        ) {
            if (selectedRoute != null) {
                RouteDismissChip(
                    route = selectedRoute,
                    liveCount = liveCountByRouteId[selectedRoute.id] ?: 0,
                    onDismiss = { viewModel.selectRoute(null) },
                )
            } else {
                SearchLinePill(onClick = { showLinePicker = true })
            }
        }

        // -----------------------------------------------------------------------
        // FABs — vertically centered, right-aligned.
        // -----------------------------------------------------------------------

        MappaFabColumn(
            is3D = is3D,
            onResetView = {
                scope.launch {
                    viewportState.flyTo(
                        CameraOptions.Builder()
                            .center(Point.fromLngLat(mapCenter.longitude(), mapCenter.latitude()))
                            .zoom(defaultZoom.toDouble())
                            .pitch(0.0)
                            .bearing(0.0)
                            .build()
                    )
                }
            },
            onRecenter = {
                scope.launch {
                    viewportState.flyTo(
                        CameraOptions.Builder()
                            .center(Point.fromLngLat(mapCenter.longitude(), mapCenter.latitude()))
                            .zoom(defaultZoom.toDouble())
                            .build()
                    )
                }
            },
            onToggle3D = {
                scope.launch {
                    viewportState.flyTo(
                        CameraOptions.Builder()
                            .pitch(if (is3D) 0.0 else 45.0)
                            .build()
                    )
                }
            },
        )

        // -----------------------------------------------------------------------
        // Floating preview card — stop + vehicle previews
        // -----------------------------------------------------------------------

        AnimatedVisibility(
            visible = selectedStop != null || selectedVehicle != null,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
            ) + fadeIn(tween(260)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
            ) + fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            PreviewCardContainer(
                modifier = Modifier.semantics {
                    testTag = if (selectedVehicle != null) "vehicle_preview_card" else "stop_preview_card"
                },
            ) {
                when {
                    selectedVehicle != null -> {
                        val (vehicle, storedColor) = selectedVehicle!!
                        val route = routes.firstOrNull { it.id == vehicle.routeId }
                        // Prefer live GTFS color from the current routes list —
                        // `storedColor` may have fallen back to accent at
                        // deeplink time before the schedule resolved the
                        // route. Fall through to stored color or accent last.
                        val vehicleColor: Color = route?.color?.takeIf { it.isNotBlank() }?.let { hex ->
                            runCatching { Color(android.graphics.Color.parseColor("#$hex")) }.getOrNull()
                        } ?: storedColor
                        val delaySeconds = vehicle.tripId?.let { tripDelays[it] } ?: 0
                        val stopName = vehicle.currentStopId?.takeIf { it.isNotBlank() }?.let { sid ->
                            viewModel.resolveStopName(sid)
                        }
                        val predictedArrivalMs = vehicle.tripId?.let { tid ->
                            vehicle.currentStopId?.takeIf { it.isNotBlank() }?.let { sid ->
                                viewModel.predictedArrivalEpochMs(tid, sid)
                            }
                        }
                        VehiclePreviewContent(
                            vehicle = vehicle,
                            vehicleColor = vehicleColor,
                            route = route,
                            stopName = stopName,
                            predictedArrivalMs = predictedArrivalMs,
                            operatorTimezoneId = viewModel.operatorTimezoneId,
                            isFollowing = isFollowingVehicle,
                            onToggleFollow = { isFollowingVehicle = !isFollowingVehicle },
                            onOpenTrip = {
                                val tid = vehicle.tripId ?: return@VehiclePreviewContent
                                val encodedTrip = java.net.URLEncoder.encode(tid, "UTF-8")
                                val encodedStop = vehicle.currentStopId
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let { java.net.URLEncoder.encode(it, "UTF-8") }
                                    ?: ""
                                val encodedRoute = java.net.URLEncoder.encode(route?.name ?: "", "UTF-8")
                                val encodedColor = java.net.URLEncoder.encode(route?.color ?: "", "UTF-8")
                                onOpenTripDetail(encodedTrip, encodedStop, encodedRoute, encodedColor)
                            },
                            onDismiss = {
                                viewModel.clearSelectedVehicle()
                                isFollowingVehicle = false
                            },
                        )
                    }
                    selectedStop != null -> {
                        selectedStop?.let { stop ->
                            StopPreviewContent(
                                stop = stop,
                                departures = stopDepartures,
                                isLoading = isDeparturesLoading,
                                onClose = { viewModel.clearSelectedStop() },
                                onNavigateToStop = onNavigateToStop,
                            )
                        }
                    }
                }
            }
        }

        // -----------------------------------------------------------------------
        // Line picker sheet
        // -----------------------------------------------------------------------

        if (showLinePicker) {
            LinePickerSheet(
                routes = routes,
                liveCounts = liveCountByRouteId,
                onDismiss = { showLinePicker = false },
                onSelectRoute = { routeId ->
                    viewModel.selectRoute(routeId)
                },
            )
        }
    }
}
