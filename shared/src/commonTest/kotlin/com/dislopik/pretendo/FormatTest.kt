package com.dislopik.pretendo

import com.dislopik.pretendo.data.Format
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Dates are parsed by hand rather than by a date library, so the arithmetic is worth
 * pinning down: leap years, month boundaries and the epoch itself.
 */
class FormatTest {

    @Test
    fun parsesTheTimestampShapeDiscourseSends() {
        // 2026-07-30T01:00:16.813Z
        val millis = Format.parseIso8601("2026-07-30T01:00:16.813Z")
        assertEquals(1785373216813L, millis)
    }

    @Test
    fun parsesTheEpochItself() {
        assertEquals(0L, Format.parseIso8601("1970-01-01T00:00:00.000Z"))
    }

    @Test
    fun handlesLeapDays() {
        // 2024-02-29 is a real date; 2024-03-01 is the day after it.
        val leapDay = Format.parseIso8601("2024-02-29T00:00:00.000Z")!!
        val nextDay = Format.parseIso8601("2024-03-01T00:00:00.000Z")!!
        assertEquals(86_400_000L, nextDay - leapDay)
    }

    @Test
    fun handlesTimestampsWithoutMilliseconds() {
        assertEquals(
            Format.parseIso8601("2026-01-02T03:04:05.000Z"),
            Format.parseIso8601("2026-01-02T03:04:05Z")
        )
    }

    @Test
    fun rejectsThingsThatAreNotTimestamps() {
        assertNull(Format.parseIso8601(null))
        assertNull(Format.parseIso8601(""))
        assertNull(Format.parseIso8601("not a date at all"))
    }

    @Test
    fun relativeTimeReadsTheWayAPersonWouldSayIt() {
        val now = Format.parseIso8601("2026-09-10T12:00:00.000Z")!!

        fun ago(millis: Long) = Format.relative(isoOf(now - millis), now)

        assertEquals("just now", ago(30_000))
        assertEquals("12m", ago(12 * 60_000))
        assertEquals("5h", ago(5 * 3_600_000))
        assertEquals("3d", ago(3 * 86_400_000L))
        // Past a week, a day count stops meaning anything, so it becomes a date.
        assertTrue(ago(40 * 86_400_000L).contains("Aug"))
    }

    @Test
    fun relativeTimeShowsTheYearOnlyWhenItDiffers() {
        val now = Format.parseIso8601("2026-09-10T12:00:00.000Z")!!
        assertEquals("1 Aug", Format.relative("2026-08-01T09:00:00.000Z", now))
        assertEquals("1 Aug 2024", Format.relative("2024-08-01T09:00:00.000Z", now))
    }

    @Test
    fun absoluteDatesSpellEverythingOut() {
        assertEquals(
            "4 Mar 2026 at 14:30",
            Format.absolute("2026-03-04T14:30:00.000Z")
        )
        // A single-digit hour keeps its leading zero so times stay the same width.
        assertEquals(
            "4 Mar 2026 at 09:05",
            Format.absolute("2026-03-04T09:05:00.000Z")
        )
    }

    @Test
    fun theAbsoluteDatesSettingPicksWhichFormIsUsed() {
        val value = "2026-03-04T14:30:00.000Z"
        assertEquals(Format.absolute(value), Format.timestamp(value, absoluteDates = true))
        assertTrue(Format.timestamp(value, absoluteDates = false).length < 12)
    }

    @Test
    fun countsShortenOnceTheyStopFittingInARow() {
        assertEquals("0", Format.count(0))
        assertEquals("999", Format.count(999))
        assertEquals("1k", Format.count(1_000))
        assertEquals("1.2k", Format.count(1_234))
        assertEquals("12.3k", Format.count(12_345))
        assertEquals("1.3m", Format.count(1_300_000))
    }

    @Test
    fun readingTimeGrowsWithTheUnitsThatFit() {
        assertEquals("30m", Format.readingTime(30 * 60))
        assertEquals("2h", Format.readingTime(2 * 3600))
        assertEquals("4d", Format.readingTime(4 * 24 * 3600L))
    }

    @Test
    fun joinDatesReadAsAMonthAndYear() {
        assertEquals("May 2024", Format.monthAndYear("2024-05-18T19:43:07.833Z"))
    }

    /** Builds a timestamp string from millis, for the relative-time cases above. */
    private fun isoOf(millis: Long): String {
        val days = millis / 86_400_000L
        val remainder = millis % 86_400_000L
        // Walk forward from the epoch: enough for the ranges these tests use.
        var year = 1970
        var remainingDays = days
        while (true) {
            val length = if (isLeap(year)) 366 else 365
            if (remainingDays < length) break
            remainingDays -= length
            year++
        }
        val monthLengths = listOf(
            31, if (isLeap(year)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
        )
        var month = 1
        for (length in monthLengths) {
            if (remainingDays < length) break
            remainingDays -= length
            month++
        }
        val day = remainingDays + 1
        val hour = remainder / 3_600_000L
        val minute = (remainder % 3_600_000L) / 60_000L
        val second = (remainder % 60_000L) / 1000L
        return "$year-${pad(month.toLong())}-${pad(day)}T" +
            "${pad(hour)}:${pad(minute)}:${pad(second)}.000Z"
    }

    private fun isLeap(year: Int) = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

    private fun pad(value: Long) = if (value < 10) "0$value" else value.toString()
}
