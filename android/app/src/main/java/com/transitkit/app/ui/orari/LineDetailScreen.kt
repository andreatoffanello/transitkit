package com.transitkit.app.ui.orari

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme

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
