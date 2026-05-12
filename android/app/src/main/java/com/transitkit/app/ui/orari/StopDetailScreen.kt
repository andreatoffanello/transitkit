package com.transitkit.app.ui.orari

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.transitkit.app.ui.components.stopIcon
import com.transitkit.app.ui.mappa.SingleStopMarker
import com.transitkit.app.ui.mappa.applyTransitKitStandardStyleConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopDetailScreen(
    stopId: String,
    stopName: String = stopId,
    onBack: () -> Unit,
    onNavigateToTrip: (tripId: String, fromStopId: String, routeColor: String, headsign: String, routeName: String) -> Unit = { _, _, _, _, _ -> },
    onNavigateToAlert: (alertId: String) -> Unit = {},
    viewModel: StopDetailViewModel = hiltViewModel(),
) {
    val departuresState by viewModel.departuresState.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val stopLocation by viewModel.stopLocation.collectAsStateWithLifecycle()
    val selectedRouteFilter by viewModel.selectedRouteFilter.collectAsStateWithLifecycle()
    val availableRoutes by viewModel.availableRoutes.collectAsStateWithLifecycle()
    val rawDepartures by viewModel.rawDepartures.collectAsStateWithLifecycle()
    val departuresByGroup by viewModel.departuresByGroup.collectAsStateWithLifecycle()
    val stopSequenceByRouteId by viewModel.stopSequenceByRouteId.collectAsStateWithLifecycle()
    val stopAlerts by viewModel.stopAlerts.collectAsStateWithLifecycle()
    val operatorTimezone = viewModel.operatorTimezone
    var showFullSchedule by remember { mutableStateOf(false) }
    val colors = TransitTheme.colors
    val transitColors = LocalTransitColors.current
    val isRefreshing = departuresState is DeparturesState.Loading
    val isDark = isSystemInDarkTheme()

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    // Prefer the resolved station name from the schedule store.
                    // Falls back to the caller-provided name, then to stopId —
                    // avoids "appalcart_asu_…" raw IDs showing in the bar when
                    // the caller forgot to pass a display name (e.g. deep
                    // links that only carry the id).
                    val resolvedName = viewModel.resolvedStopName.collectAsStateWithLifecycle().value
                    Text(
                        text = resolvedName ?: stopName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "btn_back" },
                    ) {
                        Icon(
                            painter = painterResource(LucideIcons.ChevronLeft),
                            contentDescription = stringResource(R.string.cd_indietro),
                            tint = colors.textPrimary,
                        )
                    }
                },
                actions = {
                    val context = LocalContext.current
                    stopLocation?.let { (lat, lon) ->
                        var showMapPicker by remember { mutableStateOf(false) }

                        fun canOpen(scheme: String): Boolean = try {
                            context.packageManager.resolveActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(scheme)),
                                PackageManager.MATCH_DEFAULT_ONLY,
                            ) != null
                        } catch (_: Exception) { false }

                        fun launch(uri: String) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
                        }

                        val hasGoogleMaps = remember(lat, lon) { canOpen("comgooglemaps://") }
                        val hasWaze       = remember(lat, lon) { canOpen("waze://") }

                        IconButton(onClick = { showMapPicker = true }) {
                            Icon(
                                painter = painterResource(LucideIcons.MapPin),
                                contentDescription = stringResource(R.string.cd_apri_in_maps),
                                tint = colors.accent,
                            )
                        }

                        if (showMapPicker) {
                            AlertDialog(
                                onDismissRequest = { showMapPicker = false },
                                title = { Text(stringResource(R.string.apri_in_mappe)) },
                                confirmButton = {},
                                dismissButton = {
                                    TextButton(onClick = { showMapPicker = false }) {
                                        Text(stringResource(R.string.annulla))
                                    }
                                },
                                containerColor = colors.bgSecondary,
                                titleContentColor = colors.textPrimary,
                                icon = null,
                                text = {
                                    Column {
                                        if (hasGoogleMaps) {
                                            TextButton(
                                                onClick = {
                                                    launch("comgooglemaps://?q=$lat,$lon&zoom=17")
                                                    showMapPicker = false
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                Text(
                                                    stringResource(R.string.mappa_app_google),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    color = colors.textPrimary,
                                                )
                                            }
                                        }
                                        if (hasWaze) {
                                            TextButton(
                                                onClick = {
                                                    launch("waze://?ll=$lat,$lon&navigate=false")
                                                    showMapPicker = false
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                Text(
                                                    stringResource(R.string.mappa_app_waze),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    color = colors.textPrimary,
                                                )
                                            }
                                        }
                                        // Fallback — always present
                                        TextButton(
                                            onClick = {
                                                launch("geo:$lat,$lon?q=$lat,$lon(${Uri.encode(stopName)})")
                                                showMapPicker = false
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text(
                                                stringResource(R.string.mappa_app_mappe),
                                                modifier = Modifier.fillMaxWidth(),
                                                color = colors.textPrimary,
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    }
                    val favScale by animateFloatAsState(
                        targetValue = if (isFavorite) 1.25f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                        label = "favScale",
                    )
                    IconButton(
                        onClick = viewModel::toggleFavorite,
                        modifier = Modifier.semantics { contentDescription = "btn_favorite" },
                    ) {
                        if (isFavorite) {
                            Icon(
                                painter = painterResource(LucideIcons.StarFilled),
                                contentDescription = stringResource(R.string.cd_rimuovi_preferiti),
                                tint = colors.accent,
                                modifier = Modifier.scale(favScale),
                            )
                        } else {
                            Icon(
                                painter = painterResource(LucideIcons.Star),
                                contentDescription = stringResource(R.string.cd_salva_fermata),
                                tint = colors.textSecondary,
                                modifier = Modifier.scale(favScale),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    scrolledContainerColor = colors.background,
                ),
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::loadDepartures,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Mini-map header — solo se abbiamo la posizione
                stopLocation?.let { (lat, lon) ->
                    val stopPoint = remember(lat, lon) { Point.fromLngLat(lon, lat) }
                    val viewportState = rememberMapViewportState {
                        setCameraOptions {
                            center(stopPoint)
                            zoom(12.0)
                            pitch(0.0)
                            bearing(0.0)
                        }
                    }
                    LaunchedEffect(stopPoint) {
                        // Two-phase fly-in: wait for tiles, then animate to 3D view
                        kotlinx.coroutines.delay(500)
                        val offsetPoint = Point.fromLngLat(lon, lat + 0.0006)
                        viewportState.flyTo(
                            CameraOptions.Builder()
                                .center(offsetPoint)
                                .zoom(15.0)
                                .pitch(60.0)
                                .bearing(0.0)
                                .build(),
                            animationOptions = com.mapbox.maps.plugin.animation.MapAnimationOptions
                                .mapAnimationOptions { duration(1200) },
                        )
                    }
                    val screenHeightDp = LocalConfiguration.current.screenHeightDp
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((screenHeightDp * 0.38f).dp),
                    ) {
                        MapboxMap(
                            modifier = Modifier.fillMaxSize(),
                            mapViewportState = viewportState,
                            style = { MapStyle(style = Style.STANDARD) },
                            compass = {},
                            scaleBar = {},
                        ) {
                            // Pin singolo via SymbolLayer + bitmap factory —
                            // condivide pipeline e shape con la mappa principale
                            // (MappaScreen). Single source of truth: StopMarkerBitmap.pin.
                            SingleStopMarker(
                                point = stopPoint,
                                color = transitColors.accent,
                                iconRes = stopIcon(listOf(availableRoutes.firstOrNull()?.transitType ?: 3)),
                            )

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
                    }
                }

                // Service alerts affecting this stop
                if (stopAlerts.isNotEmpty()) {
                    AlertsSection(
                        alerts = stopAlerts,
                        onClick = onNavigateToAlert,
                    )
                }

                // Contenuto principale
                when (val state = departuresState) {
                    is DeparturesState.Loading -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = colors.accent,
                        )
                    }
                    is DeparturesState.Success -> DeparturesList(
                        departures = state.departures,
                        availableRoutes = availableRoutes,
                        selectedRoute = selectedRouteFilter,
                        onRouteSelected = viewModel::selectRouteFilter,
                        onOpenFullSchedule = { showFullSchedule = true },
                        operatorTimezone = operatorTimezone,
                        stopSequenceByRouteId = stopSequenceByRouteId,
                        onNavigateToTrip = { dep ->
                            onNavigateToTrip(
                                dep.tripId,
                                stopId,
                                dep.routeColor ?: "",
                                dep.headsign,
                                dep.routeShortName,
                            )
                        },
                    )
                    is DeparturesState.Empty -> {
                        if (selectedRouteFilter != null) {
                            val filter: String = selectedRouteFilter!!
                            val routeName: String = availableRoutes
                                .firstOrNull { it.routeId == filter }?.routeName
                                ?: filter
                            EmptyStateForFilter(
                                routeName = routeName,
                                onClearFilter = { viewModel.selectRouteFilter(filter) },
                            )
                        } else {
                            EmptyState()
                        }
                    }
                    is DeparturesState.Error -> ErrorState(onRetry = { viewModel.loadDepartures() })
                }
            }
        }
    }

    if (showFullSchedule) {
        FullScheduleSheet(
            stopName = stopName,
            departuresByGroup = departuresByGroup,
            onDismiss = { showFullSchedule = false },
        )
    }
}
