package com.transitkit.app.ui.planner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.model.IntermediateStop
import com.transitkit.app.data.model.Journey
import com.transitkit.app.data.model.PlannerLocation
import com.transitkit.app.data.model.TransitLeg
import com.transitkit.app.data.model.WalkingLeg
import com.transitkit.app.data.model.durationMinutes
import com.transitkit.app.data.model.transfers
import com.transitkit.app.data.store.ConnectionsStore
import com.transitkit.app.ui.components.LineBadge
import com.transitkit.app.ui.components.LineBadgeSize
import com.transitkit.app.ui.components.parseHexColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── PlannerScreen ────────────────────────────────────────────────────────────

@Composable
fun PlannerScreen(
    onBack: () -> Unit = {},
    onNavigateToJourneyDetail: (Journey) -> Unit = {},
    onNavigateToLocationPicker: (role: String) -> Unit = {},
    viewModel: PlannerViewModel = hiltViewModel(),
) {
    val colors = TransitTheme.colors
    val origin by viewModel.origin.collectAsStateWithLifecycle()
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val whenSel by viewModel.whenSelection.collectAsStateWithLifecycle()
    val journeys by viewModel.journeys.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val hasSearched by viewModel.hasSearched.collectAsStateWithLifecycle()
    val searchError by viewModel.searchError.collectAsStateWithLifecycle()
    val connState by viewModel.connectionsState.collectAsStateWithLifecycle()

    LaunchedEffect(connState) {
        if (connState == ConnectionsStore.LoadState.READY) viewModel.onConnectionsReady()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val cdBack = stringResource(R.string.cd_indietro)
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .semantics { contentDescription = cdBack },
            ) {
                Icon(
                    painterResource(LucideIcons.ArrowLeft),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = colors.textPrimary,
                )
            }
            Text(
                text = stringResource(R.string.tab_planner),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary,
            )
        }

        // ── Input card ────────────────────────────────────────────────────────
        // Layout Movete-parity: icon column (locate-fixed + connector + map-pin)
        // a sinistra, fields stacked a destra, swap button in fondo.
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.bgSecondary,
            border = BorderStroke(0.5.dp, colors.glassBorder),
            tonalElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Icon column with vertical connector — same pattern as iOS
                // PlannerScreen.swift inputHeader (locateFixed + Rectangle + mapPin).
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.width(20.dp),
                ) {
                    Icon(
                        painter = painterResource(LucideIcons.LocateFixed),
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(20.dp),
                    )
                    Box(
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .width(1.dp)
                            .height(18.dp)
                            .background(colors.textTertiary),
                    )
                    Icon(
                        painter = painterResource(LucideIcons.MapPin),
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Stacked field labels — no inline icons (already in icon column).
                Column(modifier = Modifier.weight(1f)) {
                    StopInputRow(
                        label = stringResource(R.string.planner_from),
                        location = origin,
                        onClick = { onNavigateToLocationPicker("origin") },
                        onClear = { viewModel.setOrigin(null) },
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 0.5.dp,
                    )
                    StopInputRow(
                        label = stringResource(R.string.planner_to),
                        location = destination,
                        onClick = { onNavigateToLocationPicker("destination") },
                        onClear = { viewModel.setDestination(null) },
                    )
                }

                SwapButton(
                    onClick = { viewModel.swapStops() },
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }

        // ── When chip row ─────────────────────────────────────────────────────
        WhenChipRow(
            selection = whenSel,
            onSelectionChange = viewModel::setWhenSelection,
        )

        // ── Results area ──────────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            when {
                connState == ConnectionsStore.LoadState.IDLE -> {
                    CenteredMessage {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = colors.accent,
                            strokeWidth = 2.5.dp,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.planner_downloading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                        )
                    }
                }
                connState == ConnectionsStore.LoadState.UNAVAILABLE -> {
                    CenteredMessage {
                        Icon(
                            painterResource(LucideIcons.WifiOff),
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(32.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.planner_data_unavailable),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary,
                        )
                        Text(
                            stringResource(R.string.planner_data_unavailable_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                }
                isSearching -> {
                    CenteredMessage {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = colors.accent,
                            strokeWidth = 2.5.dp,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.planner_searching),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                        )
                    }
                }
                searchError != null -> {
                    when (searchError!!) {
                        PlannerViewModel.SearchError.Unreachable ->
                            PlannerUnreachableState(onRetry = { viewModel.retry() })
                        PlannerViewModel.SearchError.SameStop ->
                            CenteredMessage {
                                Text(
                                    stringResource(R.string.planner_same_stop),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        PlannerViewModel.SearchError.OutOfServiceArea ->
                            CenteredMessage {
                                Text(
                                    stringResource(R.string.planner_out_of_service_area),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                    }
                }
                hasSearched && journeys.isEmpty() -> {
                    CenteredMessage {
                        Icon(
                            painterResource(LucideIcons.Route),
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(32.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.planner_no_trips),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary,
                        )
                    }
                }
                journeys.isNotEmpty() -> {
                    val maxDuration = journeys.maxOf {
                        ((it.arrivalTime - it.departureTime) / 1_000L).coerceAtLeast(0L)
                    }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        // Bottom padding parity with HomeScreen / LineeScreen:
                        // keep the last journey clear of the bottom tab bar.
                        contentPadding = PaddingValues(bottom = 88.dp),
                    ) {
                        items(journeys, key = { it.id }) { journey ->
                            JourneyCard(
                                journey = journey,
                                onClick = { onNavigateToJourneyDetail(journey) },
                                maxJourneyDurationSeconds = maxDuration,
                            )
                        }
                    }
                }
                else -> {
                    // Empty state — not searched yet
                    CenteredMessage {
                        Icon(
                            painterResource(LucideIcons.Compass),
                            contentDescription = null,
                            tint = colors.textSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.planner_choose_stops),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
        }
    }

}

// ─── StopInputRow ──────────────────────────────────────────────────────────────
// Icon è disegnata nella icon column del parent (Surface in PlannerScreen).
// Questa row ha solo: text + optional clear button.

@Composable
private fun StopInputRow(
    label: String,
    location: PlannerLocation?,
    onClick: () -> Unit,
    onClear: () -> Unit,
) {
    val colors = TransitTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 14.dp)
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Label / location name
        Text(
            text = location?.name ?: label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (location != null) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (location != null) colors.textPrimary else colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        val cdClear = stringResource(R.string.cd_clear_location)
        if (location != null) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(28.dp).semantics { contentDescription = cdClear },
            ) {
                Icon(
                    painterResource(LucideIcons.X),
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

// ─── SwapButton ───────────────────────────────────────────────────────────────

@Composable
private fun SwapButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TransitTheme.colors
    val cdSwap = stringResource(R.string.cd_swap_stops)
    var rotated by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (rotated) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "swapRotation",
    )
    IconButton(
        onClick = {
            rotated = !rotated
            onClick()
        },
        modifier = modifier
            .size(32.dp)
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .semantics { contentDescription = cdSwap },
    ) {
        Icon(
            painterResource(LucideIcons.ArrowUpDown),
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier
                .size(16.dp)
                .rotate(rotation),
        )
    }
}

// ─── PlannerUnreachableState ─────────────────────────────────────────────────
// Outage del servizio percorsi: messaggio onesto (è il backend, non il viaggio)
// + retry. Distinto dal no-trips, dove riprovare non cambierebbe l'esito.
// Specchio iOS PlannerScreen.unreachableView.

@Composable
private fun PlannerUnreachableState(onRetry: () -> Unit) {
    val colors = TransitTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painterResource(LucideIcons.AlertTriangle),
            contentDescription = null,
            tint = colors.textSecondary.copy(alpha = 0.6f),
            modifier = Modifier.size(30.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.planner_unreachable_title),
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.planner_unreachable_body),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = 0.22f))
                .border(1.dp, colors.accent.copy(alpha = 0.45f), CircleShape)
                .clickable { onRetry() }
                .semantics { testTag = "planner-retry" }
                .padding(horizontal = 24.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.planner_retry),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.accent,
            )
        }
    }
}

// ─── CenteredMessage helper ──────────────────────────────────────────────────

@Composable
private fun CenteredMessage(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        content()
    }
}

