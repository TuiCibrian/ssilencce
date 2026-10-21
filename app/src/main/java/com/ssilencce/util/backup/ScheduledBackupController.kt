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
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Small integration facade for the scheduled-backup settings UI. It owns persistence and the
 * unique WorkManager schedule; the worker itself reads the same persisted state.
 */
class ScheduledBackupController(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val store = ScheduledBackupStore(appContext)
    private val _state = MutableStateFlow(store.read())

    val state: StateFlow<ScheduledBackupState> = _state.asStateFlow()

    fun refresh() {
        _state.value = store.read()
    }

    /**
     * Persists read/write access for a folder returned by [android.content.Intent.ACTION_OPEN_DOCUMENT_TREE].
     * Returns false when the provider did not grant a persistable write permission.
     */
    fun rememberDestination(uri: Uri): Boolean {
        if (appContext.hasScheduledBackupAccess(uri)) return true
        return try {
            appContext.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            true
        } catch (throwable: SecurityException) {
            Log.e(TAG, "Could not persist the scheduled backup folder permission", throwable)
            false
        }
    }

    /**
     * Saves the settings and reconciles the single unique periodic work item. Enabling requires a
     * usable destination; disabling always cancels the scheduled work.
     */
    fun configure(
        enabled: Boolean,
        frequency: ScheduledBackupFrequency,
        chargingOnly: Boolean,
        destination: Uri?,
    ): ConfigureResult {
        val target = destination ?: _state.value.destination
        if (enabled && target == null) return ConfigureResult.MissingFolder
        if (target != null) {
            if (!appContext.hasScheduledBackupAccess(target)) {
                rememberDestination(target)
            }
            if (enabled && !appContext.hasScheduledBackupAccess(target)) {
                return ConfigureResult.PermissionDenied
            }
        }
        val destinationName =
            target?.let { uri ->
                runCatching { DocumentFile.fromTreeUri(appContext, uri)?.name }.getOrNull()
            }
        store.writeSettings(
            enabled = enabled,
            frequency = frequency,
            chargingOnly = chargingOnly,
            destination = target,
            destinationName = destinationName,
        )
        if (enabled && target != null) {
            ScheduledBackupScheduler.schedule(appContext, frequency, chargingOnly)
        } else {
            ScheduledBackupScheduler.cancel(appContext)
        }
        _state.value = store.read()
        return ConfigureResult.Success
    }

    enum class ConfigureResult {
        Success,
        MissingFolder,
        PermissionDenied,
    }

    private companion object {
        const val TAG = "ScheduledBackup"
    }
}
