package com.transitkit.app.ui.orari

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.model.ScheduleRoute
import com.transitkit.app.data.model.ServiceAlert
import com.transitkit.app.ui.alerts.AlertCard

/**
 * "AVVISI" section pinned to the bottom of `StopDetailScreen`. Renders the
 * rich shared `AlertCard` — alert list is computed in `StopDetailViewModel`
 * so this view assumes the caller already filtered by stop / line scope.
 */
@Composable
internal fun AlertsSection(
    alerts: List<ServiceAlert>,
    routesById: Map<String, ScheduleRoute>,
    onClick: (alertId: String) -> Unit,
) {
    val colors = TransitTheme.colors
    Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 24.dp, bottom = 16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(LucideIcons.AlertTriangle),
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color(0xFFD97706),
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = stringResource(R.string.stop_detail_alerts_section).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                ),
                color = colors.textTertiary,
            )
        }
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            alerts.forEach { alert ->
                AlertCard(
                    alert = alert,
                    routesById = routesById,
                    onClick = { onClick(alert.id) },
                )
            }
        }
    }
}
