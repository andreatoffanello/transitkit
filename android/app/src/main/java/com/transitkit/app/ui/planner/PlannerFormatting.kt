package com.transitkit.app.ui.planner

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val hhmm = SimpleDateFormat("HH:mm", Locale.getDefault())

/** Epoch millis → "HH:mm" in the device locale. */
internal fun formatEpochTime(epochMs: Long): String = hhmm.format(Date(epochMs))

/** java.util.Date → "HH:mm" in the device locale. */
internal fun formatShortTime(date: Date): String = hhmm.format(date)
