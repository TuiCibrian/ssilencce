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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.ssilencce.util.backup

import android.content.Context
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ssilencce.util.BackupCreationResult
import com.ssilencce.util.BackupHelper
import kotlinx.coroutines.CancellationException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes one scheduled backup into the persisted SAF tree. Handled failures are recorded in the
 * settings store and returned as [Result.success] so WorkManager does not retry in a tight loop;
 * the next periodic window is the natural retry. Cancellation is rethrown after deleting the
 * partial document.
 */
class ScheduledBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val store = ScheduledBackupStore(applicationContext)
        val state = store.read()
        if (!state.enabled) return Result.success()

        val destination =
            state.destination ?: return recordFailure(store, ScheduledBackupFailure.NoFolder)
        if (!applicationContext.hasScheduledBackupAccess(destination)) {
            return recordFailure(store, ScheduledBackupFailure.PermissionLost)
        }

        val windowKey = ScheduledBackupWindow.key(state.frequency)
        if (state.lastSuccessWindow == windowKey) {
            return Result.success()
        }

        // Storage pressure is delegated to WorkManager's storage-not-low constraint and to the
        // actual SAF write; the destination volume free space is not reliably queryable here.
        val folder =
            runCatching { DocumentFile.fromTreeUri(applicationContext, destination) }
                .getOrNull()
        if (folder == null || !folder.exists() || !folder.isDirectory || !folder.canWrite()) {
            return recordFailure(store, ScheduledBackupFailure.FolderNotWritable)
        }

        val document =
            runCatching {
                folder.createFile(MIME_TYPE, createFileName(folder))
            }.getOrNull()
        if (document == null) {
            return recordFailure(store, ScheduledBackupFailure.CreateFileFailed)
        }

        return try {
            when (val result = BackupHelper.createBackupFile(applicationContext, document.uri)) {
                BackupCreationResult.Success -> {
                    store.recordSuccess(windowKey)
                    Result.success()
                }

                is BackupCreationResult.Failure -> {
                    deleteDocument(document)
                    recordFailure(store, ScheduledBackupFailure.BackupFailed, result.throwable)
                }
            }
        } catch (cancellation: CancellationException) {
            deleteDocument(document)
            throw cancellation
        } catch (throwable: Throwable) {
            deleteDocument(document)
            recordFailure(store, ScheduledBackupFailure.BackupFailed, throwable)
        }
    }

    private fun recordFailure(
        store: ScheduledBackupStore,
        failure: ScheduledBackupFailure,
        cause: Throwable? = null,
    ): Result {
        Log.e(TAG, "Scheduled backup failed: ${failure.storageValue}", cause)
        store.recordFailure(failure.storageValue)
        return Result.success()
    }

    private fun createFileName(folder: DocumentFile): String {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val base = "ssilencce-$stamp.${BackupHelper.BACKUP_EXTENSION}"
        if (folder.findFile(base) == null) return base
        var index = 2
        while (true) {
            val candidate = "ssilencce-$stamp-$index.${BackupHelper.BACKUP_EXTENSION}"
            if (folder.findFile(candidate) == null) return candidate
            index++
        }
    }

    private fun deleteDocument(document: DocumentFile) {
        try {
            if (!document.delete()) {
                Log.w(TAG, "Could not delete the partial backup ${document.uri}")
            }
        } catch (throwable: Throwable) {
            Log.e(TAG, "Failed to delete the partial backup ${document.uri}", throwable)
        }
    }

    private companion object {
        const val TAG = "ScheduledBackupWorker"
        const val MIME_TYPE = "application/octet-stream"
    }
}
