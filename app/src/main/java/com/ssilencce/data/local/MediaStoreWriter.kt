/*
 * Copyright (c) 2024 Christians Martínez Alvarado
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

package com.ssilencce.data.local

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.ssilencce.extensions.hasQ
import com.ssilencce.util.FileUtil.PLAYLISTS_DIRECTORY_NAME
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.CancellationException

/**
 * @author Christians M. A. (mardous)
 */
class MediaStoreWriter(
    private val context: Context,
    private val contentResolver: ContentResolver,
) {
    class Request(
        val displayName: String,
        val relativePath: String,
        val mimeType: String,
        val valuesInterceptor: ((ContentValues) -> Unit)? = null,
    ) {
        companion object {
            fun forImage(
                displayName: String,
                subFolder: String? = null,
                imageMimeType: String,
            ): Request {
                val relativePath =
                    if (subFolder.isNullOrEmpty()) {
                        Environment.DIRECTORY_PICTURES
                    } else {
                        "${Environment.DIRECTORY_PICTURES}/$subFolder"
                    }

                return Request(displayName, relativePath, imageMimeType)
            }

            fun forPlaylist(
                displayName: String,
                mimeType: String,
            ): Request =
                Request(
                    displayName,
                    String.format("%s/%s", Environment.DIRECTORY_MUSIC, PLAYLISTS_DIRECTORY_NAME),
                    mimeType,
                )
        }
    }

    class Result(
        val resultCode: Int,
        val uri: Uri? = null,
    ) {
        class Code {
            companion object {
                const val SUCCESS = 0
                const val NO_SCOPED_STORAGE = 1
                const val ERROR = 2
            }
        }
    }

    @Synchronized
    fun toMediaStore(
        contentUri: Uri,
        request: Request,
        streamConsumer: (OutputStream) -> Boolean,
    ): Result {
        if (!hasQ()) {
            return Result(Result.Code.NO_SCOPED_STORAGE)
        }

        var uri: Uri? = null
        var previousUri: Uri? = null
        var restorePrevious = false
        var committed = false
        var displayName = request.displayName
        var relativePath = request.relativePath.trimEnd('/') + "/"

        try {
            val values =
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.MIME_TYPE, request.mimeType)
                    request.valuesInterceptor?.invoke(this)
                }
            displayName = values.getAsString(MediaStore.MediaColumns.DISPLAY_NAME) ?: return Result(Result.Code.ERROR)
            relativePath =
                (values.getAsString(MediaStore.MediaColumns.RELATIVE_PATH) ?: return Result(Result.Code.ERROR))
                    .trimEnd('/') + "/"
            val stagedName = temporaryName(displayName, "stage")
            values.apply {
                // Interceptors must not bypass staging by supplying an existing file path.
                remove(MediaStore.MediaColumns.DATA)
                put(MediaStore.MediaColumns.DISPLAY_NAME, stagedName)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val stagedUri = contentResolver.insert(contentUri, values) ?: return Result(Result.Code.ERROR)
            uri = stagedUri
            val volumeName = verifyEntry(stagedUri, stagedName, relativePath, 1)
            if (toContentResolver(null, stagedUri, streamConsumer).resultCode != Result.Code.SUCCESS) {
                return Result(Result.Code.ERROR)
            }

            val publishValues = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            if (contentResolver.update(stagedUri, publishValues, null, null) != 1) {
                throw IOException("Could not publish staged media")
            }
            verifyEntry(stagedUri, stagedName, relativePath, 0)

            previousUri = findExisting(contentUri, displayName, relativePath, volumeName)
            previousUri?.let { previous ->
                // Free the final name without deleting any old bytes. Updates can mutate then throw.
                restorePrevious = true
                renameEntry(previous, temporaryName(displayName, "previous"), relativePath)
            }
            renameEntry(stagedUri, displayName, relativePath)
            committed = true

            // Deletion is irreversible: no fallible finalization may follow this commit point.
            previousUri?.let { deleteOutput(it) }
            try {
                contentResolver.notifyChange(contentUri, null)
            } catch (e: RuntimeException) {
                e.printStackTrace()
            }
            return Result(Result.Code.SUCCESS, stagedUri)
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: RuntimeException) {
            if (e is CancellationException) throw e
            e.printStackTrace()
        } finally {
            if (!committed) {
                uri?.let { deleteOutput(it) }
                if (restorePrevious) {
                    try {
                        previousUri?.let { renameEntry(it, displayName, relativePath) }
                    } catch (e: Exception) {
                        // A provider refusing rollback can leave the old bytes at the backup name.
                        e.printStackTrace()
                    }
                }
            }
        }
        return Result(Result.Code.ERROR)
    }

    private fun temporaryName(
        displayName: String,
        role: String,
    ): String {
        val extension = displayName.substringAfterLast('.', "")
        return "ssilencce-$role-${UUID.randomUUID()}" + if (extension.isEmpty()) "" else ".$extension"
    }

    private fun findExisting(
        contentUri: Uri,
        displayName: String,
        relativePath: String,
        volumeName: String,
    ): Uri? {
        val selection =
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND " +
                "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND " +
                "${MediaStore.MediaColumns.VOLUME_NAME} = ? AND ${MediaStore.MediaColumns.IS_PENDING} = 0"
        contentResolver
            .query(
                contentUri,
                arrayOf(MediaStore.MediaColumns._ID),
                selection,
                arrayOf(displayName, relativePath, volumeName),
                null,
            ).use { cursor ->
                if (cursor == null) throw IOException("Could not query existing media")
                if (!cursor.moveToFirst()) return null
                val previous = ContentUris.withAppendedId(contentUri, cursor.getLong(0))
                if (cursor.moveToNext()) throw IOException("Ambiguous media replacement")
                return previous
            }
    }

    private fun renameEntry(
        uri: Uri,
        displayName: String,
        relativePath: String,
    ) {
        val values =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            }
        if (contentResolver.update(uri, values, null, null) != 1) {
            throw IOException("Could not rename media: $uri")
        }
        verifyEntry(uri, displayName, relativePath, 0)
    }

    private fun verifyEntry(
        uri: Uri,
        displayName: String,
        relativePath: String,
        pending: Int,
    ): String {
        contentResolver
            .query(
                uri,
                arrayOf(
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    MediaStore.MediaColumns.IS_PENDING,
                    MediaStore.MediaColumns.VOLUME_NAME,
                ),
                null,
                null,
                null,
            ).use { cursor ->
                if (cursor == null || !cursor.moveToFirst() ||
                    cursor.getString(0) != displayName || cursor.getString(1) != relativePath ||
                    cursor.isNull(2) || cursor.getInt(2) != pending
                ) {
                    throw IOException("Media name, path or publication state was not retained")
                }
                return cursor.getString(3)?.takeIf { it.isNotEmpty() }
                    ?: throw IOException("Media volume is unavailable")
            }
    }

    private fun deleteOutput(uri: Uri) {
        try {
            if (contentResolver.delete(uri, null, null) != 1) {
                throw IOException("Could not remove media output: $uri")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toContentResolver(
        contentUri: Uri?,
        dest: Uri,
        streamConsumer: (OutputStream) -> Boolean,
    ): Result {
        try {
            val written =
                contentResolver.openOutputStream(dest).use { outputStream ->
                    outputStream != null && streamConsumer(outputStream)
                }
            if (written) {
                if (contentUri != null) {
                    contentResolver.notifyChange(contentUri, null)
                }
                return Result(Result.Code.SUCCESS, dest)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
        return Result(Result.Code.ERROR)
    }

    fun toFile(
        directory: File? = null,
        fileName: String,
        streamConsumer: (OutputStream) -> Boolean,
    ): File? {
        val result =
            runCatching {
                val file = File(directory ?: context.filesDir, fileName)
                if (file.createNewFile()) {
                    file.outputStream().use {
                        if (streamConsumer(it)) {
                            return file
                        } else {
                            null
                        }
                    }
                } else {
                    null
                }
            }
        return result.getOrNull()
    }
}
