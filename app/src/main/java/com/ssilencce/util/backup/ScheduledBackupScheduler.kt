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
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules the single periodic backup. The first attempt is eligible immediately (subject to the
 * constraints); subsequent attempts repeat at the selected interval.
 *
 * WorkManager periodic work is inexact and cannot express calendar months, so [ScheduledBackupFrequency.Monthly]
 * is a fixed 30-day cadence.
 */
object ScheduledBackupScheduler {
    const val UNIQUE_WORK_NAME = "scheduled_backup"

    fun schedule(
        context: Context,
        frequency: ScheduledBackupFrequency,
        chargingOnly: Boolean,
    ) {
        val constraints =
            Constraints
                .Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .setRequiresCharging(chargingOnly)
                .build()
        val request =
            PeriodicWorkRequestBuilder<ScheduledBackupWorker>(frequency.intervalDays, TimeUnit.DAYS)
                .setConstraints(constraints)
                .addTag(UNIQUE_WORK_NAME)
                .build()
        WorkManager
            .getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) {
        WorkManager
            .getInstance(context.applicationContext)
            .cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}

/**
 * Identifies the current schedule window. A successful backup is recorded against this key so a
 * second worker run in the same window is a no-op.
 *
 * The key is a fixed-duration bucket of epoch milliseconds, matching WorkManager's elapsed
 * interval semantics (1, 7 or 30 days). It deliberately does not use local calendar dates, ISO
 * weeks or calendar months, because those can repeat or skip a bucket around DST transitions and
 * suppress a due fixed-interval run. "Monthly" remains the user-approved "Every 30 days" cadence.
 */
object ScheduledBackupWindow {
    fun key(
        frequency: ScheduledBackupFrequency,
        nowMillis: Long = System.currentTimeMillis(),
    ): String {
        val intervalMillis = frequency.intervalDays * MILLIS_PER_DAY
        return when (frequency) {
            ScheduledBackupFrequency.Daily -> "daily:${nowMillis / intervalMillis}"
            ScheduledBackupFrequency.Weekly -> "weekly:${nowMillis / intervalMillis}"
            ScheduledBackupFrequency.Monthly -> "monthly:${nowMillis / intervalMillis}"
        }
    }

    private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
}

internal fun Context.hasScheduledBackupAccess(uri: Uri): Boolean =
    contentResolver.persistedUriPermissions.any { permission ->
        permission.isWritePermission && permission.uri == uri
    }
