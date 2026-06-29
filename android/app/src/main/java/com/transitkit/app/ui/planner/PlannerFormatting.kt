package com.transitkit.app.ui.planner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.transitkit.app.ui.components.ClockTime
import java.util.TimeZone

/**
 * Operator timezone for displaying transit times in the planner.
 * Defaults to UTC if unset — call sites inside the planner tree should always
 * see the operator zone provided by `PlannerScreen` / `JourneyDetailScreen`.
 */
val LocalOperatorTimeZone = compositionLocalOf<TimeZone> { TimeZone.getTimeZone("UTC") }

/**
 * Epoch millis → localized clock (12h/24h per device setting) in the operator's
 * timezone.
 */
@Composable
@ReadOnlyComposable
internal fun formatEpochTime(epochMs: Long): String =
    ClockTime.millis(epochMs, LocalOperatorTimeZone.current, LocalContext.current)
