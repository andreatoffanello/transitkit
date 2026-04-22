package com.transitkit.app.ui.orari

import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.transitkit.app.config.LucideIcons
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyRow
import android.content.pm.PackageManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.transitkit.app.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.hilt.navigation.compose.hiltViewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.ViewAnnotationAnchor
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.viewannotation.annotationAnchor
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import com.transitkit.app.config.LocalTransitColors
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.config.toColor
import com.transitkit.app.data.model.Departure
import com.transitkit.app.data.model.ResolvedDeparture
import com.transitkit.app.data.model.ServiceAlert
import com.transitkit.app.ui.alerts.localizedHeader
import com.transitkit.app.ui.alerts.severityColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.font.FontFamily
private const val MAPBOX_STYLE_DARK = "mapbox://styles/mapbox/navigation-night-v1"
private const val MAPBOX_STYLE_LIGHT = "mapbox://styles/mapbox/navigation-day-v1"

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
                                imageVector = Icons.Filled.Star,
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
                    val styleUri = if (isDark) MAPBOX_STYLE_DARK else MAPBOX_STYLE_LIGHT
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((screenHeightDp * 0.38f).dp),
                    ) {
                        MapboxMap(
                            modifier = Modifier.fillMaxSize(),
                            mapViewportState = viewportState,
                            style = { MapStyle(style = styleUri) },
                            compass = {},
                            scaleBar = {},
                        ) {
                            ViewAnnotation(
                                options = viewAnnotationOptions {
                                    geometry(stopPoint)
                                    annotationAnchor {
                                        anchor(ViewAnnotationAnchor.BOTTOM)
                                    }
                                    allowOverlap(true)
                                }
                            ) {
                                StopMarkerDetail(
                                    accentColor = transitColors.accent,
                                    transitType = availableRoutes.firstOrNull()?.transitType ?: 3,
                                )
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

// ---------------------------------------------------------------------------
// Stop marker for detail map header
// ---------------------------------------------------------------------------

@Composable
private fun StopMarkerDetail(accentColor: Color, transitType: Int = 3) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(accentColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            // Design system: a stop on a map is a signpost, not a vehicle.
            painter = painterResource(com.transitkit.app.ui.components.stopIcon(listOf(transitType))),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Departures list
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeparturesList(
    departures: List<Departure>,
    availableRoutes: List<ResolvedDeparture> = emptyList(),
    selectedRoute: String? = null,
    onRouteSelected: (String?) -> Unit = {},
    onOpenFullSchedule: () -> Unit = {},
    operatorTimezone: String = "UTC",
    stopSequenceByRouteId: Map<String, String> = emptyMap(),
    onNavigateToTrip: (Departure) -> Unit = {},
) {
    val colors = TransitTheme.colors
    val haptic = LocalHapticFeedback.current
    var showMore by remember { mutableStateOf(false) }
    val firstDepartures = departures.take(5)
    val extraDepartures = if (departures.size > 5) departures.drop(5) else emptyList()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp),
    ) {
        // Transit type label + "Prossime partenze" header — mirrors iOS StopDetailView
        if (availableRoutes.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    // Transit type chips (Bus, Tram, etc.)
                    val transitTypes = availableRoutes.map { it.transitType }.distinct().sorted()
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        transitTypes.forEach { type ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    painter = painterResource(transitTypeIcon(type)),
                                    contentDescription = null,
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = transitTypeName(type),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    LineBadgeRow(
                        routes = availableRoutes,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.prossime_partenze),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                    )
                }
            }
        }
        if (availableRoutes.size > 1) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    item {
                        val anySelected = selectedRoute != null
                        val tuttiAlpha by animateFloatAsState(
                            targetValue = if (anySelected) 0.35f else 1f,
                            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                            label = "tuttiAlpha",
                        )
                        FilterChip(
                            selected = selectedRoute == null,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onRouteSelected(null)
                            },
                            modifier = Modifier.alpha(tuttiAlpha),
                            label = {
                                Text(
                                    stringResource(R.string.filter_all),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.accent,
                                selectedLabelColor = Color.White,
                                containerColor = colors.accent.copy(alpha = 0.18f),
                                labelColor = colors.accent,
                            ),
                            border = null,
                            shape = RoundedCornerShape(20.dp),
                        )
                    }
                    items(availableRoutes, key = { it.routeId }) { route ->
                        val chipColor = route.routeColor.takeIf { it.isNotBlank() }
                            ?.let { runCatching { it.toColor() }.getOrNull() }
                            ?: TransitTheme.colors.accent
                        val routeTextColor = route.routeTextColor.takeIf { it.isNotBlank() }
                            ?.let { runCatching { it.toColor() }.getOrNull() }
                            ?: contrastOn(chipColor)
                        val isThisSelected = selectedRoute == route.routeId
                        val anySelected = selectedRoute != null
                        val chipAlpha by animateFloatAsState(
                            targetValue = if (anySelected && !isThisSelected) 0.35f else 1f,
                            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                            label = "chipAlpha",
                        )
                        FilterChip(
                            selected = isThisSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onRouteSelected(route.routeId)
                            },
                            modifier = Modifier.alpha(chipAlpha),
                            label = {
                                Text(
                                    route.routeName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = chipColor,
                                selectedLabelColor = routeTextColor,
                                containerColor = colors.accent.copy(alpha = 0.18f),
                                labelColor = colors.accent,
                            ),
                            border = null,
                            shape = RoundedCornerShape(20.dp),
                        )
                    }
                }
            }
        }
        item {
            Text(
                text = stringResource(R.string.label_oggi),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textTertiary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
            )
        }
        item(key = "departure_card") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.bgSecondary)
                    .border(1.dp, colors.glassBorder, RoundedCornerShape(14.dp)),
            ) {
                firstDepartures.forEachIndexed { index, departure ->
                    DepartureRow(
                        departure = departure,
                        isNext = index == 0,
                        operatorTimezone = operatorTimezone,
                        stopSequence = stopSequenceByRouteId[departure.routeId],
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateToTrip(departure)
                        },
                    )
                    if (index < firstDepartures.lastIndex || extraDepartures.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            color = colors.separator,
                            thickness = 0.5.dp,
                        )
                    }
                }
                if (extraDepartures.isNotEmpty()) {
                    AnimatedVisibility(
                        visible = showMore,
                        enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) + fadeIn(tween(200)),
                        exit = shrinkVertically(animationSpec = tween(150)) + fadeOut(tween(150)),
                    ) {
                        Column {
                            extraDepartures.forEachIndexed { index, departure ->
                                DepartureRow(
                                    departure = departure,
                                    isNext = false,
                                    operatorTimezone = operatorTimezone,
                                    stopSequence = stopSequenceByRouteId[departure.routeId],
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onNavigateToTrip(departure)
                                    },
                                )
                                if (index < extraDepartures.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 16.dp),
                                        color = colors.separator,
                                        thickness = 0.5.dp,
                                    )
                                }
                            }
                        }
                    }
                    if (!showMore) {
                        TextButton(
                            onClick = { showMore = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                stringResource(R.string.mostra_altri_partenze, extraDepartures.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.accent,
                            )
                        }
                    }
                }
            }
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                OutlinedButton(
                    onClick = onOpenFullSchedule,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.6f)),
                ) {
                    Icon(
                        painterResource(LucideIcons.Clock),
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.orario_completo),
                        color = colors.accent,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Departure row
// ---------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DepartureRow(departure: Departure, isNext: Boolean = false, operatorTimezone: String = "UTC", stopSequence: String? = null, onClick: () -> Unit = {}) {
    val colors = TransitTheme.colors
    val displayTime = departure.realtimeDepartureTime ?: departure.departureTime
    val hasDelay = (departure.delay ?: 0) > 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(Modifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick,
            )
            .semantics {
                contentDescription = buildString {
                    append("Linea ${departure.routeShortName}")
                    append(", direzione ${departure.headsign}")
                    minutesUntil(departure.realtimeDepartureTime ?: departure.departureTime, operatorTimezone)?.let { mins ->
                        if (mins == 0) append(", in partenza adesso")
                        else append(", in $mins minut${if (mins == 1) "o" else "i"}")
                    }
                }
            },
    ) {
        // "Prossima" badge removed — position-in-list (first row) already
        // communicates that the departure is the next one.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = if (isNext) 14.dp else 10.dp,
                ),
        ) {
            // Line badge — design-system component (iOS DepartureRow parity).
            com.transitkit.app.ui.components.LineBadge(
                name = departure.routeShortName,
                colorHex = departure.routeColor,
                textColorHex = departure.routeTextColor,
                size = com.transitkit.app.ui.components.LineBadgeSize.Large,
            )

            Spacer(Modifier.width(12.dp))

            // Stop sequence (scrolling marquee) or headsign fallback — mirrors iOS DepartureRow MarqueeText
            if (stopSequence != null) {
                Text(
                    text = stopSequence,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .basicMarquee(iterations = Int.MAX_VALUE),
                )
            } else {
                Text(
                    text = departure.headsign,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.width(8.dp))

            // Realtime live indicator (pulsing ring) stays inline — it's a
            // freshness badge, not part of the time glyph.
            if (departure.isRealtime) {
                val realtimePulse = rememberInfiniteTransition(label = "realtimePulse")
                val pulseScale by realtimePulse.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.6f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                    label = "pulseScale",
                )
                val pulseAlpha by realtimePulse.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                    label = "pulseAlpha",
                )
                Box(
                    modifier = Modifier.size(14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .scale(pulseScale)
                            .background(colors.realtimeGreen.copy(alpha = pulseAlpha), CircleShape),
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(colors.realtimeGreen, CircleShape),
                    )
                }
                Spacer(Modifier.width(6.dp))
            }

            // Canonical stacked time display: minutes (top) / HH:mm (bottom).
            Column(horizontalAlignment = Alignment.End) {
                val timeState = com.transitkit.app.ui.components.departureTimeState(
                    displayTime,
                    operatorTimezone,
                )
                com.transitkit.app.ui.components.TimeDisplay(
                    state = timeState,
                    isEmphasis = isNext,
                )
                if (hasDelay) {
                    val delayMin = (departure.delay ?: 0) / 60
                    Text(
                        text = stringResource(R.string.label_ritardo_min, delayMin),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.realtimeRed,
                    )
                }
            }
        }
        if (isNext) {
            Spacer(Modifier.height(6.dp))
        }
    }
}

