package com.transitkit.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.config.surfaceOverMap
import com.transitkit.app.data.model.Departure
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.ui.components.LineBadge
import com.transitkit.app.ui.components.LineBadgeSize
import com.transitkit.app.ui.components.TimeDisplay
import com.transitkit.app.ui.components.departureTimeState
import com.transitkit.app.ui.components.stopIcon

// ---------------------------------------------------------------------------
// SectionHeader — shared by FavoritesSection and NearbySection
// ---------------------------------------------------------------------------

@Composable
internal fun SectionHeader(
    text: String,
    @androidx.annotation.DrawableRes icon: Int? = null,
    modifier: Modifier = Modifier,
) {
    val colors = TransitTheme.colors
    Row(
        modifier = modifier.padding(top = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
        )
    }
}

// ---------------------------------------------------------------------------
// FavoritesSection + EmptyFavoritesCard + StopCard + StopCardDepartureRow
// ---------------------------------------------------------------------------

@Composable
internal fun FavoritesSection(
    stops: List<ResolvedStop>,
    departures: Map<String, List<Departure>>,
    liveTripIds: Set<String>,
    operatorTimezone: String,
    onStopClick: (ResolvedStop) -> Unit,
    onBrowseStops: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(
            text = stringResource(R.string.home_section_favorites),
            icon = LucideIcons.Star,
        )
        if (stops.isEmpty()) {
            EmptyFavoritesCard(onBrowseStops = onBrowseStops)
        } else {
            stops.take(5).forEach { stop ->
                StopCard(
                    stop = stop,
                    departures = departures[stop.id] ?: emptyList(),
                    liveTripIds = liveTripIds,
                    operatorTimezone = operatorTimezone,
                    onClick = { onStopClick(stop) },
                )
            }
        }
    }
}

@Composable
internal fun EmptyFavoritesCard(
    onBrowseStops: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TransitTheme.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceOverMap,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colors.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(LucideIcons.Star),
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = stringResource(R.string.home_empty_favorites_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.home_empty_favorites_body),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onBrowseStops,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = Color.White,
                ),
            ) {
                Text(stringResource(R.string.home_empty_favorites_cta))
            }
        }
    }
}

@Composable
internal fun StopCard(
    stop: ResolvedStop,
    departures: List<Departure>,
    liveTripIds: Set<String>,
    operatorTimezone: String,
    distanceMeters: Double? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TransitTheme.colors
    val haptic = LocalHapticFeedback.current
    val isImminent = departures.firstOrNull()?.let {
        isWithinFiveMinutes(it, operatorTimezone)
    } ?: false

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceOverMap,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    painter = painterResource(id = stopIcon(stop.transitTypes)),
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stop.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                distanceMeters?.let {
                    Text(
                        text = walkingTimeLabel(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textTertiary,
                    )
                }
            }

            if (departures.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_departures),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary,
                )
            } else {
                val shown = departures.take(3)
                Column {
                    shown.forEachIndexed { index, dep ->
                        StopCardDepartureRow(
                            departure = dep,
                            isLive = dep.tripId in liveTripIds,
                            operatorTimezone = operatorTimezone,
                        )
                        if (index < shown.size - 1) {
                            HorizontalDivider(
                                color = colors.separator,
                                thickness = 0.5.dp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun StopCardDepartureRow(
    departure: Departure,
    isLive: Boolean,
    operatorTimezone: String,
) {
    val colors = TransitTheme.colors
    val context = LocalContext.current
    val rawTime = departure.realtimeDepartureTime ?: departure.departureTime
    val timeState = departureTimeState(
        rawTime,
        operatorTimezone,
        formatClock = { com.transitkit.app.ui.components.ClockTime.gtfs(it, context) },
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LineBadge(
            name = departure.routeShortName.take(5),
            colorHex = departure.routeColor,
            textColorHex = departure.routeTextColor,
            transitType = departure.transitType,
            size = LineBadgeSize.Medium,
        )
        Text(
            text = departure.headsign,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        // Live dot ora INLINE col countdown dentro TimeDisplay — niente
        // LiveIndicator pulse separato (era animazione infinita in LazyColumn).
        TimeDisplay(state = timeState, liveDot = isLive)
    }
}
