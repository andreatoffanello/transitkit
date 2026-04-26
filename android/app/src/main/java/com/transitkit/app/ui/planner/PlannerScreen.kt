package com.transitkit.app.ui.planner

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.data.model.TransitLeg
import com.transitkit.app.data.model.WalkingLeg
import com.transitkit.app.data.model.durationMinutes
import com.transitkit.app.data.model.transfers
import com.transitkit.app.data.store.ConnectionsStore
import com.transitkit.app.ui.components.LineBadge
import com.transitkit.app.ui.components.LineBadgeSize
import com.transitkit.app.ui.components.parseHexColor
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ─── PlannerScreen ────────────────────────────────────────────────────────────

@Composable
fun PlannerScreen(
    onNavigateToJourneyDetail: (Journey) -> Unit = {},
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

    // Re-trigger search when connections become ready
    LaunchedEffect(connState) {
        if (connState == ConnectionsStore.LoadState.READY) viewModel.onConnectionsReady()
    }

    var showOriginSheet by remember { mutableStateOf(false) }
    var showDestSheet by remember { mutableStateOf(false) }
    var showWhenSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Text(
            text = stringResource(R.string.tab_planner),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary,
        )

        // ── Input card ────────────────────────────────────────────────────────
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 0.dp,
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                // Origin row
                StopInputRow(
                    label = stringResource(R.string.planner_from),
                    stop = origin,
                    isOrigin = true,
                    onClick = { showOriginSheet = true },
                    onClear = { viewModel.setOrigin(null) },
                )
                // Divider + swap
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
                // Destination row
                StopInputRow(
                    label = stringResource(R.string.planner_to),
                    stop = destination,
                    isOrigin = false,
                    onClick = { showDestSheet = true },
                    onClear = { viewModel.setDestination(null) },
                )
            }
        }

        // ── When chip ─────────────────────────────────────────────────────────
        WhenChip(
            selection = whenSel,
            onClick = { showWhenSheet = true },
        )

        // ── Results area ──────────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            when {
                connState == ConnectionsStore.LoadState.DOWNLOADING -> {
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
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(journeys, key = { it.id }) { journey ->
                            JourneyCard(
                                journey = journey,
                                onClick = { onNavigateToJourneyDetail(journey) },
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

    // ── Sheets ────────────────────────────────────────────────────────────────
    if (showOriginSheet) {
        StopSearchSheet(
            title = stringResource(R.string.planner_from),
            onDismiss = { showOriginSheet = false },
            onSelect = { stop ->
                viewModel.setOrigin(stop)
                showOriginSheet = false
            },
            viewModel = viewModel,
        )
    }
    if (showDestSheet) {
        StopSearchSheet(
            title = stringResource(R.string.planner_to),
            onDismiss = { showDestSheet = false },
            onSelect = { stop ->
                viewModel.setDestination(stop)
                showDestSheet = false
            },
            viewModel = viewModel,
        )
    }
    if (showWhenSheet) {
        WhenBottomSheet(
            current = whenSel,
            onDismiss = { showWhenSheet = false },
            onSelect = { sel ->
                viewModel.setWhenSelection(sel)
                showWhenSheet = false
            },
        )
    }
}

// ─── StopInputRow ──────────────────────────────────────────────────────────────

@Composable
private fun StopInputRow(
    label: String,
    stop: ResolvedStop?,
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

        // Label / stop name
        Text(
            text = stop?.name ?: label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (stop != null) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (stop != null) colors.textPrimary else colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        if (stop != null) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(28.dp).semantics { contentDescription = "Clear stop" },
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
            painterResource(LucideIcons.ArrowLeftRight),
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier
                .size(16.dp)
                .rotate(rotation),
        )
    }
}

// ─── WhenChip ─────────────────────────────────────────────────────────────────

@Composable
private fun WhenChip(selection: PlannerViewModel.WhenSelection, onClick: () -> Unit) {
    val colors = TransitTheme.colors
    val label = when (selection.mode) {
        1 -> "Depart at ${formatShortTime(selection.date)}"
        2 -> "Arrive by ${formatShortTime(selection.date)}"
        else -> stringResource(R.string.planner_now)
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "When: $label" },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painterResource(LucideIcons.Clock),
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
            )
            Icon(
                painterResource(LucideIcons.ChevronDown),
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

// ─── JourneyCard ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JourneyCard(journey: Journey, onClick: () -> Unit) {
    val colors = TransitTheme.colors
    val depFmt = remember(journey.departureTime) { formatEpochTime(journey.departureTime) }
    val arrFmt = remember(journey.arrivalTime) { formatEpochTime(journey.arrivalTime) }
    val durationMin = journey.durationMinutes
    val transfers = journey.transfers
    val transitLegs = journey.legs.filterIsInstance<TransitLeg>()
    val departsInMin = ((journey.departureTime - System.currentTimeMillis()) / 60_000L).toInt()

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Journey $depFmt to $arrFmt" },
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Time row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$depFmt → $arrFmt",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "$durationMin min",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textSecondary,
                )
            }

            // Line badges
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                transitLegs.forEach { leg ->
                    LineBadge(
                        name = leg.routeName,
                        colorHex = leg.routeColor,
                        textColorHex = leg.routeTextColor,
                        size = LineBadgeSize.Small,
                    )
                }
            }

            // Footer row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (departsInMin in 0..60) {
                    val departsLabel = if (departsInMin < 1) "Departing now"
                    else stringResource(R.string.planner_departs_in, "$departsInMin min")
                    Text(
                        text = departsLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (departsInMin < 5) colors.accent else colors.textSecondary,
                    )
                }
                if (transfers > 0) {
                    val changeLabel = if (transfers == 1)
                        "1 ${stringResource(R.string.planner_change)}"
                    else
                        "$transfers ${stringResource(R.string.planner_changes)}"
                    Text(
                        text = changeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
}

// ─── JourneyDetailScreen ──────────────────────────────────────────────────────

@Composable
fun JourneyDetailScreen(journey: Journey, onBack: () -> Unit) {
    val colors = TransitTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
            ) {
                Icon(painterResource(LucideIcons.ArrowLeft), contentDescription = null)
            }
            Text(
                text = stringResource(R.string.planner_journey),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            )
            val depFmt = formatEpochTime(journey.departureTime)
            val arrFmt = formatEpochTime(journey.arrivalTime)
            Text(
                text = "$depFmt → $arrFmt",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(end = 16.dp),
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            items(journey.legs) { leg ->
                when (leg) {
                    is TransitLeg -> TransitLegRow(leg)
                    is WalkingLeg -> WalkingLegRow(leg)
                }
            }
        }
    }
}

