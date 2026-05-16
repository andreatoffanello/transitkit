package com.transitkit.app.ui.orari

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme

/**
 * LineDetailScreen — redesign Movete parity (no hero map).
 *
 * Struttura iOS / Movete "Maroon":
 *  - TopAppBar: back + center route badge+name + actions [star, expand-map]
 *  - "● N IN SERVIZIO" header + live vehicle cards (label + prossima fermata)
 *  - "○ Fermate servite" + timeline stops (esistente)
 *
 * Star toolbar: aggiunge/rimuove rotta dai preferiti — base per future
 * subscription agli alert per linea.
 *
 * Expand toolbar: apre la mappa principale già focalizzata su questa rotta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineDetailScreen(
    routeId: String,
    onBack: () -> Unit,
    onNavigateToStop: (stopId: String, stopName: String) -> Unit,
    onNavigateToAlert: (alertId: String) -> Unit = {},
    onNavigateToMap: (routeId: String) -> Unit = {},
    onNavigateToTrip: (tripId: String, fromStopId: String, routeColor: String, headsign: String, routeName: String) -> Unit = { _, _, _, _, _ -> },
    viewModel: LineDetailViewModel = hiltViewModel(),
) {
    val route by viewModel.route.collectAsStateWithLifecycle()
    val stops by viewModel.stops.collectAsStateWithLifecycle()
    val directions by viewModel.directions.collectAsStateWithLifecycle()
    val selectedDirectionIndex by viewModel.selectedDirectionIndex.collectAsStateWithLifecycle()
    val liveVehicleCards by viewModel.liveVehicleCards.collectAsStateWithLifecycle()
    val routeColorByName by viewModel.routeColorByName.collectAsStateWithLifecycle()
    val routeAlerts by viewModel.routeAlerts.collectAsStateWithLifecycle()
    val routesById by viewModel.routesById.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val colors = TransitTheme.colors

    val lineColor = remember(route?.color) {
        val hex = route?.color ?: ""
        if (hex.isNotBlank())
            runCatching { Color(android.graphics.Color.parseColor("#$hex")) }.getOrDefault(colors.accent)
        else colors.accent
    }
    val lineTextColor = remember(route?.textColor, lineColor) {
        val hex = route?.textColor ?: ""
        if (hex.isNotBlank()) {
            runCatching { Color(android.graphics.Color.parseColor("#$hex")) }
                .getOrDefault(routeBadgeContrast(lineColor))
        } else routeBadgeContrast(lineColor)
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    route?.let { r ->
                        // Titolo centrato Movete-style: small badge + nome rotta.
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(lineColor),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = r.name.take(3),
                                    color = lineTextColor,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Text(
                                text = r.longName.ifBlank { r.name },
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
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
                            contentDescription = stringResource(R.string.cd_indietro),
                            tint = colors.textPrimary,
                        )
                    }
                },
                actions = {
                    // Favorite toggle — gestisce alert per linea futuro.
                    val favScale by animateFloatAsState(
                        targetValue = if (isFavorite) 1.20f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        label = "favScale",
                    )
                    IconButton(
                        onClick = viewModel::toggleFavorite,
                        modifier = Modifier.semantics { contentDescription = "btn_favorite_line" },
                    ) {
                        AnimatedContent(
                            targetState = isFavorite,
                            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                            label = "linefav",
                        ) { fav ->
                            Icon(
                                painter = painterResource(
                                    if (fav) LucideIcons.StarFilled else LucideIcons.Star
                                ),
                                contentDescription = stringResource(
                                    if (fav) R.string.cd_rimuovi_preferiti else R.string.cd_aggiungi_preferiti
                                ),
                                tint = if (fav) colors.accent else colors.textSecondary,
                                modifier = Modifier.size(18.dp).scale(favScale),
                            )
                        }
                    }
                    // Expand → apri linea sulla mappa principale.
                    val haptic = LocalHapticFeedback.current
                    IconButton(
                        onClick = {
                            route?.let { r ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateToMap(r.id)
                            }
                        },
                        modifier = Modifier.semantics { contentDescription = "btn_open_line_on_map" },
                    ) {
                        Icon(
                            painter = painterResource(LucideIcons.Maximize2),
                            contentDescription = stringResource(R.string.map_expand),
                            tint = colors.textPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    scrolledContainerColor = colors.background,
                ),
            )
        },
    ) { innerPadding ->
        if (stops.isEmpty() && route != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(LucideIcons.BusFront),
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.sequenza_fermate_non_disponibile),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 88.dp),
        ) {
            // Service alerts (per route) — Movete-style rich cards inline
            // under the header. Tap drills into the alert detail screen.
            if (routeAlerts.isNotEmpty()) {
                items(routeAlerts, key = { "line-alert-${it.id}" }) { alert ->
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 4.dp,
                        )
                    ) {
                        com.transitkit.app.ui.alerts.AlertCard(
                            alert = alert,
                            routesById = routesById,
                            onClick = { onNavigateToAlert(alert.id) },
                        )
                    }
                }
            }

            // "● N IN SERVIZIO" + horizontal scroll of compact live cards —
            // Movete parity (May 2026). Tap drills into TripDetail so the user
            // follows the vehicle stop-by-stop.
            if (liveVehicleCards.isNotEmpty()) {
                item {
                    InServizioHeader(count = liveVehicleCards.size, accent = lineColor)
                }
                item {
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                    ) {
                        items(liveVehicleCards, key = { it.vehicleId }) { card ->
                            LiveVehicleCardView(
                                card = card,
                                lineColor = lineColor,
                                onClick = {
                                    val tid = card.tripId
                                    if (!tid.isNullOrBlank() && !card.firstStopId.isNullOrBlank()) {
                                        onNavigateToTrip(
                                            tid,
                                            card.firstStopId,
                                            card.routeColor,
                                            card.headsign,
                                            card.routeName,
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }

            // Direction picker — solo se >1 direzione.
            if (directions.size > 1) {
                item {
                    DirectionPicker(
                        directions = directions,
                        selectedIndex = selectedDirectionIndex,
                        lineColor = lineColor,
                        lineTextColor = lineTextColor,
                        onSelect = viewModel::selectDirection,
                    )
                    HorizontalDivider(color = colors.separator, thickness = 0.5.dp)
                }
            }

            // "○ Fermate servite" header
            item {
                FermateServiteHeader(
                    count = stops.size,
                    accent = lineColor,
                )
            }
            itemsIndexed(stops, key = { _, stop -> stop.id }) { index, stop ->
                val haptic = LocalHapticFeedback.current
                val onClick = remember(stop.id, haptic) {
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToStop(stop.id, stop.name)
                    }
                }
                StopTimelineRow(
                    stop = stop,
                    index = index,
                    total = stops.size,
                    accentColor = lineColor,
                    currentRouteName = route?.name ?: "",
                    routeColorByName = routeColorByName,
                    onClick = onClick,
                )
            }
        }
    }
}

// --- Section headers ---------------------------------------------------------

@Composable
private fun InServizioHeader(count: Int, accent: Color) {
    val colors = TransitTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(colors.realtimeGreen, CircleShape),
        )
        Text(
            text = stringResource(R.string.line_in_servizio, count),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun FermateServiteHeader(count: Int, accent: Color) {
    val colors = TransitTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painter = painterResource(LucideIcons.MapPin),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(R.string.fermate_servite),
            style = MaterialTheme.typography.labelMedium,
            color = colors.textSecondary,
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .background(accent.copy(alpha = 0.15f), RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// --- Live vehicle card -------------------------------------------------------

@Composable
private fun LiveVehicleCardView(
    card: LiveVehicleCard,
    lineColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TransitTheme.colors
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = modifier
            .width(220.dp)
            .clickable(enabled = !card.tripId.isNullOrBlank() && !card.firstStopId.isNullOrBlank()) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .semantics { contentDescription = "vehicle_card_${card.vehicleId}" },
        shape = RoundedCornerShape(14.dp),
        color = colors.bgSecondary,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Header row: bus icon + "Vettura …" + live dot, mirrors Movete &
            // iOS so the eye lands on the vehicle identifier first.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(LucideIcons.BusFront),
                    contentDescription = null,
                    tint = lineColor,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stringResource(R.string.vehicle_label_format, card.label),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(colors.realtimeGreen, CircleShape),
                )
            }
            // "PROSSIMA FERMATA" caption + stop name (or "TRACCIATO LIVE"
            // fallback so the card never reads as broken when the feed lacks
            // the current_stop_id field).
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (card.nextStopName != null) {
                        stringResource(R.string.vehicle_next_stop_caption)
                    } else {
                        stringResource(R.string.vehicle_live_tracking)
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                    color = colors.textTertiary,
                )
                Text(
                    text = card.nextStopName ?: stringResource(R.string.vehicle_next_stop_unknown),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (card.nextStopName != null) colors.textPrimary else colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// --- Direction picker --------------------------------------------------------

@Composable
private fun DirectionPicker(
    directions: List<com.transitkit.app.data.model.RouteDirection>,
    selectedIndex: Int,
    lineColor: Color,
    lineTextColor: Color,
    onSelect: (Int) -> Unit,
) {
    val colors = TransitTheme.colors
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.glassFill),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        directions.forEachIndexed { index, direction ->
            val selected = index == selectedIndex
            val bg = if (selected) lineColor else Color.Transparent
            val fg = if (selected) lineTextColor else colors.textSecondary
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(3.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bg)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(index)
                    }
                    .semantics { contentDescription = "direction_chip_$index" }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = direction.headsign.ifBlank { stringResource(R.string.direzione_numero, index + 1) },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = fg,
                )
            }
        }
    }
}
