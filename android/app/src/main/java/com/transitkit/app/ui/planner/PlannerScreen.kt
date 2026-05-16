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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    val origin by viewModel.origin.collectAsState()
    val destination by viewModel.destination.collectAsState()
    val whenSel by viewModel.whenSelection.collectAsState()
    val journeys by viewModel.journeys.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val hasSearched by viewModel.hasSearched.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    val connState by viewModel.connectionsState.collectAsState()

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
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .semantics { contentDescription = "Back" },
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
        // Match the Home planner_home_box surface so the From/To card looks
        // identical across screens (was a M3 surfaceVariant lavender tint).
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.bgSecondary,
            border = BorderStroke(0.5.dp, colors.glassBorder),
            tonalElevation = 0.dp,
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                StopInputRow(
                    label = stringResource(R.string.planner_from),
                    location = origin,
                    isOrigin = true,
                    onClick = { onNavigateToLocationPicker("origin") },
                    onClear = { viewModel.setOrigin(null) },
                )
                Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 48.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 0.5.dp,
                    )
                    SwapButton(
                        onClick = { viewModel.swapStops() },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
                StopInputRow(
                    label = stringResource(R.string.planner_to),
                    location = destination,
                    isOrigin = false,
                    onClick = { onNavigateToLocationPicker("destination") },
                    onClear = { viewModel.setDestination(null) },
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
                            "Trip data unavailable",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary,
                        )
                        Text(
                            "Check your connection and try again.",
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
                    CenteredMessage {
                        Text(
                            searchError!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
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
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            "Choose a start and end stop",
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

@Composable
private fun StopInputRow(
    label: String,
    location: PlannerLocation?,
    isOrigin: Boolean,
    onClick: () -> Unit,
    onClear: () -> Unit,
) {
    val colors = TransitTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp)
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Timeline dot
        androidx.compose.foundation.Canvas(modifier = Modifier.size(20.dp)) {
            val r = 6.dp.toPx()
            val cx = size.width / 2
            val cy = size.height / 2
            if (isOrigin) {
                drawCircle(color = colors.accent, radius = r, center = androidx.compose.ui.geometry.Offset(cx, cy))
            } else {
                drawCircle(
                    color = colors.accent,
                    radius = r,
                    center = androidx.compose.ui.geometry.Offset(cx, cy),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
                )
            }
        }

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

        if (location != null) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(28.dp).semantics { contentDescription = "Clear location" },
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
            .semantics { contentDescription = "Swap origin and destination" },
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

