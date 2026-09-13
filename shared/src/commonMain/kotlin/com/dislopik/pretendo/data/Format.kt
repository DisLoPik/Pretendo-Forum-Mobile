package com.dislopik.pretendo.data

/**
 * Dates and counts, formatted for a phone.
 *
 * Discourse always sends UTC timestamps in one shape (`2026-07-30T01:00:16.813Z`), so
 * these are parsed directly rather than through a date library: it keeps the app free of
 * a dependency whose API is still moving, and makes the "show full dates" accessibility
 * option easy to satisfy exactly.
 */
object Format {

    private val MONTHS = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    /** Milliseconds since the epoch, or null if the string is not a timestamp. */
    fun parseIso8601(text: String?): Long? {
        if (text == null || text.length < 19) return null
        return runCatching {
            val year = text.substring(0, 4).toInt()
            val month = text.substring(5, 7).toInt()
            val day = text.substring(8, 10).toInt()
            val hour = text.substring(11, 13).toInt()
            val minute = text.substring(14, 16).toInt()
            val second = text.substring(17, 19).toInt()
            val millis = if (text.length >= 23 && text[19] == '.') {
                text.substring(20, 23).toInt()
            } else {
                0
            }
            val days = daysFromCivil(year, month, day)
            ((days * 86_400L) + (hour * 3600L) + (minute * 60L) + second) * 1000L + millis
        }.getOrNull()
    }

    /**
     * "just now", "12m", "5h", "3d", then a date once a post is over a week old, which is
     * the point at which "42d" stops telling anyone anything useful.
     */
    fun relative(timestamp: String?, now: Long = currentTimeMillis()): String {
        val millis = parseIso8601(timestamp) ?: return ""
        val elapsed = now - millis
        return when {
            elapsed < 0 -> "just now"
            elapsed < 60_000 -> "just now"
            elapsed < 3_600_000 -> "${elapsed / 60_000}m"
            elapsed < 86_400_000 -> "${elapsed / 3_600_000}h"
            elapsed < 7 * 86_400_000L -> "${elapsed / 86_400_000}d"
            else -> shortDate(millis, includeYearIfDifferent = true, now = now)
        }
    }

    /** The full date and time, for the "Always show full dates" accessibility option. */
    fun absolute(timestamp: String?): String {
        val millis = parseIso8601(timestamp) ?: return ""
        val (year, month, day) = civilFromMillis(millis)
        val secondsOfDay = floorMod(millis / 1000L, 86_400L)
        val hour = (secondsOfDay / 3600L).toInt()
        val minute = ((secondsOfDay % 3600L) / 60L).toInt()
        return "$day ${MONTHS[month - 1]} $year at ${twoDigits(hour)}:${twoDigits(minute)}"
    }

    /** Picks whichever of [relative] and [absolute] the reader asked for. */
    fun timestamp(value: String?, absoluteDates: Boolean, now: Long = currentTimeMillis()): String =
        if (absoluteDates) absolute(value) else relative(value, now)

    fun shortDate(
        millis: Long,
        includeYearIfDifferent: Boolean = true,
        now: Long = currentTimeMillis()
    ): String {
        val (year, month, day) = civilFromMillis(millis)
        val (currentYear, _, _) = civilFromMillis(now)
        val suffix = if (includeYearIfDifferent && year != currentYear) " $year" else ""
        return "$day ${MONTHS[month - 1]}$suffix"
    }

    /** The month and year a member joined, for a profile header. */
    fun monthAndYear(timestamp: String?): String {
        val millis = parseIso8601(timestamp) ?: return ""
        val (year, month, _) = civilFromMillis(millis)
        return "${MONTHS[month - 1]} $year"
    }

    /** 1 -> "1", 1200 -> "1.2k", 1_300_000 -> "1.3m". */
    fun count(value: Int): String = when {
        value < 1_000 -> value.toString()
        value < 1_000_000 -> shorten(value, 1_000, "k")
        else -> shorten(value, 1_000_000, "m")
    }

    /** Discourse reports reading time in seconds; a profile wants hours. */
    fun readingTime(seconds: Long): String {
        val minutes = seconds / 60
        return when {
            minutes < 60 -> "${minutes}m"
            minutes < 60 * 24 -> "${minutes / 60}h"
            else -> "${minutes / (60 * 24)}d"
        }
    }

    /**
     * Kept in integer arithmetic on purpose: going through a Double loses the tenth it is
     * trying to report, because a value such as 12.3 cannot be held exactly.
     */
    private fun shorten(value: Int, unit: Int, suffix: String): String {
        val whole = value / unit
        val tenths = (value % unit) * 10 / unit
        return if (tenths == 0) "$whole$suffix" else "$whole.$tenths$suffix"
    }

    private fun twoDigits(value: Int): String = if (value < 10) "0$value" else value.toString()

    private fun civilFromMillis(millis: Long): Triple<Int, Int, Int> =
        civilFromDays(floorDiv(millis, 86_400_000L))

    /**
     * Howard Hinnant's days-from-civil algorithm, and its inverse. Both are exact for any
     * date the forum can hold and need no lookup tables.
     */
    private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
        val y = if (month <= 2) year - 1 else year
        val era = floorDiv(y.toLong(), 400L)
        val yoe = y - era * 400
        val doy = (153 * (if (month > 2) month - 3 else month + 9) + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146_097 + doe - 719_468
    }

    private fun civilFromDays(days: Long): Triple<Int, Int, Int> {
        val z = days + 719_468
        val era = floorDiv(z, 146_097L)
        val doe = z - era * 146_097
        val yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = (doy - (153 * mp + 2) / 5 + 1).toInt()
        val m = (if (mp < 10) mp + 3 else mp - 9).toInt()
        return Triple((if (m <= 2) y + 1 else y).toInt(), m, d)
    }

    private fun floorDiv(a: Long, b: Long): Long {
        val q = a / b
        return if (a % b != 0L && (a xor b) < 0) q - 1 else q
    }

    private fun floorMod(a: Long, b: Long): Long = a - floorDiv(a, b) * b
}
