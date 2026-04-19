package com.transitkit.app.ui.orari

import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.transitkit.app.config.LucideIcons
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.transitkit.app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.data.model.ScheduleRoute

@Composable
fun LineDetailScreen(
    routeId: String,
    onBack: () -> Unit,
    onNavigateToStop: (stopId: String, stopName: String) -> Unit,
    onNavigateToAlert: (alertId: String) -> Unit = {},
    onNavigateToMap: (routeId: String) -> Unit = {},
    viewModel: LineDetailViewModel = hiltViewModel(),
) {
    val route by viewModel.route.collectAsStateWithLifecycle()
    val stops by viewModel.stops.collectAsStateWithLifecycle()
    val directions by viewModel.directions.collectAsStateWithLifecycle()
    val selectedDirectionIndex by viewModel.selectedDirectionIndex.collectAsStateWithLifecycle()
    val liveCount by viewModel.liveVehicleCount.collectAsStateWithLifecycle()
    val routeColorByName by viewModel.routeColorByName.collectAsStateWithLifecycle()
    val routeAlerts by viewModel.routeAlerts.collectAsStateWithLifecycle()
    val colors = TransitTheme.colors

    val lineColor = remember(route?.color) {
        val hex = route?.color ?: ""
        if (hex.isNotBlank())
            runCatching { Color(android.graphics.Color.parseColor("#$hex")) }.getOrDefault(colors.accent)
        else colors.accent
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        route?.let { r ->
            LineDetailHeader(route = r, stopCount = stops.size, liveCount = liveCount, onBack = onBack)
        } ?: run {
            // Minimal fallback header while loading
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.accent)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 4.dp, vertical = 8.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(LucideIcons.ChevronLeft),
                        contentDescription = stringResource(R.string.cd_indietro),
                        tint = Color.White,
                    )
                }
            }
        }

        if (stops.isEmpty() && route != null) {
            // No stop sequence available
            Box(
                modifier = Modifier
                    .fillMaxSize()
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
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp),
        ) {
            // Open in map button
            route?.let { r ->
                item {
                    val haptic = LocalHapticFeedback.current
                    val openInMapLabel = stringResource(R.string.open_in_map)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(lineColor.copy(alpha = 0.10f))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onNavigateToMap(r.id)
                                }
                                .semantics { contentDescription = openInMapLabel }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                painter = painterResource(LucideIcons.Map),
                                contentDescription = null,
                                tint = lineColor,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = openInMapLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = lineColor,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                painter = painterResource(LucideIcons.ChevronRight),
                                contentDescription = null,
                                tint = lineColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
            // Service alerts affecting this route
            if (routeAlerts.isNotEmpty()) {
                item {
                    AlertsSection(
                        alerts = routeAlerts,
                        onClick = onNavigateToAlert,
                    )
                }
            }
            // Direction picker — shown only when route has multiple directions
            if (directions.size > 1) {
                item {
                    val haptic = LocalHapticFeedback.current
                    val lineTextOnSelected = remember(lineColor) { routeBadgeContrast(lineColor) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.glassFill),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        directions.forEachIndexed { index, direction ->
                            val selected = index == selectedDirectionIndex
                            val bg = if (selected) lineColor else Color.Transparent
                            val fg = if (selected) lineTextOnSelected else colors.textSecondary
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(3.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bg)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.selectDirection(index)
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
                    HorizontalDivider(color = colors.separator, thickness = 0.5.dp)
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(LucideIcons.MapPin),
                        contentDescription = null,
                        tint = lineColor,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.fermate_servite),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary,
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .background(lineColor.copy(alpha = 0.15f), RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stops.size.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = lineColor,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            itemsIndexed(stops, key = { _, stop -> stop.id }) { index, stop ->
                val haptic = LocalHapticFeedback.current
                val onClick = remember(stop.id, haptic) { {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigateToStop(stop.id, stop.name)
                } }
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

// ---------------------------------------------------------------------------
// Transit type helper
// ---------------------------------------------------------------------------

private fun transitTypeDisplayName(transitType: Int): String = when (transitType) {
    0 -> "Tram"
    1 -> "Metro"
    2 -> "Treno"
    4 -> "Ferry"
    else -> "Bus"
}

/** WCAG relative-luminance-based foreground picker for GTFS route badge backgrounds. */
private fun routeBadgeContrast(bg: Color): Color {
    fun lin(c: Float): Float = if (c <= 0.03928f) c / 12.92f else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    val l = 0.2126f * lin(bg.red) + 0.7152f * lin(bg.green) + 0.0722f * lin(bg.blue)
    return if (l < 0.5f) Color.White else Color(0xFF111827)
}

// ---------------------------------------------------------------------------
// Header with color gradient + stop count pill
// ---------------------------------------------------------------------------

@Composable
private fun LineDetailHeader(
    route: ScheduleRoute,
    stopCount: Int,
    liveCount: Int,
    onBack: () -> Unit,
) {
    val accentColor = TransitTheme.colors.accent
    val lineColor = remember(route.color, accentColor) {
        runCatching { Color(android.graphics.Color.parseColor("#${route.color}")) }
            .getOrDefault(accentColor)
    }
    val lineTextColor = remember(route.textColor) {
        if (route.textColor.isNotBlank())
            runCatching { Color(android.graphics.Color.parseColor("#${route.textColor}")) }
                .getOrDefault(Color.White)
        else Color.White
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        lineColor,
                        lineColor.copy(alpha = 0.72f),
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 4.dp, vertical = 8.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.semantics { contentDescription = "btn_back" },
        ) {
            Icon(
                painter = painterResource(LucideIcons.ChevronLeft),
                contentDescription = stringResource(R.string.cd_indietro),
                tint = Color.White,
            )
        }

        Column(
            modifier = Modifier.padding(start = 16.dp, top = 52.dp, end = 16.dp, bottom = 16.dp),
        ) {
            // Route badge
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = route.name.take(5),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = lineTextColor,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = route.longName.ifBlank { route.name },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val transitTypeLabel = transitTypeDisplayName(route.transitType)
            val displayedName = route.longName.ifBlank { route.name }
            if (!displayedName.contains(transitTypeLabel, ignoreCase = true)) {
                Text(
                    text = transitTypeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.72f),
                )
            }
            if (stopCount > 0) {
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.stop_count_fermate, stopCount),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                    if (liveCount > 0) {
                        Box(
                            modifier = Modifier
                                .background(TransitTheme.colors.realtimeGreen.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(TransitTheme.colors.realtimeGreen, CircleShape),
                                )
                                Text(
                                    text = stringResource(R.string.live_count, liveCount),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TransitTheme.colors.realtimeGreen,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Stop row with vertical timeline
// ---------------------------------------------------------------------------

@Composable
private fun StopTimelineRow(
    stop: ResolvedStop,
    index: Int,
    total: Int,
    accentColor: Color,
    currentRouteName: String,
    routeColorByName: Map<String, String> = emptyMap(),
    onClick: () -> Unit,
) {
    val colors = TransitTheme.colors
    val isFirst = index == 0
    val isLast = index == total - 1

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "line_stop_${stop.id}" }
            .padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Timeline column: full-height line + circle overlay
        Box(
            modifier = Modifier
                .width(56.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            // Top half line — hidden for first stop
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight(0.5f)
                        .background(accentColor)
                        .align(Alignment.TopCenter),
                )
            }
            // Bottom half line — hidden for last stop
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight(0.5f)
                        .background(accentColor)
                        .align(Alignment.BottomCenter),
                )
            }
            // Node circle
            Box(
                modifier = Modifier
                    .size(if (isFirst || isLast) 14.dp else 10.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFirst || isLast) accentColor else accentColor.copy(alpha = 0.6f)
                    ),
            )
        }

        // Stop name + coincidence badges
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stop.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isFirst || isLast) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isFirst || isLast) colors.textPrimary else colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val otherLines = stop.routeNames.filter { it.isNotBlank() && it != currentRouteName }
            if (otherLines.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    Icon(
                        painter = painterResource(LucideIcons.ArrowLeftRight),
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = stringResource(R.string.label_coincidenza),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textTertiary,
                    )
                }
                val maxVisible = 4
                val visible = otherLines.take(maxVisible)
                val overflow = otherLines.size - visible.size
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    visible.forEach { lineName ->
                        val gtfsHex = routeColorByName[lineName]?.takeIf { it.isNotBlank() }
                        val isColored = gtfsHex != null
                        val badgeColor = gtfsHex
                            ?.let { runCatching { Color(android.graphics.Color.parseColor("#$it")) }.getOrNull() }
                            ?: accentColor.copy(alpha = 0.15f)
                        val fgColor = if (isColored) routeBadgeContrast(badgeColor) else accentColor
                        Box(
                            modifier = Modifier
                                .height(16.dp)
                                .background(badgeColor, RoundedCornerShape(3.dp))
                                .padding(horizontal = 5.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = lineName,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = fgColor,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    if (overflow > 0) {
                        Box(
                            modifier = Modifier
                                .height(16.dp)
                                .background(colors.bgSecondary, RoundedCornerShape(3.dp))
                                .padding(horizontal = 5.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "+$overflow",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = colors.textTertiary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        Icon(
            painter = painterResource(LucideIcons.ChevronRight),
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(16.dp),
        )
    }
}
