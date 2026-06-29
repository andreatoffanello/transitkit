package com.transitkit.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transitkit.app.config.TransitTheme

// -----------------------------------------------------------------------------
// TimeDisplay — single source of truth for departure-time rendering.
// -----------------------------------------------------------------------------
// Layout:
//
//   row 1 — countdown        "5m" / "1h 23m" / "2h"  (or pulsing arrow if now)
//           [liveDot]        opt. green dot inline when realtime
//   row 2 — scheduled/actual "16:20"  (monospaced, secondary color)
//
// Threshold (default 60 — keep all callsites aligned):
//   diff <= 60 min  → Minutes / Hours / HoursMinutes (relative)
//   diff >  60 min  → Absolute (clock time)
//
// Past departures are dropped upstream by ScheduleRepository — when a Passed
// state slips through (race between fetch and render) it stays dimmed in place.
// No day-after wrap: home and stop-detail must agree on what "now" means.
// -----------------------------------------------------------------------------

sealed class DepartureTimeState {
    /** Fallback / past threshold — clock only. */
    data class Absolute(val clock: String) : DepartureTimeState()
    /** Countdown 1-59 min — rendered as "Xm". */
    data class Minutes(val minutes: Int, val clock: String) : DepartureTimeState()
    /** 60-1439 min, with non-zero remainder — rendered as "Xh Ym". */
    data class HoursMinutes(val hours: Int, val minutes: Int, val clock: String) : DepartureTimeState()
    /** 60-1439 min, exact hour — rendered as "Xh". */
    data class Hours(val hours: Int, val clock: String) : DepartureTimeState()
    /** Departing right now (0 min). */
    data class Departing(val clock: String) : DepartureTimeState()
    /** Already gone — dimmed tertiary tint. */
    data class Passed(val clock: String) : DepartureTimeState()
}

/**
 * Computes a [DepartureTimeState] from raw minutes-from-midnight.
 *
 * @param departureMinutes scheduled (or realtime-adjusted) minute of day
 * @param nowMinutes       current minute of day in operator timezone
 * @param clockHHmm        already-formatted "HH:mm" string for the departure
 * @param relativeThreshold soglia (min) oltre cui mostrare orario assoluto.
 *   Default 60. Passa 1440 per "sempre relativo entro 24h".
 */
fun computeDepartureTimeState(
    departureMinutes: Int,
    nowMinutes: Int,
    clockHHmm: String,
    relativeThreshold: Int = 60,
): DepartureTimeState {
    val diff = departureMinutes - nowMinutes
    return when {
        diff < 0 -> DepartureTimeState.Passed(clockHHmm)
        diff == 0 -> DepartureTimeState.Departing(clockHHmm)
        diff < 60 -> DepartureTimeState.Minutes(diff, clockHHmm)
        diff < relativeThreshold -> {
            val h = diff / 60
            val m = diff % 60
            if (m == 0) DepartureTimeState.Hours(h, clockHHmm)
            else DepartureTimeState.HoursMinutes(h, m, clockHHmm)
        }
        else -> DepartureTimeState.Absolute(clockHHmm)
    }
}

/**
 * Computes a [DepartureTimeState] from an "HH:mm" or "HH:mm:ss" schedule
 * string, using the operator timezone as the reference for "now". Safe to
 * call with any string — returns [DepartureTimeState.Absolute] if parsing
 * fails so the caller always has something to render.
 *
 * @param relativeThreshold see [computeDepartureTimeState]
 * @param formatClock maps the raw GTFS time string to the clock label shown to
 *   the user. Defaults to the raw "HH:mm" (24h) for non-UI callers that only
 *   read the countdown; composable callers pass `{ ClockTime.gtfs(it, context) }`
 *   so the clock respects the device 12-/24-hour setting.
 */
fun departureTimeState(
    timeStr: String,
    operatorTimezoneId: String = "UTC",
    relativeThreshold: Int = 60,
    formatClock: (String) -> String = { it.take(5) },
): DepartureTimeState {
    val clock = formatClock(timeStr)
    return try {
        val parts = timeStr.split(":")
        val depMinutes = parts[0].toInt() * 60 + parts[1].toInt()
        val tz = java.util.TimeZone.getTimeZone(operatorTimezoneId)
        val cal = java.util.Calendar.getInstance(tz)
        val nowMin = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        computeDepartureTimeState(depMinutes, nowMin, clock, relativeThreshold)
    } catch (_: Exception) {
        DepartureTimeState.Absolute(clock)
    }
}