@Composable
private fun TransitLegRow(leg: TransitLeg) {
    val colors = TransitTheme.colors
    var expanded by remember { mutableStateOf(false) }
    val depFmt = formatEpochTime(leg.departureTime)
    val arrFmt = formatEpochTime(leg.arrivalTime)

    Column {
        // Board row
        TimelineRow(
            time = depFmt,
            nodeColor = parseHexColor(leg.routeColor, fallback = colors.accent),
            isTerminal = true,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    leg.boardStop.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                LineBadge(
                    name = leg.routeName,
                    colorHex = leg.routeColor,
                    textColorHex = leg.routeTextColor,
                    size = LineBadgeSize.Small,
                )
            }
        }

        // Intermediate stops (expandable)
        if (leg.intermediateStops.isNotEmpty()) {
            TimelineRow(
                time = "",
                nodeColor = parseHexColor(leg.routeColor, fallback = colors.accent),
                isTerminal = false,
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { expanded = !expanded }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.planner_stops_collapsed, leg.intermediateStops.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.accent,
                    )
                    Icon(
                        painterResource(if (expanded) LucideIcons.ChevronUp else LucideIcons.ChevronDown),
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    leg.intermediateStops.forEach { stop ->
                        IntermediateStopRow(stop, parseHexColor(leg.routeColor, fallback = colors.accent))
                    }
                }
            }
        }

        // Alight row
        TimelineRow(
            time = arrFmt,
            nodeColor = parseHexColor(leg.routeColor, fallback = colors.accent),
            isTerminal = true,
        ) {
            Text(
                leg.alightStop.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
            )
        }
    }
}

