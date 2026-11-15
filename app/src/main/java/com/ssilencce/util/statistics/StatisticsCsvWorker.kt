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

package com.ssilencce.util.statistics

import android.content.Context
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ssilencce.data.model.ContentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime

/**
 * Daily check for the latest completed statistics period. Handled failures are recorded in
 * [StatisticsCsvStore] and returned as [Result.success] so WorkManager does not retry in a tight
 * loop; the next daily check retries the same pending key. Older missed periods are never
 * backfilled.
 *
 * The persisted per-cadence last key is authoritative: a period already completed (Success or
 * NoData) is not exported again. If the user later deletes an exported file, it is not
 * recreated; only a newly completed period triggers a new export.
 *
 * Each automatic CSV is written to a feature-owned `.partial` staging document and renamed to
 * its final name only after the complete write. A process death therefore leaves at most a
 * stale staging document, which the next run deletes before rewriting; a final document is never
 * written in place.
 */
class StatisticsCsvWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val store = StatisticsCsvStore(applicationContext)
        val state = store.read()
        if (!state.enabled) return Result.success()

        val cadence = state.cadence
        val pending =
            runCatching {
                state.pendingKey
                    ?.takeIf { state.pendingCadence == cadence }
                    ?.let { StatisticsCsvSchedule.windowFromKey(cadence, it) }
            }.getOrNull()
        if (state.pendingKey != null && pending == null) {
            runCatching { store.clearPending() }
                .onFailure { Log.w(TAG, "Could not clear an invalid pending statistics period", it) }
        }
        val target = pending ?: StatisticsCsvSchedule.latestCompleted(cadence, ZonedDateTime.now())
        if (!StatisticsCsvSchedule.needsExport(target.key, pending?.key, state.lastKey(cadence))) {
            return Result.success()
        }

        return try {
            export(store, state, target)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            Log.e(TAG, "Automatic statistics CSV failed", throwable)
            recordFailure(store, StatisticsCsvFailure.ExportFailed, cadence, target.key)
        }
    }

    private suspend fun export(
        store: StatisticsCsvStore,
        state: StatisticsCsvState,
        window: StatisticsCsvWindow,
    ): Result {
        val destination =
            state.destination
                ?: return recordFailure(store, StatisticsCsvFailure.NoFolder, window.cadence, window.key)
        if (!applicationContext.hasStatisticsCsvAccess(destination)) {
            return recordFailure(store, StatisticsCsvFailure.PermissionLost, window.cadence, window.key)
        }
        val folder =
            runCatching { DocumentFile.fromTreeUri(applicationContext, destination) }
                .getOrNull()
        if (folder == null || !folder.exists() || !folder.isDirectory || !folder.canWrite()) {
            return recordFailure(store, StatisticsCsvFailure.FolderNotWritable, window.cadence, window.key)
        }
        if (!cleanStaleStagingFiles(folder)) {
            return recordFailure(store, StatisticsCsvFailure.ExportFailed, window.cadence, window.key)
        }

        val expectedTypes = listOf(ContentType.TopTracks, ContentType.TopArtists)
        val missingTypes = mutableListOf<ContentType>()
        var existingFiles = 0
        for (contentType in expectedTypes) {
            val fileName = StatisticsCsvExporter.automaticFileName(contentType, window) ?: continue
            val existing = folder.findFile(fileName)
            if (existing == null) {
                missingTypes += contentType
                continue
            }
            if (!existing.isFile) {
                return recordFailure(store, StatisticsCsvFailure.CreateFileFailed, window.cadence, window.key)
            }
            val length = runCatching { existing.length() }.getOrNull()
            if (length != null && length > 0L) {
                existingFiles++
                continue
            }
            when (probeExistingFile(existing)) {
                ExistingFileContent.Content -> {
                    existingFiles++
                }

                ExistingFileContent.Empty -> {
                    // A confirmed zero-byte document is a partial write, not a completed CSV.
                    if (!existing.delete()) {
                        return recordFailure(store, StatisticsCsvFailure.ExportFailed, window.cadence, window.key)
                    }
                    missingTypes += contentType
                }

                ExistingFileContent.Unknown -> {
                    // Some providers report an unknown length and cannot be read back. Keep the
                    // document rather than risking deletion of a valid CSV; retry next day.
                    return recordFailure(
                        store,
                        StatisticsCsvFailure.ExistingFileUnverified,
                        window.cadence,
                        window.key,
                    )
                }
            }
        }

        if (missingTypes.isEmpty()) {
            store.recordSuccess(window.cadence, window.key)
            return Result.success()
        }

        var wroteAny = false
        for (contentType in missingTypes) {
            val document =
                StatisticsCsvExporter.prepareAutomatic(contentType, window)
                    ?: continue
            val stagingName = StatisticsCsvFiles.stagingName(document.fileName)
            val staging =
                runCatching { folder.createFile(MIME_TYPE, stagingName) }
                    .getOrNull()
                    ?: return recordFailure(store, StatisticsCsvFailure.CreateFileFailed, window.cadence, window.key)
            try {
                StatisticsCsvExporter.write(applicationContext.contentResolver, staging.uri, document)
                val renamed = runCatching { staging.renameTo(document.fileName) }.getOrDefault(false)
                if (!renamed) {
                    deleteDocument(staging)
                    Log.e(TAG, "Could not finalize ${document.fileName}")
                    return recordFailure(store, StatisticsCsvFailure.ExportFailed, window.cadence, window.key)
                }
                wroteAny = true
            } catch (cancellation: CancellationException) {
                deleteDocument(staging)
                throw cancellation
            } catch (throwable: Throwable) {
                deleteDocument(staging)
                Log.e(TAG, "Could not write ${document.fileName}", throwable)
                return recordFailure(store, StatisticsCsvFailure.ExportFailed, window.cadence, window.key)
            }
        }

        if (wroteAny || existingFiles > 0) {
            store.recordSuccess(window.cadence, window.key)
        } else {
            store.recordNoData(window.cadence, window.key)
        }
        return Result.success()
    }

    private suspend fun cleanStaleStagingFiles(folder: DocumentFile): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                folder
                    .listFiles()
                    .filter { it.isFile && StatisticsCsvFiles.isStagingName(it.name) }
                    .all { it.delete() }
            }.getOrDefault(false)
        }

    private suspend fun probeExistingFile(document: DocumentFile): ExistingFileContent =
        withContext(Dispatchers.IO) {
            try {
                val stream =
                    applicationContext.contentResolver.openInputStream(document.uri)
                        ?: return@withContext ExistingFileContent.Unknown
                stream.use { input ->
                    if (input.read() == -1) ExistingFileContent.Empty else ExistingFileContent.Content
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                Log.w(TAG, "Could not verify the existing statistics CSV ${document.uri}", throwable)
                ExistingFileContent.Unknown
            }
        }

    private fun recordFailure(
        store: StatisticsCsvStore,
        failure: StatisticsCsvFailure,
        cadence: StatisticsCsvCadence,
        key: String,
    ): Result {
        Log.e(TAG, "Automatic statistics CSV failed: ${failure.storageValue}")
        store.recordFailure(failure, cadence, key)
        return Result.success()
    }

    private fun deleteDocument(document: DocumentFile) {
        try {
            if (!document.delete()) {
                Log.w(TAG, "Could not delete the partial statistics CSV ${document.uri}")
            }
        } catch (throwable: Throwable) {
            Log.e(TAG, "Failed to delete the partial statistics CSV ${document.uri}", throwable)
        }
    }

    private enum class ExistingFileContent {
        Empty,
        Content,
        Unknown,
    }

    private companion object {
        const val TAG = "StatisticsCsvWorker"
        const val MIME_TYPE = "text/csv"
    }
}
