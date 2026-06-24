package com.transitkit.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.config.surfaceOverMap
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.data.model.ScheduleRoute

@Composable
internal fun NearbySection(
    nearbyStops: List<Pair<ResolvedStop, Double>>,
    routesByName: Map<String, ScheduleRoute>,
    permissionGranted: Boolean,
    onEnableLocation: () -> Unit,
    onStopClick: (ResolvedStop) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        !permissionGranted -> EnableLocationChip(
            onClick = onEnableLocation,
            modifier = modifier.padding(horizontal = 16.dp),
        )
        nearbyStops.isNotEmpty() -> {
            // Movete-parity: horizontal scroll of fixed-width cards. Header
            // sits flush with the section padding; the LazyRow is edge-to-edge
            // so the last card peeks past the right edge as an affordance.
            Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(
                    text = stringResource(R.string.home_section_nearby),
                    icon = LucideIcons.MapPin,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 2.dp,
                    ),
                ) {
                    items(nearbyStops, key = { it.first.id }) { (stop, distance) ->
                        val routes = remember(stop.routeNames, routesByName) {
                            stop.routeNames.mapNotNull { routesByName[it] }.distinctBy { it.id }
                        }
                        NearbyStopCard(
                            stop = stop,
                            distanceMeters = distance,
                            routes = routes,
                            onClick = { onStopClick(stop) },
                        )
                    }
                }
            }
        }
        else -> Unit
    }
}

@Composable
internal fun NearbyStopRow(
    stop: ResolvedStop,
    distanceMeters: Double,
    onClick: () -> Unit,
) {
    val colors = TransitTheme.colors
    val haptic = LocalHapticFeedback.current
    val distanceLabel = walkingTimeLabel(distanceMeters)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = painterResource(LucideIcons.BusFront),
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = stop.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = distanceLabel,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
        )
        Icon(
            painter = painterResource(LucideIcons.ChevronRight),
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
internal fun EnableLocationChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TransitTheme.colors
    val haptic = LocalHapticFeedback.current
    val cd = stringResource(R.string.home_enable_location_chip)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceOverMap)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .semantics { contentDescription = cd }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(LucideIcons.MapPin),
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = stringResource(R.string.home_enable_location_chip),
            style = MaterialTheme.typography.labelMedium,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(LucideIcons.ChevronRight),
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(12.dp),
        )
    }
}
