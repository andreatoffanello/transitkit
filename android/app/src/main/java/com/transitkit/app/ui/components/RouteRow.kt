package com.transitkit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transitkit.app.R
import com.transitkit.app.config.LocalTransitColors
import com.transitkit.app.data.model.ScheduleRoute

@Composable
internal fun RouteRow(
    route: ScheduleRoute,
    stopSequence: String?,
    liveCount: Int,
    onClick: () -> Unit = {},
) {
    val colors = LocalTransitColors.current
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
        LineBadge(
            route = route,
            // iOS LinesListView parity: route rows are Large without icon
            // (the modal chip is rendered separately in the row metadata).
            size = LineBadgeSize.Large,
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
        // STATICO — niente pulse animation. RouteRow vive in LazyColumn
        // che ricompone periodicamente (vehicle store refresh); un
        // `infiniteRepeatable` qui causerebbe layout instability.
        if (liveCount > 0) {
            LiveCountStaticBadge(count = liveCount)
        }
        // NO trailing chevron — removed per spec (iOS parity unification)
    }
}

@Composable
private fun LiveCountStaticBadge(count: Int) {
    val colors = LocalTransitColors.current
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
                .background(colors.realtimeGreen, CircleShape),
        )
        Text(
            text = pluralStringResource(R.plurals.lines_live_count, count, count),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.realtimeGreen,
        )
    }
}
