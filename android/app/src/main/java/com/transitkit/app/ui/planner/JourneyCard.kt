package com.transitkit.app.ui.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.model.Journey
import com.transitkit.app.data.model.TransitLeg
import com.transitkit.app.data.model.WalkingLeg
import com.transitkit.app.data.model.durationMinutes
import com.transitkit.app.data.model.durationSeconds
import com.transitkit.app.data.model.transfers
import com.transitkit.app.ui.components.LineBadge
import com.transitkit.app.ui.components.LineBadgeSize
import com.transitkit.app.ui.components.parseHexColor

/**
 * Journey summary card — large times, duration pill, proportional strip with
 * one segment per leg colored by route, line badges row. Movete parity.
 *
 * @param maxJourneyDurationSeconds reference duration across all visible journeys,
 *        used so longer trips render proportionally wider than shorter ones.
 */
@Composable
fun JourneyCard(
    journey: Journey,
    onClick: () -> Unit,
    maxJourneyDurationSeconds: Long = 0,
) {
    val colors = TransitTheme.colors
    val depFmt = formatEpochTime(journey.departureTime)
    val arrFmt = formatEpochTime(journey.arrivalTime)

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Journey $depFmt to $arrFmt" },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Row 1 — departure → arrival times + duration pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = com.transitkit.app.ui.components.ClockTime.annotated(
                            depFmt, MaterialTheme.typography.headlineSmall.fontSize, colors.textPrimary,
                        ),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                    )
                    Icon(
                        painterResource(LucideIcons.ArrowRight),
                        contentDescription = null,
                        tint = colors.textSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = com.transitkit.app.ui.components.ClockTime.annotated(
                            arrFmt, MaterialTheme.typography.headlineSmall.fontSize, colors.textPrimary,
                        ),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${journey.durationMinutes} min",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textSecondary,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }

            Spacer(Modifier.height(10.dp))

            // Row 2 — proportional strip
            JourneyStrip(
                journey = journey,
                maxJourneyDurationSeconds = maxJourneyDurationSeconds.coerceAtLeast(journey.durationSeconds),
            )

            Spacer(Modifier.height(10.dp))

            // Row 3 — line badges + transfers count
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                journey.legs.filterIsInstance<TransitLeg>().forEach { leg ->
                    LineBadge(
                        name = leg.routeName,
                        colorHex = leg.routeColor,
                        textColorHex = leg.routeTextColor,
                        size = LineBadgeSize.Small,
                    )
                }
                Spacer(Modifier.weight(1f))
                val transfersLabel = when (journey.transfers) {
                    0 -> stringResource(R.string.planner_direct)
                    1 -> stringResource(R.string.planner_one_transfer)
                    else -> stringResource(R.string.planner_n_transfers, journey.transfers)
                }
                Text(
                    text = transfersLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun JourneyStrip(
    journey: Journey,
    maxJourneyDurationSeconds: Long,
) {
    val legs = journey.legs
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp),
    ) {
        if (legs.isEmpty() || journey.durationSeconds <= 0) return@BoxWithConstraints

        // Per-leg duration in seconds
        val legSeconds = legs.map { leg ->
            when (leg) {
                is TransitLeg -> ((leg.arrivalTime - leg.departureTime) / 1_000L).coerceAtLeast(0L)
                is WalkingLeg -> leg.walkSeconds.toLong()
            }
        }
        val totalLegSec = legSeconds.sum().coerceAtLeast(1L)

        val fraction = if (maxJourneyDurationSeconds > 0)
            journey.durationSeconds.toFloat() / maxJourneyDurationSeconds.toFloat()
        else 1f
        val usedWidth = (maxWidth.value * fraction).coerceAtLeast(8f).dp
        val gapTotal = ((legs.size - 1) * 3).dp
        val available = (usedWidth.value - gapTotal.value).coerceAtLeast(0f)

        Row(
            modifier = Modifier
                .width(usedWidth)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            legs.forEachIndexed { idx, leg ->
                val legFrac = legSeconds[idx].toFloat() / totalLegSec.toFloat()
                val width = (available * legFrac).coerceAtLeast(4f).dp
                val color: Color = when (leg) {
                    is TransitLeg -> parseHexColor(leg.routeColor, fallback = MaterialTheme.colorScheme.primary)
                    is WalkingLeg -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                }
                Box(
                    modifier = Modifier
                        .width(width)
                        .fillMaxHeight()
                        .background(color, RoundedCornerShape(3.dp)),
                )
            }
        }
    }
}
