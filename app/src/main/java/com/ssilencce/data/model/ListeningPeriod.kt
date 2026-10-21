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

package com.ssilencce.data.model

import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/**
 * Periods available for play statistics. Cutoffs use the device's local time zone.
 * Period data starts when play events begin being recorded; [AllTime] uses the aggregate
 * play-count table and therefore also includes earlier plays.
 */
enum class ListeningPeriod {
    ThisWeek,
    ThisMonth,
    Last12Months,
    ThisYear,
    AllTime,
    ;

    fun cutoffMillis(now: ZonedDateTime = ZonedDateTime.now()): Long? =
        when (this) {
            ThisWeek -> {
                now
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .toLocalDate()
                    .atStartOfDay(now.zone)
                    .toInstant()
                    .toEpochMilli()
            }

            ThisMonth -> {
                now
                    .withDayOfMonth(1)
                    .toLocalDate()
                    .atStartOfDay(now.zone)
                    .toInstant()
                    .toEpochMilli()
            }

            Last12Months -> {
                now.minusMonths(12).toInstant().toEpochMilli()
            }

            ThisYear -> {
                now
                    .withDayOfYear(1)
                    .toLocalDate()
                    .atStartOfDay(now.zone)
                    .toInstant()
                    .toEpochMilli()
            }

            AllTime -> {
                null
            }
        }
}
