package com.transitkit.app.ui.orari

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.config.toColor
import com.transitkit.app.data.model.Departure
import com.transitkit.app.data.model.ResolvedDeparture

@Composable
internal fun DeparturesList(
    departures: List<Departure>,
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

    // Group by headsign so a multi-direction stop (e.g. a corridor where the
    // same route passes in both ways with different destinations) doesn't
    // interleave the two directions by time — the user wants to scan their
    // own way only. `departures` is already sorted upstream, and `groupBy`
    // preserves first-occurrence order, so the resulting `directionGroups`
    // are sorted by earliest departure naturally. Pattern from Movete
    // `133091db2`. Falls back to flat 5+5 when there's a single direction.
    val directionGroups = remember(departures) {
        departures.groupBy { it.headsign }.toList()
    }
    val useGroups = directionGroups.size >= 2

    // Plain Column (NOT LazyColumn): the whole stop body lives inside a single
    // verticalScroll in StopDetailScreen so the map hero scrolls AWAY WITH the
    // departures (iOS parity) instead of the list scrolling under a pinned map.
    // The inline list is bounded (5+5 / take(8) per direction), so laziness is
    // unnecessary and a nested same-direction scroll would crash.
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.label_oggi),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textTertiary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
        )
        if (useGroups) {
            directionGroups.forEachIndexed { groupIdx, (headsign, groupDeps) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = if (groupIdx == 0) 0.dp else 14.dp,
                        bottom = 6.dp,
                    ),
                ) {
                    Icon(
                        painter = painterResource(LucideIcons.ArrowRight),
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = headsign.ifBlank { stringResource(R.string.prossime_partenze) },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textSecondary,
                    )
                }
                val visibleDeps = groupDeps.take(8)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.bgSecondary)
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(14.dp)),
                ) {
                    visibleDeps.forEachIndexed { index, departure ->
                        DepartureRow(
                            departure = departure,
                            isNext = groupIdx == 0 && index == 0,
                            operatorTimezone = operatorTimezone,
                            stopSequence = stopSequenceByRouteId[departure.routeId],
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateToTrip(departure)
                            },
                        )
                        if (index < visibleDeps.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 16.dp),
                                color = colors.separator,
                                thickness = 0.5.dp,
                            )
                        }
                    }
                }
            }
        } else {
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

internal fun transitTypeIcon(type: Int): Int = when (type) {
    0 -> LucideIcons.Train
    1 -> LucideIcons.Train
    2 -> LucideIcons.Train
    4 -> LucideIcons.Ship
    else -> LucideIcons.BusFront
}

internal fun transitTypeName(type: Int): String = when (type) {
    0 -> "Tram"
    1 -> "Metro"
    2 -> "Treno"
    4 -> "Ferry"
    else -> "Bus"
}

/**
 * Header sezione partenze: transit type label + filter row (badges = filter)
 * + "Upcoming departures" titolo. Sempre visibile sopra qualsiasi state
 * (Success / Empty / Loading / Error) così l'utente può sempre tappare
 * "Tutti" per ripulire il filtro.
 */
@Composable
internal fun StopFilterHeader(
    availableRoutes: List<ResolvedDeparture>,
    selectedRoute: String?,
    onRouteSelected: (String?) -> Unit,
) {
    if (availableRoutes.isEmpty()) return
    val colors = TransitTheme.colors
    val haptic = LocalHapticFeedback.current
    Column(modifier = Modifier.padding(top = 12.dp)) {
        val transitTypes = availableRoutes.map { it.transitType }.distinct().sorted()
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
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
        // Badge row full-width con fade destro — indica che è scrollabile.
        Box(modifier = Modifier.fillMaxWidth()) {
            LineBadgeRow(
                routes = availableRoutes,
                selectedRouteId = selectedRoute,
                onRouteSelected = { id ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onRouteSelected(id)
                },
                contentPadding = PaddingValues(start = 16.dp, end = 40.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0.75f to Color.Transparent,
                                1.0f to colors.background,
                            )
                        )
                    )
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.prossime_partenze),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary,
        )
    }
}
