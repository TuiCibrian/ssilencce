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

import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import com.ssilencce.data.model.Song
import com.ssilencce.extensions.files.findSAFDocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * File-system operations for the Folders tab.
 *
 * Writes go straight to java.io when the destination is writable (legacy storage) and fall back
 * to the persisted SAF trees, which is the only writable path on modern scoped storage.
 * Everything runs off the main thread and ends with a MediaStore rescan.
 */
class FolderFileOperations(
    private val context: Context,
) {
    sealed interface Result {
        data class Success(
            val affectedPaths: List<String> = emptyList(),
        ) : Result

        data class Failure(
            val reason: FailureReason,
        ) : Result

        data object NeedsSafAccess : Result
    }

    enum class FailureReason {
        EmptyName,
        InvalidName,
        AlreadyExists,
        NotFound,
        IoError,
    }

    data class TransferResult(
        val copied: Int,
        val moved: Int,
        val failed: Int,
        val skipped: Int,
        val missingSources: Int,
        val pendingDeleteUris: List<Uri>,
        val recoveryIntentSender: IntentSender?,
        val destinationPaths: List<String>,
    ) {
        val needsOriginalRemoval: Boolean
            get() = pendingDeleteUris.isNotEmpty()
    }

    sealed interface TransferOutcome {
        data class Completed(
            val result: TransferResult,
        ) : TransferOutcome

        data class Failure(
            val reason: FailureReason,
        ) : TransferOutcome

        data object NeedsSafAccess : TransferOutcome
    }

    suspend fun createFolder(
        parentPath: String,
        name: String,
    ): Result =
        withContext(Dispatchers.IO) {
            val folderName = name.trim()
            validateName(folderName)?.let { return@withContext Result.Failure(it) }

            val parent = File(parentPath)
            val directTarget = File(parent, folderName)
            if (directTarget.exists()) {
                return@withContext Result.Failure(FailureReason.AlreadyExists)
            }
            if (parent.isDirectory && parent.canWrite()) {
                if (!directTarget.mkdir()) {
                    return@withContext Result.Failure(FailureReason.IoError)
                }
                rescan(listOf(directTarget.absolutePath))
                return@withContext Result.Success(listOf(directTarget.absolutePath))
            }

            val parentDocument =
                context.findSAFDocumentFile(parentPath)
                    ?: return@withContext Result.NeedsSafAccess
            if (parentDocument.findFile(folderName) != null) {
                return@withContext Result.Failure(FailureReason.AlreadyExists)
            }
            if (parentDocument.createDirectory(folderName) == null) {
                return@withContext Result.Failure(FailureReason.IoError)
            }
            val createdPath = joinPath(parentPath, folderName)
            rescan(listOf(createdPath))
            Result.Success(listOf(createdPath))
        }

    suspend fun renameFolder(
        path: String,
        newName: String,
    ): Result =
        withContext(Dispatchers.IO) {
            val folderName = newName.trim()
            validateName(folderName)?.let { return@withContext Result.Failure(it) }

            val source = File(path)
            val parentPath = source.parent ?: return@withContext Result.Failure(FailureReason.NotFound)
            if (folderName == source.name) {
                return@withContext Result.Success(emptyList())
            }
            val targetPath = joinPath(parentPath, folderName)
            if (File(targetPath).exists()) {
                return@withContext Result.Failure(FailureReason.AlreadyExists)
            }

            val oldSongPaths = songPathsUnder(path)
            val renamedDirectly =
                source.canWrite() && source.renameTo(File(targetPath))
            if (!renamedDirectly) {
                val sourceDocument =
                    context.findSAFDocumentFile(path)
                        ?: return@withContext Result.NeedsSafAccess
                val renamed =
                    runCatching {
                        DocumentsContract.renameDocument(
                            context.contentResolver,
                            sourceDocument.uri,
                            folderName,
                        )
                    }.getOrNull()
                if (renamed == null) {
                    return@withContext Result.Failure(FailureReason.IoError)
                }
            }

            val oldPrefix = path.trimEnd('/') + "/"
            val newPrefix = targetPath.trimEnd('/') + "/"
            val newSongPaths = oldSongPaths.map { it.replaceFirst(oldPrefix, newPrefix) }
            rescan(newSongPaths + targetPath)
            removeStaleMediaRows(oldPrefix)
            Result.Success(listOf(targetPath))
        }

    /**
     * Copies or moves [songs] into [destinationPath]. For a move, the original is deleted through
     * SAF, a direct file delete or MediaStore; when the platform requires user confirmation the
     * caller receives the affected URIs plus a recoverable intent sender to request it.
     */
    suspend fun transferSongs(
        songs: List<Song>,
        destinationPath: String,
        copy: Boolean,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): TransferOutcome =
        withContext(Dispatchers.IO) {
            if (songs.isEmpty()) {
                return@withContext TransferOutcome.Failure(FailureReason.NotFound)
            }
            val destinationDirectory = File(destinationPath)
            val destinationDocument =
                if (destinationDirectory.isDirectory && destinationDirectory.canWrite()) {
                    null
                } else {
                    context.findSAFDocumentFile(destinationPath)
                }
            if (!destinationDirectory.canWrite() && destinationDocument == null) {
                return@withContext TransferOutcome.NeedsSafAccess
            }

            val existingNames = mutableSetOf<String>()
            val destinationPaths = mutableListOf<String>()
            val removedSourcePaths = mutableListOf<String>()
            val pendingDeleteUris = mutableListOf<Uri>()
            var recoveryIntentSender: IntentSender? = null
            var copied = 0
            var moved = 0
            var failed = 0
            var skipped = 0
            var missingSources = 0

            songs.forEachIndexed { index, song ->
                currentCoroutineContext().ensureActive()
                if (song.data.substringBeforeLast('/').equals(destinationPath.trimEnd('/'), ignoreCase = true)) {
                    skipped++
                    onProgress(index + 1, songs.size)
                    return@forEachIndexed
                }
                val sourceFile = File(song.data)
                val input =
                    runCatching { openSource(song.uri, sourceFile) }.getOrNull()
                if (input == null) {
                    missingSources++
                    onProgress(index + 1, songs.size)
                    return@forEachIndexed
                }

                val requestedName =
                    uniqueName(song.fileName) { candidate ->
                        candidate in existingNames ||
                            destinationDocument?.findFile(candidate) != null ||
                            File(destinationDirectory, candidate).exists()
                    }
                val written =
                    if (destinationDocument != null) {
                        writeToDocument(
                            input = input,
                            document = destinationDocument,
                            mimeType = mimeTypeOf(requestedName),
                            displayName = requestedName,
                        )
                    } else {
                        writeToFile(input, File(destinationDirectory, requestedName))
                    }

                if (written == null) {
                    failed++
                } else {
                    existingNames += written.name
                    destinationPaths += joinPath(destinationPath, written.name)
                    if (copy) {
                        copied++
                    } else {
                        when (val deletion = deleteSource(song)) {
                            SourceDeletion.Deleted -> {
                                moved++
                                removedSourcePaths += song.data
                            }

                            is SourceDeletion.PendingConfirmation -> {
                                pendingDeleteUris += song.uri
                                recoveryIntentSender = recoveryIntentSender ?: deletion.intentSender
                            }

                            SourceDeletion.Failed -> {
                                failed++
                            }
                        }
                    }
                }
                onProgress(index + 1, songs.size)
            }

            rescan(destinationPaths + removedSourcePaths)
            TransferOutcome.Completed(
                TransferResult(
                    copied = copied,
                    moved = moved,
                    failed = failed,
                    skipped = skipped,
                    missingSources = missingSources,
                    pendingDeleteUris = pendingDeleteUris,
                    recoveryIntentSender = recoveryIntentSender,
                    destinationPaths = destinationPaths,
                ),
            )
        }

    /** Deletes source media after the user approved a delete request. Returns the URIs that could not be removed. */
    suspend fun deleteApprovedSources(uris: List<Uri>): List<Uri> =
        withContext(Dispatchers.IO) {
            val remaining =
                uris.filter { uri ->
                    val deleted =
                        runCatching {
                            context.contentResolver.delete(uri, null, null)
                        }.getOrDefault(0)
                    deleted <= 0
                }
            if (remaining.size < uris.size) {
                context.contentResolver.notifyChange(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    null,
                )
            }
            remaining
        }

    private sealed interface WrittenFile {
        val name: String
    }

    private data class Written(
        override val name: String,
    ) : WrittenFile

    private fun writeToFile(
        input: InputStream,
        destination: File,
    ): WrittenFile? =
        runCatching {
            input.use { source ->
                FileOutputStream(destination).use { output ->
                    source.copyTo(output, COPY_BUFFER_SIZE)
                }
            }
            Written(destination.name)
        }.getOrElse {
            destination.delete()
            null
        }

    private fun writeToDocument(
        input: InputStream,
        document: DocumentFile,
        mimeType: String,
        displayName: String,
    ): WrittenFile? {
        val created =
            runCatching { document.createFile(mimeType, displayName) }.getOrNull()
                ?: return null
        val copied =
            runCatching {
                input.use { source ->
                    val output =
                        context.contentResolver.openOutputStream(created.uri)
                            ?: error("No output stream for ${created.uri}")
                    output.use { destination ->
                        source.copyTo(destination, COPY_BUFFER_SIZE)
                    }
                }
                true
            }.getOrDefault(false)
        if (!copied) {
            runCatching { DocumentsContract.deleteDocument(context.contentResolver, created.uri) }
            return null
        }
        return Written(created.name ?: displayName)
    }

    private fun openSource(
        uri: Uri,
        file: File,
    ): InputStream? =
        runCatching { context.contentResolver.openInputStream(uri) }.getOrNull()
            ?: runCatching { file.inputStream() }.getOrNull()

    private sealed interface SourceDeletion {
        data object Deleted : SourceDeletion

        data object Failed : SourceDeletion

        data class PendingConfirmation(
            val intentSender: IntentSender?,
        ) : SourceDeletion
    }

    private fun deleteSource(song: Song): SourceDeletion {
        val path = song.data
        // SAF is preferred: it works on every supported API without user prompts once the tree
        // has been granted.
        context.findSAFDocumentFile(path)?.let { document ->
            val deleted =
                runCatching {
                    DocumentsContract.deleteDocument(context.contentResolver, document.uri)
                }.getOrDefault(false)
            if (deleted) return SourceDeletion.Deleted
        }

        val file = File(path)
        if (file.exists() && file.delete()) {
            return SourceDeletion.Deleted
        }

        return try {
            val deleted = context.contentResolver.delete(song.uri, null, null)
            if (deleted > 0) SourceDeletion.Deleted else SourceDeletion.Failed
        } catch (error: SecurityException) {
            // Android 10 exposes a recoverable intent sender; Android 11+ requires
            // MediaStore.createDeleteRequest, which the dialog owns.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                SourceDeletion.PendingConfirmation(recoverableIntentSenderOrNull(error))
            } else {
                SourceDeletion.Failed
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun recoverableIntentSenderOrNull(error: SecurityException): IntentSender? = (error as? RecoverableSecurityException)?.userAction?.actionIntent?.intentSender

    private fun validateName(name: String): FailureReason? =
        when {
            name.isEmpty() -> FailureReason.EmptyName
            name.contains('/') || name.contains('\\') || name.contains('\u0000') -> FailureReason.InvalidName
            else -> null
        }

    private fun uniqueName(
        name: String,
        exists: (String) -> Boolean,
    ): String {
        if (!exists(name)) return name
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val extension = if (dot > 0) name.substring(dot) else ""
        for (suffix in 1..MAX_NAME_SUFFIX) {
            val candidate = "$base ($suffix)$extension"
            if (!exists(candidate)) return candidate
        }
        return "$base (${System.currentTimeMillis()})$extension"
    }

    private fun mimeTypeOf(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: "audio/*"
    }

    private fun joinPath(
        parent: String,
        child: String,
    ): String = parent.trimEnd('/') + "/" + child

    private fun songPathsUnder(path: String): List<String> {
        val prefix = path.trimEnd('/') + "/"
        return runCatching {
            context.contentResolver
                .query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Audio.Media.DATA),
                    "${MediaStore.Audio.Media.DATA} LIKE ?",
                    arrayOf("$prefix%"),
                    null,
                )?.use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) {
                            cursor.getString(0)?.let { add(it) }
                        }
                    }
                }.orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun removeStaleMediaRows(oldPrefix: String) {
        runCatching {
            context.contentResolver.delete(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                "${MediaStore.Audio.Media.DATA} LIKE ?",
                arrayOf("$oldPrefix%"),
            )
        }
    }

    private fun rescan(paths: Collection<String>) {
        if (paths.isEmpty()) return
        runCatching {
            MediaScannerConnection.scanFile(context, paths.toTypedArray(), null) { _, _ -> }
        }
        runCatching {
            context.contentResolver.notifyChange(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
            )
        }
    }

    companion object {
        private const val COPY_BUFFER_SIZE = 64 * 1024
        private const val MAX_NAME_SUFFIX = 999
    }
}
