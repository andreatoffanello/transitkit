package com.transitkit.app.ui.planner

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.model.Journey
import com.transitkit.app.data.model.TransitLeg
import com.transitkit.app.data.model.WalkingLeg

/**
 * Journey detail screen — orchestrator. Delegates rendering to:
 * - [JourneyHeader] — time range + subtitle
 * - [JourneyMapPreview] / [JourneyMapFullscreen] — embedded + fullscreen map
 * - [TransitLegRow] / [WalkingLegRow] / [EndpointRow] — timeline rows
 *
 * Three-column timeline: time | node | content. Movete parity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyDetailScreen(
    journey: Journey,
    onBack: () -> Unit,
    originName: String? = null,
    destinationName: String? = null,
    userLocation: Pair<Double, Double>? = null,
) {
    val colors = TransitTheme.colors
    var expandedLegs by remember(journey.id) {
        val transitLegs = journey.legs.filterIsInstance<TransitLeg>()
        // Auto-expand intermediates for direct (single-transit-leg) journeys.
        val initial = if (transitLegs.size == 1) setOf(transitLegs[0].tripId) else emptySet()
        mutableStateOf(initial)
    }
    var showFullscreenMap by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.planner_detail_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(LucideIcons.ArrowLeft),
                            contentDescription = stringResource(R.string.cd_indietro),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.textPrimary,
                    navigationIconContentColor = colors.textPrimary,
                ),
            )
        },
        containerColor = colors.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            item { JourneyHeader(journey) }
            item {
                Spacer(Modifier.height(14.dp))
                JourneyMapPreview(
                    journey = journey,
                    userLocation = userLocation,
                    onTap = { showFullscreenMap = true },
                )
                Spacer(Modifier.height(18.dp))
            }

            // Bookend — origin when first leg is a walk and user picked a free location
            if (journey.legs.firstOrNull() is WalkingLeg && originName != null) {
                item {
                    EndpointRow(
                        timeMs = journey.departureTime,
                        name = originName,
                        role = EndpointRole.Origin,
                    )
                }
            }

            itemsIndexed(journey.legs) { idx, leg ->
                when (leg) {
                    is TransitLeg -> {
                        val prevTransit = journey.legs.getOrNull(idx - 1) as? TransitLeg
                        val nextTransit = journey.legs.getOrNull(idx + 1) as? TransitLeg
                        val isDirectTransfer = prevTransit?.alightStop?.id == leg.boardStop.id &&
                            prevTransit != null
                        val isBeforeTransfer = nextTransit?.boardStop?.id == leg.alightStop.id &&
                            nextTransit != null

                        TransitLegRow(
                            leg = leg,
                            isFirst = idx == 0,
                            isLast = idx == journey.legs.lastIndex,
                            isDirectTransfer = isDirectTransfer,
                            isBeforeTransfer = isBeforeTransfer,
                            isExpanded = expandedLegs.contains(leg.tripId),
                            onToggleExpand = {
                                expandedLegs = if (expandedLegs.contains(leg.tripId))
                                    expandedLegs - leg.tripId else expandedLegs + leg.tripId
                            },
                        )
                    }
                    is WalkingLeg -> WalkingLegRow(leg = leg)
                }
            }

            // Bookend — destination when last leg is a walk
            if (journey.legs.lastOrNull() is WalkingLeg && destinationName != null) {
                item {
                    EndpointRow(
                        timeMs = journey.arrivalTime,
                        name = destinationName,
                        role = EndpointRole.Destination,
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }

        if (showFullscreenMap) {
            JourneyMapFullscreen(
                journey = journey,
                userLocation = userLocation,
                onClose = { showFullscreenMap = false },
            )
        }
    }
}
