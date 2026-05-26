package com.transitkit.app.ui.planner

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle
import com.transitkit.app.R
import com.transitkit.app.config.LocalTransitColors
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.api.MapboxGeocodingService
import com.transitkit.app.data.model.PlannerLocation
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.ui.mappa.StopSymbolLayer
import com.transitkit.app.ui.mappa.applyTransitKitStandardStyleConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Treat the pin as "on a stop" if a GTFS stop is within this distance.
private const val STOP_SNAP_DISTANCE_METERS = 25.0

/**
 * Full-screen map for picking a free position (Movete parity). Fixed center pin —
 * user pans the map under it. On every camera idle we reverse-geocode the
 * coordinates via Mapbox Geocoding API v6 and show the resolved place name
 * (street / POI / address). If the pin lands within ~25m of a known GTFS stop
 * we snap to the stop and show its name instead.
 *
 * Routing internally uses the nearest stop ID for that position; the displayed
 * name carries the geocoded place forward to the planner From/To rows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerMapScreen(
    role: String,
    source: String,
    plannerViewModel: PlannerViewModel,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = TransitTheme.colors
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    val allStops by plannerViewModel.allStops.collectAsStateWithLifecycle()
    val currentLocation by plannerViewModel.currentLocation.collectAsStateWithLifecycle()

    val initialCenter = remember {
        val (lat, lon) = currentLocation ?: plannerViewModel.mapFallbackCenter
        Point.fromLngLat(lon, lat)
    }
    val initialZoom = remember {
        if (currentLocation != null) 14.5 else plannerViewModel.mapDefaultZoom
    }

    val viewportState = rememberMapViewportState {
        setCameraOptions {
            center(initialCenter)
            zoom(initialZoom)
            pitch(0.0)
            bearing(0.0)
        }
    }

    var centerLat by remember { mutableDoubleStateOf(initialCenter.latitude()) }
    var centerLon by remember { mutableDoubleStateOf(initialCenter.longitude()) }

    // What the user sees in the bottom card.
    val resolvingLabel = stringResource(R.string.planner_picker_map_resolving)
    var resolvedName by remember { mutableStateOf(resolvingLabel) }
    // True if the pin sits on a known stop (snap). Drives icon + routing path.
    var isStopPin by remember { mutableStateOf(false) }
    // Stop used for the actual routing call (always the nearest one, regardless of name shown).
    var routingStop by remember { mutableStateOf<ResolvedStop?>(null) }

    // Fly to GPS once on first arrival
    var didFlyToGps by remember { mutableStateOf(currentLocation != null) }
    LaunchedEffect(currentLocation) {
        val loc = currentLocation ?: return@LaunchedEffect
        if (!didFlyToGps) {
            didFlyToGps = true
            viewportState.flyTo(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(loc.second, loc.first))
                    .zoom(14.5)
                    .pitch(0.0)
                    .build()
            )
        }
    }

    // Debounced resolve: nearest stop for routing + reverse geocode for label.
    LaunchedEffect(centerLat, centerLon) {
        delay(400)
        if (allStops.isEmpty()) return@LaunchedEffect
        val nearest = plannerViewModel.nearestStopWithDistance(centerLat, centerLon)
        routingStop = nearest?.first
        if (nearest != null && nearest.second <= STOP_SNAP_DISTANCE_METERS) {
            resolvedName = nearest.first.name
            isStopPin = true
        } else {
            isStopPin = false
            resolvedName = resolvingLabel
            val name = MapboxGeocodingService.reverseGeocode(centerLat, centerLon)
            resolvedName = name ?: "%.5f, %.5f".format(centerLat, centerLon)
        }
    }

    fun confirm() {
        // Pin coordinates are the source of truth. When snapped to a stop we mark
        // kind=Stop so the planner knows it can route directly; otherwise kind=Place
        // and the planner's internal routing layer snaps to the nearest stop.
        val pick = if (isStopPin && routingStop != null) {
            PlannerLocation.fromStop(routingStop!!)
        } else {
            PlannerLocation(
                kind = PlannerLocation.Kind.Place,
                name = resolvedName,
                lat = centerLat,
                lon = centerLon,
            )
        }
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        when (source) {
            "home" -> if (role == "origin") plannerViewModel.setHomeOrigin(pick)
                      else plannerViewModel.setHomeDestination(pick)
            else   -> if (role == "origin") plannerViewModel.setOrigin(pick)
                      else plannerViewModel.setDestination(pick)
        }
        onConfirm()
    }

    fun centerOnUser() {
        val loc = currentLocation ?: return
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        scope.launch {
            viewportState.flyTo(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(loc.second, loc.first))
                    .zoom(15.0)
                    .pitch(0.0)
                    .build()
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (role == "origin")
                            stringResource(R.string.planner_picker_origin_title)
                        else
                            stringResource(R.string.planner_picker_destination_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(LucideIcons.ArrowLeft),
                            contentDescription = "Back",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.textPrimary,
                    navigationIconContentColor = colors.textPrimary,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = viewportState,
                style = { MapStyle(style = Style.STANDARD) },
                compass = {},
                scaleBar = {},
            ) {
                com.transitkit.app.ui.mappa.UserLocationPuck()

                MapEffect(isDark) { mapView ->
                    val styleCancel = applyTransitKitStandardStyleConfig(
                        mapView,
                        isDark = isDark,
                        show3D = false,
                    )
                    kotlinx.coroutines.awaitCancellation().also { styleCancel.cancel() }
                }
                MapEffect(Unit) { mapView ->
                    mapView.mapboxMap.subscribeCameraChanged {
                        val cs = mapView.mapboxMap.cameraState
                        centerLat = cs.center.latitude()
                        centerLon = cs.center.longitude()
                    }
                }
                StopSymbolLayer(
                    stops = allStops,
                    selectedStop = if (isStopPin) routingStop else null,
                    selectedRoute = null,
                    accentColor = LocalTransitColors.current.accent,
                )
            }

            // Fixed center pin — iOS parity (item #4):
            //  - Halo accent 20% reso via `drawBehind` sul dot, fuori dal
            //    layout flow → NON crea gap tra fondo dot e top stem.
            //  - Camera in movimento: stem si allunga 10→20dp e il dot si
            //    solleva di 8dp (spring damped). Lo stretch dello stem copre
            //    l'8dp di lift mantenendo dot e stem visivamente connessi.
            PickerCenterPin(
                accent = colors.accent,
                centerLat = centerLat,
                centerLon = centerLon,
            )

            // Center-on-me FAB — vertically centered on right edge
            if (currentLocation != null) {
                Surface(
                    onClick = ::centerOnUser,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                        .size(44.dp),
                    shape = CircleShape,
                    color = colors.background,
                    shadowElevation = 6.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painterResource(LucideIcons.Crosshair),
                            contentDescription = stringResource(R.string.planner_picker_my_location),
                            tint = colors.accent,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            // Bottom card
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            ) {
                PickerBottomCard(
                    resolvedName = resolvedName,
                    isStopPin = isStopPin,
                    stopsLoading = allStops.isEmpty(),
                    canConfirm = routingStop != null,
                    onConfirm = ::confirm,
                )
                Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars))
            }
        }
    }
}

@Composable
private fun PickerBottomCard(
    resolvedName: String,
    isStopPin: Boolean,
    stopsLoading: Boolean,
    canConfirm: Boolean,
    onConfirm: () -> Unit,
) {
    val colors = TransitTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = colors.background,
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Leading icon — bus icon when snapped to a stop, pin icon for a place
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(colors.accent.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(
                        if (isStopPin) LucideIcons.BusFront else LucideIcons.MapPin
                    ),
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(18.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        if (isStopPin) R.string.planner_picker_selected_stop
                        else R.string.planner_picker_selected_position
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary,
                )
                if (stopsLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = colors.accent,
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = stringResource(R.string.planner_picker_loading_stops),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                        )
                    }
                } else {
                    Text(
                        text = resolvedName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Button(
                onClick = onConfirm,
                enabled = canConfirm,
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    disabledContainerColor = colors.accent.copy(alpha = 0.35f),
                    contentColor = Color.White,
                    disabledContentColor = Color.White.copy(alpha = 0.6f),
                ),
            ) {
                Text(
                    text = stringResource(R.string.planner_picker_confirm),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/**
 * Pin centrale del picker map: dot 22dp + stem + halo (fuori layout).
 * Animato sul movimento camera per "respiro" responsive durante drag.
 */
