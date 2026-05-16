package com.transitkit.app.ui.orari

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.config.toColor
import com.transitkit.app.data.model.ResolvedDeparture

@Composable
internal fun FullScheduleSheet(
    stopName: String,
    departuresByGroup: Map<String, List<ResolvedDeparture>>,
    onDismiss: () -> Unit,
) {
    val colors = TransitTheme.colors
    val sortedGroups = remember(departuresByGroup) { departuresByGroup.keys.toList() }

    var selectedGroup by remember(sortedGroups) { mutableStateOf(sortedGroups.firstOrNull()) }
    var filterRouteId by remember { mutableStateOf<String?>(null) }

    // Reset route filter when day group changes
    LaunchedEffect(selectedGroup) { filterRouteId = null }

    val groupDepartures = remember(selectedGroup, departuresByGroup) {
        departuresByGroup[selectedGroup] ?: emptyList()
    }
    val availableRoutes = remember(groupDepartures) { groupDepartures.distinctBy { it.routeId } }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
        // Header with back chevron
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.semantics { contentDescription = "btn_close_schedule" },
            ) {
                Icon(painterResource(LucideIcons.ChevronLeft), contentDescription = null, tint = colors.textPrimary)
            }
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
        } // end Column
    } // end Surface
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
