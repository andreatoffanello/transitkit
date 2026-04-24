package com.transitkit.app.ui.orari

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.transitkit.app.R
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.model.Departure

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DepartureRow(departure: Departure, isNext: Boolean = false, operatorTimezone: String = "UTC", stopSequence: String? = null, onClick: () -> Unit = {}) {
    val colors = TransitTheme.colors
    val displayTime = departure.realtimeDepartureTime ?: departure.departureTime
    val hasDelay = (departure.delay ?: 0) > 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(Modifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick,
            )
            .semantics {
                contentDescription = buildString {
                    append("Linea ${departure.routeShortName}")
                    append(", direzione ${departure.headsign}")
                    minutesUntil(departure.realtimeDepartureTime ?: departure.departureTime, operatorTimezone)?.let { mins ->
                        if (mins == 0) append(", in partenza adesso")
                        else append(", in $mins minut${if (mins == 1) "o" else "i"}")
                    }
                }
            },
    ) {
        // "Prossima" badge removed — position-in-list (first row) already
        // communicates that the departure is the next one.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = if (isNext) 14.dp else 10.dp,
                ),
        ) {
            // Line badge — design-system component (iOS DepartureRow parity).
            com.transitkit.app.ui.components.LineBadge(
                name = departure.routeShortName,
                colorHex = departure.routeColor,
                textColorHex = departure.routeTextColor,
                size = com.transitkit.app.ui.components.LineBadgeSize.Large,
            )

            Spacer(Modifier.width(12.dp))

            // Stop sequence (scrolling marquee) or headsign fallback — mirrors iOS DepartureRow MarqueeText
            if (stopSequence != null) {
                Text(
                    text = stopSequence,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .basicMarquee(iterations = Int.MAX_VALUE),
                )
            } else {
                Text(
                    text = departure.headsign,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.width(8.dp))

            // Realtime live indicator (pulsing ring) stays inline — it's a
            // freshness badge, not part of the time glyph.
            if (departure.isRealtime) {
                val realtimePulse = rememberInfiniteTransition(label = "realtimePulse")
                val pulseScale by realtimePulse.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.6f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                    label = "pulseScale",
                )
                val pulseAlpha by realtimePulse.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                    label = "pulseAlpha",
                )
                Box(
                    modifier = Modifier.size(14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .scale(pulseScale)
                            .background(colors.realtimeGreen.copy(alpha = pulseAlpha), CircleShape),
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(colors.realtimeGreen, CircleShape),
                    )
                }
                Spacer(Modifier.width(6.dp))
            }

            // Canonical stacked time display: minutes (top) / HH:mm (bottom).
            Column(horizontalAlignment = Alignment.End) {
                val timeState = com.transitkit.app.ui.components.departureTimeState(
                    displayTime,
                    operatorTimezone,
                )
                com.transitkit.app.ui.components.TimeDisplay(
                    state = timeState,
                    isEmphasis = isNext,
                )
                if (hasDelay) {
                    val delayMin = (departure.delay ?: 0) / 60
                    Text(
                        text = stringResource(R.string.label_ritardo_min, delayMin),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.realtimeRed,
                    )
                }
            }
        }
        if (isNext) {
            Spacer(Modifier.height(6.dp))
        }
    }
}
