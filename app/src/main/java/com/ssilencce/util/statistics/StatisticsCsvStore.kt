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
import android.net.Uri
import androidx.annotation.StringRes
import com.ssilencce.R

enum class StatisticsCsvLastResult {
    None,
    Success,
    NoData,
    Failure,
}

enum class StatisticsCsvFailure(
    val storageValue: String,
    @StringRes val messageRes: Int,
) {
    NoFolder("no_folder", R.string.automatic_statistics_csv_error_no_folder),
    PermissionLost("permission_lost", R.string.automatic_statistics_csv_error_permission),
    FolderNotWritable("folder_not_writable", R.string.automatic_statistics_csv_error_folder),
    CreateFileFailed("create_file_failed", R.string.automatic_statistics_csv_error_create_file),
    ExportFailed("export_failed", R.string.automatic_statistics_csv_error_export),
    ExistingFileUnverified("existing_file_unverified", R.string.automatic_statistics_csv_error_existing_file),
    ;

    companion object {
        fun fromStorageValue(value: String?): StatisticsCsvFailure? = entries.firstOrNull { it.storageValue == value }
    }
}

data class StatisticsCsvState(
    val enabled: Boolean,
    val cadence: StatisticsCsvCadence,
    val destination: Uri?,
    val destinationName: String?,
    val lastAttemptAt: Long,
    val lastResult: StatisticsCsvLastResult,
    val lastError: String?,
    val lastWeeklyKey: String?,
    val lastMonthlyKey: String?,
    val lastYearlyKey: String?,
    val pendingKey: String?,
    val pendingCadence: StatisticsCsvCadence?,
) {
    fun lastKey(cadence: StatisticsCsvCadence): String? =
        when (cadence) {
            StatisticsCsvCadence.Weekly -> lastWeeklyKey
            StatisticsCsvCadence.Monthly -> lastMonthlyKey
            StatisticsCsvCadence.Yearly -> lastYearlyKey
        }
}

/**
 * Dedicated settings store for the automatic statistics CSV schedule. It deliberately stays
 * separate from the backup schedule and the default preference file.
 */
class StatisticsCsvStore(
    context: Context,
) {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(): StatisticsCsvState {
        val lastResult =
            preferences
                .getString(KEY_LAST_RESULT, null)
                ?.let { stored -> StatisticsCsvLastResult.entries.firstOrNull { it.name == stored } }
                ?: StatisticsCsvLastResult.None
        return StatisticsCsvState(
            enabled = preferences.getBoolean(KEY_ENABLED, false),
            cadence = StatisticsCsvCadence.fromStorageValue(preferences.getString(KEY_CADENCE, null)),
            destination = preferences.getString(KEY_DESTINATION, null)?.let(Uri::parse),
            destinationName = preferences.getString(KEY_DESTINATION_NAME, null),
            lastAttemptAt = preferences.getLong(KEY_LAST_ATTEMPT_AT, 0L),
            lastResult = lastResult,
            lastError = preferences.getString(KEY_LAST_ERROR, null),
            lastWeeklyKey = preferences.getString(KEY_LAST_WEEKLY_KEY, null),
            lastMonthlyKey = preferences.getString(KEY_LAST_MONTHLY_KEY, null),
            lastYearlyKey = preferences.getString(KEY_LAST_YEARLY_KEY, null),
            pendingKey = preferences.getString(KEY_PENDING_KEY, null),
            pendingCadence =
                preferences
                    .getString(KEY_PENDING_CADENCE, null)
                    ?.let { StatisticsCsvCadence.fromStorageValue(it) },
        )
    }

    fun writeSettings(
        enabled: Boolean,
        cadence: StatisticsCsvCadence,
        destination: Uri?,
        destinationName: String?,
    ) {
        val previousCadence = read().cadence
        val editor =
            preferences
                .edit()
                .putBoolean(KEY_ENABLED, enabled)
                .putString(KEY_CADENCE, cadence.storageValue)
        if (cadence != previousCadence) {
            editor.remove(KEY_PENDING_KEY).remove(KEY_PENDING_CADENCE)
        }
        if (destination != null) {
            editor.putString(KEY_DESTINATION, destination.toString())
            if (destinationName != null) {
                editor.putString(KEY_DESTINATION_NAME, destinationName)
            } else {
                editor.remove(KEY_DESTINATION_NAME)
            }
        } else {
            editor.remove(KEY_DESTINATION).remove(KEY_DESTINATION_NAME)
        }
        editor.apply()
    }

    fun recordSuccess(
        cadence: StatisticsCsvCadence,
        key: String,
        at: Long = System.currentTimeMillis(),
    ) = recordCompleted(cadence, key, StatisticsCsvLastResult.Success, at)

    fun recordNoData(
        cadence: StatisticsCsvCadence,
        key: String,
        at: Long = System.currentTimeMillis(),
    ) = recordCompleted(cadence, key, StatisticsCsvLastResult.NoData, at)

    fun recordFailure(
        failure: StatisticsCsvFailure,
        cadence: StatisticsCsvCadence,
        key: String,
        at: Long = System.currentTimeMillis(),
    ) {
        preferences
            .edit()
            .putLong(KEY_LAST_ATTEMPT_AT, at)
            .putString(KEY_LAST_RESULT, StatisticsCsvLastResult.Failure.name)
            .putString(KEY_LAST_ERROR, failure.storageValue)
            .putString(KEY_PENDING_KEY, key)
            .putString(KEY_PENDING_CADENCE, cadence.storageValue)
            .apply()
    }

    fun clearPending() {
        preferences
            .edit()
            .remove(KEY_PENDING_KEY)
            .remove(KEY_PENDING_CADENCE)
            .apply()
    }

    private fun recordCompleted(
        cadence: StatisticsCsvCadence,
        key: String,
        result: StatisticsCsvLastResult,
        at: Long,
    ) {
        preferences
            .edit()
            .putLong(KEY_LAST_ATTEMPT_AT, at)
            .putString(KEY_LAST_RESULT, result.name)
            .remove(KEY_LAST_ERROR)
            .putString(lastKeyName(cadence), key)
            .remove(KEY_PENDING_KEY)
            .remove(KEY_PENDING_CADENCE)
            .apply()
    }

    private fun lastKeyName(cadence: StatisticsCsvCadence): String =
        when (cadence) {
            StatisticsCsvCadence.Weekly -> KEY_LAST_WEEKLY_KEY
            StatisticsCsvCadence.Monthly -> KEY_LAST_MONTHLY_KEY
            StatisticsCsvCadence.Yearly -> KEY_LAST_YEARLY_KEY
        }

    companion object {
        const val PREFERENCES_NAME = "statistics_csv"
        const val PREFERENCE_KEY = "automatic_statistics_csv"

        private const val KEY_ENABLED = "enabled"
        private const val KEY_CADENCE = "cadence"
        private const val KEY_DESTINATION = "destination"
        private const val KEY_DESTINATION_NAME = "destination_name"
        private const val KEY_LAST_ATTEMPT_AT = "last_attempt_at"
        private const val KEY_LAST_RESULT = "last_result"
        private const val KEY_LAST_ERROR = "last_error"
        private const val KEY_LAST_WEEKLY_KEY = "last_weekly_key"
        private const val KEY_LAST_MONTHLY_KEY = "last_monthly_key"
        private const val KEY_LAST_YEARLY_KEY = "last_yearly_key"
        private const val KEY_PENDING_KEY = "pending_key"
        private const val KEY_PENDING_CADENCE = "pending_cadence"
    }
}