@Composable
private fun IntermediateStopRow(stop: IntermediateStop, lineColor: Color) {
    val colors = TransitTheme.colors
    TimelineRow(
        time = stop.time,
        nodeColor = lineColor,
        isTerminal = false,
        dotSize = 6.dp,
    ) {
        Text(
            stop.name,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun WalkingLegRow(leg: WalkingLeg) {
    val colors = TransitTheme.colors
    val walkMin = (leg.walkSeconds / 60).coerceAtLeast(1)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Dashed line connector + icon
        Box(
            modifier = Modifier.width(46.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(LucideIcons.MapPin),
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = stringResource(R.string.planner_walk_min, walkMin),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun TimelineRow(
    time: String,
    nodeColor: Color,
    isTerminal: Boolean,
    dotSize: androidx.compose.ui.unit.Dp = 10.dp,
    content: @Composable () -> Unit,
) {
    val colors = TransitTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // Time column
        Text(
            text = time,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = colors.textSecondary,
            modifier = Modifier.width(46.dp).padding(top = 2.dp),
        )

        // Node column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(20.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .background(
                        if (isTerminal) nodeColor else nodeColor.copy(alpha = 0.4f),
                        CircleShape,
                    ),
            )
            if (!isTerminal) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(24.dp)
                        .background(nodeColor.copy(alpha = 0.3f)),
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Content
        Box(modifier = Modifier.weight(1f).padding(bottom = if (isTerminal) 8.dp else 0.dp)) {
            content()
        }
    }
}

// ─── StopSearchSheet ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StopSearchSheet(
    title: String,
    onDismiss: () -> Unit,
    onSelect: (ResolvedStop) -> Unit,
    viewModel: PlannerViewModel,
) {
    val colors = TransitTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }

    // Access stops from ScheduleRepository via the ViewModel
    val allStops by viewModel.allStops.collectAsState()
    val filtered = remember(query, allStops) {
        if (query.isBlank()) allStops.take(80)
        else allStops.filter { it.name.contains(query, ignoreCase = true) }.take(50)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search stops…", color = colors.textSecondary) },
                leadingIcon = {
                    Icon(painterResource(LucideIcons.Search), contentDescription = null, modifier = Modifier.size(18.dp))
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .semantics { contentDescription = "Search stops" },
                shape = RoundedCornerShape(12.dp),
            )
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp),
            ) {
                items(filtered, key = { it.id }) { stop ->
                    StopSearchRow(
                        stop = stop,
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onSelect(stop) }
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StopSearchRow(stop: ResolvedStop, onClick: () -> Unit) {
    val colors = TransitTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics { contentDescription = stop.name },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painterResource(LucideIcons.MapPin),
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(16.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stop.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (stop.routeNames.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    stop.routeNames.take(6).forEachIndexed { idx, name ->
                        val color = stop.routeColors.getOrNull(idx)
                        LineBadge(
                            name = name,
                            colorHex = color,
                            size = LineBadgeSize.Small,
                        )
                    }
                }
            }
        }
    }
}

// ─── WhenBottomSheet ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WhenBottomSheet(
    current: PlannerViewModel.WhenSelection,
    onDismiss: () -> Unit,
    onSelect: (PlannerViewModel.WhenSelection) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var mode by remember { mutableStateOf(current.mode) }
    var selectedDate by remember { mutableStateOf(current.date) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "When",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )

            // Segmented button
            val options = listOf(
                stringResource(R.string.planner_now),
                stringResource(R.string.planner_depart_at),
                stringResource(R.string.planner_arrive_by),
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { idx, label ->
                    SegmentedButton(
                        selected = mode == idx,
                        onClick = { mode = idx },
                        shape = SegmentedButtonDefaults.itemShape(idx, options.size),
                        modifier = Modifier.semantics { contentDescription = label },
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Date/time picker (only for modes 1 and 2)
            AnimatedVisibility(
                visible = mode != 0,
                enter = expandVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painterResource(LucideIcons.Clock),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    val cal = Calendar.getInstance().apply { time = selectedDate }
                    Text(
                        text = formatFullDateTime(selectedDate),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        cal.set(Calendar.YEAR, year)
                                        cal.set(Calendar.MONTH, month)
                                        cal.set(Calendar.DAY_OF_MONTH, day)
                                        TimePickerDialog(
                                            context,
                                            { _, hour, minute ->
                                                cal.set(Calendar.HOUR_OF_DAY, hour)
                                                cal.set(Calendar.MINUTE, minute)
                                                selectedDate = cal.time
                                            },
                                            cal.get(Calendar.HOUR_OF_DAY),
                                            cal.get(Calendar.MINUTE),
                                            true,
                                        ).show()
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH),
                                ).show()
                            }
                            .padding(4.dp),
                    )
                }
            }

            // Confirm button
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = TransitTheme.colors.accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        val sel = PlannerViewModel.WhenSelection(mode = mode, date = if (mode == 0) Date() else selectedDate)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onSelect(sel) }
                    }
                    .semantics { contentDescription = "Confirm when selection" },
            ) {
                Text(
                    "Done",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    modifier = Modifier.padding(16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }

            Spacer(Modifier.height(16.dp))
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

// ─── Time formatting helpers ─────────────────────────────────────────────────

private val hhmm = SimpleDateFormat("HH:mm", Locale.getDefault())
private val fullDt = SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault())

private fun formatEpochTime(epochMs: Long): String = hhmm.format(Date(epochMs))
private fun formatShortTime(date: Date): String = hhmm.format(date)
private fun formatFullDateTime(date: Date): String = fullDt.format(date)
