package com.transitkit.app.ui.orari

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.transitkit.app.config.LucideIcons
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.transitkit.app.R
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.model.StopTime
import com.transitkit.app.ui.mappa.iconForTransitType
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    onBack: () -> Unit,
    onNavigateToStop: (stopId: String) -> Unit,
    onShowVehicleOnMap: (vehicleId: String) -> Unit = {},
    viewModel: TripDetailViewModel = hiltViewModel(),
) {
    val tripState by viewModel.tripState.collectAsStateWithLifecycle()
    val stopCoincidences by viewModel.stopCoincidences.collectAsStateWithLifecycle()
    val routeColorByName by viewModel.routeColorByName.collectAsStateWithLifecycle()
    val accentColor = TransitTheme.colors.accent
    val lineColor = remember(viewModel.routeColor, accentColor) {
        com.transitkit.app.ui.components.parseHexColor(viewModel.routeColor, fallback = accentColor)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (viewModel.routeName.isNotBlank()) {
                            com.transitkit.app.ui.components.LineBadge(
                                name = viewModel.routeName,
                                colorHex = viewModel.routeColor.takeIf { it.isNotBlank() },
                                // iOS TripDetailView parity: header badge is Large.
                                size = com.transitkit.app.ui.components.LineBadgeSize.Large,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(lineColor, CircleShape),
                            )
                        }
                        Column {
                            Text(
                                text = viewModel.headsign.ifBlank { stringResource(R.string.trip_detail_title_fallback) },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = TransitTheme.colors.textPrimary,
                            )
                            if (tripState is TripState.Success) {
                                Text(
                                    text = stringResource(R.string.stop_count_fermate, (tripState as TripState.Success).stops.size),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TransitTheme.colors.textSecondary,
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "btn_back" },
                    ) {
                        Icon(
                            painter = painterResource(LucideIcons.ChevronLeft),
                            contentDescription = stringResource(R.string.cd_indietro_trip),
                            tint = TransitTheme.colors.textPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TransitTheme.colors.background,
                ),
            )
        },
        containerColor = TransitTheme.colors.background,
    ) { padding ->
        AnimatedContent(
            targetState = tripState,
            transitionSpec = {
                fadeIn(tween(durationMillis = 220, easing = FastOutSlowInEasing)) togetherWith
                fadeOut(tween(durationMillis = 150))
            },
            label = "tripStateTransition",
        ) { state ->
            when (state) {
                is TripState.Loading -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = lineColor)
                }

                is TripState.Error -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.messageRes == R.string.trip_error_no_stops) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(TransitTheme.colors.textTertiary.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(LucideIcons.Clock),
                                    contentDescription = null,
                                    tint = TransitTheme.colors.textTertiary,
                                    modifier = Modifier.size(36.dp),
                                )
                            }
                            Text(
                                text = stringResource(state.messageRes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TransitTheme.colors.textPrimary,
                                textAlign = TextAlign.Center,
                            )
                            androidx.compose.material3.Button(
                                onClick = onBack,
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = lineColor,
                                    contentColor = Color.White,
                                ),
                                shape = RoundedCornerShape(24.dp),
                            ) {
                                Text(stringResource(R.string.action_back_home), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(state.messageRes),
                            color = TransitTheme.colors.textTertiary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(32.dp),
                        )
                    }
                }

                is TripState.Success -> {
                    val liveOriginIndex by viewModel.liveOriginIndex.collectAsStateWithLifecycle()
                    val hasLiveVehicle by viewModel.hasLiveVehicle.collectAsStateWithLifecycle()
                    val liveVehicle by viewModel.liveVehicle.collectAsStateWithLifecycle()
                    val liveDelayMinutes by viewModel.liveDelayMinutes.collectAsStateWithLifecycle()
                    val routeTransitType by viewModel.routeTransitType.collectAsStateWithLifecycle()
                    val listState = rememberLazyListState()
                    // Scroll once to the initial origin on first successful load.
                    // Subsequent vehicle movements update the highlight but keep
                    // the user's scroll position — jumping the list on every
                    // poll would be disorienting.
                    LaunchedEffect(Unit) {
                        delay(150)
                        listState.animateScrollToItem(liveOriginIndex.coerceAtLeast(0))
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        // Live vehicle box — shown ONLY when there is a live vehicle.
                        // Hidden when liveVehicle is null → screen unchanged when no RT feed.
                        if (liveVehicle != null) {
                            val v = liveVehicle!!
                            item(key = "live_vehicle_box") {
                                LiveVehicleBox(
                                    transitType = routeTransitType,
                                    delayMinutes = liveDelayMinutes ?: 0,
                                    lineColor = lineColor,
                                    onClick = { onShowVehicleOnMap(v.vehicleId) },
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .padding(top = 4.dp, bottom = 12.dp),
                                )
                            }
                        }

                        itemsIndexed(
                            items = state.stops,
                            key = { _, stop -> "${stop.stopId}_${stop.departureTime}" },
                        ) { index, stop ->
                            TripStopRow(
                                stop = stop,
                                index = index,
                                originIndex = liveOriginIndex,
                                showCurrentPill = hasLiveVehicle,
                                isFirst = index == 0,
                                isLast = index == state.stops.lastIndex,
                                lineColor = lineColor,
                                coincidences = stopCoincidences[stop.stopId] ?: emptyList(),
                                routeColorByName = routeColorByName,
                                onClick = { onNavigateToStop(stop.stopId) },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Live Vehicle Box ──────────────────────────────────────────────────────────
// Shown at the TOP of the trip ONLY when a live vehicle is resolved.
// Mirrors iOS TripDetailView `liveVehicleBox`: transit icon circle · status dot ·
// "In transito" · delay pill · "Vedi su mappa" CTA. Tap → switches to Map tab
// focused on that vehicle via the deep-link route `transitkit://map/vehicle/{id}`.
//
// Progress text (DoVe: vehicle position description) is intentionally omitted —
// `vehicleProgress()` helper is absent in transit-engine; omit as iOS commit did.

@Composable
private fun LiveVehicleBox(
    transitType: Int,
    delayMinutes: Int,
    lineColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val late = delayMinutes > 0
    val orange = Color(0xFFFF9500)
    val green = Color(0xFF34C759)
    val statusTint = if (late) orange else green
    val iconRes = iconForTransitType(transitType)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .border(1.dp, lineColor.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
            .padding(12.dp)
            .semantics { testTag = "trip_live_vehicle_box" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Transit mode icon in accent-colored circle
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(lineColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(21.dp),
                tint = lineColor,
            )
        }

        // Status column: dot · "In transito" · separator · delay pill
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(statusTint),
            )
            Text(
                text = stringResource(R.string.trip_in_transit),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "·",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
            Text(
                text = if (late) stringResource(R.string.trip_delay_minutes, delayMinutes)
                       else stringResource(R.string.trip_on_time),
                fontSize = 13.sp,
                fontWeight = if (late) FontWeight.SemiBold else FontWeight.Normal,
                color = if (late) orange else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // "See on map" CTA
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(
                painter = painterResource(LucideIcons.MapPin),
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = lineColor,
            )
            Text(
                text = stringResource(R.string.trip_see_on_map),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = lineColor,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TripStopRow(
    stop: StopTime,
    index: Int,
    originIndex: Int,
    showCurrentPill: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    lineColor: Color,
    coincidences: List<String>,
    routeColorByName: Map<String, String> = emptyMap(),
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    // `isCurrent` drives the highlight ring + accent text color for the
    // active row in the timeline. The "Now" pill — which actively claims a
    // bus is AT this stop — only renders when we have a real live vehicle.
    // Without [showCurrentPill] gating, a future trip whose `liveOriginIndex`
    // falls back to `originIndex` (boarding stop) would lie to the rider.
    val isPast = index < originIndex
    val isCurrent = index == originIndex
    val isTerminal = isFirst || isLast

    val dotColor = if (isPast) lineColor.copy(alpha = 0.45f) else lineColor
    val dotSize = when {
        isCurrent -> 14.dp
        isTerminal -> 12.dp
        else -> 8.dp
    }
    val textColor = when {
        isPast -> TransitTheme.colors.textTertiary
        isCurrent -> lineColor
        else -> TransitTheme.colors.textPrimary
    }
    val rowBg = if (isCurrent) lineColor.copy(alpha = 0.08f) else Color.Transparent
    val lineAboveColor = if (isPast) lineColor.copy(alpha = 0.35f) else lineColor
    // Line below: dimmed if current stop (next segment is future) — stays full below origin
    val lineBelowColor = if (isPast) lineColor.copy(alpha = 0.35f) else lineColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .then(
                if (isCurrent) {
                    Modifier.drawWithContent {
                        drawContent()
                        drawRect(
                            color = lineColor,
                            topLeft = Offset.Zero,
                            size = Size(3.dp.toPx(), size.height),
                        )
                    }
                } else Modifier
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .semantics { contentDescription = "trip_stop_row_${stop.stopId}" }
            .padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Timeline column
        Box(
            modifier = Modifier.width(52.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Line above (not for first)
                if (!isFirst) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(12.dp)
                            .background(lineAboveColor),
                    )
                }
                // Dot
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .background(dotColor, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isTerminal && !isCurrent) {
                        // Inner ring for terminal stops — matches screen background for hollow effect
                        Box(
                            modifier = Modifier
                                .size(dotSize - 4.dp)
                                .background(TransitTheme.colors.background, CircleShape),
                        )
                        Box(
                            modifier = Modifier
                                .size(dotSize - 8.dp)
                                .background(dotColor, CircleShape),
                        )
                    } else if (isCurrent) {
                        // Inner circle for current stop — matches screen background
                        Box(
                            modifier = Modifier
                                .size(dotSize - 6.dp)
                                .background(TransitTheme.colors.background, CircleShape),
                        )
                    }
                }
                // Line below (not for last)
                if (!isLast) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(12.dp)
                            .background(lineBelowColor),
                    )
                }
            }
        }

        // Content column
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stop.stopName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isTerminal || isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    color = textColor,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isCurrent && showCurrentPill) {
                    Box(
                        modifier = Modifier
                            .background(lineColor, RoundedCornerShape(50))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.trip_stop_attuale),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
                Text(
                    text = stop.departureTime.take(5),
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    color = if (isPast) TransitTheme.colors.textTertiary else TransitTheme.colors.textSecondary,
                )
            }
            if (!isPast && coincidences.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 3.dp),
                ) {
                    Icon(
                        painter = painterResource(LucideIcons.ArrowLeftRight),
                        contentDescription = null,
                        tint = TransitTheme.colors.textTertiary,
                        modifier = Modifier.size(11.dp),
                    )
                    coincidences.take(4).forEach { coincidenceRoute ->
                        val rawHex = routeColorByName[coincidenceRoute]?.takeIf { it.isNotBlank() }
                        val badgeColor = rawHex?.let { com.transitkit.app.ui.components.parseHexColor(it, fallback = lineColor.copy(alpha = 0.15f)) }
                            ?: lineColor.copy(alpha = 0.15f)
                        val isColored = routeColorByName[coincidenceRoute]?.isNotBlank() == true
                        Box(
                            modifier = Modifier
                                .height(16.dp)
                                .background(if (isColored) badgeColor.copy(alpha = 0.9f) else badgeColor, RoundedCornerShape(3.dp))
                                .padding(horizontal = 5.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = coincidenceRoute,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = if (isColored) Color.White else lineColor,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }

        Icon(
            painter = painterResource(LucideIcons.ChevronRight),
            contentDescription = null,
            tint = if (isPast)
                TransitTheme.colors.textTertiary.copy(alpha = 0.5f)
            else
                TransitTheme.colors.textTertiary,
            modifier = Modifier.size(16.dp),
        )
    }
}
