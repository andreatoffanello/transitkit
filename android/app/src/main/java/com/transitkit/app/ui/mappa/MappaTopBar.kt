package com.transitkit.app.ui.mappa

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.transitkit.app.R
import com.transitkit.app.config.LocalTransitColors
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.data.model.ScheduleRoute
import com.transitkit.app.ui.components.LineBadge
import com.transitkit.app.ui.components.LineBadgeSize
import com.transitkit.app.ui.components.LiveIndicator

// ---------------------------------------------------------------------------
// Search pill — "Cerca linea"
// ---------------------------------------------------------------------------

@Composable
internal fun SearchLinePill(onClick: () -> Unit) {
    val colors = LocalTransitColors.current
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.bgSecondary,
        shadowElevation = 4.dp,
        onClick = onClick,
        modifier = Modifier.semantics { testTag = "map_line_picker_pill" },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(LucideIcons.Search),
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.mappa_cerca_linea),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Route dismiss chip
// ---------------------------------------------------------------------------

@Composable
internal fun RouteDismissChip(
    route: ScheduleRoute,
    liveCount: Int,
    onDismiss: () -> Unit,
) {
    val colors = LocalTransitColors.current
    val cdDismiss = stringResource(R.string.mappa_rimuovi_overlay)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.bgSecondary,
        shadowElevation = 4.dp,
        modifier = Modifier.semantics { testTag = "map_route_dismiss_chip" },
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LineBadge(route = route, size = LineBadgeSize.Medium)
            if (liveCount > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    LiveIndicator(size = 6.dp, animated = false)
                    Text(
                        text = "$liveCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(28.dp)
                    .semantics {
                        testTag = "btn_route_dismiss"
                        contentDescription = cdDismiss
                    },
            ) {
                Icon(
                    painter = painterResource(LucideIcons.X),
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}
