package com.transitkit.app.ui.components

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Formats transit clock times respecting the device's 12-/24-hour setting.
 *
 * US riders default to 12-hour ("4:45 PM"); a rider who turned on
 * Settings → System → Date & time → "Use 24-hour format" sees "16:45". We never
 * hardcode the hour cycle — [DateFormat.getTimeFormat] resolves it from the
 * system setting + locale, matching the web `formatClockTime` behavior and the
 * iOS `ClockTime` helper.
 */
object ClockTime {

    /**
     * GTFS wall-clock "HH:mm" / "HH:mm:ss" → localized clock. The hour/minute
     * are taken verbatim (the string is already operator-local), so this is
     * timezone-independent. Hours ≥ 24 wrap ("25:10" → "1:10 AM"). Returns the
     * input unchanged if it can't be parsed.
     */
    fun gtfs(timeStr: String, context: Context): String {
        val parts = timeStr.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: return timeStr
        val m = parts.getOrNull(1)?.toIntOrNull() ?: return timeStr
        return formatTimeFmt(calendarUtc(h, m).time, UTC, context)
    }

    /** An instant (epoch millis) → localized clock in [tz]. */
    fun millis(epochMs: Long, tz: TimeZone, context: Context): String =
        formatTimeFmt(Date(epochMs), tz, context)

    /** Hour-only schedule-rail header: GTFS 2-digit hour "16" → "4 PM" / "16". */
    fun hourHeader(twoDigitHour: String, context: Context): String {
        val h = twoDigitHour.toIntOrNull() ?: return twoDigitHour
        val locale = primaryLocale(context)
        val skeleton = if (DateFormat.is24HourFormat(context)) "H" else "ha"
        val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
        return SimpleDateFormat(pattern, locale).apply { timeZone = UTC }
            .format(calendarUtc(h, 0).time)
    }

    /**
     * Localized "MMM d, <time>" for an instant in [tz] — alert validity windows.
     * The time portion follows the device 12-/24-hour setting.
     */
    fun dayAtTime(epochMs: Long, tz: TimeZone, context: Context): String {
        val locale = primaryLocale(context)
        val datePattern = DateFormat.getBestDateTimePattern(locale, "MMMd")
        val date = SimpleDateFormat(datePattern, locale).apply { timeZone = tz }.format(Date(epochMs))
        return "$date, ${millis(epochMs, tz, context)}"
    }

    /**
     * Renders a formatted clock string ("4:45 PM", or the rail header "4 PM")
     * with the AM/PM meridiem set smaller (0.7×) and more muted than the
     * numerals — the Apple Clock / Citymapper treatment. The numerals inherit
     * the host `Text` style; only the meridiem span is restyled. Pass the host
     * Text's [baseSize] and [baseColor] so the meridiem scales/tints relative to
     * it. On 24h locales (no meridiem) returns the plain string unchanged.
     */
    fun annotated(formatted: String, baseSize: TextUnit, baseColor: Color): AnnotatedString {
        val split = splitMeridiem(formatted) ?: return AnnotatedString(formatted)
        return buildAnnotatedString {
            append(split.first)
            withStyle(
                SpanStyle(
                    fontSize = baseSize * 0.7f,
                    color = baseColor.copy(alpha = baseColor.alpha * 0.55f),
                ),
            ) {
                append(" ${split.second}")
            }
        }
    }

    private val amPmSymbols: List<String> by lazy {
        DateFormatSymbols(Locale.getDefault()).amPmStrings.filter { it.isNotEmpty() }
    }

    /** (numerals, meridiem) when the string ends with the locale AM/PM symbol. */
    private fun splitMeridiem(s: String): Pair<String, String>? {
        for (sym in amPmSymbols) {
            if (s.endsWith(sym)) {
                val numerals = s.dropLast(sym.length).trim()
                if (numerals.isNotEmpty()) return numerals to sym
            }
        }
        return null
    }

    private val UTC: TimeZone = TimeZone.getTimeZone("UTC")

    private fun formatTimeFmt(date: Date, tz: TimeZone, context: Context): String {
        val fmt = DateFormat.getTimeFormat(context)
        fmt.timeZone = tz
        return fmt.format(date)
    }

    private fun calendarUtc(hour: Int, minute: Int): Calendar =
        Calendar.getInstance(UTC).apply {
            set(Calendar.HOUR_OF_DAY, hour % 24)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

    private fun primaryLocale(context: Context): Locale =
        context.resources.configuration.locales[0]
}
