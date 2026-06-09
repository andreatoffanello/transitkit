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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.stringResource
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.model.IntermediateStop
import com.transitkit.app.data.model.TransitLeg
import com.transitkit.app.data.model.WalkingLeg
import com.transitkit.app.ui.components.LineBadge
import com.transitkit.app.ui.components.LineBadgeSize
import com.transitkit.app.ui.components.parseHexColor

// ── Layout constants (shared with JourneyDetailScreen) ───────────────────────

internal val K_TIME_W = 46.dp
internal val K_NODE_W = 14.dp
internal val K_LINE_W = 2.5.dp
internal val K_DOT_D = 12.dp
internal val K_COL_GAP = 12.dp
internal val K_DOT_TOP_PAD = 4.dp
internal val K_TIME_TOP_PAD = 1.dp

// ── EndpointRole ──────────────────────────────────────────────────────────────

internal enum class EndpointRole { Origin, Destination }

// ── TransitLegRow ─────────────────────────────────────────────────────────────

@Composable
internal fun TransitLegRow(
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

// ── BoardRow ──────────────────────────────────────────────────────────────────

@Composable
internal fun BoardRow(
    leg: TransitLeg,
    nodeColor: Color,
    isFirst: Boolean,
    isDirectTransfer: Boolean,
) {
    val colors = TransitTheme.colors
    val dotPadTop = if (isDirectTransfer) 0.dp else K_DOT_TOP_PAD
    Row(
        horizontalArrangement = Arrangement.spacedBy(K_COL_GAP),
        modifier = Modifier.height(IntrinsicSize.Min),
    ) {
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
                modifier = Modifier.fillMaxWidth(),
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
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                Spacer(Modifier.weight(1f))
                val legMin = ((leg.arrivalTime - leg.departureTime) / 60_000L)
                    .coerceAtLeast(1L)
                    .toInt()
                Text(
                    text = stringResource(R.string.planner_leg_duration, legMin),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary,
                )
            }
        }
    }
}

// ── MiddleSection ─────────────────────────────────────────────────────────────

@Composable
internal fun MiddleSection(
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
                        text = if (n == 1) stringResource(R.string.planner_stop_count_one)
                               else stringResource(R.string.planner_stop_count_other, n),
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

// ── IntermediateStopRow ───────────────────────────────────────────────────────

@Composable
internal fun IntermediateStopRow(stop: IntermediateStop, nodeColor: Color) {
    val colors = TransitTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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
            text = formatEpochTime(stop.arrivalTime),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textTertiary,
        )
    }
}

// ── AlightRow ─────────────────────────────────────────────────────────────────

@Composable
internal fun AlightRow(
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

// ── TransferConnectorRow ──────────────────────────────────────────────────────

@Composable
internal fun TransferConnectorRow() {
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
                text = stringResource(R.string.planner_transfer_label),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
    }
}

// ── WalkingLegRow ─────────────────────────────────────────────────────────────

@Composable
internal fun WalkingLegRow(leg: WalkingLeg) {
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
                text = stringResource(R.string.planner_walk_label, walkMin),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = colors.textSecondary,
            )
            if (leg.walkMeters >= 50) {
                Text(text = "·", style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
                Text(
                    text = stringResource(R.string.planner_walk_meters, leg.walkMeters),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary,
                )
            }
        }
    }
}

// ── EndpointRow ───────────────────────────────────────────────────────────────

@Composable
internal fun EndpointRow(timeMs: Long, name: String, role: EndpointRole) {
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
                Spacer(Modifier.weight(1f, fill = true))
            }
            Text(
                text = if (role == EndpointRole.Origin) stringResource(R.string.planner_endpoint_departure)
                       else stringResource(R.string.planner_endpoint_arrival),
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

// ── DashedVertical ────────────────────────────────────────────────────────────

@Composable
internal fun DashedVertical(modifier: Modifier = Modifier) {
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
