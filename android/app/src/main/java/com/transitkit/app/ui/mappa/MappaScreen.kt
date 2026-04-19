package com.transitkit.app.ui.mappa

import android.Manifest
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.ViewAnnotationAnchor
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.viewannotation.annotationAnchor
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import com.transitkit.app.R
import com.transitkit.app.config.LocalTransitColors
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.gtfsrt.VehiclePosition
import com.transitkit.app.data.gtfsrt.VehicleStatus
import com.transitkit.app.data.model.ResolvedDeparture
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.data.model.ScheduleRoute
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Stile Mapbox — movete parity
private const val MAPBOX_STYLE_DARK = "mapbox://styles/mapbox/navigation-night-v1"
private const val MAPBOX_STYLE_LIGHT = "mapbox://styles/mapbox/navigation-day-v1"

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
    viewModel: MappaViewModel = hiltViewModel(),
) {
    val transitColors = LocalTransitColors.current
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
    val haptic = LocalHapticFeedback.current
    val isDark = isSystemInDarkTheme()

    // Camera state (viewport) — movete parity
    val viewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(mapCenter.longitude, mapCenter.latitude))
            zoom(defaultZoom.toDouble())
        }
    }

    // AtomicLong avoids Compose stale-closure: onMapClickListener captures the ref once
    // at composition but always reads the latest timestamp via .get().
    val lastAnnotationTapMs = remember { java.util.concurrent.atomic.AtomicLong(0L) }

    // Camera snapshot polled 300ms (viewportState doesn't expose Compose state).
    // Tracks zoom/bearing/pitch + bounding box for viewport-filtering.
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

    val tier by remember { derivedStateOf { zoomTierFor(currentZoom) } }
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
    val styleUri = if (isDark) MAPBOX_STYLE_DARK else MAPBOX_STYLE_LIGHT

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

    // Fit camera ai punti polilinea quando cambia la rotta selezionata
    LaunchedEffect(routePolylines) {
        if (routePolylines.isEmpty()) return@LaunchedEffect
        val all = routePolylines.flatten()
        if (all.size < 2) return@LaunchedEffect
        val minLat = all.minOf { it.latitude }
        val maxLat = all.maxOf { it.latitude }
        val minLon = all.minOf { it.longitude }
        val maxLon = all.maxOf { it.longitude }
        val center = Point.fromLngLat((minLon + maxLon) / 2, (minLat + maxLat) / 2)
        viewportState.flyTo(
            CameraOptions.Builder()
                .center(center)
                .zoom(12.5)
                .build()
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {

        MapboxMap(
            modifier = Modifier
                .fillMaxSize()
                .semantics { testTag = "mappa_tab" },
            mapViewportState = viewportState,
            style = { MapStyle(style = styleUri) },
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
            // ---- Route polyline (doppio stroke: bianco sotto + colore sopra)
            if (selectedRoute != null && routePolylines.isNotEmpty() && selectedLineColor != null) {
                routePolylines.forEachIndexed { idx, linePoints ->
                    val points = linePoints.map { Point.fromLngLat(it.longitude, it.latitude) }
                    if (points.size >= 2) {
                        key("poly_w_${selectedRoute.id}_$idx") {
                            PolylineAnnotation(points = points) {
                                lineColor = Color.White
                                lineWidth = 7.0
                                lineOpacity = 0.7
                            }
                        }
                        key("poly_c_${selectedRoute.id}_$idx") {
                            PolylineAnnotation(points = points) {
                                lineColor = selectedLineColor
                                lineWidth = 4.0
                                lineOpacity = 0.95
                            }
                        }
                    }
                }
            }

            // ---- Stops ----
            // Colore fermata: GTFS della linea SELEZIONATA se presente, altrimenti
            // il colore accent di transitkit (non ambra movete, non la prima linea
            // della fermata che darebbe un mosaico illeggibile).
            // Render selected stop last for correct z-order (Mapbox ViewAnnotation).
            val (selectedStops, otherStops) = stops.partition { it.id == selectedStop?.id }
            (otherStops + selectedStops).forEach { stop ->
                val stopColor = remember(stop.id, selectedRouteId, selectedRoute?.color) {
                    selectedRoute?.color?.takeIf { it.isNotBlank() }?.let { hex ->
                        runCatching { Color(android.graphics.Color.parseColor("#$hex")) }.getOrNull()
                    }
                } ?: transitColors.accent
                val isSelectedStop = selectedStop?.id == stop.id
                val dominantType = stop.transitTypes.firstOrNull() ?: 3

                key(stop.id) {
                    ViewAnnotation(
                        options = viewAnnotationOptions {
                            geometry(Point.fromLngLat(stop.lon, stop.lat))
                            annotationAnchor {
                                anchor(
                                    if (tier == MapZoomTier.Street) ViewAnnotationAnchor.BOTTOM
                                    else ViewAnnotationAnchor.CENTER
                                )
                            }
                            allowOverlap(true)
                        }
                    ) {
                        Box(
                            modifier = Modifier.pointerInteropFilter { event ->
                                if (event.action == android.view.MotionEvent.ACTION_UP) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    lastAnnotationTapMs.set(System.currentTimeMillis())
                                    isFollowingVehicle = false
                                    viewModel.clearSelectedVehicle()
                                    viewModel.selectStop(stop)
                                }
                                true
                            }
                        ) {
                            if (tier == MapZoomTier.Street) {
                                StopPinView(
                                    color = stopColor,
                                    transitType = dominantType,
                                    isSelected = isSelectedStop,
                                )
                            } else {
                                StopDotView(tier = tier, color = stopColor)
                            }
                        }
                    }
                }
            }

            // ---- Vehicles ----
            // Route-selected: tutti i veicoli di quella linea.
            // Unselected: solo a tier Street, viewport-filtered è fatto a monte dal VM.
            // Render selected vehicle last for correct z-order (Mapbox ViewAnnotation).
            val visibleVehicles = when {
                selectedRoute != null -> vehiclesWithColor.filter { it.first.routeId == selectedRoute.id }
                tier == MapZoomTier.Street -> vehiclesWithColor
                else -> vehiclesWithColor  // a city/neighborhood mostra comunque dot (movete 1:1)
            }
            val (selectedVehicles, otherVehicles) = visibleVehicles.partition { it.first.vehicleId == selectedVehicle?.first?.vehicleId }
            (otherVehicles + selectedVehicles).forEach { (vehicle, routeColor) ->
                val route = routes.firstOrNull { it.id == vehicle.routeId }
                val routeName = route?.name?.take(4) ?: ""

                key(vehicle.vehicleId) {
                    ViewAnnotation(
                        options = viewAnnotationOptions {
                            geometry(Point.fromLngLat(vehicle.lon, vehicle.lat))
                            annotationAnchor { anchor(ViewAnnotationAnchor.CENTER) }
                            allowOverlap(true)
                        }
                    ) {
                        Box(
                            modifier = Modifier.pointerInteropFilter { event ->
                                if (event.action == android.view.MotionEvent.ACTION_UP) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    lastAnnotationTapMs.set(System.currentTimeMillis())
                                    viewModel.clearSelectedStop()
                                    viewModel.selectVehicle(vehicle, routeColor)
                                    isFollowingVehicle = true
                                    scope.launch {
                                        viewportState.flyTo(
                                            CameraOptions.Builder()
                                                .center(Point.fromLngLat(vehicle.lon, vehicle.lat))
                                                .zoom(maxOf(currentZoom, 16.5))
                                                .build()
                                        )
                                    }
                                }
                                true
                            }
                        ) {
                            VehicleAnnotationView(
                                lineColor = routeColor,
                                routeName = routeName,
                                tier = tier,
                                haloAlpha = haloAlpha,
                            )
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(end = 16.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val cdVistaPredefinita = stringResource(R.string.cd_vista_predefinita)
                val cdCentraMappa = stringResource(R.string.cd_centra_mappa)
                val cdVista3D = stringResource(R.string.cd_vista_3d)
                val cdVista2D = stringResource(R.string.cd_vista_2d)

                SmallFloatingActionButton(
                    onClick = {
                        scope.launch {
                            viewportState.flyTo(
                                CameraOptions.Builder()
                                    .center(Point.fromLngLat(mapCenter.longitude, mapCenter.latitude))
                                    .zoom(defaultZoom.toDouble())
                                    .pitch(0.0)
                                    .bearing(0.0)
                                    .build()
                            )
                        }
                    },
                    containerColor = transitColors.bgSecondary,
                    contentColor = transitColors.textPrimary,
                    modifier = Modifier
                        .shadow(elevation = 6.dp, shape = CircleShape)
                        .semantics {
                            testTag = "btn_map_reset_view"
                            contentDescription = cdVistaPredefinita
                        },
                ) {
                    Icon(
                        painter = painterResource(LucideIcons.Map),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }

                SmallFloatingActionButton(
                    onClick = {
                        scope.launch {
                            viewportState.flyTo(
                                CameraOptions.Builder()
                                    .center(Point.fromLngLat(mapCenter.longitude, mapCenter.latitude))
                                    .zoom(defaultZoom.toDouble())
                                    .build()
                            )
                        }
                    },
                    containerColor = transitColors.bgSecondary,
                    contentColor = transitColors.textPrimary,
                    modifier = Modifier
                        .shadow(elevation = 6.dp, shape = CircleShape)
                        .semantics {
                            testTag = "btn_map_recenter"
                            contentDescription = cdCentraMappa
                        },
                ) {
                    Icon(
                        painter = painterResource(LucideIcons.Crosshair),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }

                SmallFloatingActionButton(
                    onClick = {
                        scope.launch {
                            viewportState.flyTo(
                                CameraOptions.Builder()
                                    .pitch(if (is3D) 0.0 else 45.0)
                                    .build()
                            )
                        }
                    },
                    containerColor = if (is3D) transitColors.accent else transitColors.bgSecondary,
                    contentColor = if (is3D) Color.White else transitColors.textPrimary,
                    modifier = Modifier
                        .shadow(elevation = 6.dp, shape = CircleShape)
                        .semantics {
                            testTag = "btn_map_toggle_3d"
                            contentDescription = if (is3D) cdVista2D else cdVista3D
                        },
                ) {
                    Icon(
                        painter = painterResource(LucideIcons.Box),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

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

// ---------------------------------------------------------------------------
// Search pill — "Cerca linea"
// ---------------------------------------------------------------------------

@Composable
private fun SearchLinePill(onClick: () -> Unit) {
    val colors = LocalTransitColors.current
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.bgSecondary,
        shadowElevation = 4.dp,
        onClick = onClick,
        modifier = Modifier.semantics { testTag = "map_line_picker_pill" },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(LucideIcons.Search),
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.mappa_cerca_linea),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Route dismiss chip
// ---------------------------------------------------------------------------

@Composable
private fun RouteDismissChip(
    route: ScheduleRoute,
    liveCount: Int,
    onDismiss: () -> Unit,
) {
    val colors = LocalTransitColors.current
    val badgeColor = remember(route.color) {
        if (route.color.isNotBlank())
            runCatching { Color(android.graphics.Color.parseColor("#${route.color}")) }.getOrNull()
        else null
    } ?: colors.accent
    val fg = remember(route.textColor, badgeColor) {
        if (route.textColor.isNotBlank())
            runCatching { Color(android.graphics.Color.parseColor("#${route.textColor}")) }.getOrNull()
        else null
    } ?: contrastingTextColor(badgeColor)

    val cdDismiss = stringResource(R.string.mappa_rimuovi_overlay)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.bgSecondary,
        shadowElevation = 4.dp,
        modifier = Modifier.semantics { testTag = "map_route_dismiss_chip" },
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeColor)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = route.name.take(5),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = fg,
                )
            }
            if (liveCount > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(colors.realtimeGreen),
                    )
                    Text(
                        text = "$liveCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(28.dp)
                    .semantics {
                        testTag = "btn_route_dismiss"
                        contentDescription = cdDismiss
                    },
            ) {
                Icon(
                    painter = painterResource(LucideIcons.X),
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Preview card container
// ---------------------------------------------------------------------------

@Composable
private fun PreviewCardContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = LocalTransitColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(colors.bgSecondary),
    ) {
        content()
    }
}

// ---------------------------------------------------------------------------
// Utilities
// ---------------------------------------------------------------------------

internal fun contrastingTextColor(bg: Color): Color {
    fun channel(c: Float): Double {
        val v = c.toDouble()
        return if (v <= 0.03928) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
    }
    val l = 0.2126 * channel(bg.red) + 0.7152 * channel(bg.green) + 0.0722 * channel(bg.blue)
    return if (l < 0.5) Color.White else Color.Black
}

@DrawableRes
internal fun iconForTransitType(type: Int) = when (type) {
    0 -> LucideIcons.Tram
    1 -> LucideIcons.Train
    2 -> LucideIcons.Train
    4 -> LucideIcons.Ship
    else -> LucideIcons.BusFront
}

@DrawableRes
internal fun stopPinIcon(transitTypes: Set<Int>): Int {
    val busTypes = setOf(3, 11)
    val hasBus = transitTypes.any { it in busTypes } || transitTypes.isEmpty()
    if (hasBus) return LucideIcons.Signpost
    val priority = listOf(4, 0, 1, 2, 6, 7, 5, 12)
    for (type in priority) if (type in transitTypes) return iconForTransitType(type)
    return LucideIcons.Signpost
}

// ---------------------------------------------------------------------------
// Stop preview content
// ---------------------------------------------------------------------------

@Composable
private fun StopPreviewContent(
    stop: ResolvedStop,
    departures: List<ResolvedDeparture>,
    isLoading: Boolean,
    onClose: () -> Unit,
    onNavigateToStop: (stopId: String) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stop.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TransitTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.semantics { testTag = "btn_stop_sheet_close" },
            ) {
                Icon(
                    painter = painterResource(LucideIcons.X),
                    contentDescription = stringResource(R.string.action_chiudi),
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        val distinctTransitTypes = stop.transitTypes.distinct().ifEmpty {
            departures.map { it.transitType }.distinct()
        }
        if (distinctTransitTypes.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
            ) {
                distinctTransitTypes.forEach { type ->
                    Icon(
                        painter = painterResource(iconForTransitType(type)),
                        contentDescription = null,
                        tint = TransitTheme.colors.textTertiary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        val staticRoutes = stop.routeNames.mapIndexed { i, name ->
            Triple(name, stop.routeIds.getOrElse(i) { name }, stop.routeColors.getOrElse(i) { "" })
        }
        if (staticRoutes.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                items(staticRoutes) { (routeName, _, routeColor) ->
                    val chipColor = routeColor
                        .takeIf { it.isNotBlank() }
                        ?.let { runCatching { Color(android.graphics.Color.parseColor("#$it")) }.getOrNull() }
                        ?: LocalTransitColors.current.accent
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .background(chipColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            routeName.take(5),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = TransitTheme.colors.accent,
                    )
                }
            }
            departures.isEmpty() -> {
                Text(
                    text = stringResource(R.string.mappa_nessuna_partenza),
                    style = MaterialTheme.typography.bodySmall,
                    color = TransitTheme.colors.textSecondary,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
            else -> {
                departures.take(5).forEach { dep ->
                    SheetDepartureRow(dep)
                }
            }
        }

        val accentColor = LocalTransitColors.current.accent
        Button(
            onClick = { onNavigateToStop(stop.id) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = Color.White,
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(painterResource(LucideIcons.Clock), null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.mappa_vedi_orari),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        val context = LocalContext.current
        OutlinedButton(
            onClick = {
                val lat = stop.lat
                val lon = stop.lon
                val uri = android.net.Uri.parse(
                    "geo:$lat,$lon?q=$lat,$lon(${android.net.Uri.encode(stop.name)})"
                )
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
        ) {
            Icon(painterResource(LucideIcons.MapPin), null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.mappa_apri_maps),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SheetDepartureRow(departure: ResolvedDeparture) {
    val colors = LocalTransitColors.current
    val routeColor = if (departure.routeColor.isNotBlank())
        runCatching { Color(android.graphics.Color.parseColor("#${departure.routeColor}")) }.getOrElse { colors.accent }
    else colors.accent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 22.dp)
                .background(routeColor, RoundedCornerShape(5.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = departure.routeName.take(5),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
            )
        }
        Text(
            text = departure.headsign,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = departure.departureTime.take(5),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary,
        )
    }
}

@Composable
private fun VehiclePreviewContent(
    vehicle: VehiclePosition,
    vehicleColor: Color,
    route: ScheduleRoute?,
    stopName: String? = null,
    predictedArrivalMs: Long? = null,
    operatorTimezoneId: String = "UTC",
    isFollowing: Boolean = false,
    onToggleFollow: () -> Unit = {},
    onOpenTrip: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val colors = TransitTheme.colors

    // Tick every 15s so freshness + countdown stay live (matches feed cadence).
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(15_000)
            now = System.currentTimeMillis()
        }
    }

    // --- Derived state -----------------------------------------------------

    val dirs = route?.directions ?: emptyList()
    val routeNamesLower = listOfNotNull(route?.name, route?.longName)
        .map { it.lowercase() }
        .toSet()
    val directionLabel = dirs
        .mapNotNull { it.headsign?.trim()?.takeIf { h -> h.isNotBlank() && !routeNamesLower.contains(h.lowercase()) } }
        .take(2)
        .joinToString(" → ")

    // ETA state — matches iOS: minutes countdown when within 60 min, absolute
    // clock when farther, "arriving" when imminent. Clock always shown in
    // operator's local time.
    val etaMinutes: Int? = predictedArrivalMs?.let {
        val diff = it - now
        if (diff < -60_000L) null
        else ((diff + 30_000L) / 60_000L).toInt().coerceAtLeast(0)
    }
    val etaClock: String? = predictedArrivalMs?.let {
        val fmt = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getTimeZone(operatorTimezoneId)
        }
        fmt.format(java.util.Date(it))
    }
    val ageSec = ((now / 1000L) - vehicle.timestamp).coerceAtLeast(0L)
    val freshnessText: String? = when {
        vehicle.timestamp <= 0 -> null
        ageSec < 60 -> stringResource(R.string.vehicle_updated_sec, ageSec.toInt())
        else -> stringResource(R.string.vehicle_updated_min, (ageSec / 60).toInt())
    }
    val stopPrefix = when (vehicle.currentStatus) {
        com.transitkit.app.data.gtfsrt.VehicleStatus.IN_TRANSIT_TO -> stringResource(R.string.vehicle_next_stop)
        com.transitkit.app.data.gtfsrt.VehicleStatus.STOPPED_AT -> stringResource(R.string.vehicle_stopped_at)
        com.transitkit.app.data.gtfsrt.VehicleStatus.INCOMING_AT -> stringResource(R.string.vehicle_incoming_at)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // --- Row 1 — Badge + name + #id + wheelchair + close ------------
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(vehicleColor, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = route?.name?.take(4) ?: stringResource(R.string.vehicle_label_default),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
            Text(
                text = route?.longName?.takeIf { it.isNotBlank() }
                    ?: route?.name
                    ?: stringResource(R.string.mappa_veicolo_live),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (vehicle.label.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .background(colors.textPrimary.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "#${vehicle.label}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary,
                    )
                }
            }
            if (vehicle.wheelchair == com.transitkit.app.data.gtfsrt.WheelchairStatus.ACCESSIBLE) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(colors.accent.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(LucideIcons.Accessibility),
                        contentDescription = stringResource(R.string.vehicle_accessible),
                        tint = colors.accent,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(32.dp)
                    .semantics { testTag = "btn_vehicle_sheet_close" },
            ) {
                Icon(
                    painter = painterResource(LucideIcons.X),
                    contentDescription = stringResource(R.string.action_chiudi),
                    tint = colors.textTertiary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // --- Row 2 — Direction + occupancy (no delay badge; delay is in ETA) ---
        if (directionLabel.isNotBlank() || vehicle.occupancyStatus != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (directionLabel.isNotBlank()) {
                    Icon(
                        painter = painterResource(LucideIcons.ArrowRight),
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = directionLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                vehicle.occupancyStatus?.let { status ->
                    occupancyLabel(status)?.let { label ->
                        Box(
                            modifier = Modifier
                                .background(colors.textPrimary.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }

        // --- Row 3 — Stop row: label + name (left) + mins/clock stack (right) ---
        if (stopName != null) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        painter = painterResource(LucideIcons.MapPin),
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = stopPrefix,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textTertiary,
                    )
                }
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stopName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    // ETA stack: minutes (top) + HH:mm (bottom) — parity with iOS
                    if (etaMinutes != null && etaClock != null) {
                        Column(horizontalAlignment = Alignment.End) {
                            when {
                                // Imminent arrival — show arrow instead of "0'"
                                etaMinutes <= 0 -> {
                                    Text(
                                        text = "→",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colors.textPrimary,
                                    )
                                    Text(
                                        text = etaClock,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    )
                                }
                                etaMinutes <= 60 -> {
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = "$etaMinutes",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = colors.textPrimary,
                                        )
                                        Text(
                                            text = "'",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = colors.textSecondary,
                                        )
                                    }
                                    Text(
                                        text = etaClock,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    )
                                }
                                else -> {
                                    Text(
                                        text = etaClock,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Row 4 — Live badge inline + freshness ----------------------
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(colors.realtimeGreen, CircleShape),
            )
            Text(
                text = stringResource(R.string.mappa_live),
                style = MaterialTheme.typography.labelSmall,
                color = colors.realtimeGreen,
                fontWeight = FontWeight.SemiBold,
            )
            if (freshnessText != null) {
                Text(
                    text = "·",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textTertiary,
                )
                Text(
                    text = freshnessText,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textTertiary,
                )
            }
        }

        // --- Row 5 — Full-width action buttons: follow + open trip -------
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val followBg = if (isFollowing) colors.accent else colors.textPrimary.copy(alpha = 0.08f)
            val followFg = if (isFollowing) Color.White else colors.textPrimary
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(followBg, RoundedCornerShape(12.dp))
                    .clickable(onClick = onToggleFollow)
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(LucideIcons.Crosshair),
                    contentDescription = null,
                    tint = followFg,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(
                        if (isFollowing) R.string.vehicle_unfollow else R.string.vehicle_follow
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = followFg,
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(colors.textPrimary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenTrip)
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(LucideIcons.Route),
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.vehicle_open_trip),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
            }
        }
    }
}

/** Maps GTFS-RT OccupancyStatus to a short localized label, or null if no useful info. */
@Composable
private fun occupancyLabel(status: com.transitkit.app.data.gtfsrt.OccupancyStatus): String? {
    return when (status) {
        com.transitkit.app.data.gtfsrt.OccupancyStatus.EMPTY,
        com.transitkit.app.data.gtfsrt.OccupancyStatus.MANY_SEATS_AVAILABLE ->
            stringResource(R.string.occupancy_seats_available)
        com.transitkit.app.data.gtfsrt.OccupancyStatus.FEW_SEATS_AVAILABLE ->
            stringResource(R.string.occupancy_few_seats)
        com.transitkit.app.data.gtfsrt.OccupancyStatus.STANDING_ROOM_ONLY ->
            stringResource(R.string.occupancy_standing_only)
        com.transitkit.app.data.gtfsrt.OccupancyStatus.CRUSHED_STANDING_ROOM_ONLY,
        com.transitkit.app.data.gtfsrt.OccupancyStatus.FULL ->
            stringResource(R.string.occupancy_full)
        com.transitkit.app.data.gtfsrt.OccupancyStatus.NOT_ACCEPTING_PASSENGERS,
        com.transitkit.app.data.gtfsrt.OccupancyStatus.NOT_BOARDABLE ->
            stringResource(R.string.occupancy_not_boarding)
        com.transitkit.app.data.gtfsrt.OccupancyStatus.NO_DATA_AVAILABLE -> null
    }
}
