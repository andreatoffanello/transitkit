package com.transitkit.app.ui.planner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.model.IntermediateStop
import com.transitkit.app.data.model.Journey
import com.transitkit.app.data.model.TransitLeg
import com.transitkit.app.data.model.WalkingLeg
import com.transitkit.app.data.model.durationMinutes
import com.transitkit.app.data.model.totalWalkSeconds
import com.transitkit.app.data.model.transfers
import com.transitkit.app.ui.components.LineBadge
import com.transitkit.app.ui.components.LineBadgeSize
import com.transitkit.app.ui.components.parseHexColor

// ── Layout constants ──────────────────────────────────────────────────────────

private val K_TIME_W = 46.dp
private val K_NODE_W = 14.dp
private val K_LINE_W = 2.5.dp
private val K_DOT_D = 12.dp
private val K_COL_GAP = 12.dp
// Top padding to align the dot vertically with the time text first-line baseline.
private val K_DOT_TOP_PAD = 4.dp
private val K_TIME_TOP_PAD = 1.dp

/**
 * Three-column journey timeline (time | node | content). Bookend rows for the
 * user-picked origin / destination when the journey begins or ends with a
 * walking leg. Transit legs render board / middle / alight rows with a transfer
 * connector between consecutive transit legs sharing a stop. Walking legs are
 * a dashed vertical connector with footprints icon. Movete parity.
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
                        text = "Journey",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(LucideIcons.ArrowLeft),
                            contentDescription = "Back",
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

// ── Map preview + fullscreen ──────────────────────────────────────────────────

@Composable
private fun JourneyMapPreview(
    journey: Journey,
    userLocation: Pair<Double, Double>?,
    onTap: () -> Unit,
) {
    val colors = TransitTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
    ) {
        JourneyMapView(
            journey = journey,
            userLocation = userLocation,
            accentColor = colors.accent,
            modifier = Modifier.fillMaxSize(),
        )
        // Overlay sibling captures the tap before the MapView's native gesture handler.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onTap),
        )
        // Expand pill — top-right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(LucideIcons.Maximize2),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = colors.textPrimary,
            )
        }
    }
}

@Composable
private fun JourneyMapFullscreen(
    journey: Journey,
    userLocation: Pair<Double, Double>?,
    onClose: () -> Unit,
) {
    val colors = TransitTheme.colors
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background),
        ) {
            JourneyMapView(
                journey = journey,
                userLocation = userLocation,
                accentColor = colors.accent,
                modifier = Modifier.fillMaxSize(),
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
            ) {
                Icon(
                    painterResource(LucideIcons.X),
                    contentDescription = "Close map",
                    modifier = Modifier.size(18.dp),
                    tint = colors.textPrimary,
                )
            }
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun JourneyHeader(journey: Journey) {
    val colors = TransitTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatEpochTime(journey.departureTime),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                painterResource(LucideIcons.ArrowRight),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = colors.textSecondary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = formatEpochTime(journey.arrivalTime),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = headerSubtitle(journey),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
    }
}

private fun headerSubtitle(journey: Journey): String {
    val mins = journey.durationMinutes
    val transfersStr = when (journey.transfers) {
        0 -> "direct"
        1 -> "1 transfer"
        else -> "${journey.transfers} transfers"
    }
    val walkMin = journey.totalWalkSeconds / 60
    val walkStr = if (walkMin >= 2) " · $walkMin min walking" else ""
    return "$mins min · $transfersStr$walkStr"
}

// ── Transit leg ───────────────────────────────────────────────────────────────

@Composable
private fun TransitLegRow(
    leg: TransitLeg,
    isFirst: Boolean,
    isLast: Boolean,
    isDirectTransfer: Boolean,
    isBeforeTransfer: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
) {
    val colors = TransitTheme.colors
    val nodeColor = parseHexColor(leg.routeColor, fallback = colors.accent)

    Column {
        BoardRow(
            leg = leg,
            nodeColor = nodeColor,
            isFirst = isFirst,
            isDirectTransfer = isDirectTransfer,
        )
        MiddleSection(
            leg = leg,
            nodeColor = nodeColor,
            isExpanded = isExpanded,
            onToggleExpand = onToggleExpand,
        )
        AlightRow(
            leg = leg,
            nodeColor = nodeColor,
            isLast = isLast,
            isBeforeTransfer = isBeforeTransfer,
        )
        if (isBeforeTransfer) TransferConnectorRow()
    }
}

@Composable
private fun BoardRow(
    leg: TransitLeg,
    nodeColor: Color,
    isFirst: Boolean,
    isDirectTransfer: Boolean,
) {
    val colors = TransitTheme.colors
    // Top padding to align the dot's center with the stop name baseline.
    val dotPadTop = if (isDirectTransfer) 0.dp else K_DOT_TOP_PAD
    Row(
        horizontalArrangement = Arrangement.spacedBy(K_COL_GAP),
        modifier = Modifier.height(IntrinsicSize.Min),
    ) {
        // Time column — top-aligned with dot
        Box(modifier = Modifier.width(K_TIME_W)) {
            if (!isDirectTransfer) {
                Text(
                    text = formatEpochTime(leg.departureTime),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth().padding(top = K_TIME_TOP_PAD),
                    color = colors.textPrimary,
                )
            }
        }
        // Node column — dot at top, continuous colored line filling remaining height
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(K_NODE_W).fillMaxHeight(),
        ) {
            Spacer(Modifier.height(dotPadTop))
            Box(
                modifier = Modifier
                    .size(K_DOT_D)
                    .clip(CircleShape)
                    .background(nodeColor),
            )
            Box(
                modifier = Modifier
                    .width(K_LINE_W)
                    .weight(1f)
                    .background(nodeColor),
            )
        }
        // Content column — stop name, then route badge + headsign
        Column(
            modifier = Modifier.weight(1f).padding(bottom = 6.dp),
        ) {
            if (!isDirectTransfer) {
                Text(
                    text = leg.boardStop.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(4.dp))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                LineBadge(
                    name = leg.routeName,
                    colorHex = leg.routeColor,
                    textColorHex = leg.routeTextColor,
                    size = LineBadgeSize.Small,
                )
                if (leg.headsign.isNotBlank()) {
                    Icon(
                        painterResource(LucideIcons.ArrowRight),
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        tint = colors.textSecondary.copy(alpha = 0.5f),
                    )
                    Text(
                        text = leg.headsign,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiddleSection(
    leg: TransitLeg,
    nodeColor: Color,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
) {
    val colors = TransitTheme.colors
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 0f else -90f,
        label = "chevron-rot",
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(K_COL_GAP),
        modifier = Modifier.height(IntrinsicSize.Min),
    ) {
        Spacer(Modifier.width(K_TIME_W))
        Box(
            modifier = Modifier.width(K_NODE_W).fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(K_LINE_W)
                    .fillMaxHeight()
                    .background(nodeColor),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            if (leg.intermediateStops.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clickable { onToggleExpand() }
                        .padding(vertical = 10.dp),
                ) {
                    Icon(
                        painterResource(LucideIcons.ChevronDown),
                        contentDescription = null,
                        modifier = Modifier.size(11.dp).rotate(rotation),
                        tint = colors.textSecondary,
                    )
                    val n = leg.intermediateStops.size
                    Text(
                        text = if (n == 1) "1 stop" else "$n stops",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 10.dp),
                    ) {
                        leg.intermediateStops.forEach { stop ->
                            IntermediateStopRow(stop = stop, nodeColor = nodeColor)
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun IntermediateStopRow(stop: IntermediateStop, nodeColor: Color) {
    val colors = TransitTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Hollow dot (background-bordered circle) anchored on the colored line
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(nodeColor)
                .padding(1.5.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
        )
        Text(
            text = stop.name,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stop.time,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textTertiary,
        )
    }
}

@Composable
private fun AlightRow(
    leg: TransitLeg,
    nodeColor: Color,
    @Suppress("UNUSED_PARAMETER") isLast: Boolean,
    @Suppress("UNUSED_PARAMETER") isBeforeTransfer: Boolean,
) {
    val colors = TransitTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(K_COL_GAP),
        modifier = Modifier.height(IntrinsicSize.Min),
    ) {
        Text(
            text = formatEpochTime(leg.arrivalTime),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.End,
            modifier = Modifier.width(K_TIME_W).padding(top = K_TIME_TOP_PAD),
            color = colors.textPrimary,
        )
        // Node column — hollow dot at the top, no line below (the colored
        // segment terminates here; what follows is a walking / transfer row).
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(K_NODE_W).fillMaxHeight(),
        ) {
            Spacer(Modifier.height(K_DOT_TOP_PAD))
            Box(
                modifier = Modifier
                    .size(K_DOT_D)
                    .clip(CircleShape)
                    .background(nodeColor)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
            )
        }
        Text(
            text = leg.alightStop.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f).padding(bottom = 6.dp),
        )
    }
}

// ── Transfer connector ────────────────────────────────────────────────────────

@Composable
private fun TransferConnectorRow() {
    val colors = TransitTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(K_COL_GAP),
        modifier = Modifier.padding(vertical = 8.dp).height(IntrinsicSize.Min),
    ) {
        Spacer(Modifier.width(K_TIME_W))
        DashedVertical(modifier = Modifier.width(K_NODE_W).fillMaxHeight())
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                painterResource(LucideIcons.Repeat2),
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = colors.textSecondary,
            )
            Text(
                text = "Transfer",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
    }
}

// ── Walking leg ───────────────────────────────────────────────────────────────

@Composable
private fun WalkingLegRow(leg: WalkingLeg) {
    val colors = TransitTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(K_COL_GAP),
        modifier = Modifier.padding(vertical = 10.dp).height(IntrinsicSize.Min),
    ) {
        Spacer(Modifier.width(K_TIME_W))
        DashedVertical(modifier = Modifier.width(K_NODE_W).fillMaxHeight())
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                painterResource(LucideIcons.Footprints),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = colors.textSecondary,
            )
            val walkMin = (leg.walkSeconds / 60).coerceAtLeast(1)
            Text(
                text = "Walk $walkMin min",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = colors.textSecondary,
            )
            if (leg.walkMeters >= 50) {
                Text(text = "·", style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
                Text(
                    text = "${leg.walkMeters} m",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary,
                )
            }
        }
    }
}

// ── Endpoint bookends ─────────────────────────────────────────────────────────

private enum class EndpointRole { Origin, Destination }

@Composable
private fun EndpointRow(timeMs: Long, name: String, role: EndpointRole) {
    val colors = TransitTheme.colors
    val connectorColor = colors.textSecondary.copy(alpha = 0.45f)
    Row(
        modifier = Modifier.padding(vertical = 6.dp).height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(K_COL_GAP),
    ) {
        Text(
            text = formatEpochTime(timeMs),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(K_TIME_W).padding(top = K_TIME_TOP_PAD),
            textAlign = TextAlign.End,
            color = colors.textPrimary,
        )
        Column(
            modifier = Modifier.width(K_NODE_W).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (role == EndpointRole.Origin) {
                // Pad to align dot with content top, then connector below fills
                Spacer(Modifier.height(K_DOT_TOP_PAD))
                Box(
                    modifier = Modifier
                        .size(K_DOT_D)
                        .clip(CircleShape)
                        .background(colors.accent),
                )
                Box(
                    modifier = Modifier
                        .width(K_LINE_W)
                        .weight(1f)
                        .background(connectorColor),
                )
            } else {
                // Destination: connector fills from top, then hollow ring
                Box(
                    modifier = Modifier
                        .width(K_LINE_W)
                        .weight(1f)
                        .background(connectorColor),
                )
                Box(
                    modifier = Modifier
                        .size(K_DOT_D)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(2.dp, colors.textPrimary, CircleShape),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            if (role == EndpointRole.Destination) {
                // Push name to bottom so it aligns with the hollow ring
                Spacer(Modifier.weight(1f, fill = true))
            }
            Text(
                text = if (role == EndpointRole.Origin) "DEPARTURE" else "ARRIVAL",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                ),
                color = colors.textSecondary,
            )
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textPrimary,
                maxLines = 2,
            )
        }
    }
}

// ── Dashed vertical ───────────────────────────────────────────────────────────

@Composable
private fun DashedVertical(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    Canvas(modifier = modifier.heightIn(min = 22.dp).fillMaxHeight()) {
        val x = size.width / 2f
        drawLine(
            color = color,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(3.dp.toPx(), 3.dp.toPx()), 0f,
            ),
        )
    }
}
