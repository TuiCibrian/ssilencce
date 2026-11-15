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

package com.ssilencce.util

import com.ssilencce.core.model.filesystem.FolderInfo
import com.ssilencce.data.local.repository.Repository
import com.ssilencce.data.model.Folder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

/**
 * Builds the [FolderInfo] snapshot for the folder info dialog. Everything is derived from
 * MediaStore data so it also works for folders that are not directly readable through java.io
 * on scoped storage.
 */
object FolderInfoProvider {
    suspend fun load(
        repository: Repository,
        path: String,
    ): FolderInfo =
        withContext(Dispatchers.IO) {
            val folder = repository.folderByPath(path)
            val directSongs = folder.songs
            val children = runCatching { repository.filesInPath(path).children }.getOrDefault(emptyList())
            val subFolderCount = children.count { it is Folder }
            val allSongs =
                runCatching { repository.songsByFolder(path, includeSubfolders = true) }
                    .getOrDefault(directSongs)
            val fileLastModifiedSeconds =
                runCatching { File(path).lastModified() / MILLIS_PER_SECOND }.getOrDefault(-1L)
            val createdSeconds =
                runCatching {
                    Files
                        .readAttributes(File(path).toPath(), BasicFileAttributes::class.java)
                        .creationTime()
                        .toMillis() / MILLIS_PER_SECOND
                }.getOrDefault(-1L)
            FolderInfo(
                path = path,
                songCount = directSongs.size,
                totalSongCount = allSongs.size,
                subFolderCount = subFolderCount,
                totalDurationMs = allSongs.sumOf { it.duration },
                totalSizeBytes = allSongs.sumOf { it.size },
                lastModifiedSeconds =
                    (directSongs.maxOfOrNull { it.rawDateModified } ?: -1L)
                        .coerceAtLeast(fileLastModifiedSeconds),
                createdSeconds = createdSeconds,
            )
        }

    private const val MILLIS_PER_SECOND = 1000L
}