// Removed: private `LineChip` composable. All line badges now go through
// the design-system `com.transitkit.app.ui.components.LineBadge`.


private fun transitTypeIcon(type: Int): Int = when (type) {
    0 -> LucideIcons.Train
    1 -> LucideIcons.Train
    2 -> LucideIcons.Train
    4 -> LucideIcons.Ship
    else -> LucideIcons.BusFront
}

private fun transitTypeName(type: Int): String = when (type) {
    0 -> "Tram"
    1 -> "Metro"
    2 -> "Treno"
    4 -> "Ferry"
    else -> "Bus"
}

// ---------------------------------------------------------------------------
// Empty / Error states
// ---------------------------------------------------------------------------

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(LucideIcons.Clock),
            contentDescription = null,
            tint = TransitTheme.colors.textTertiary,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = stringResource(R.string.nessuna_partenza_oggi),
            style = MaterialTheme.typography.bodyMedium,
            color = TransitTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EmptyStateForFilter(routeName: String, onClearFilter: () -> Unit) {
    val colors = TransitTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(LucideIcons.Clock),
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = stringResource(R.string.nessuna_partenza_per_linea, routeName),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onClearFilter) {
            Text(
                text = stringResource(R.string.rimuovi_filtro),
                color = colors.accent,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(TransitTheme.colors.realtimeRed.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(LucideIcons.WifiOff),
                contentDescription = null,
                tint = TransitTheme.colors.realtimeRed,
                modifier = Modifier.size(36.dp),
            )
        }
        Text(
            text = stringResource(R.string.partenze_non_disponibili),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TransitTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.controlla_connessione),
            style = MaterialTheme.typography.bodySmall,
            color = TransitTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = TransitTheme.colors.accent,
                contentColor = Color.White,
            ),
            shape = RoundedCornerShape(24.dp),
        ) {
            Icon(painterResource(LucideIcons.RefreshCw), contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_riprova), fontWeight = FontWeight.SemiBold)
        }
    }
}

