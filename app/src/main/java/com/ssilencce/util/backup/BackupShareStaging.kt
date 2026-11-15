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

package com.ssilencce.util.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.ssilencce.extensions.fileProviderAuthority
import com.ssilencce.extensions.files.getFormattedFileName
import com.ssilencce.util.BackupHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Copies a just-written manual backup into a private cache slot that the existing FileProvider
 * can expose for sharing. Scheduled backups never call this path.
 */
object BackupShareStaging {
    const val DIRECTORY_NAME = "backup_share"

    private const val TAG = "BackupShareStaging"
    private val stagingMutex = Mutex()

    data class StagedBackup(
        val uri: Uri,
        val file: File,
        val displayName: String,
    )

    suspend fun stageFromSaf(
        context: Context,
        source: Uri,
    ): StagedBackup? =
        withContext(Dispatchers.IO) {
            stagingMutex.withLock {
                stageLocked(context, source)
            }
        }

    fun isShareable(
        context: Context,
        file: File,
    ): Boolean =
        file.isFile &&
            BackupSharePolicy.isSizeValid(file.length()) &&
            isStagedFile(context, file)

    fun isStagedFile(
        context: Context,
        file: File,
    ): Boolean {
        val directoryPath = runCatching { stagingDirectory(context).canonicalPath }.getOrNull() ?: return false
        val filePath = runCatching { file.canonicalPath }.getOrNull() ?: return false
        return BackupSharePolicy.isContained(filePath, directoryPath)
    }

    fun clear(context: Context) {
        stagingDirectory(context).deleteRecursively()
    }

    private suspend fun stageLocked(
        context: Context,
        source: Uri,
    ): StagedBackup? {
        val directory = prepareDirectory(context) ?: return null
        val document = runCatching { DocumentFile.fromSingleUri(context, source) }.getOrNull()
        val displayName =
            document
                ?.name
                ?.takeIf { it.isNotBlank() }
                ?: getFormattedFileName("Backup", BackupHelper.BACKUP_EXTENSION)
        val safeName = BackupSharePolicy.sanitizeDisplayName(displayName, BackupHelper.BACKUP_EXTENSION)

        val estimatedSize = document?.length() ?: 0L
        if (estimatedSize > 0L) {
            if (!BackupSharePolicy.isSizeValid(estimatedSize)) {
                Log.w(TAG, "Backup is too large to stage for sharing")
                return null
            }
            if (directory.usableSpace in 1L until estimatedSize) {
                Log.w(TAG, "Not enough cache space to stage the backup for sharing")
                return null
            }
        }

        val temporary =
            try {
                File.createTempFile(BackupSharePolicy.TEMP_PREFIX, BackupSharePolicy.TEMP_SUFFIX, directory)
            } catch (error: IOException) {
                Log.e(TAG, "Could not create a backup share temp file", error)
                return null
            }
        try {
            val input =
                context.contentResolver.openInputStream(source)
                    ?: throw IOException("Backup source could not be opened")
            input.use { sourceStream ->
                temporary.outputStream().use { targetStream ->
                    sourceStream.copyToWithCancellation(targetStream)
                }
            }
            if (!BackupSharePolicy.isSizeValid(temporary.length())) {
                Log.w(TAG, "Staged backup is empty or too large")
                return null
            }
            val target = File(directory, safeName)
            val uri = FileProvider.getUriForFile(context, context.fileProviderAuthority, target)
            moveIntoPlace(temporary, target)
            deleteStale(directory, target)
            return StagedBackup(uri, target, displayName)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            Log.e(TAG, "Could not stage the backup for sharing", throwable)
            return null
        } finally {
            if (temporary.exists()) temporary.delete()
        }
    }

    private fun prepareDirectory(context: Context): File? {
        val directory = stagingDirectory(context)
        if (directory.exists()) {
            if (!directory.isDirectory) {
                Log.e(TAG, "Backup share path is not a directory")
                return null
            }
            directory
                .listFiles()
                ?.filter { BackupSharePolicy.isTemporaryName(it.name) }
                ?.forEach { it.delete() }
            return directory
        }
        if (!directory.mkdirs()) {
            Log.e(TAG, "Could not create the backup share directory")
            return null
        }
        return directory
    }

    private fun stagingDirectory(context: Context): File = File(context.cacheDir, DIRECTORY_NAME)

    private fun moveIntoPlace(
        temporary: File,
        target: File,
    ) {
        try {
            Files.move(
                temporary.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                temporary.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: FileAlreadyExistsException) {
            Files.move(
                temporary.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
    }

    private fun deleteStale(
        directory: File,
        keep: File,
    ) {
        val names = directory.list().orEmpty().toList()
        BackupSharePolicy.staleFileNames(names, keep.name).forEach { name ->
            File(directory, name).delete()
        }
    }

    private suspend fun InputStream.copyToWithCancellation(output: OutputStream) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            currentCoroutineContext().ensureActive()
            val read = read(buffer)
            if (read == -1) break
            output.write(buffer, 0, read)
        }
    }
}
