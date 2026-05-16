package com.transitkit.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.data.model.ScheduleRoute

/**
 * Horizontal-scroll card for a nearby stop on the Home tab. Mirrors the iOS
 * `NearbyStopCard` and the Movete reference: stop pin icon + name, then a
 * compact row of line badges below, with walking time pinned to the trailing
 * side. The card is the unit of horizontal scrolling — never wrap it in a
 * Surface from the call site.
 */
@Composable
internal fun NearbyStopCard(
    stop: ResolvedStop,
    distanceMeters: Double,
    routes: List<ScheduleRoute>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TransitTheme.colors
    val haptic = LocalHapticFeedback.current
    val walking = walkingTimeLabel(distanceMeters)

    Surface(
        modifier = modifier
            .width(240.dp)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .semantics { contentDescription = "home_nearby_stop_${stop.id}" },
        shape = RoundedCornerShape(14.dp),
        color = colors.glassFill,
        border = BorderStroke(0.5.dp, colors.glassBorder),
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: pin icon + stop name (up to 2 lines so taller names don't
            // get truncated to a single ellipsis like the old compact row did).
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(LucideIcons.MapPin),
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stop.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(10.dp))

            // Line badges row + walking time. When the stop has no resolvable
            // routes (rare, stations with feed gaps), fall back to just walking
            // time so the card never looks empty.
            if (routes.isEmpty()) {
                Text(
                    text = walking,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textTertiary,
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val visible = routes.take(MAX_VISIBLE_BADGES)
                    val overflow = routes.size - visible.size
                    visible.forEach { route ->
                        com.transitkit.app.ui.components.LineBadge(
                            route = route,
                            size = com.transitkit.app.ui.components.LineBadgeSize.Small,
                        )
                    }
                    if (overflow > 0) {
                        OverflowChip(count = overflow)
                    }
                    Spacer(modifier = Modifier.width(0.dp))
                    Box(modifier = Modifier.padding(start = 4.dp)) {
                        Text(
                            text = walking,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textTertiary,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverflowChip(count: Int) {
    val colors = TransitTheme.colors
    Box(
        modifier = Modifier
            .background(
                color = colors.textPrimary.copy(alpha = 0.08f),
                shape = RoundedCornerShape(50),
            )
            .padding(horizontal = 6.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "+$count",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary,
        )
    }
}

private const val MAX_VISIBLE_BADGES = 4