// ---------------------------------------------------------------------------
// Full Schedule Sheet
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullScheduleSheet(
    stopName: String,
    departuresByGroup: Map<String, List<ResolvedDeparture>>,
    onDismiss: () -> Unit,
) {
    val colors = TransitTheme.colors
    val sortedGroups = remember(departuresByGroup) { departuresByGroup.keys.toList() }

    // Ephemeral sheet-local state — not worth ViewModel storage
    var selectedGroup by remember(sortedGroups) { mutableStateOf(sortedGroups.firstOrNull()) }
    var filterRouteId by remember { mutableStateOf<String?>(null) }

    // Reset route filter when day group changes
    LaunchedEffect(selectedGroup) { filterRouteId = null }

    val groupDepartures = remember(selectedGroup, departuresByGroup) {
        departuresByGroup[selectedGroup] ?: emptyList()
    }
    val availableRoutes = remember(groupDepartures) { groupDepartures.distinctBy { it.routeId } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.background,
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.orario_completo),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
                Text(
                    stopName,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.semantics { contentDescription = "btn_close_schedule" },
            ) {
                Icon(painterResource(LucideIcons.X), contentDescription = null, tint = colors.textTertiary)
            }
        }

        // Day-group selector (only when > 1 distinct group)
        if (sortedGroups.size > 1) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                items(sortedGroups) { groupKey ->
                    val isSelected = groupKey == selectedGroup
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedGroup = groupKey },
                        label = {
                            Text(
                                dayGroupLabel(groupKey),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.accent,
                            selectedLabelColor = Color.White,
                            containerColor = colors.glassFill,
                            labelColor = colors.textPrimary,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = colors.glassBorder,
                            selectedBorderColor = Color.Transparent,
                        ),
                        shape = RoundedCornerShape(20.dp),
                    )
                }
            }
        }

        // Route filter (only when > 1 route serves this stop)
        if (availableRoutes.size > 1) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                item {
                    FilterChip(
                        selected = filterRouteId == null,
                        onClick = { filterRouteId = null },
                        modifier = Modifier.alpha(if (filterRouteId != null) 0.35f else 1f),
                        label = { Text(stringResource(R.string.filter_all), fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.accent,
                            selectedLabelColor = Color.White,
                            containerColor = colors.accent.copy(alpha = 0.18f),
                            labelColor = colors.accent,
                        ),
                        border = null,
                        shape = RoundedCornerShape(20.dp),
                    )
                }
                items(availableRoutes, key = { it.routeId }) { route ->
                    val chipColor = route.routeColor.takeIf { it.isNotBlank() }
                        ?.let { runCatching { it.toColor() }.getOrNull() }
                        ?: colors.accent
                    val routeTextColor = route.routeTextColor.takeIf { it.isNotBlank() }
                        ?.let { runCatching { it.toColor() }.getOrNull() }
                        ?: contrastOn(chipColor)
                    val isThis = filterRouteId == route.routeId
                    FilterChip(
                        selected = isThis,
                        onClick = { filterRouteId = if (isThis) null else route.routeId },
                        modifier = Modifier.alpha(if (filterRouteId != null && !isThis) 0.35f else 1f),
                        label = { Text(route.routeName, fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = chipColor,
                            selectedLabelColor = routeTextColor,
                            containerColor = colors.accent.copy(alpha = 0.18f),
                            labelColor = colors.accent,
                        ),
                        border = null,
                        shape = RoundedCornerShape(20.dp),
                    )
                }
            }
        }

        HorizontalDivider(color = colors.separator, thickness = 0.5.dp)

        AnimatedContent(
            targetState = selectedGroup,
            transitionSpec = {
                fadeIn(animationSpec = tween(150, easing = FastOutSlowInEasing)) togetherWith
                    fadeOut(animationSpec = tween(100, easing = FastOutSlowInEasing))
            },
            label = "scheduleGroupContent",
        ) { group ->
            val animGroupDepartures = remember(group, departuresByGroup) {
                departuresByGroup[group] ?: emptyList()
            }
            val animVisibleDepartures = remember(animGroupDepartures, filterRouteId) {
                val filtered = if (filterRouteId != null) animGroupDepartures.filter { it.routeId == filterRouteId } else animGroupDepartures
                filtered.sortedBy { it.minutesFromMidnight }
                    .groupBy { it.departureTime.take(2) }
                    .entries.sortedBy { it.key }
            }
            if (animGroupDepartures.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            painter = painterResource(LucideIcons.Clock),
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(32.dp),
                        )
                        Text(
                            stringResource(R.string.no_departures),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                        )
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                    animVisibleDepartures.forEach { (hour, rows) ->
                        item(key = "hour_${group}_$hour") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .padding(top = 16.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    hour,
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = colors.textTertiary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                HorizontalDivider(modifier = Modifier.weight(1f), color = colors.separator, thickness = 0.5.dp)
                            }
                        }
                        items(rows, key = { "${group}_${hour}_${it.minutesFromMidnight}_${it.routeId}_${it.tripId}" }) { dep ->
                            FullScheduleRow(dep)
                        }
                    }
                }
            }
        }
    }
}

