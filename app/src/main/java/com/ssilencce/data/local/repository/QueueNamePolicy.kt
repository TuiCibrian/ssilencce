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

package com.ssilencce.data.local.repository

/**
 * Pure named-queue naming and limit rules shared by the repository and the manager UI.
 * [MAX_NAMED_QUEUES] and [MAX_NAME_LENGTH] are enforced in the repository as well, so callers
 * cannot bypass the UI.
 */
object QueueNamePolicy {
    const val MAX_NAMED_QUEUES = 100
    const val MAX_NAME_LENGTH = 80
    const val DEFAULT_NAME = "Queue"

    enum class Error {
        Blank,
        TooLong,
        Duplicate,
    }

    sealed interface Validation {
        data class Valid(
            val name: String,
        ) : Validation

        data class Invalid(
            val error: Error,
        ) : Validation
    }

    fun normalize(requested: String): String = requested.trim().ifBlank { DEFAULT_NAME }

    fun validate(
        requested: String,
        existingNames: Collection<String>,
        exceptName: String? = null,
    ): Validation {
        val name = requested.trim()
        if (name.isBlank()) return Validation.Invalid(Error.Blank)
        if (name.length > MAX_NAME_LENGTH) return Validation.Invalid(Error.TooLong)
        if (existingNames.any { it.equals(name, ignoreCase = true) && !it.equals(exceptName, ignoreCase = true) }) {
            return Validation.Invalid(Error.Duplicate)
        }
        return Validation.Valid(name)
    }

    /**
     * Existing suffix rule used by "Keep both". The base is truncated so the suffix still fits
     * within [MAX_NAME_LENGTH].
     */
    fun uniqueName(
        requested: String,
        existingNames: Collection<String>,
    ): String {
        val base = normalize(requested)
        val lowered = existingNames.map { it.lowercase() }.toSet()
        if (base.lowercase() !in lowered) return base.take(MAX_NAME_LENGTH)

        var suffix = 2
        while (true) {
            val suffixText = " ($suffix)"
            val candidate =
                if (base.length + suffixText.length <= MAX_NAME_LENGTH) {
                    base + suffixText
                } else {
                    base
                        .take((MAX_NAME_LENGTH - suffixText.length).coerceAtLeast(1))
                        .trimEnd() + suffixText
                }
            if (candidate.lowercase() !in lowered) return candidate
            suffix++
        }
    }

    fun canCreate(existingCount: Int): Boolean = existingCount < MAX_NAMED_QUEUES
}