@Composable
private fun PickerCenterPin(
    accent: Color,
    centerLat: Double,
    centerLon: Double,
) {
    // True per 300ms dopo l'ultimo cambiamento centerLat/Lon.
    var isCameraMoving by remember { mutableStateOf(false) }
    LaunchedEffect(centerLat, centerLon) {
        isCameraMoving = true
        delay(300)
        isCameraMoving = false
    }
    val stemHeight by animateDpAsState(
        targetValue = if (isCameraMoving) 20.dp else 10.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "pickerStemHeight",
    )
    val dotLift by animateDpAsState(
        targetValue = if (isCameraMoving) 8.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "pickerDotLift",
    )
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Dot (22dp accent) + halo come drawBehind — l'halo NON entra
            // nel layout, quindi nessun gap fra fondo dot e top stem.
            Box(
                modifier = Modifier
                    .offset(y = -dotLift)
                    .size(22.dp)
                    .drawBehind {
                        drawCircle(
                            color = accent.copy(alpha = 0.20f),
                            radius = 24.dp.toPx(),
                        )
                    }
                    .background(accent, CircleShape),
            )
            Box(
                modifier = Modifier
                    .size(width = 2.dp, height = stemHeight)
                    .background(accent.copy(alpha = 0.35f)),
            )
            Spacer(Modifier.height(6.dp))
        }
    }
}
