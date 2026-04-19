package com.transitkit.app.ui.orari

import androidx.compose.ui.res.painterResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.transitkit.app.config.LucideIcons
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.transitkit.app.R
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.transitkit.app.config.TransitColors
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.data.model.ScheduleRoute
import kotlinx.coroutines.delay

@Composable
fun OrariScreen(
    onNavigateToStop: (stopId: String, stopName: String) -> Unit = { _, _ -> },
    onNavigateToLine: (routeId: String) -> Unit = { },
    viewModel: OrariViewModel = hiltViewModel(),
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val stops by viewModel.stops.collectAsStateWithLifecycle()
    val recentStopIds by viewModel.recentStopIds.collectAsStateWithLifecycle()
    val selectedTransitType by viewModel.selectedTransitType.collectAsStateWithLifecycle()
    val availableTransitTypes by viewModel.availableTransitTypes.collectAsStateWithLifecycle()
    val groupedStops by viewModel.groupedStops.collectAsStateWithLifecycle()
    val scheduleLoadError by viewModel.scheduleLoadError.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val colors = TransitTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
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
        OrariSearchBar(
            query = searchQuery,
            placeholder = stringResource(R.string.search_placeholder_stops),
            onQueryChange = viewModel::onSearchQueryChanged,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        // Loading indicator — shown below search bar during initial data load
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = colors.accent,
                trackColor = colors.accent.copy(alpha = 0.12f),
            )
        }

        HorizontalDivider(color = colors.separator, thickness = 0.5.dp)

        StopsTab(
            stops = stops,
            groupedStops = groupedStops,
            searchQuery = searchQuery,
            recentStopIds = recentStopIds,
            availableTransitTypes = availableTransitTypes,
            selectedTransitType = selectedTransitType,
            onTransitTypeSelected = viewModel::selectTransitType,
            onStopClick = { stop ->
                viewModel.recordStopVisit(stop.id)
                onNavigateToStop(stop.id, stop.name)
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Search bar
// ---------------------------------------------------------------------------

@Composable
private fun OrariSearchBar(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TransitTheme.colors
    val focusManager = LocalFocusManager.current
    val isDark = isSystemInDarkTheme()
    val searchBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
    val searchBorder = colors.accent.copy(alpha = 0.4f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(searchBg)
            .border(1.dp, searchBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
    ) {
        Icon(
            painter = painterResource(LucideIcons.Search),
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    style = TextStyle(color = colors.textSecondary, fontSize = 15.sp),
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = colors.textPrimary, fontSize = 15.sp),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "search_schedules" },
            )
        }
        if (query.isNotEmpty()) {
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier
                    .size(20.dp)
                    .semantics { contentDescription = "btn_clear_search" },
            ) {
                Icon(
                    painter = painterResource(LucideIcons.X),
                    contentDescription = stringResource(R.string.search_clear),
                    tint = colors.textTertiary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Stops tab
// ---------------------------------------------------------------------------

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun StopsTab(
    stops: List<ResolvedStop>,
    groupedStops: Map<Int, List<ResolvedStop>>,
    searchQuery: String,
    recentStopIds: List<String>,
    availableTransitTypes: List<Int>,
    selectedTransitType: Int?,
    onTransitTypeSelected: (Int?) -> Unit,
    onStopClick: (ResolvedStop) -> Unit,
) {
    val colors = TransitTheme.colors
    val haptic = LocalHapticFeedback.current
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

    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    Column(modifier = Modifier.fillMaxSize()) {
        // Transit type filter chips — only shown when there are multiple types
        if (availableTransitTypes.size > 1) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 6.dp),
            ) {
                item(key = "filter_all") {
                    FilterChip(
                        selected = selectedTransitType == null,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTransitTypeSelected(null)
                        },
                        label = { Text(stringResource(R.string.filter_all), style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.accent,
                            selectedLabelColor = Color.White,
                            containerColor = colors.accent.copy(alpha = 0.18f),
                            labelColor = colors.accent,
                        ),
                        border = null,
                        modifier = Modifier.semantics { contentDescription = "filter_type_all" },
                    )
                }
                items(availableTransitTypes, key = { "filter_type_$it" }) { type ->
                    val label = when (type) {
                        0 -> stringResource(R.string.transit_type_tram)
                        1 -> stringResource(R.string.transit_type_metro)
                        2 -> stringResource(R.string.transit_type_treno)
                        3 -> stringResource(R.string.transit_type_bus)
                        4 -> stringResource(R.string.transit_type_ferry)
                        else -> stringResource(R.string.transit_type_unknown, type)
                    }
                    FilterChip(
                        selected = selectedTransitType == type,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTransitTypeSelected(type)
                        },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(transitTypeIcon(listOf(type))),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.accent,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = colors.accent.copy(alpha = 0.18f),
                            labelColor = colors.accent,
                            iconColor = colors.accent,
                        ),
                        border = null,
                        modifier = Modifier.semantics { contentDescription = "filter_type_$type" },
                    )
                }
            }
        }

        if (stops.isEmpty() && searchQuery.isNotBlank()) {
            // Empty state — no results for query
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 40.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    painter = painterResource(LucideIcons.Search),
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(40.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.empty_stops_query, searchQuery),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
            return@Column
        }

        // Resolve recent stops only when query is blank and history is non-empty
        val stopById = remember(stops) { stops.associateBy { it.id } }
        val recentStops = remember(recentStopIds, stopById) {
            recentStopIds.take(3).mapNotNull { stopById[it] }
        }
        val showRecents = searchQuery.isBlank() && recentStops.isNotEmpty()
        val showGrouped = groupedStops.size > 1

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .nestedScroll(scrollConnection),
            contentPadding = PaddingValues(bottom = 88.dp),
        ) {
        if (showRecents) {
            item(key = "header_recenti") {
                Text(
                    text = stringResource(R.string.section_recenti),
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            itemsIndexed(recentStops, key = { _, stop -> "recent_${stop.id}" }) { index, stop ->
                StaggeredStopCard(
                    stop = stop,
                    index = index,
                    animate = searchQuery.isBlank(),
                    onClick = { onStopClick(stop) },
                )
            }
            item(key = "divider_all") {
                HorizontalDivider(
                    color = colors.separator,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            item(key = "header_tutte") {
                Text(
                    text = stringResource(R.string.section_tutte_fermate),
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        if (searchQuery.isNotBlank() && stops.isNotEmpty()) {
            item {
                Text(
                    text = pluralStringResource(R.plurals.fermate_trovate, stops.size, stops.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }

        if (showGrouped) {
            groupedStops.forEach { (transitType, typeStops) ->
                stickyHeader(key = "header_$transitType") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.background)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            painter = painterResource(transitTypeIcon(listOf(transitType))),
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = transitTypeLabel(transitType, context),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textSecondary,
                            letterSpacing = 0.5.sp,
                        )
                        Text(
                            text = "${typeStops.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textTertiary,
                        )
                    }
                }
                itemsIndexed(typeStops, key = { _, s -> s.id }) { index, stop ->
                    StaggeredStopCard(
                        stop = stop,
                        index = index,
                        animate = false,
                        onClick = { onStopClick(stop) },
                    )
                }
            }
        } else {
            itemsIndexed(stops, key = { _, stop -> stop.id }) { index, stop ->
                StaggeredStopCard(
                    stop = stop,
                    index = index,
                    animate = searchQuery.isBlank(),
                    onClick = { onStopClick(stop) },
                )
            }
        }
    } // LazyColumn
    } // Column
}

// ---------------------------------------------------------------------------
// Transit type icon helper
// ---------------------------------------------------------------------------


private fun transitTypeIcon(types: List<Int>): Int {
    return when (types.firstOrNull() ?: 3) {
        0 -> LucideIcons.Train
        1 -> LucideIcons.Train
        2 -> LucideIcons.Train
        4 -> LucideIcons.Ship
        else -> LucideIcons.Bus
    }
}

private fun transitTypeLabel(type: Int, context: android.content.Context): String = when (type) {
    0 -> context.getString(R.string.transit_type_tram)
    1 -> context.getString(R.string.transit_type_metropolitana)
    2 -> context.getString(R.string.transit_type_treno)
    4 -> context.getString(R.string.transit_type_traghetto)
    else -> context.getString(R.string.transit_type_bus)
}

// ---------------------------------------------------------------------------
// Stop card — staggered fade-in, glass surface
// ---------------------------------------------------------------------------

private val StopEntranceEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

@Composable
private fun StaggeredStopCard(
    stop: ResolvedStop,
    index: Int,
    animate: Boolean,
    onClick: () -> Unit,
) {
    val colors = TransitTheme.colors
    val shouldAnimate = animate && index <= 15
    var appeared by rememberSaveable { mutableStateOf(!shouldAnimate) }

    LaunchedEffect(stop.id, animate) {
        if (shouldAnimate) {
            appeared = false
            delay(index.toLong() * 30L)
            appeared = true
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = StopEntranceEasing),
        label = "stop_alpha",
    )
    val offsetY by animateFloatAsState(
        targetValue = if (appeared) 0f else 10f,
        animationSpec = tween(durationMillis = 300, easing = StopEntranceEasing),
        label = "stop_offsetY",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                translationY = offsetY * density
            }
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.glassFill)
            .border(1.dp, colors.glassBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .semantics { contentDescription = "stop_row_${stop.id}" },
    ) {
        // Transit type icon chip
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.accent.copy(alpha = 0.10f)),
        ) {
            Icon(
                painter = painterResource(transitTypeIcon(stop.transitTypes)),
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stop.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
                maxLines = 1,
            )
            val routes = stop.routeNames.take(6)
            val routeColorHexes = stop.routeColors.take(6)
            if (routes.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(routes.size) { i ->
                        com.transitkit.app.ui.components.LineBadge(
                            name = routes[i],
                            colorHex = routeColorHexes.getOrElse(i) { "" },
                            size = com.transitkit.app.ui.components.LineBadgeSize.Small,
                        )
                    }
                    if (stop.routeNames.size > 6) {
                        item {
                            val colors = TransitTheme.colors
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

        Icon(
            painter = painterResource(LucideIcons.Star),
            contentDescription = stringResource(R.string.cd_salva_fermata),
            tint = colors.textTertiary,
            modifier = Modifier
                .size(18.dp)
                .padding(end = 4.dp),
        )

        Icon(
            painter = painterResource(LucideIcons.ChevronRight),
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(18.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Route chip
// ---------------------------------------------------------------------------

@Composable
private fun RouteChip(routeName: String, color: Color = TransitTheme.colors.accent) {
    val fg = routeBadgeContrast(color)
    Box(
        modifier = Modifier
            .background(color, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = routeName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg,
            maxLines = 1,
        )
    }
}

/** WCAG relative-luminance-based foreground picker for GTFS route badge backgrounds. */
private fun routeBadgeContrast(bg: Color): Color {
    fun lin(c: Float): Float = if (c <= 0.03928f) c / 12.92f else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    val l = 0.2126f * lin(bg.red) + 0.7152f * lin(bg.green) + 0.0722f * lin(bg.blue)
    return if (l < 0.5f) Color.White else Color(0xFF111827)
}

// ---------------------------------------------------------------------------
// Lines tab
// ---------------------------------------------------------------------------

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
                    painterResource(LucideIcons.Bus),
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
            size = com.transitkit.app.ui.components.LineBadgeSize.Medium,
            showTransitIcon = true,
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
