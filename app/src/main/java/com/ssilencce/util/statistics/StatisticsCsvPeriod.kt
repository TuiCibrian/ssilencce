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

package com.ssilencce.util.statistics

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

enum class StatisticsCsvCadence(
    val storageValue: String,
) {
    Weekly("weekly"),
    Monthly("monthly"),
    Yearly("yearly"),
    ;

    companion object {
        fun fromStorageValue(value: String?): StatisticsCsvCadence = entries.firstOrNull { it.storageValue == value } ?: Weekly
    }
}

/**
 * One completed local calendar period. [endDateInclusive] is inclusive for the file name; the
 * play-event query end is the next day at local midnight, exclusive.
 */
data class StatisticsCsvWindow(
    val cadence: StatisticsCsvCadence,
    val startDate: LocalDate,
    val endDateInclusive: LocalDate,
) {
    val key: String
        get() =
            when (cadence) {
                StatisticsCsvCadence.Weekly -> "weekly:$startDate"
                StatisticsCsvCadence.Monthly -> "monthly:${YearMonth.from(startDate)}"
                StatisticsCsvCadence.Yearly -> "yearly:${startDate.year}"
            }

    fun startMillis(zone: ZoneId): Long = startDate.atStartOfDay(zone).toInstant().toEpochMilli()

    fun endExclusiveMillis(zone: ZoneId): Long =
        endDateInclusive
            .plusDays(1)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()

    fun fileStem(): String = "${startDate}_$endDateInclusive"
}

/**
 * Calendar arithmetic for the latest completed week/month/year. Android-free so it can be
 * probed on the JVM with a fixed clock.
 */
object StatisticsCsvSchedule {
    fun latestCompleted(
        cadence: StatisticsCsvCadence,
        now: ZonedDateTime,
    ): StatisticsCsvWindow {
        val today = now.toLocalDate()
        val currentStart =
            when (cadence) {
                StatisticsCsvCadence.Weekly -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                StatisticsCsvCadence.Monthly -> today.withDayOfMonth(1)
                StatisticsCsvCadence.Yearly -> today.withDayOfYear(1)
            }
        val previousStart =
            when (cadence) {
                StatisticsCsvCadence.Weekly -> currentStart.minusWeeks(1)
                StatisticsCsvCadence.Monthly -> currentStart.minusMonths(1)
                StatisticsCsvCadence.Yearly -> currentStart.minusYears(1)
            }
        return StatisticsCsvWindow(
            cadence = cadence,
            startDate = previousStart,
            endDateInclusive = currentStart.minusDays(1),
        )
    }

    /**
     * Whether the target still needs a run. A failed period stays pending and always retries;
     * otherwise the persisted completed key (Success or NoData) suppresses a duplicate export
     * for the same period, even when the provider cannot enumerate the existing files.
     */
    fun needsExport(
        latestKey: String,
        pendingKey: String?,
        lastKey: String?,
    ): Boolean = pendingKey != null || lastKey != latestKey

    /**
     * Rebuilds a pending target from its persisted key. Returns null for malformed or
     * mismatched keys so the worker can fall back to the latest completed period.
     */
    fun windowFromKey(
        cadence: StatisticsCsvCadence,
        key: String,
    ): StatisticsCsvWindow? {
        val prefix = "${cadence.storageValue}:"
        if (!key.startsWith(prefix)) return null
        return runCatching {
            val body = key.substring(prefix.length)
            when (cadence) {
                StatisticsCsvCadence.Weekly -> {
                    val start = LocalDate.parse(body)
                    if (start.year !in MIN_KEY_YEAR..MAX_KEY_YEAR || start.dayOfWeek != DayOfWeek.MONDAY) {
                        null
                    } else {
                        StatisticsCsvWindow(cadence, start, start.plusDays(6))
                    }
                }

                StatisticsCsvCadence.Monthly -> {
                    val month = YearMonth.parse(body)
                    if (month.year !in MIN_KEY_YEAR..MAX_KEY_YEAR) {
                        null
                    } else {
                        StatisticsCsvWindow(
                            cadence = cadence,
                            startDate = month.atDay(1),
                            endDateInclusive = month.atEndOfMonth(),
                        )
                    }
                }

                StatisticsCsvCadence.Yearly -> {
                    val year = body.toIntOrNull()
                    if (year == null || year !in MIN_KEY_YEAR..MAX_KEY_YEAR) {
                        null
                    } else {
                        StatisticsCsvWindow(
                            cadence = cadence,
                            startDate = LocalDate.of(year, 1, 1),
                            endDateInclusive = LocalDate.of(year, 12, 31),
                        )
                    }
                }
            }
        }.getOrNull()
    }

    private const val MIN_KEY_YEAR = 1
    private const val MAX_KEY_YEAR = 9999
}

/**
 * Automatic CSV naming and staged-delivery helpers. A file is written under a feature-owned
 * `<final>.partial` name and renamed only after the complete write; stale staging documents are
 * removed at the start of a run, while final and unowned documents are never deleted.
 */
object StatisticsCsvFiles {
    const val AUTO_PREFIX = "ssilencce-auto-top-"
    const val STAGING_SUFFIX = ".partial"

    fun tracksName(window: StatisticsCsvWindow): String = "${AUTO_PREFIX}tracks-${window.fileStem()}.csv"

    fun artistsName(window: StatisticsCsvWindow): String = "${AUTO_PREFIX}artists-${window.fileStem()}.csv"

    fun stagingName(finalName: String): String = finalName + STAGING_SUFFIX

    fun isStagingName(name: String?): Boolean = name != null && name.startsWith(AUTO_PREFIX) && name.endsWith(STAGING_SUFFIX)
}
