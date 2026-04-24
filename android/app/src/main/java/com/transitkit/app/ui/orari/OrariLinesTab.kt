package com.transitkit.app.ui.orari

// ---------------------------------------------------------------------------
// DEAD CODE — kept behavior-preserving during split from OrariScreen.kt.
// `LinesTab` and this file's `RouteListItem` are NOT referenced by the current
// `OrariScreen` orchestrator (only `StopsTab` is wired). The canonical route
// list lives in `ui/linee/LineeScreen.kt` (has its own private `RouteListItem`).
// Left in place intentionally per split task instructions ("NO rimozione dead
// code, annota"). Candidate for removal in a follow-up cleanup pass.
// ---------------------------------------------------------------------------

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitColors
import com.transitkit.app.data.model.ScheduleRoute

@Composable
private fun LinesTab(
    routes: List<ScheduleRoute>,
    stopNamesByRouteId: Map<String, String>,
    query: String,
    colors: TransitColors,
    recentRouteIds: List<String> = emptyList(),
    onRouteClick: (routeId: String) -> Unit = {},
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
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
            .entries
            .sortedBy { it.key }
            .map { it.key to it.value }
    }
    val hasMultipleTypes = groupedRoutes.size > 1

    val routeById = remember(routes) { routes.associateBy { it.id } }
    val recentRoutes = remember(recentRouteIds, routeById) {
        recentRouteIds.mapNotNull { routeById[it] }
    }
    val showRecents = query.isBlank() && recentRoutes.isNotEmpty()
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    if (routes.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painterResource(LucideIcons.BusFront),
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(48.dp),
                )
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
        if (showRecents) {
            item(key = "header_recenti_linee") {
                Text(
                    text = stringResource(R.string.section_recenti),
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
            item(key = "recenti_linee_card") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.bgSecondary)
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(16.dp)),
                ) {
                    recentRoutes.forEachIndexed { index, route ->
                        RouteListItem(
                            route = route,
                            stopSequence = stopNamesByRouteId[route.id],
                            colors = colors,
                            onClick = { onRouteClick(route.id) },
                        )
                        if (index < recentRoutes.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 76.dp),
                                color = colors.separator,
                                thickness = 0.5.dp,
                            )
                        }
                    }
                }
            }
            item(key = "divider_all_linee") {
                HorizontalDivider(
                    color = colors.separator,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            item(key = "header_tutte_linee") {
                Text(
                    text = stringResource(R.string.section_tutte_linee),
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
        }

        if (query.isNotBlank() && routes.isNotEmpty()) {
            item {
                Text(
                    pluralStringResource(R.plurals.linee_trovate, routes.size, routes.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
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
                            .clickable {
                                collapsedTypes = if (isCollapsed) collapsedTypes - type else collapsedTypes + type
                            }
                            .padding(horizontal = 4.dp)
                            .height(44.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            painter = painterResource(transitTypeIcon(listOf(type))),
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = transitTypeLabel(type, context),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textSecondary,
                        )
                        Box(
                            modifier = Modifier
                                .background(colors.textTertiary.copy(alpha = 0.1f), RoundedCornerShape(50))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "${typeRoutes.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textTertiary,
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            painter = painterResource(if (isCollapsed) LucideIcons.ChevronRight else LucideIcons.ChevronDown),
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(18.dp),
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
                                typeRoutes.forEachIndexed { index, route ->
                                    RouteListItem(route = route, stopSequence = stopNamesByRouteId[route.id], colors = colors, onClick = { onRouteClick(route.id) })
                                    if (index < typeRoutes.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 76.dp),
                                            color = colors.separator,
                                            thickness = 0.5.dp,
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        } else {
            // Single transit type — individual lazy items for recycling
            items(routes, key = { it.id }) { route ->
                val index = routes.indexOf(route)
                val isFirst = index == 0
                val isLast = index == routes.lastIndex
                val cornerRadius = 16.dp
                val topRadius = if (isFirst) cornerRadius else 0.dp
                val bottomRadius = if (isLast) cornerRadius else 0.dp
                val shape = RoundedCornerShape(
                    topStart = topRadius, topEnd = topRadius,
                    bottomStart = bottomRadius, bottomEnd = bottomRadius,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(colors.bgSecondary)
                        .border(1.dp, colors.glassBorder, shape),
                ) {
                    RouteListItem(
                        route = route,
                        stopSequence = stopNamesByRouteId[route.id],
                        colors = colors,
                        onClick = { onRouteClick(route.id) },
                    )
                    if (!isLast) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 76.dp),
                            color = colors.separator,
                            thickness = 0.5.dp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteListItem(
    route: ScheduleRoute,
    stopSequence: String?,
    colors: TransitColors,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = "route_row_${route.id}" }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        com.transitkit.app.ui.components.LineBadge(
            route = route,
            // iOS LinesListView parity: route rows are Large without icon.
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
                    Text(
                        text = "↔ ${route.directions.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                    )
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
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                )
            } else {
                route.directions.firstOrNull()?.let { dir ->
                    val subtitle = dir.headsign
                    if (subtitle.isNotBlank() && !subtitle.equals(route.longName, ignoreCase = true)) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        Icon(
            painterResource(LucideIcons.ChevronRight),
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(18.dp),
        )
    }
}
