package com.transitkit.app.ui.mappa

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.gtfsrt.VehiclePosition
import com.transitkit.app.data.model.ScheduleRoute

@Composable
internal fun VehiclePreviewContent(
    vehicle: VehiclePosition,
    vehicleColor: Color,
    route: ScheduleRoute?,
    stopName: String? = null,
    predictedArrivalMs: Long? = null,
    operatorTimezoneId: String = "UTC",
    isFollowing: Boolean = false,
    onToggleFollow: () -> Unit = {},
    onOpenTrip: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val colors = TransitTheme.colors

    // Tick every 15s so freshness + countdown stay live (matches feed cadence).
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(15_000)
            now = System.currentTimeMillis()
        }
    }

    // --- Derived state -----------------------------------------------------

    val dirs = route?.directions ?: emptyList()
    val routeNamesLower = listOfNotNull(route?.name, route?.longName)
        .map { it.lowercase() }
        .toSet()
    val directionLabel = dirs
        .mapNotNull { it.headsign?.trim()?.takeIf { h -> h.isNotBlank() && !routeNamesLower.contains(h.lowercase()) } }
        .take(2)
        .joinToString(" → ")

    // ETA state — matches iOS: minutes countdown when within 60 min, absolute
    // clock when farther, "arriving" when imminent. Clock always shown in
    // operator's local time.
    val etaMinutes: Int? = predictedArrivalMs?.let {
        val diff = it - now
        if (diff < -60_000L) null
        else ((diff + 30_000L) / 60_000L).toInt().coerceAtLeast(0)
    }
    val etaClock: String? = predictedArrivalMs?.let {
        val fmt = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getTimeZone(operatorTimezoneId)
        }
        fmt.format(java.util.Date(it))
    }
    val ageSec = ((now / 1000L) - vehicle.timestamp).coerceAtLeast(0L)
    val freshnessText: String? = when {
        vehicle.timestamp <= 0 -> null
        ageSec < 60 -> stringResource(R.string.vehicle_updated_sec, ageSec.toInt())
        else -> stringResource(R.string.vehicle_updated_min, (ageSec / 60).toInt())
    }
    val stopPrefix = when (vehicle.currentStatus) {
        com.transitkit.app.data.gtfsrt.VehicleStatus.IN_TRANSIT_TO -> stringResource(R.string.vehicle_next_stop)
        com.transitkit.app.data.gtfsrt.VehicleStatus.STOPPED_AT -> stringResource(R.string.vehicle_stopped_at)
        com.transitkit.app.data.gtfsrt.VehicleStatus.INCOMING_AT -> stringResource(R.string.vehicle_incoming_at)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // --- Row 1 — Badge + name + #id + wheelchair + close ------------
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (route != null) {
                com.transitkit.app.ui.components.LineBadge(
                    route = route,
                    // iOS VehicleDetailSheet parity: header badge is Medium.
                    size = com.transitkit.app.ui.components.LineBadgeSize.Medium,
                )
            } else {
                com.transitkit.app.ui.components.LineBadge(
                    name = stringResource(R.string.vehicle_label_default),
                    colorHex = null,
                    size = com.transitkit.app.ui.components.LineBadgeSize.Medium,
                )
            }
            Text(
                text = route?.longName?.takeIf { it.isNotBlank() }
                    ?: route?.name
                    ?: stringResource(R.string.mappa_veicolo_live),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (vehicle.label.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .background(colors.textPrimary.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "#${vehicle.label}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary,
                    )
                }
            }
            if (vehicle.wheelchair == com.transitkit.app.data.gtfsrt.WheelchairStatus.ACCESSIBLE) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(colors.accent.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(LucideIcons.Accessibility),
                        contentDescription = stringResource(R.string.vehicle_accessible),
                        tint = colors.accent,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(32.dp)
                    .semantics { testTag = "btn_vehicle_sheet_close" },
            ) {
                Icon(
                    painter = painterResource(LucideIcons.X),
                    contentDescription = stringResource(R.string.action_chiudi),
                    tint = colors.textTertiary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // --- Row 2 — Direction + occupancy (no delay badge; delay is in ETA) ---
        if (directionLabel.isNotBlank() || vehicle.occupancyStatus != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (directionLabel.isNotBlank()) {
                    Icon(
                        painter = painterResource(LucideIcons.ArrowRight),
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = directionLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                vehicle.occupancyStatus?.let { status ->
                    occupancyLabel(status)?.let { label ->
                        Box(
                            modifier = Modifier
                                .background(colors.textPrimary.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }

        // --- Row 3 — Stop row: label + name (left) + mins/clock stack (right) ---
        if (stopName != null) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        painter = painterResource(LucideIcons.MapPin),
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = stopPrefix,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textTertiary,
                    )
                }
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stopName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    // ETA stack: minutes (top) + HH:mm (bottom) — parity with iOS
                    if (etaMinutes != null && etaClock != null) {
                        Column(horizontalAlignment = Alignment.End) {
                            when {
                                // Imminent arrival — show arrow instead of "0'"
                                etaMinutes <= 0 -> {
                                    Text(
                                        text = "→",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colors.textPrimary,
                                    )
                                    Text(
                                        text = etaClock,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    )
                                }
                                etaMinutes <= 60 -> {
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = "$etaMinutes",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = colors.textPrimary,
                                        )
                                        Text(
                                            text = "'",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = colors.textSecondary,
                                        )
                                    }
                                    Text(
                                        text = etaClock,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    )
                                }
                                else -> {
                                    Text(
                                        text = etaClock,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Row 4 — Live badge inline + freshness ----------------------
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(colors.realtimeGreen, CircleShape),
            )
            Text(
                text = stringResource(R.string.mappa_live),
                style = MaterialTheme.typography.labelSmall,
                color = colors.realtimeGreen,
                fontWeight = FontWeight.SemiBold,
            )
            if (freshnessText != null) {
                Text(
                    text = "·",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textTertiary,
                )
                Text(
                    text = freshnessText,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textTertiary,
                )
            }
        }

        // --- Row 5 — Full-width action buttons: follow + open trip -------
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val followBg = if (isFollowing) colors.accent else colors.textPrimary.copy(alpha = 0.08f)
            val followFg = if (isFollowing) Color.White else colors.textPrimary
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(followBg, RoundedCornerShape(12.dp))
                    .clickable(onClick = onToggleFollow)
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(LucideIcons.Crosshair),
                    contentDescription = null,
                    tint = followFg,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(
                        if (isFollowing) R.string.vehicle_unfollow else R.string.vehicle_follow
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = followFg,
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(colors.textPrimary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenTrip)
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(LucideIcons.Route),
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.vehicle_open_trip),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
            }
        }
    }
}

/** Maps GTFS-RT OccupancyStatus to a short localized label, or null if no useful info. */
@Composable
private fun occupancyLabel(status: com.transitkit.app.data.gtfsrt.OccupancyStatus): String? {
    return when (status) {
        com.transitkit.app.data.gtfsrt.OccupancyStatus.EMPTY,
        com.transitkit.app.data.gtfsrt.OccupancyStatus.MANY_SEATS_AVAILABLE ->
            stringResource(R.string.occupancy_seats_available)
        com.transitkit.app.data.gtfsrt.OccupancyStatus.FEW_SEATS_AVAILABLE ->
            stringResource(R.string.occupancy_few_seats)
        com.transitkit.app.data.gtfsrt.OccupancyStatus.STANDING_ROOM_ONLY ->
            stringResource(R.string.occupancy_standing_only)
        com.transitkit.app.data.gtfsrt.OccupancyStatus.CRUSHED_STANDING_ROOM_ONLY,
        com.transitkit.app.data.gtfsrt.OccupancyStatus.FULL ->
            stringResource(R.string.occupancy_full)
        com.transitkit.app.data.gtfsrt.OccupancyStatus.NOT_ACCEPTING_PASSENGERS,
        com.transitkit.app.data.gtfsrt.OccupancyStatus.NOT_BOARDABLE ->
            stringResource(R.string.occupancy_not_boarding)
        com.transitkit.app.data.gtfsrt.OccupancyStatus.NO_DATA_AVAILABLE -> null
    }
}
