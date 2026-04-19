package com.transitkit.app.ui.orari

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.transitkit.app.config.LucideIcons
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.transitkit.app.R
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.model.StopTime
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    onBack: () -> Unit,
    onNavigateToStop: (stopId: String) -> Unit,
    viewModel: TripDetailViewModel = hiltViewModel(),
) {
    val tripState by viewModel.tripState.collectAsStateWithLifecycle()
    val stopCoincidences by viewModel.stopCoincidences.collectAsStateWithLifecycle()
    val routeColorByName by viewModel.routeColorByName.collectAsStateWithLifecycle()
    val accentColor = TransitTheme.colors.accent
    val lineColor = remember(viewModel.routeColor, accentColor) {
        runCatching {
            Color(android.graphics.Color.parseColor("#${viewModel.routeColor}"))
        }.getOrDefault(accentColor)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (viewModel.routeName.isNotBlank()) {
                            com.transitkit.app.ui.components.LineBadge(
                                name = viewModel.routeName,
                                colorHex = viewModel.routeColor.takeIf { it.isNotBlank() },
                                size = com.transitkit.app.ui.components.LineBadgeSize.Medium,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(lineColor, CircleShape),
                            )
                        }
                        Column {
                            Text(
                                text = viewModel.headsign.ifBlank { stringResource(R.string.trip_detail_title_fallback) },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = TransitTheme.colors.textPrimary,
                            )
                            if (tripState is TripState.Success) {
                                Text(
                                    text = stringResource(R.string.stop_count_fermate, (tripState as TripState.Success).stops.size),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TransitTheme.colors.textSecondary,
                                )
                            }
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
                            contentDescription = stringResource(R.string.cd_indietro_trip),
                            tint = TransitTheme.colors.textPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TransitTheme.colors.background,
                ),
            )
        },
        containerColor = TransitTheme.colors.background,
    ) { padding ->
        AnimatedContent(
            targetState = tripState,
            transitionSpec = {
                fadeIn(tween(durationMillis = 220, easing = FastOutSlowInEasing)) togetherWith
                fadeOut(tween(durationMillis = 150))
            },
            label = "tripStateTransition",
        ) { state ->
            when (state) {
                is TripState.Loading -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = lineColor)
                }

                is TripState.Error -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(state.messageRes),
                        color = TransitTheme.colors.textTertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp),
                    )
                }

                is TripState.Success -> {
                    val liveOriginIndex by viewModel.liveOriginIndex.collectAsStateWithLifecycle()
                    val listState = rememberLazyListState()
                    // Scroll once to the initial origin on first successful load.
                    // Subsequent vehicle movements update the highlight but keep
                    // the user's scroll position — jumping the list on every
                    // poll would be disorienting.
                    LaunchedEffect(Unit) {
                        delay(150)
                        listState.animateScrollToItem(liveOriginIndex.coerceAtLeast(0))
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        itemsIndexed(
                            items = state.stops,
                            key = { _, stop -> "${stop.stopId}_${stop.departureTime}" },
                        ) { index, stop ->
                            TripStopRow(
                                stop = stop,
                                index = index,
                                originIndex = liveOriginIndex,
                                isFirst = index == 0,
                                isLast = index == state.stops.lastIndex,
                                lineColor = lineColor,
                                coincidences = stopCoincidences[stop.stopId] ?: emptyList(),
                                routeColorByName = routeColorByName,
                                onClick = { onNavigateToStop(stop.stopId) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TripStopRow(
    stop: StopTime,
    index: Int,
    originIndex: Int,
    isFirst: Boolean,
    isLast: Boolean,
    lineColor: Color,
    coincidences: List<String>,
    routeColorByName: Map<String, String> = emptyMap(),
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val isPast = index < originIndex
    val isCurrent = index == originIndex
    val isTerminal = isFirst || isLast

    val dotColor = if (isPast) lineColor.copy(alpha = 0.45f) else lineColor
    val dotSize = when {
        isCurrent -> 14.dp
        isTerminal -> 12.dp
        else -> 8.dp
    }
    val textColor = when {
        isPast -> TransitTheme.colors.textTertiary
        isCurrent -> lineColor
        else -> TransitTheme.colors.textPrimary
    }
    val rowBg = if (isCurrent) lineColor.copy(alpha = 0.08f) else Color.Transparent
    val lineAboveColor = if (isPast) lineColor.copy(alpha = 0.35f) else lineColor
    // Line below: dimmed if current stop (next segment is future) — stays full below origin
    val lineBelowColor = if (isPast) lineColor.copy(alpha = 0.35f) else lineColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .then(
                if (isCurrent) {
                    Modifier.drawWithContent {
                        drawContent()
                        drawRect(
                            color = lineColor,
                            topLeft = Offset.Zero,
                            size = Size(3.dp.toPx(), size.height),
                        )
                    }
                } else Modifier
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .semantics { contentDescription = "trip_stop_row_${stop.stopId}" }
            .padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Timeline column
        Box(
            modifier = Modifier.width(52.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Line above (not for first)
                if (!isFirst) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(12.dp)
                            .background(lineAboveColor),
                    )
                }
                // Dot
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .background(dotColor, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isTerminal && !isCurrent) {
                        // Inner ring for terminal stops — matches screen background for hollow effect
                        Box(
                            modifier = Modifier
                                .size(dotSize - 4.dp)
                                .background(TransitTheme.colors.background, CircleShape),
                        )
                        Box(
                            modifier = Modifier
                                .size(dotSize - 8.dp)
                                .background(dotColor, CircleShape),
                        )
                    } else if (isCurrent) {
                        // Inner circle for current stop — matches screen background
                        Box(
                            modifier = Modifier
                                .size(dotSize - 6.dp)
                                .background(TransitTheme.colors.background, CircleShape),
                        )
                    }
                }
                // Line below (not for last)
                if (!isLast) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(12.dp)
                            .background(lineBelowColor),
                    )
                }
            }
        }

        // Content column
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stop.stopName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isTerminal || isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    color = textColor,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .background(lineColor, RoundedCornerShape(50))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.trip_stop_attuale),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
                Text(
                    text = stop.departureTime.take(5),
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    color = if (isPast) TransitTheme.colors.textTertiary else TransitTheme.colors.textSecondary,
                )
            }
            if (!isPast && coincidences.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 3.dp),
                ) {
                    Icon(
                        painter = painterResource(LucideIcons.ArrowLeftRight),
                        contentDescription = null,
                        tint = TransitTheme.colors.textTertiary,
                        modifier = Modifier.size(11.dp),
                    )
                    coincidences.take(4).forEach { coincidenceRoute ->
                        val badgeColor = routeColorByName[coincidenceRoute]?.takeIf { it.isNotBlank() }
                            ?.let { runCatching { Color(android.graphics.Color.parseColor("#$it")) }.getOrNull() }
                            ?: lineColor.copy(alpha = 0.15f)
                        val isColored = routeColorByName[coincidenceRoute]?.isNotBlank() == true
                        Box(
                            modifier = Modifier
                                .height(16.dp)
                                .background(if (isColored) badgeColor.copy(alpha = 0.9f) else badgeColor, RoundedCornerShape(3.dp))
                                .padding(horizontal = 5.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = coincidenceRoute,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = if (isColored) Color.White else lineColor,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }

        Icon(
            painter = painterResource(LucideIcons.ChevronRight),
            contentDescription = null,
            tint = if (isPast)
                TransitTheme.colors.textTertiary.copy(alpha = 0.5f)
            else
                TransitTheme.colors.textTertiary,
            modifier = Modifier.size(16.dp),
        )
    }
}
