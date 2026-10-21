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
import android.net.Uri
import androidx.annotation.StringRes
import com.ssilencce.R

enum class ScheduledBackupFrequency(
    val storageValue: String,
    @StringRes val labelRes: Int,
    val intervalDays: Long,
) {
    Daily("daily", R.string.daily, 1),
    Weekly("weekly", R.string.weekly, 7),

    // WorkManager periodic work cannot express calendar months; this is a fixed 30-day cadence,
    // so the user-facing label is "Every 30 days" rather than a calendar month.
    Monthly("monthly", R.string.every_30_days, 30),
    ;

    companion object {
        fun fromStorageValue(value: String?): ScheduledBackupFrequency = entries.firstOrNull { it.storageValue == value } ?: Daily
    }
}

enum class ScheduledBackupLastResult {
    None,
    Success,
    Failure,
}

enum class ScheduledBackupFailure(
    val storageValue: String,
    @StringRes val messageRes: Int,
) {
    NoFolder("no_folder", R.string.scheduled_backup_error_no_folder),
    PermissionLost("permission_lost", R.string.scheduled_backup_error_permission),
    FolderNotWritable("folder_not_writable", R.string.scheduled_backup_error_folder),
    CreateFileFailed("create_file_failed", R.string.scheduled_backup_error_create_file),
    BackupFailed("backup_failed", R.string.scheduled_backup_error_backup),
    ;

    companion object {
        fun fromStorageValue(value: String?): ScheduledBackupFailure? = entries.firstOrNull { it.storageValue == value }
    }
}

data class ScheduledBackupState(
    val enabled: Boolean,
    val frequency: ScheduledBackupFrequency,
    val destination: Uri?,
    val destinationName: String?,
    val chargingOnly: Boolean,
    val lastRunAt: Long,
    val lastResult: ScheduledBackupLastResult,
    val lastError: String?,
    val lastSuccessWindow: String?,
)

/**
 * Persists the scheduled-backup settings in a dedicated preferences file so the feature does not
 * need keys in the shared preferences facade.
 */
class ScheduledBackupStore(
    context: Context,
) {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(): ScheduledBackupState {
        val lastResult =
            preferences
                .getString(KEY_LAST_RESULT, null)
                ?.let { stored -> ScheduledBackupLastResult.entries.firstOrNull { it.name == stored } }
                ?: ScheduledBackupLastResult.None
        return ScheduledBackupState(
            enabled = preferences.getBoolean(KEY_ENABLED, false),
            frequency = ScheduledBackupFrequency.fromStorageValue(preferences.getString(KEY_FREQUENCY, null)),
            destination = preferences.getString(KEY_DESTINATION, null)?.let(Uri::parse),
            destinationName = preferences.getString(KEY_DESTINATION_NAME, null),
            chargingOnly = preferences.getBoolean(KEY_CHARGING_ONLY, false),
            lastRunAt = preferences.getLong(KEY_LAST_RUN_AT, 0L),
            lastResult = lastResult,
            lastError = preferences.getString(KEY_LAST_ERROR, null),
            lastSuccessWindow = preferences.getString(KEY_LAST_SUCCESS_WINDOW, null),
        )
    }

    fun writeSettings(
        enabled: Boolean,
        frequency: ScheduledBackupFrequency,
        chargingOnly: Boolean,
        destination: Uri?,
        destinationName: String?,
    ) {
        val editor =
            preferences
                .edit()
                .putBoolean(KEY_ENABLED, enabled)
                .putString(KEY_FREQUENCY, frequency.storageValue)
                .putBoolean(KEY_CHARGING_ONLY, chargingOnly)
        if (destination != null) {
            editor.putString(KEY_DESTINATION, destination.toString())
            editor.putString(KEY_DESTINATION_NAME, destinationName)
        } else {
            editor.remove(KEY_DESTINATION)
            editor.remove(KEY_DESTINATION_NAME)
        }
        editor.apply()
    }

    fun recordSuccess(
        windowKey: String,
        at: Long = System.currentTimeMillis(),
    ) {
        preferences
            .edit()
            .putLong(KEY_LAST_RUN_AT, at)
            .putString(KEY_LAST_RESULT, ScheduledBackupLastResult.Success.name)
            .remove(KEY_LAST_ERROR)
            .putString(KEY_LAST_SUCCESS_WINDOW, windowKey)
            .apply()
    }

    fun recordFailure(
        error: String?,
        at: Long = System.currentTimeMillis(),
    ) {
        preferences
            .edit()
            .putLong(KEY_LAST_RUN_AT, at)
            .putString(KEY_LAST_RESULT, ScheduledBackupLastResult.Failure.name)
            .putString(KEY_LAST_ERROR, error)
            .apply()
    }

    companion object {
        const val PREFERENCES_NAME = "scheduled_backup"

        private const val KEY_ENABLED = "enabled"
        private const val KEY_FREQUENCY = "frequency"
        private const val KEY_DESTINATION = "destination"
        private const val KEY_DESTINATION_NAME = "destination_name"
        private const val KEY_CHARGING_ONLY = "charging_only"
        private const val KEY_LAST_RUN_AT = "last_run_at"
        private const val KEY_LAST_RESULT = "last_result"
        private const val KEY_LAST_ERROR = "last_error"
        private const val KEY_LAST_SUCCESS_WINDOW = "last_success_window"
    }
}
