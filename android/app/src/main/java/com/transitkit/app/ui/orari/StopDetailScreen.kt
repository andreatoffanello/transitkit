package com.transitkit.app.ui.orari

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme

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
    val resolvedStop by viewModel.resolvedStop.collectAsStateWithLifecycle()
    val selectedRouteFilter by viewModel.selectedRouteFilter.collectAsStateWithLifecycle()
    val availableRoutes by viewModel.availableRoutes.collectAsStateWithLifecycle()
    val rawDepartures by viewModel.rawDepartures.collectAsStateWithLifecycle()
    val departuresByGroup by viewModel.departuresByGroup.collectAsStateWithLifecycle()
    val stopSequenceByRouteId by viewModel.stopSequenceByRouteId.collectAsStateWithLifecycle()
    val stopAlerts by viewModel.stopAlerts.collectAsStateWithLifecycle()
    val routesById by viewModel.routesById.collectAsStateWithLifecycle()
    val operatorTimezone = viewModel.operatorTimezone
    var showFullSchedule by remember { mutableStateOf(false) }
    var showExpandedMap by remember { mutableStateOf(false) }
    val colors = TransitTheme.colors
    val isRefreshing = departuresState is DeparturesState.Loading
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

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
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
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
                                modifier = Modifier.size(18.dp).scale(favScale),
                            )
                        } else {
                            Icon(
                                painter = painterResource(LucideIcons.Star),
                                contentDescription = stringResource(R.string.cd_salva_fermata),
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp).scale(favScale),
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
                // Hero map della fermata — iOS parity (item #5) + Movete parity
                // (CompactMap "Colosseo"): 3D scenico, road labels, marker
                // identico al main map. Tap → overlay espanso fullscreen.
                resolvedStop?.let { stop ->
                    StopDetailMapHero(
                        stop = stop,
                        accent = colors.accent,
                        onExpand = { showExpandedMap = true },
                        modifier = Modifier,
                    )
                }

                // Alerts chip — taps scroll the list to the AVVISI section
                // pinned to the bottom of `DeparturesList`. Movete parity.
                if (stopAlerts.isNotEmpty()) {
                    StopAlertsChip(
                        count = stopAlerts.size,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            scope.launch {
                                listState.animateScrollToItem(Int.MAX_VALUE)
                            }
                        },
                    )
                }

                // Filter header — SEMPRE visibile sopra qualunque state.
                // Permette di tappare "All" per pulire il filtro anche quando
                // la lista è vuota (empty state).
                StopFilterHeader(
                    availableRoutes = availableRoutes,
                    selectedRoute = selectedRouteFilter,
                    onRouteSelected = viewModel::selectRouteFilter,
                )

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
                        listState = listState,
                        trailingContent = {
                            if (stopAlerts.isNotEmpty()) {
                                item(key = "alerts-section") {
                                    AlertsSection(
                                        alerts = stopAlerts,
                                        routesById = routesById,
                                        onClick = onNavigateToAlert,
                                    )
                                }
                            }
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

    if (showExpandedMap) {
        resolvedStop?.let { stop ->
            StopDetailExpandedMap(
                stop = stop,
                accent = colors.accent,
                onDismiss = { showExpandedMap = false },
            )
        }
    }
}

/**
 * Chip below the map hero — taps scroll the inline departures list down
 * to the AVVISI section. Matches Movete's "N avvisi su questa fermata" pill.
 */
@Composable
private fun StopAlertsChip(
    count: Int,
    onClick: () -> Unit,
) {
    val colors = TransitTheme.colors
    val warning = Color(0xFFD97706)
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.bgSecondary)
            .border(BorderStroke(1.dp, warning.copy(alpha = 0.4f)), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics { contentDescription = "stop_alerts_chip" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(LucideIcons.AlertTriangle),
            contentDescription = null,
            tint = warning,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = androidx.compose.ui.res.pluralStringResource(
                R.plurals.stop_alerts_chip,
                count,
                count,
            ),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(LucideIcons.ChevronDown),
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(11.dp),
        )
    }
}
