package com.transitkit.app.ui.alerts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.model.AlertSeverity
import com.transitkit.app.data.model.ScheduleRoute
import com.transitkit.app.data.model.ServiceAlert
import com.transitkit.app.ui.components.LineBadge
import com.transitkit.app.ui.components.LineBadgeSize

/**
 * Movete-style alert card — used in the global alert list and in the
 * contextual alert sections inside stop / line detail.
 *
 * Layout: cause icon + actionable title (cause · effect), header text as
 * secondary detail (2 lines), horizontal line badges for affected routes.
 * The bureaucratic `headerText` from the feed is intentionally demoted to
 * the body — the title row uses a human-readable cause/effect combination.
 *
 * @param routesById map from route id → `ScheduleRoute` used to render the
 *   affected-line badges. Callers usually pass the schedule's full route
 *   index; pass `emptyMap()` to hide badges entirely.
 */
@Composable
fun AlertCard(
    alert: ServiceAlert,
    routesById: Map<String, ScheduleRoute>,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = TransitTheme.colors
    val severity = severityAccent(alert.severity)
    val baseModifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(colors.bgSecondary)
        .border(BorderStroke(1.dp, severity.copy(alpha = 0.35f)), RoundedCornerShape(12.dp))

    val tappable = if (onClick != null) {
        baseModifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(),
            onClick = onClick,
        )
    } else baseModifier

    Column(
        modifier = tappable.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(alertCauseIcon(alert.cause)),
                contentDescription = null,
                tint = severity,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = displayTitle(alert),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        val header = localizedHeader(alert)
        if (header.isNotBlank()) {
            Text(
                text = header,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        val resolvedRoutes = remember(alert.affectedRouteIds, routesById) {
            alert.affectedRouteIds
                .mapNotNull { routesById[it] }
                .sortedBy { it.name }
                .take(12)
        }
        if (resolvedRoutes.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                resolvedRoutes.forEach { route ->
                    LineBadge(route = route, size = LineBadgeSize.Small)
                }
            }
        }
    }
}

@Composable
private fun severityAccent(severity: AlertSeverity): Color = when (severity) {
    AlertSeverity.SEVERE  -> Color(0xFFDC2626)
    AlertSeverity.WARNING -> Color(0xFFD97706)
    AlertSeverity.INFO    -> TransitTheme.colors.accent
    AlertSeverity.UNKNOWN -> Color(0xFFD97706)
}