/** Maps a sorted weekday-key back to a human label. */
@Composable
private fun dayGroupLabel(key: String): String {
    val days = key.split(",").toSet()
    val weekdays = setOf("monday", "tuesday", "wednesday", "thursday", "friday")
    return when {
        days == weekdays -> stringResource(R.string.day_group_feriali)
        days == setOf("saturday") -> stringResource(R.string.day_group_sabato)
        days == setOf("sunday") -> stringResource(R.string.day_group_festivi)
        days == setOf("saturday", "sunday") -> stringResource(R.string.day_group_weekend)
        days.size == 7 -> stringResource(R.string.day_group_ogni_giorno)
        days.size == 1 -> days.first().replaceFirstChar { it.uppercase() }.take(3)
        else -> days.map { it.take(2).replaceFirstChar { c -> c.uppercase() } }.joinToString("/")
    }
}

@Composable
private fun FullScheduleRow(dep: ResolvedDeparture) {
    val colors = TransitTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .semantics { contentDescription = "schedule_dep_${dep.routeId}_${dep.departureTime}" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            dep.departureTime.take(5),
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = colors.textPrimary,
            fontWeight = FontWeight.Medium,
        )
        com.transitkit.app.ui.components.LineBadge(
            name = dep.routeName.take(5),
            colorHex = dep.routeColor,
            textColorHex = dep.routeTextColor,
            size = com.transitkit.app.ui.components.LineBadgeSize.Small,
        )
        Text(
            dep.headsign.ifBlank { "" },
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

// ---------------------------------------------------------------------------
// LineBadgeRow
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LineBadgeRow(routes: List<ResolvedDeparture>, modifier: Modifier = Modifier) {
    val colors = TransitTheme.colors
    val uniqueRoutes = remember(routes) { routes.distinctBy { it.routeId } }
    if (uniqueRoutes.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val displayRoutes = if (uniqueRoutes.size <= 8 || expanded) uniqueRoutes else uniqueRoutes.take(8)
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        displayRoutes.forEach { route ->
            com.transitkit.app.ui.components.LineBadge(
                name = route.routeName.take(6),
                colorHex = route.routeColor,
                textColorHex = route.routeTextColor,
                // iOS parity: stop-detail coincidences use Medium (not Small).
                size = com.transitkit.app.ui.components.LineBadgeSize.Medium,
            )
        }
        if (uniqueRoutes.size > 8) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(colors.accent.copy(alpha = 0.15f))
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = if (expanded) stringResource(R.string.action_meno) else "+${uniqueRoutes.size - 8}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.accent,
                )
                Icon(
                    painter = painterResource(if (expanded) LucideIcons.ChevronUp else LucideIcons.ChevronDown),
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun formatTime(raw: String): String {
    return try {
        val parts = raw.split(":")
        val hour = parts[0].toInt() % 24
        val minute = parts[1].toInt()
        "%02d:%02d".format(hour, minute)
    } catch (_: Exception) {
        raw.take(5)
    }
}

/** WCAG relative-luminance-based contrast picker: returns white on dark bg, black on light bg. */
private fun contrastOn(bg: Color): Color {
    fun lin(c: Float): Float = if (c <= 0.03928f) c / 12.92f else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    val l = 0.2126f * lin(bg.red) + 0.7152f * lin(bg.green) + 0.0722f * lin(bg.blue)
    return if (l < 0.5f) Color.White else Color(0xFF111827)
}

// ---------------------------------------------------------------------------
// Alerts section (shared look & feel with LineDetailScreen)
// ---------------------------------------------------------------------------

@Composable
internal fun AlertsSection(
    alerts: List<ServiceAlert>,
    onClick: (alertId: String) -> Unit,
) {
    val colors = TransitTheme.colors
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter = painterResource(LucideIcons.AlertTriangle),
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = stringResource(R.string.stop_detail_alerts_section).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                ),
                color = colors.textSecondary,
            )
        }
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.bgSecondary)
                .border(1.dp, colors.glassBorder, RoundedCornerShape(12.dp)),
        ) {
            alerts.forEachIndexed { index, alert ->
                AlertSectionRow(alert = alert, onClick = { onClick(alert.id) })
                if (index < alerts.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 14.dp),
                        color = colors.separator,
                        thickness = 0.5.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertSectionRow(alert: ServiceAlert, onClick: () -> Unit) {
    val colors = TransitTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick,
            )
            .semantics { contentDescription = "alert_row_${alert.id}" }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(severityColor(alert.severity)),
        )
        Text(
            text = localizedHeader(alert),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(LucideIcons.ChevronRight),
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(14.dp),
        )
    }
}

/** Returns minutes until [timeStr] (HH:mm:ss, can exceed 24h). Null if >60 min or already passed. */
private fun minutesUntil(timeStr: String, timezone: String = "UTC"): Int? {
    return try {
        val parts = timeStr.split(":")
        val depTotalMin = parts[0].toInt() * 60 + parts[1].toInt()
        val tz = java.util.TimeZone.getTimeZone(timezone)
        val cal = java.util.Calendar.getInstance(tz)
        val nowMin = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        val diff = depTotalMin - nowMin
        if (diff in 0..60) diff else null
    } catch (_: Exception) {
        null
    }
}
