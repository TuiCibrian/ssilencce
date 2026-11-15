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

package com.ssilencce.data.local

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_AUDIO
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.media.MediaScannerConnection
import android.os.Environment
import androidx.core.content.ContextCompat
import com.ssilencce.data.local.extra.ExtraFormatScanner
import com.ssilencce.data.local.room.InclExclDao
import com.ssilencce.extensions.hasT
import com.ssilencce.util.Preferences
import com.ssilencce.util.StorageUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import kotlin.coroutines.resume

/**
 * Asks Android's media scanner to refresh the folders that form the user's library.
 *
 * The library is read from MediaStore, so a rescan means requesting a MediaStore refresh for the
 * configured scan scope instead of re-indexing files ourselves.
 */
object LibraryRescan : KoinComponent {
    private val inclExclDao: InclExclDao by inject()

    // Process-lifetime scope: a rescan should survive the screen that requested it.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val scanMutex = Mutex()
    private var scheduledJob: Job? = null

    fun scanAsync(
        context: Context,
        onFinished: ((Int) -> Unit)? = null,
    ) {
        val appContext = context.applicationContext
        scope.launch {
            val scanned =
                try {
                    scan(appContext)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    0
                }
            onFinished?.let {
                withContext(Dispatchers.Main) { it(scanned) }
            }
        }
    }

    /**
     * Coalesces bursts of mount events (for example, several volumes mounting together) into a
     * single rescan. A new event cancels the pending one and restarts the debounce window.
     */
    fun scheduleDebounced(
        context: Context,
        delayMillis: Long = DEFAULT_DEBOUNCE_MS,
    ) {
        val appContext = context.applicationContext
        scheduledJob?.cancel()
        scheduledJob =
            scope.launch {
                delay(delayMillis)
                try {
                    scan(appContext)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // A mount event is best-effort; a failed rescan must not crash the process.
                }
            }
    }

    suspend fun scan(
        context: Context,
        includeFolders: List<String>? = null,
        mainFolder: String? = null,
    ): Int {
        val scanned =
            scanMutex.withLock {
                val paths = resolveScanPaths(includeFolders, mainFolder)
                scanPaths(context.applicationContext, paths)
            }
        // MediaStore does not index DSF/DFF (and misses some AIFF) files, so refresh the
        // supplementary cache at the same time as the system scan.
        runCatching { ExtraFormatScanner.scan(context.applicationContext) }
        return scanned
    }

    fun hasScanPermission(context: Context): Boolean =
        if (hasT()) {
            ContextCompat.checkSelfPermission(context, READ_MEDIA_AUDIO) == PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED
        }

    private suspend fun resolveScanPaths(
        includeFolders: List<String>?,
        mainFolder: String?,
    ): List<String> =
        withContext(Dispatchers.IO) {
            val configured =
                includeFolders
                    ?: if (Preferences.whitelistEnabled) {
                        inclExclDao.whitelistPaths().map { it.path }
                    } else {
                        emptyList()
                    }
            val validConfigured = configured.filter(::isReadableDirectory)
            val targets = validConfigured.ifEmpty { storageRoots() }
            val currentMainFolder = mainFolder ?: Preferences.startDirectory.path
            (targets + currentMainFolder)
                .distinct()
                .filter { it != File.separator && isReadableDirectory(it) }
        }

    private fun storageRoots(): List<String> =
        StorageUtil
            .refreshStorageVolumes()
            .map { it.filePath }
            .plus(Environment.getExternalStorageDirectory().path)
            .distinct()

    private fun isReadableDirectory(path: String): Boolean = runCatching { File(path).isDirectory }.getOrDefault(false)

    private suspend fun scanPaths(
        context: Context,
        paths: List<String>,
    ): Int {
        if (paths.isEmpty()) return 0
        return withTimeoutOrNull(SCAN_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                var completed = 0
                MediaScannerConnection.scanFile(context, paths.toTypedArray(), null) { _, _ ->
                    completed++
                    if (completed >= paths.size && continuation.isActive) {
                        continuation.resume(paths.size)
                    }
                }
            }
        } ?: paths.size
    }

    private const val DEFAULT_DEBOUNCE_MS = 2_500L
    private const val SCAN_TIMEOUT_MS = 120_000L
}
