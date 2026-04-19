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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transitkit.app.config.TransitTheme

// -----------------------------------------------------------------------------
// TimeDisplay — single source of truth for departure-time rendering.
// -----------------------------------------------------------------------------
// Layout (parity iOS):
//
//   row 1 — countdown        "5'"   (or pulsing dot when departing now)
//   row 2 — scheduled/actual "16:20"  (monospaced, secondary color)
//
// When the departure is more than 60 min away the countdown collapses to the
// absolute clock time alone. Past departures render in a tertiary tint.
// -----------------------------------------------------------------------------

sealed class DepartureTimeState {
    /** Departure > 60 min away — show the clock only. */
    data class Absolute(val clock: String) : DepartureTimeState()
    /** Countdown (1-60 min). */
    data class Minutes(val minutes: Int, val clock: String) : DepartureTimeState()
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
 */
fun computeDepartureTimeState(
    departureMinutes: Int,
    nowMinutes: Int,
    clockHHmm: String,
): DepartureTimeState {
    val diff = departureMinutes - nowMinutes
    return when {
        diff < 0 -> DepartureTimeState.Passed(clockHHmm)
        diff == 0 -> DepartureTimeState.Departing(clockHHmm)
        diff <= 60 -> DepartureTimeState.Minutes(diff, clockHHmm)
        else -> DepartureTimeState.Absolute(clockHHmm)
    }
}

/**
 * Computes a [DepartureTimeState] from an "HH:mm" or "HH:mm:ss" schedule
 * string, using the operator timezone as the reference for "now". Safe to
 * call with any string — returns [DepartureTimeState.Absolute] if parsing
 * fails so the caller always has something to render.
 */
fun departureTimeState(
    timeStr: String,
    operatorTimezoneId: String = "UTC",
): DepartureTimeState {
    val hhmm = timeStr.take(5)
    return try {
        val parts = timeStr.split(":")
        val depMinutes = parts[0].toInt() * 60 + parts[1].toInt()
        val tz = java.util.TimeZone.getTimeZone(operatorTimezoneId)
        val cal = java.util.Calendar.getInstance(tz)
        val nowMin = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        computeDepartureTimeState(depMinutes, nowMin, hhmm)
    } catch (_: Exception) {
        DepartureTimeState.Absolute(hhmm)
    }
}

/**
 * Renders a departure time in the canonical stacked layout: minutes on top,
 * absolute clock on the bottom. Drop it in wherever a schedule or live-feed
 * departure needs to be shown consistently.
 *
 * - `isEmphasis`: optional — renders the countdown larger and in the accent
 *   color (used for the "next departure" row at the top of the stop detail).
 */
@Composable
fun TimeDisplay(
    state: DepartureTimeState,
    modifier: Modifier = Modifier,
    isEmphasis: Boolean = false,
) {
    val colors = TransitTheme.colors
    val minutesFont: TextUnit = if (isEmphasis) 20.sp else 17.sp
    val tickFont: TextUnit = if (isEmphasis) 14.sp else 12.sp
    val clockFont: TextUnit = 11.sp

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
    ) {
        when (state) {
            is DepartureTimeState.Departing -> {
                DepartingPulse(isEmphasis = isEmphasis)
                ClockText(state.clock, fontSize = clockFont, tint = colors.textSecondary)
            }
            is DepartureTimeState.Minutes -> {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${state.minutes}",
                        fontSize = minutesFont,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (state.minutes <= 5) colors.realtimeGreen else colors.textPrimary,
                    )
                    Text(
                        text = "'",
                        fontSize = tickFont,
                        fontWeight = FontWeight.Bold,
                        color = if (state.minutes <= 5) colors.realtimeGreen else colors.textSecondary,
                    )
                }
                ClockText(state.clock, fontSize = clockFont, tint = colors.textSecondary)
            }
            is DepartureTimeState.Absolute -> {
                // Far-future: clock as the primary glyph, no row 2.
                Text(
                    text = state.clock,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = colors.textPrimary,
                )
            }
            is DepartureTimeState.Passed -> {
                Text(
                    text = state.clock,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    color = colors.textTertiary,
                )
            }
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
            text = "\u2192",
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
        text = clock,
        fontSize = fontSize,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Monospace,
        color = tint,
    )
}
