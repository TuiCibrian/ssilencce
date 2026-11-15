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

package com.ssilencce.ui.screen.sleeptimer

/**
 * One persisted time-based sleep-timer choice. Song-count starts are intentionally not part of
 * the history; only durations and end-at times are reused.
 */
sealed interface SleepTimerHistoryEntry {
    data class Duration(
        val minutes: Int,
    ) : SleepTimerHistoryEntry

    data class EndAt(
        val hour: Int,
        val minute: Int,
    ) : SleepTimerHistoryEntry

    companion object {
        const val MAX_ENTRIES = 5

        private const val ENTRY_SEPARATOR = "|"
        private const val VALUE_SEPARATOR = ":"
        private const val DURATION_PREFIX = "d"
        private const val END_AT_PREFIX = "e"
        private const val MIN_DURATION_MINUTES = 5
        private const val MAX_DURATION_MINUTES = 180

        fun decode(raw: String?): List<SleepTimerHistoryEntry> {
            if (raw.isNullOrBlank()) return emptyList()
            return raw
                .split(ENTRY_SEPARATOR)
                .mapNotNull(::decodeEntry)
                .distinct()
                .take(MAX_ENTRIES)
        }

        fun encode(entries: List<SleepTimerHistoryEntry>): String = entries.take(MAX_ENTRIES).joinToString(ENTRY_SEPARATOR, transform = ::encodeEntry)

        fun add(
            entries: List<SleepTimerHistoryEntry>,
            entry: SleepTimerHistoryEntry,
        ): List<SleepTimerHistoryEntry> = (listOf(entry) + entries.filterNot { it == entry }).take(MAX_ENTRIES)

        private fun decodeEntry(raw: String): SleepTimerHistoryEntry? {
            val parts = raw.split(VALUE_SEPARATOR)
            if (parts.size < 2) return null
            return when (parts[0]) {
                DURATION_PREFIX -> {
                    val minutes = parts.getOrNull(1)?.toIntOrNull() ?: return null
                    Duration(minutes).takeIf { it.minutes in MIN_DURATION_MINUTES..MAX_DURATION_MINUTES }
                }

                END_AT_PREFIX -> {
                    val hour = parts.getOrNull(1)?.toIntOrNull() ?: return null
                    val minute = parts.getOrNull(2)?.toIntOrNull() ?: return null
                    EndAt(hour, minute).takeIf { it.hour in 0..23 && it.minute in 0..59 }
                }

                else -> {
                    null
                }
            }
        }

        private fun encodeEntry(entry: SleepTimerHistoryEntry): String =
            when (entry) {
                is Duration -> listOf(DURATION_PREFIX, entry.minutes).joinToString(VALUE_SEPARATOR)
                is EndAt -> listOf(END_AT_PREFIX, entry.hour, entry.minute).joinToString(VALUE_SEPARATOR)
            }
    }
}
