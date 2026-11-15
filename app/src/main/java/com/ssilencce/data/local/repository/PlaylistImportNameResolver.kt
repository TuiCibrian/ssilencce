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

package com.ssilencce.data.local.repository

/**
 * Pure naming rules for interactive playlist import.
 *
 * Names are compared exactly and case-sensitively, matching the DAO's SQL equality. Rename
 * input is trimmed before validation and storage; the source name is kept as provided because
 * it is already the stored device-playlist name.
 */
object PlaylistImportNameResolver {
    const val MAX_SUFFIX = 100

    /** Returns the first free source name or `sourceName N`, or null when 1..100 are taken. */
    fun resolveName(
        sourceName: String,
        existingNames: Collection<String>,
    ): String? {
        val existing = existingNames.toHashSet()
        if (sourceName !in existing) return sourceName
        for (suffix in 1..MAX_SUFFIX) {
            val candidate = "$sourceName $suffix"
            if (candidate !in existing) return candidate
        }
        return null
    }

    fun validateRename(
        rawName: String,
        existingNames: Collection<String>,
    ): RenameValidation {
        val name = rawName.trim()
        return when {
            name.isEmpty() -> RenameValidation.Blank
            name in existingNames -> RenameValidation.Duplicate(name)
            else -> RenameValidation.Valid(name)
        }
    }

    sealed interface RenameValidation {
        data object Blank : RenameValidation

        data class Duplicate(
            val name: String,
        ) : RenameValidation

        data class Valid(
            val name: String,
        ) : RenameValidation
    }
}
