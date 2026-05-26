package com.transitkit.app.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.transitkit.app.R
import com.transitkit.app.data.model.Departure
import com.transitkit.app.ui.components.DepartureTimeState
import com.transitkit.app.ui.components.departureTimeState

@Composable
internal fun walkingTimeLabel(meters: Double): String {
    val minutes = kotlin.math.ceil(meters / 80.0).toInt()
    return when {
        minutes <= 1 -> stringResource(R.string.walking_1_min)
        minutes > 10 -> stringResource(R.string.walking_10_plus_min)
        else -> stringResource(R.string.walking_n_min, minutes)
    }
}

internal fun isWithinFiveMinutes(dep: Departure, tz: String): Boolean {
    val raw = dep.realtimeDepartureTime ?: dep.departureTime
    return when (val state = departureTimeState(raw, tz)) {
        is DepartureTimeState.Departing -> true
        is DepartureTimeState.Minutes -> state.minutes in 0..5
        else -> false
    }
}
