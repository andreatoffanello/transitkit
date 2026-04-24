package com.transitkit.app.ui.orari

import androidx.compose.ui.graphics.Color

@Suppress("unused")
internal fun formatTime(raw: String): String {
    return try {
        val parts = raw.split(":")
        val hour = parts[0].toInt() % 24
        val minute = parts[1].toInt()
        "%02d:%02d".format(hour, minute)
    } catch (_: Exception) {
        raw.take(5)
    }
}

/** WCAG relative-luminance-based contrast picker: returns white on dark bg, black on light bg. */
internal fun contrastOn(bg: Color): Color {
    fun lin(c: Float): Float = if (c <= 0.03928f) c / 12.92f else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    val l = 0.2126f * lin(bg.red) + 0.7152f * lin(bg.green) + 0.0722f * lin(bg.blue)
    return if (l < 0.5f) Color.White else Color(0xFF111827)
}

/** Returns minutes until [timeStr] (HH:mm:ss, can exceed 24h). Null if >60 min or already passed. */
internal fun minutesUntil(timeStr: String, timezone: String = "UTC"): Int? {
    return try {
        val parts = timeStr.split(":")
        val depTotalMin = parts[0].toInt() * 60 + parts[1].toInt()
        val tz = java.util.TimeZone.getTimeZone(timezone)
        val cal = java.util.Calendar.getInstance(tz)
        val nowMin = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        val diff = depTotalMin - nowMin
        if (diff in 0..60) diff else null
    } catch (_: Exception) {
        null
    }
}
