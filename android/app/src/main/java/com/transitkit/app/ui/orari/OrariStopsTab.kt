package com.transitkit.app.ui.orari

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.model.ResolvedStop

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun StopsTab(
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
