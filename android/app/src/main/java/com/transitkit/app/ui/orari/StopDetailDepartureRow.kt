package com.transitkit.app.ui.orari

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.transitkit.app.R
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.model.Departure

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DepartureRow(
    departure: Departure,
    isNext: Boolean = false,
    operatorTimezone: String = "UTC",
    stopSequence: String? = null,
    onClick: () -> Unit = {},
) {
    val colors = TransitTheme.colors
    val displayTime = departure.realtimeDepartureTime ?: departure.departureTime
    val hasDelay = (departure.delay ?: 0) > 0

    val lineCd = stringResource(R.string.cd_line_format, departure.routeShortName ?: "")
    val directionCd = stringResource(R.string.cd_direction_format, departure.headsign ?: "")
    val departingNowCd = stringResource(R.string.cd_departing_now)
    val minutes = remember(displayTime) {
        minutesUntil(displayTime, operatorTimezone)
    }
    val inMinutesCd = minutes?.takeIf { it != 0 }?.let { stringResource(R.string.cd_in_minutes, it) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick,
            )
            .semantics {
                contentDescription = buildString {
                    append(lineCd)
                    append(", ")
                    append(directionCd)
                    minutes?.let { mins ->
                        append(", ")
                        append(if (mins == 0) departingNowCd else inMinutesCd ?: "")
                    }
                }
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            // Line badge — consistent Large size for all rows
            com.transitkit.app.ui.components.LineBadge(
                name = departure.routeShortName,
                colorHex = departure.routeColor,
                textColorHex = departure.routeTextColor,
                size = com.transitkit.app.ui.components.LineBadgeSize.Large,
            )

            Spacer(Modifier.width(10.dp))

            // Destination (headsign) + description (stop sequence) — static ellipsis.
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = departure.headsign,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                if (stopSequence != null && stopSequence != departure.headsign) {
                    Text(
                        text = stopSequence,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textTertiary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Time: countdown + clock time stacked.
            // Live dot (se realtime) reso INLINE col countdown dentro TimeDisplay
            // — niente colonna pulse fluttuante separata.
            Column(horizontalAlignment = Alignment.End) {
                val timeState = com.transitkit.app.ui.components.departureTimeState(
                    displayTime,
                    operatorTimezone,
                )
                com.transitkit.app.ui.components.TimeDisplay(
                    state = timeState,
                    isEmphasis = isNext,
                    liveDot = departure.isRealtime,
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
    }
}
