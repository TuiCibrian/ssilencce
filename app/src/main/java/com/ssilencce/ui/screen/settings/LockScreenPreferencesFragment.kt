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

package com.ssilencce.ui.screen.settings

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.ssilencce.R
import com.ssilencce.core.model.lockscreen.LockScreenWallpaper
import com.ssilencce.extensions.showToast
import com.ssilencce.ui.screen.lockscreen.LockScreenPlayerActivity
import com.ssilencce.util.LOCK_SCREEN_WALLPAPER
import com.ssilencce.util.LOCK_SCREEN_WALLPAPER_URI
import com.ssilencce.util.Preferences
import com.ssilencce.util.oem.LockScreenPermissionGuide
import com.ssilencce.util.oem.PowerManagementGuide

class LockScreenPreferencesFragment : PreferenceScreenFragment() {
    private val wallpaperPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) return@registerForActivityResult
            runCatching {
                requireContext()
                    .contentResolver
                    .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Preferences.lockScreenWallpaperUri = uri.toString()
            Preferences.lockScreenWallpaper = LockScreenWallpaper.Custom
            updateWallpaperPreferences()
        }

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        addPreferencesFromResource(R.xml.preferences_screen_lock_screen)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        bindPreferences()
    }

    override fun onSharedPreferenceChanged(
        preferences: SharedPreferences,
        key: String?,
    ) {
        super.onSharedPreferenceChanged(preferences, key)
        if (!isAdded || view == null) return
        if (key == LOCK_SCREEN_WALLPAPER || key == LOCK_SCREEN_WALLPAPER_URI) {
            updateWallpaperPreferences()
        }
    }

    private fun bindPreferences() {
        findPreference<Preference>(LOCK_SCREEN_WALLPAPER)?.let { preference ->
            (preference as? ListPreference)?.value = Preferences.lockScreenWallpaper.name
        }
        findPreference<Preference>(LOCK_SCREEN_WALLPAPER_URI)?.setOnPreferenceClickListener {
            wallpaperPicker.launch(arrayOf("image/*"))
            true
        }
        findPreference<Preference>(LOCK_SCREEN_PREVIEW)?.setOnPreferenceClickListener {
            LockScreenPlayerActivity.launch(requireContext(), preview = true)
            true
        }
        findPreference<Preference>(LOCK_SCREEN_PERMISSION_GUIDE)?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.nav_lock_screen_permissions)
            true
        }
        updateWallpaperPreferences()
    }

    private fun updateWallpaperPreferences() {
        val customImage = findPreference<Preference>(LOCK_SCREEN_WALLPAPER_URI) ?: return
        val isCustom = Preferences.lockScreenWallpaper == LockScreenWallpaper.Custom
        customImage.isVisible = isCustom
        customImage.summary =
            getString(
                if (Preferences.lockScreenWallpaperUri.isNullOrBlank()) {
                    R.string.lock_screen_wallpaper_image_summary_none
                } else {
                    R.string.lock_screen_wallpaper_image_summary_selected
                },
            )
    }

    companion object {
        private const val LOCK_SCREEN_PREVIEW = "lock_screen_preview"
        private const val LOCK_SCREEN_PERMISSION_GUIDE = "lock_screen_permission_guide"
    }
}

/**
 * Renders the OEM-specific lock screen permission walkthrough and opens the relevant system
 * screens. The actual detection and intents live in [LockScreenPermissionGuide].
 */
class LockScreenPermissionsFragment : PreferenceScreenFragment() {
    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        addPreferencesFromResource(R.xml.preferences_screen_lock_screen_permissions)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        bindGuide()
    }

    override fun onResume() {
        super.onResume()
        updateOverlayStatus()
    }

    private fun bindGuide() {
        val context = requireContext()
        val family = PowerManagementGuide.detect()
        findPreference<Preference>(LockScreenPermissionGuide.KEY_STEPS)?.apply {
            title =
                if (family == PowerManagementGuide.OemFamily.Generic) {
                    getString(R.string.lock_screen_guide_steps_title_generic)
                } else {
                    getString(R.string.lock_screen_guide_steps_title, getString(family.displayNameRes))
                }
            summary = LockScreenPermissionGuide.steps(context).joinToString("\n")
            isSelectable = false
        }

        findPreference<Preference>(LockScreenPermissionGuide.KEY_OVERLAY_PERMISSION)?.setOnPreferenceClickListener {
            if (!LockScreenPermissionGuide.openOverlaySettings(context)) {
                context.showToast(R.string.lock_screen_permission_settings_unavailable)
            }
            true
        }

        findPreference<Preference>(LockScreenPermissionGuide.KEY_NOTIFICATION_SETTINGS)?.setOnPreferenceClickListener {
            if (!LockScreenPermissionGuide.openNotificationSettings(context)) {
                context.showToast(R.string.lock_screen_permission_settings_unavailable)
            }
            true
        }

        findPreference<Preference>(LockScreenPermissionGuide.KEY_APP_SETTINGS)?.setOnPreferenceClickListener {
            PowerManagementGuide.openAppDetails(context)
            true
        }

        findPreference<Preference>(LockScreenPermissionGuide.KEY_BATTERY_GUIDE)?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.nav_battery_preferences)
            true
        }

        findPreference<Preference>(LockScreenPermissionGuide.KEY_REFRESH)?.setOnPreferenceClickListener {
            updateOverlayStatus()
            true
        }

        updateOverlayStatus()
    }

    private fun updateOverlayStatus() {
        val granted = LockScreenPermissionGuide.hasOverlayPermission(requireContext())
        findPreference<Preference>(LockScreenPermissionGuide.KEY_OVERLAY_PERMISSION)?.summary =
            getString(
                if (granted) {
                    R.string.lock_screen_overlay_granted
                } else {
                    R.string.lock_screen_overlay_denied
                },
            )
    }
}
