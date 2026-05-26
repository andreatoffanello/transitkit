package com.transitkit.app.ui.planner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Operator timezone for displaying transit times in the planner.
 * Defaults to UTC if unset — call sites inside the planner tree should always
 * see the operator zone provided by `PlannerScreen` / `JourneyDetailScreen`.
 */
val LocalOperatorTimeZone = compositionLocalOf<TimeZone> { TimeZone.getTimeZone("UTC") }

/** Epoch millis → "HH:mm" formatted in the operator's timezone. */
@Composable
@ReadOnlyComposable
internal fun formatEpochTime(epochMs: Long): String {
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    fmt.timeZone = LocalOperatorTimeZone.current
    return fmt.format(Date(epochMs))
}
