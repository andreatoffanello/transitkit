package com.transitkit.app.ui.mappa

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.transitkit.app.R
import com.transitkit.app.config.LocalTransitColors
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.data.model.ScheduleRoute
import com.transitkit.app.ui.components.HideBottomBarWhileVisible
import com.transitkit.app.ui.components.LineBadge
import com.transitkit.app.ui.components.LineBadgeSize
import com.transitkit.app.ui.components.RouteRow
import com.transitkit.app.ui.components.TransitSearchBar
import com.transitkit.app.ui.components.fuzzyScore

/**
 * Line + stop picker — full-screen overlay shown above the map. Lives inside
 * [MappaScreen]'s composition so it inherits the activity's status-bar
 * chrome, and hides the activity tab bar for as long as it's visible via
 * [HideBottomBarWhileVisible] so the picker reads as a second-level screen.
 *
 * Blank query: per-transit-type grouped sections with [RouteRow].
 * Typing: fuzzy-matched lines (first, precedence) + stops under "FERMATE" header.
 *
 * Tap a row → caller updates view-model + closes overlay. Back gesture also
 * dismisses (see [BackHandler]).
 */
@Composable
internal fun LinePickerSheet(
    routes: List<ScheduleRoute>,
    stops: List<ResolvedStop>,
    liveCounts: Map<String, Int>,
    stopSequences: Map<String, String>,
    onDismiss: () -> Unit,
    onSelectRoute: (routeId: String) -> Unit,
    onSelectStop: (ResolvedStop) -> Unit,
) {
    HideBottomBarWhileVisible()
    BackHandler(onBack = onDismiss)

    val colors = LocalTransitColors.current
    var query by remember { mutableStateOf("") }

    // Blank-state: live-first sort within each section.
    val sortedRoutes = remember(routes, liveCounts) {
        routes.sortedWith(
            compareByDescending<ScheduleRoute> { liveCounts[it.id] ?: 0 }
                .thenBy { it.name }
        )
    }

    // Search results (query non-blank).
    val lineResults = remember(query, routes, liveCounts) {
        if (query.isBlank()) emptyList()
        else if (query.length < 2) routes.filter { r ->
            r.name.contains(query, ignoreCase = true) || r.longName.contains(query, ignoreCase = true)
        }
        else routes
            .filter { r -> maxOf(fuzzyScore(r.longName.ifBlank { r.name }, query), fuzzyScore(r.name, query)) > 0 }
            .sortedByDescending { r -> maxOf(fuzzyScore(r.longName.ifBlank { r.name }, query), fuzzyScore(r.name, query)) }
    }

    val stopResults = remember(query, stops) {
        if (query.isBlank()) emptyList()
        else if (query.length < 2) stops.filter { it.name.contains(query, ignoreCase = true) }
        else stops
            .map { stop -> stop to fuzzyScore(stop.name, query) }
            .filter { (_, score) -> score > 0 }
            .sortedByDescending { (_, score) -> score }
            .map { (stop, _) -> stop }
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
                capitalization = KeyboardCapitalization.None,
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
                        val routesForType = sortedRoutes.filter { it.transitType == type }
                        if (routesForType.isNotEmpty()) {
                            item(key = "header_$type") {
                                SectionHeader(type)
                            }
                            items(routesForType, key = { it.id }) { route ->
                                RouteRow(
                                    route = route,
                                    stopSequence = stopSequences[route.id],
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
                    val other = sortedRoutes.filter { it.transitType !in sectionOrder }
                    if (other.isNotEmpty()) {
                        item(key = "header_other") { SectionHeader(-1) }
                        items(other, key = { it.id }) { route ->
                            RouteRow(
                                route = route,
                                stopSequence = stopSequences[route.id],
                                liveCount = liveCounts[route.id] ?: 0,
                                onClick = {
                                    onSelectRoute(route.id)
                                    onDismiss()
                                },
                            )
                        }
                    }
                } else {
                    // Lines first (precedence), then stops under a section header.
                    val hasLines = lineResults.isNotEmpty()
                    val hasStops = stopResults.isNotEmpty()

                    if (hasLines && hasStops) {
                        item(key = "header_lines") {
                            SectionHeader(type = null, label = stringResource(R.string.mappa_line_picker_section_lines))
                        }
                    }
                    items(lineResults, key = { "line_${it.id}" }) { route ->
                        RouteRow(
                            route = route,
                            stopSequence = stopSequences[route.id],
                            liveCount = liveCounts[route.id] ?: 0,
                            onClick = {
                                onSelectRoute(route.id)
                                onDismiss()
                            },
                        )
                    }
                    if (hasStops) {
                        item(key = "header_stops") {
                            SectionHeader(type = null, label = stringResource(R.string.mappa_line_picker_section_stops))
                        }
                        items(stopResults, key = { "stop_${it.id}" }) { stop ->
                            StopPickerRow(stop = stop, onClick = {
                                onSelectStop(stop)
                                onDismiss()
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(type: Int? = null, label: String? = null) {
    val colors = LocalTransitColors.current
    val text = label ?: when (type) {
        0 -> stringResource(R.string.mappa_line_picker_section_tram)
        1 -> stringResource(R.string.mappa_line_picker_section_metro)
        2 -> stringResource(R.string.mappa_line_picker_section_rail)
        3 -> stringResource(R.string.mappa_line_picker_section_bus)
        4 -> stringResource(R.string.mappa_line_picker_section_ferry)
        else -> stringResource(R.string.mappa_line_picker_section_other)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = colors.textTertiary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp),
    )
}

@Composable
private fun StopPickerRow(stop: ResolvedStop, onClick: () -> Unit) {
    val colors = LocalTransitColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics { testTag = "line_picker_stop_${stop.id}" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stop.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val routeNames = stop.routeNames.take(6)
            val routeColorHexes = stop.routeColors.take(6)
            if (routeNames.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    routeNames.forEachIndexed { i, n ->
                        LineBadge(
                            name = n,
                            colorHex = routeColorHexes.getOrElse(i) { "" },
                            size = LineBadgeSize.Small,
                        )
                    }
                    if (stop.routeNames.size > 6) {
                        Text(
                            text = "+${stop.routeNames.size - 6}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textTertiary,
                            modifier = Modifier
                                .background(colors.textTertiary.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }
    }
}
