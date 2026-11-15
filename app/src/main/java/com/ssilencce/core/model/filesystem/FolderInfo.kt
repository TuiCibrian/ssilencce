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

package com.ssilencce.core.model.filesystem

/**
 * Snapshot of a folder shown by the folder info dialog. Counts and sizes include songs in
 * sub-folders; [songCount] is the number of songs directly inside the folder.
 */
data class FolderInfo(
    val path: String,
    val songCount: Int,
    val totalSongCount: Int,
    val subFolderCount: Int,
    val totalDurationMs: Long,
    val totalSizeBytes: Long,
    val lastModifiedSeconds: Long,
    val createdSeconds: Long,
)