/**
 * Renders a departure time in the canonical stacked layout: countdown on top,
 * absolute clock on the bottom. Drop it in wherever a schedule or live-feed
 * departure needs to be shown consistently.
 *
 * - `isEmphasis`: optional — renders the countdown larger and in accent color
 *   (used for the "next departure" row at the top of the stop detail).
 * - `liveDot`: optional — green dot inline a sinistra del countdown, indica
 *   feed realtime. Niente gap tra dot e digit (stesso frame minWidth=52dp).
 */
@Composable
fun TimeDisplay(
    state: DepartureTimeState,
    modifier: Modifier = Modifier,
    isEmphasis: Boolean = false,
    liveDot: Boolean = false,
) {
    val colors = TransitTheme.colors
    val countdownFont: TextUnit = if (isEmphasis) 20.sp else 15.sp
    val clockFont: TextUnit = 11.sp

    Column(
        modifier = modifier.defaultMinSize(minWidth = 52.dp),
        horizontalAlignment = Alignment.End,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Live dot inline — mai durante Departing (l'arrow pulse già lo segnala)
            // e mai per Passed (partenza è già andata, no point).
            // Live badge (pill "● LIVE") — visibile solo nei case relativi.
            // Mai durante Departing (l'arrow pulse già lo segnala) né Passed.
            // Parità Movete: liveDot=true ≡ delay plausibile presente (non
            // mera presenza veicolo nel positions feed).
            val showLiveDot = liveDot
                && state !is DepartureTimeState.Departing
                && state !is DepartureTimeState.Passed
            if (showLiveDot) {
                LiveBadge()
            }
            when (state) {
                is DepartureTimeState.Departing -> {
                    DepartingPulse(isEmphasis = isEmphasis)
                }
                is DepartureTimeState.Minutes -> {
                    Text(
                        text = "${state.minutes}m",
                        fontSize = countdownFont,
                        fontWeight = FontWeight.Bold,
                        color = if (state.minutes <= 5) colors.realtimeGreen else colors.textPrimary,
                    )
                }
                is DepartureTimeState.Hours -> {
                    Text(
                        text = "${state.hours}h",
                        fontSize = countdownFont,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                }
                is DepartureTimeState.HoursMinutes -> {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${state.hours}h",
                            fontSize = countdownFont,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = "${state.minutes}m",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textSecondary,
                        )
                    }
                }
                is DepartureTimeState.Absolute -> {
                    Text(
                        text = ClockTime.annotated(state.clock, 15.sp, colors.textPrimary),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                    )
                }
                is DepartureTimeState.Passed -> {
                    Text(
                        text = ClockTime.annotated(state.clock, 13.sp, colors.textTertiary),
                        fontSize = 13.sp,
                        color = colors.textTertiary,
                    )
                }
            }
        }
        // Clock secondario solo per i case relativi (Minutes / Hours / HoursMinutes
        // / Departing). Absolute e Passed sono già un orario, niente duplicazione.
        when (state) {
            is DepartureTimeState.Departing -> ClockText(state.clock, clockFont, colors.textSecondary)
            is DepartureTimeState.Minutes -> ClockText(state.clock, clockFont, colors.textSecondary)
            is DepartureTimeState.Hours -> ClockText(state.clock, clockFont, colors.textSecondary)
            is DepartureTimeState.HoursMinutes -> ClockText(state.clock, clockFont, colors.textSecondary)
            is DepartureTimeState.Absolute, is DepartureTimeState.Passed -> Unit
        }
    }
}

@Composable
private fun DepartingPulse(isEmphasis: Boolean) {
    val colors = TransitTheme.colors
    val transition = rememberInfiniteTransition(label = "departingPulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(if (isEmphasis) 8.dp else 7.dp)
                .alpha(pulseAlpha)
                .background(colors.realtimeGreen, CircleShape),
        )
        Text(
            text = "→",
            fontSize = if (isEmphasis) 20.sp else 17.sp,
            fontWeight = FontWeight.Bold,
            color = colors.realtimeGreen,
        )
    }
}

@Composable
private fun ClockText(
    clock: String,
    fontSize: TextUnit,
    tint: Color,
) {
    Text(
        text = ClockTime.annotated(clock, fontSize, tint),
        fontSize = fontSize,
        fontWeight = FontWeight.Medium,
        color = tint,
    )
}
