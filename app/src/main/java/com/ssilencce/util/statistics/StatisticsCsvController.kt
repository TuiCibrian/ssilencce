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
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Settings/WorkManager facade for the automatic statistics CSV schedule. It owns the dedicated
 * store and the single daily unique work item.
 */
class StatisticsCsvController(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val store = StatisticsCsvStore(appContext)
    private val _state = MutableStateFlow(store.read())

    val state: StateFlow<StatisticsCsvState> = _state.asStateFlow()

    fun refresh() {
        _state.value = store.read()
    }

    fun rememberDestination(uri: Uri): Boolean {
        if (appContext.hasStatisticsCsvAccess(uri)) return true
        return try {
            appContext.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            true
        } catch (error: SecurityException) {
            Log.e(TAG, "Could not persist the statistics CSV folder permission", error)
            false
        }
    }

    /**
     * Disabling cancels the daily work but keeps the configured folder, cadence and last keys so
     * re-enabling can resume without re-exporting a completed period.
     */
    fun configure(
        enabled: Boolean,
        cadence: StatisticsCsvCadence,
        destination: Uri?,
    ): ConfigureResult {
        val target = destination ?: _state.value.destination
        if (enabled && target == null) return ConfigureResult.MissingFolder
        if (target != null) {
            if (!appContext.hasStatisticsCsvAccess(target)) {
                rememberDestination(target)
            }
            if (enabled && !appContext.hasStatisticsCsvAccess(target)) {
                return ConfigureResult.PermissionDenied
            }
        }
        store.writeSettings(
            enabled = enabled,
            cadence = cadence,
            destination = target,
            destinationName = target?.let(::resolveFolderName) ?: _state.value.destinationName,
        )
        if (enabled && target != null) {
            StatisticsCsvScheduler.schedule(appContext)
        } else {
            StatisticsCsvScheduler.cancel(appContext)
        }
        _state.value = store.read()
        return ConfigureResult.Success
    }

    private fun resolveFolderName(uri: Uri): String? =
        runCatching { DocumentFile.fromTreeUri(appContext, uri)?.name }.getOrNull()
            ?: uri.lastPathSegment

    enum class ConfigureResult {
        Success,
        MissingFolder,
        PermissionDenied,
    }

    private companion object {
        const val TAG = "StatisticsCsv"
    }
}

internal fun Context.hasStatisticsCsvAccess(uri: Uri): Boolean =
    contentResolver.persistedUriPermissions.any { permission ->
        permission.uri == uri && permission.isReadPermission && permission.isWritePermission
    }
