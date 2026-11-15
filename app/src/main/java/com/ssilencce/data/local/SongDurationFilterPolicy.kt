/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.ssilencce.data.local

/**
 * Pure maximum/minimum song-duration filter rules.
 *
 * A maximum of 0 means unlimited. Both bounds are inclusive and are expressed in whole seconds;
 * callers format the MediaStore duration column in milliseconds.
 */
object SongDurationFilterPolicy {
    const val MAX_SECONDS = 7200
    const val STEP_SECONDS = 60

    fun isMaximumEnabled(maxSeconds: Int): Boolean = maxSeconds > 0

    fun isValidRange(
        minSeconds: Int,
        maxSeconds: Int,
    ): Boolean =
        minSeconds in 0..MAX_SECONDS &&
            maxSeconds in 0..MAX_SECONDS &&
            (maxSeconds == 0 || maxSeconds >= minSeconds)

    /**
     * Returns the inclusive duration predicates in minimum-then-maximum order.
     * A bound of zero (or a negative defensive value) contributes no predicate.
     */
    fun selectionFragments(
        column: String,
        minSeconds: Int,
        maxSeconds: Int,
    ): List<String> =
        buildList {
            if (minSeconds > 0) {
                add("$column >= ${minSeconds.toLong() * 1000L}")
            }
            if (maxSeconds > 0) {
                add("$column <= ${maxSeconds.toLong() * 1000L}")
            }
        }
}
