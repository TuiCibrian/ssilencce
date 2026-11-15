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
 * Rows removed by the "Remove missing songs" maintenance action. All values are row counts so a
 * single summary can report exactly what was cleaned.
 */
data class MissingSongsCleanupResult(
    val missingSongs: Int,
    val playlistEntries: Int,
    val playCountEntries: Int,
    val playEventEntries: Int,
    val historyEntries: Int,
)
