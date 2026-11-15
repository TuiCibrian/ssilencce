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

package com.ssilencce.util.oem

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.ssilencce.R

/**
 * OEM-agnostic entry points for the permissions required to appear over the lock screen.
 * Android blocks background apps from opening activities unless they can draw over other apps,
 * and some vendors additionally gate lock screen media and auto-start. All intents are resolved
 * before use and fall back to the app details screen.
 */
object LockScreenPermissionGuide {
    const val KEY_OVERLAY_PERMISSION = "lock_screen_overlay_permission"
    const val KEY_NOTIFICATION_SETTINGS = "lock_screen_notification_settings"
    const val KEY_APP_SETTINGS = "lock_screen_app_settings"
    const val KEY_BATTERY_GUIDE = "lock_screen_battery_guide"
    const val KEY_STEPS = "lock_screen_oem_steps"
    const val KEY_REFRESH = "lock_screen_refresh_status"

    fun hasOverlayPermission(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun openOverlaySettings(context: Context): Boolean {
        val intent =
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            )
        return startSafely(context, intent) || PowerManagementGuide.openAppDetails(context)
    }

    fun openNotificationSettings(context: Context): Boolean {
        val intent =
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        return startSafely(context, intent) ||
            startSafely(
                context,
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
            ) ||
            PowerManagementGuide.openAppDetails(context)
    }

    fun steps(context: Context): List<String> {
        val family = PowerManagementGuide.detect()
        val vendor = context.getString(family.displayNameRes)
        return listOf(
            context.getString(R.string.lock_screen_guide_step_overlay),
            context.getString(R.string.lock_screen_guide_step_lock_screen, vendor),
            context.getString(R.string.lock_screen_guide_step_background),
            context.getString(R.string.lock_screen_guide_step_retry),
        )
    }

    private fun startSafely(
        context: Context,
        intent: Intent,
    ): Boolean {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(context.packageManager) == null) return false
        return runCatching { context.startActivity(intent) }.isSuccess
    }
}
