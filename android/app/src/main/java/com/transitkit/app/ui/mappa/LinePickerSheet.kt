package com.transitkit.app.ui.mappa

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.transitkit.app.R
import com.transitkit.app.config.LocalTransitColors
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.data.model.ScheduleRoute
import com.transitkit.app.ui.components.HideBottomBarWhileVisible
import com.transitkit.app.ui.components.LineBadge
import com.transitkit.app.ui.components.LineBadgeSize
import com.transitkit.app.ui.components.LiveIndicator
import com.transitkit.app.ui.components.TransitSearchBar

/**
 * Line picker — full-screen overlay shown above the map. Lives inside
 * [MappaScreen]'s composition so it inherits the activity's status-bar
 * chrome, and hides the activity tab bar for as long as it's visible via
 * [HideBottomBarWhileVisible] so the picker reads as a second-level screen.
 *
 * Tap a row → caller updates view-model + closes overlay. Back gesture also
 * dismisses (see [BackHandler]).
 */
@Composable
internal fun LinePickerSheet(
    routes: List<ScheduleRoute>,
    liveCounts: Map<String, Int>,
    onDismiss: () -> Unit,
    onSelectRoute: (routeId: String) -> Unit,
) {
    HideBottomBarWhileVisible()
    BackHandler(onBack = onDismiss)

    val colors = LocalTransitColors.current
    var query by remember { mutableStateOf("") }

    // Filter by query, then sort: live-vehicle routes first, then alpha by short name.
    val filteredRoutes = remember(query, routes, liveCounts) {
        val base = if (query.isBlank()) routes
        else routes.filter { route ->
            route.name.contains(query, ignoreCase = true) ||
                route.longName.contains(query, ignoreCase = true)
        }
        base.sortedWith(
            compareByDescending<ScheduleRoute> { liveCounts[it.id] ?: 0 }
                .thenBy { it.name }
        )
    }

    // Section order mirrors movete — metro first, bus last (of the common modes), other at bottom.
    //   type 1 = metro, 2 = rail, 0 = tram, 3 = bus, 4 = ferry, other = ALTRO.
    val sectionOrder = listOf(1, 0, 2, 3, 4)

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTag = "line_picker_sheet" },
        color = colors.background,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        painter = painterResource(LucideIcons.ChevronLeft),
                        contentDescription = stringResource(R.string.cd_indietro),
                        tint = colors.textPrimary,
                    )
                }
                Text(
                    text = stringResource(R.string.mappa_line_picker_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
            }

            TransitSearchBar(
                query = query,
                placeholder = stringResource(R.string.mappa_line_picker_placeholder),
                onQueryChange = { query = it },
                a11yTag = "line_picker_search",
                capitalization = KeyboardCapitalization.Characters,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { testTag = "line_picker_list" },
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                if (query.isBlank()) {
                    sectionOrder.forEach { type ->
                        val routesForType = filteredRoutes.filter { it.transitType == type }
                        if (routesForType.isNotEmpty()) {
                            item(key = "header_$type") {
                                SectionHeader(type)
                            }
                            items(routesForType, key = { it.id }) { route ->
                                LinePickerRow(
                                    route = route,
                                    liveCount = liveCounts[route.id] ?: 0,
                                    onClick = {
                                        onSelectRoute(route.id)
                                        onDismiss()
                                    },
                                )
                            }
                        }
                    }
                    // Any routes with unknown / uncommon transit type land under "ALTRO".
                    val other = filteredRoutes.filter { it.transitType !in sectionOrder }
                    if (other.isNotEmpty()) {
                        item(key = "header_other") { SectionHeader(-1) }
                        items(other, key = { it.id }) { route ->
                            LinePickerRow(
                                route = route,
                                liveCount = liveCounts[route.id] ?: 0,
                                onClick = {
                                    onSelectRoute(route.id)
                                    onDismiss()
                                },
                            )
                        }
                    }
                } else {
                    // While typing, skip section headers for tighter results list.
                    items(filteredRoutes, key = { it.id }) { route ->
                        LinePickerRow(
                            route = route,
                            liveCount = liveCounts[route.id] ?: 0,
                            onClick = {
                                onSelectRoute(route.id)
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(type: Int) {
    val colors = LocalTransitColors.current
    val label = when (type) {
        0 -> stringResource(R.string.mappa_line_picker_section_tram)
        1 -> stringResource(R.string.mappa_line_picker_section_metro)
        2 -> stringResource(R.string.mappa_line_picker_section_rail)
        3 -> stringResource(R.string.mappa_line_picker_section_bus)
        4 -> stringResource(R.string.mappa_line_picker_section_ferry)
        else -> stringResource(R.string.mappa_line_picker_section_other)
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = colors.textTertiary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp),
    )
}

@Composable
private fun LinePickerRow(
    route: ScheduleRoute,
    liveCount: Int,
    onClick: () -> Unit,
) {
    val colors = LocalTransitColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .semantics { testTag = "line_picker_row_${route.id}" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LineBadge(route = route, size = LineBadgeSize.Medium)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = route.longName.ifBlank { route.name },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (route.longName.isNotBlank() && route.longName != route.name) {
                Text(
                    text = route.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (liveCount > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                LiveIndicator(size = 6.dp, animated = false)
                Text(
                    text = "$liveCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.realtimeGreen,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
