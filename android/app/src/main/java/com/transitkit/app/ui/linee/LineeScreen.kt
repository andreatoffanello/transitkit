package com.transitkit.app.ui.linee

import androidx.compose.ui.res.painterResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitColors
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.model.ScheduleRoute
import com.transitkit.app.ui.components.TransitSearchBar
import com.transitkit.app.ui.orari.OrariTab
import com.transitkit.app.ui.orari.OrariViewModel
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.geometry.Offset

@Composable
fun LineeScreen(
    onNavigateToLine: (routeId: String) -> Unit = {},
    onNavigateToStop: (stopId: String, stopName: String) -> Unit = { _, _ -> },
    viewModel: OrariViewModel = hiltViewModel(),
) {
    val colors = TransitTheme.colors
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val routes by viewModel.routes.collectAsStateWithLifecycle()
    val stopNamesByRouteId by viewModel.stopNamesByRouteId.collectAsStateWithLifecycle()
    val recentRouteIds by viewModel.recentRouteIds.collectAsStateWithLifecycle()
    val favoriteRouteIds by viewModel.favoriteRouteIds.collectAsStateWithLifecycle()
    val liveCountByRouteId by viewModel.liveCountByRouteId.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val scheduleLoadError by viewModel.scheduleLoadError.collectAsStateWithLifecycle()

    // Force the viewmodel into LINES mode
    LaunchedEffect(Unit) {
        viewModel.onTabSelected(OrariTab.LINES)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Text(
            text = stringResource(R.string.tab_linee),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
            ),
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        if (scheduleLoadError != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.realtimeRed.copy(alpha = 0.10f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(LucideIcons.WifiOff), contentDescription = null, tint = colors.realtimeRed, modifier = Modifier.size(18.dp))
                Text(scheduleLoadError!!, style = MaterialTheme.typography.bodySmall, color = colors.realtimeRed)
            }
        }

        // Search bar
        TransitSearchBar(
            query = searchQuery,
            placeholder = stringResource(R.string.search_placeholder_lines),
            onQueryChange = viewModel::onSearchQueryChanged,
            a11yTag = "search_lines",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = colors.accent,
                trackColor = colors.accent.copy(alpha = 0.12f),
            )
        }

        // Lines content
        LineeContent(
            routes = routes,
            stopNamesByRouteId = stopNamesByRouteId,
            query = searchQuery,
            colors = colors,
            recentRouteIds = recentRouteIds,
            favoriteRouteIds = favoriteRouteIds,
            liveCountByRouteId = liveCountByRouteId,
            onRouteClick = { routeId ->
                viewModel.recordRouteVisit(routeId)
                onNavigateToLine(routeId)
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Lines content
// ---------------------------------------------------------------------------

@Composable
private fun LineeContent(
    routes: List<ScheduleRoute>,
    stopNamesByRouteId: Map<String, String>,
    query: String,
    colors: TransitColors,
    recentRouteIds: List<String> = emptyList(),
    favoriteRouteIds: List<String> = emptyList(),
    liveCountByRouteId: Map<String, Int> = emptyMap(),
    onRouteClick: (routeId: String) -> Unit = {},
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) keyboardController?.hide()
                return Offset.Zero
            }
        }
    }

    var collapsedTypes by remember { mutableStateOf<Set<Int>>(emptySet()) }
    val groupedRoutes: List<Pair<Int, List<ScheduleRoute>>> = remember(routes) {
        routes.groupBy { it.transitType }
            .entries.sortedBy { it.key }
            .map { it.key to it.value }
    }
    val hasMultipleTypes = groupedRoutes.size > 1
    val routeById = remember(routes) { routes.associateBy { it.id } }
    val favoriteRoutes = remember(favoriteRouteIds, routeById) {
        favoriteRouteIds.mapNotNull { routeById[it] }
    }
    val favoriteIdSet = remember(favoriteRouteIds) { favoriteRouteIds.toSet() }
    // Recents exclude routes already shown as favorites — avoid duplicate rows
    // when an item is both starred and recently opened.
    val recentRoutes = remember(recentRouteIds, routeById, favoriteIdSet) {
        recentRouteIds.mapNotNull { routeById[it] }.filter { it.id !in favoriteIdSet }
    }
    val showFavorites = query.isBlank() && favoriteRoutes.isNotEmpty()
    val showRecents = query.isBlank() && recentRoutes.isNotEmpty()
    val context = LocalContext.current
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    if (routes.isEmpty() && !showRecents && !showFavorites) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(painterResource(LucideIcons.Signpost), null, tint = colors.textTertiary, modifier = Modifier.size(48.dp))
                Text(
                    stringResource(R.string.no_lines_available),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.nestedScroll(scrollConnection),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        if (showFavorites) {
            item(key = "header_mie_linee") {
                SectionLabel(stringResource(R.string.section_mie_linee), colors)
            }
            item(key = "mie_linee_card") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.bgSecondary)
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(16.dp)),
                ) {
                    favoriteRoutes.forEachIndexed { idx, route ->
                        RouteListItem(route, stopNamesByRouteId[route.id], liveCountByRouteId[route.id] ?: 0, colors) { onRouteClick(route.id) }
                        if (idx < favoriteRoutes.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(start = 76.dp), color = colors.separator, thickness = 0.5.dp)
                        }
                    }
                }
            }
            if (showRecents) {
                item(key = "spacer_after_favorites") {
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
        if (showRecents) {
            item(key = "header_recenti") {
                SectionLabel(stringResource(R.string.section_recenti), colors)
            }
            item(key = "recenti_card") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.bgSecondary)
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(16.dp)),
                ) {
                    recentRoutes.forEachIndexed { idx, route ->
                        RouteListItem(route, stopNamesByRouteId[route.id], liveCountByRouteId[route.id] ?: 0, colors) { onRouteClick(route.id) }
                        if (idx < recentRoutes.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(start = 76.dp), color = colors.separator, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
        if (showFavorites || showRecents) {
            item(key = "divider_all") {
                HorizontalDivider(color = colors.separator, thickness = 0.5.dp, modifier = Modifier.padding(top = 8.dp))
            }
            item(key = "header_tutte") {
                SectionLabel(stringResource(R.string.section_tutte_linee), colors)
            }
        }

        if (query.isNotBlank() && routes.isNotEmpty()) {
            item {
                Text(
                    pluralStringResource(R.plurals.linee_trovate, routes.size, routes.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
        }

        if (hasMultipleTypes) {
            groupedRoutes.forEach { (type, typeRoutes) ->
                item(key = "header_$type") {
                    val isCollapsed = collapsedTypes.contains(type)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { collapsedTypes = if (isCollapsed) collapsedTypes - type else collapsedTypes + type }
                            .padding(horizontal = 4.dp)
                            .height(44.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(painter = painterResource(transitTypeIcon(listOf(type))), contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                        Text(
                            transitTypeLabel(type, context),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textSecondary,
                        )
                        Box(
                            modifier = Modifier
                                .background(colors.textTertiary.copy(alpha = 0.1f), RoundedCornerShape(50))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text("${typeRoutes.size}", style = MaterialTheme.typography.labelSmall, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textTertiary)
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(
                            painterResource(if (isCollapsed) LucideIcons.ChevronRight else LucideIcons.ChevronDown),
                            null, tint = colors.textTertiary, modifier = Modifier.size(18.dp),
                        )
                    }
                }
                item(key = "content_$type") {
                    val isCollapsed = collapsedTypes.contains(type)
                    AnimatedVisibility(
                        visible = !isCollapsed,
                        enter = fadeIn(tween(200)) + expandVertically(tween(200, easing = FastOutSlowInEasing)),
                        exit = fadeOut(tween(150)) + shrinkVertically(tween(150)),
                    ) {
                        Column {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(colors.bgSecondary)
                                    .border(1.dp, colors.glassBorder, RoundedCornerShape(16.dp)),
                            ) {
                                typeRoutes.forEachIndexed { idx, route ->
                                    RouteListItem(route, stopNamesByRouteId[route.id], liveCountByRouteId[route.id] ?: 0, colors) { onRouteClick(route.id) }
                                    if (idx < typeRoutes.lastIndex) {
                                        HorizontalDivider(modifier = Modifier.padding(start = 76.dp), color = colors.separator, thickness = 0.5.dp)
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
        } else {
            items(routes, key = { it.id }) { route ->
                val idx = routes.indexOf(route)
                val isFirst = idx == 0; val isLast = idx == routes.lastIndex
                val r = 16.dp
                val shape = RoundedCornerShape(
                    topStart = if (isFirst) r else 0.dp, topEnd = if (isFirst) r else 0.dp,
                    bottomStart = if (isLast) r else 0.dp, bottomEnd = if (isLast) r else 0.dp,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(colors.bgSecondary)
                        .border(1.dp, colors.glassBorder, shape),
                ) {
                    RouteListItem(route, stopNamesByRouteId[route.id], liveCountByRouteId[route.id] ?: 0, colors) { onRouteClick(route.id) }
                    if (!isLast) {
                        HorizontalDivider(modifier = Modifier.padding(start = 76.dp), color = colors.separator, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Section label
// ---------------------------------------------------------------------------

@Composable
private fun SectionLabel(text: String, colors: TransitColors) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
        fontWeight = FontWeight.SemiBold,
        color = colors.textTertiary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
    )
}

// ---------------------------------------------------------------------------
// Route list item
// ---------------------------------------------------------------------------

@Composable
private fun RouteListItem(
    route: ScheduleRoute,
    stopSequence: String?,
    liveCount: Int,
    colors: TransitColors,
    onClick: () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .semantics { contentDescription = "route_row_${route.id}" }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        com.transitkit.app.ui.components.LineBadge(
            route = route,
            // iOS LinesListView parity: route rows are Large without icon
            // (the modal chip is rendered separately in the row metadata).
            size = com.transitkit.app.ui.components.LineBadgeSize.Large,
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = route.longName.ifBlank { route.name },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (route.directions.size > 1) {
                    Text("↔ ${route.directions.size}", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                }
            }
            val seq = stopSequence?.takeIf { it.isNotBlank() }
            if (seq != null) {
                Text(
                    text = seq,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                route.directions.firstOrNull()?.let { dir ->
                    val subtitle = dir.headsign
                    if (subtitle.isNotBlank() && !subtitle.equals(route.longName, ignoreCase = true)) {
                        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // iOS parity (item #17): live count chip statico, prima della chevron.
        // STATICO — niente pulse animation. RouteListItem vive in LazyColumn
        // che ricompone periodicamente (vehicle store refresh); un
        // `infiniteRepeatable` qui causerebbe layout instability.
        if (liveCount > 0) {
            LiveCountStaticBadge(count = liveCount, colors = colors)
        }
        Icon(painterResource(LucideIcons.ChevronRight), null, tint = colors.textTertiary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun LiveCountStaticBadge(count: Int, colors: TransitColors) {
    Row(
        modifier = Modifier
            .background(colors.realtimeGreen.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(colors.realtimeGreen, androidx.compose.foundation.shape.CircleShape),
        )
        Text(
            text = pluralStringResource(R.plurals.lines_live_count, count, count),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.realtimeGreen,
        )
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------


private fun transitTypeIcon(types: List<Int>): Int = when (types.firstOrNull()) {
    0 -> LucideIcons.Train
    1, 2 -> LucideIcons.Train
    4 -> LucideIcons.Ship
    else -> LucideIcons.BusFront
}

private fun transitTypeLabel(type: Int, context: android.content.Context): String = when (type) {
    0 -> context.getString(R.string.transit_type_tram)
    1 -> context.getString(R.string.transit_type_metropolitana)
    2 -> context.getString(R.string.transit_type_treno)
    4 -> context.getString(R.string.transit_type_traghetto)
    else -> context.getString(R.string.transit_type_bus)
}
