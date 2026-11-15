/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ssilencce.playback

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

/**
 * Resolves a future local wall-clock end time strictly after [now].
 *
 * `ZonedDateTime.of` follows the zone rules: a nonexistent local time in a DST gap is
 * shifted forward to the first valid instant, while an ambiguous local time in an overlap
 * uses the earlier offset. If that resolved instant is not strictly after [now], the next
 * calendar day is used. No clocks or offsets are mutated.
 */
object SleepTimerEndTime {
    fun delayMillis(
        now: ZonedDateTime,
        hour: Int,
        minute: Int,
    ): Long {
        requireValid(hour, minute)
        val target = targetFor(now.toLocalDate(), now.zone, hour, minute)
        val next = if (target.isAfter(now)) target else targetFor(now.toLocalDate().plusDays(1), now.zone, hour, minute)
        return Duration.between(now, next).toMillis()
    }

    fun nextFullHour(now: ZonedDateTime): Pair<Int, Int> {
        val nextLocal = now.toLocalDate().atTime(now.hour, 0).plusHours(1)
        val resolved = ZonedDateTime.of(nextLocal, now.zone)
        val next = if (resolved.isAfter(now)) resolved else ZonedDateTime.of(nextLocal.plusHours(1), now.zone)
        return next.hour to next.minute
    }

    fun format(
        hour: Int,
        minute: Int,
    ): String {
        requireValid(hour, minute)
        return String.format(Locale.ROOT, "%02d:%02d", hour, minute)
    }

    private fun targetFor(
        date: LocalDate,
        zone: ZoneId,
        hour: Int,
        minute: Int,
    ): ZonedDateTime = ZonedDateTime.of(date, LocalTime.of(hour, minute), zone)

    private fun requireValid(
        hour: Int,
        minute: Int,
    ) {
        require(hour in 0..23) { "hour must be in 0..23" }
        require(minute in 0..59) { "minute must be in 0..59" }
    }
}
