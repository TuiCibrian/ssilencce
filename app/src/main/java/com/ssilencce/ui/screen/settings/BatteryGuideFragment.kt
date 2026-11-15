package com.ssilencce.ui.screen.settings

import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import com.ssilencce.R
import com.ssilencce.extensions.showToast
import com.ssilencce.util.oem.PowerManagementGuide

/**
 * Explains how to keep background playback alive on OEM skins with aggressive battery managers.
 */
class BatteryGuideFragment : PreferenceScreenFragment() {
    private var family = PowerManagementGuide.OemFamily.Generic

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        addPreferencesFromResource(R.xml.preferences_screen_battery)
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
        updateBatteryOptimizationStatus()
    }

    private fun bindGuide() {
        val context = requireContext()
        family = PowerManagementGuide.detect()
        val familyLabel = getString(family.displayNameRes)

        findPreference<Preference>(PowerManagementGuide.KEY_BATTERY_OPTIMIZATION)?.apply {
            setOnPreferenceClickListener {
                if (!PowerManagementGuide.openBatteryOptimizationSettings(context)) {
                    context.showToast(R.string.battery_guide_battery_settings_unavailable)
                }
                true
            }
        }

        findPreference<Preference>(PowerManagementGuide.KEY_STEPS)?.apply {
            title =
                if (family == PowerManagementGuide.OemFamily.Generic) {
                    getString(R.string.battery_guide_steps_title_generic)
                } else {
                    getString(R.string.battery_guide_steps_title, familyLabel)
                }
            summary = PowerManagementGuide.steps(context, family).joinToString("\n")
            isSelectable = false
        }

        findPreference<Preference>(PowerManagementGuide.KEY_AUTO_START)?.apply {
            isVisible = PowerManagementGuide.hasAutoStartSettings(family)
            setOnPreferenceClickListener {
                if (!PowerManagementGuide.openAutoStartSettings(context, family)) {
                    context.showToast(R.string.battery_guide_autostart_unavailable)
                }
                true
            }
        }

        findPreference<Preference>(PowerManagementGuide.KEY_MIUI_SHORTCUTS)?.apply {
            isVisible = family == PowerManagementGuide.OemFamily.Xiaomi
            setOnPreferenceClickListener {
                if (!PowerManagementGuide.openShortcutPermissionSettings(context)) {
                    context.showToast(R.string.battery_guide_shortcuts_unavailable)
                }
                true
            }
        }

        findPreference<Preference>(PowerManagementGuide.KEY_APP_SETTINGS)?.apply {
            setOnPreferenceClickListener {
                PowerManagementGuide.openAppDetails(context)
                true
            }
        }

        findPreference<Preference>(PowerManagementGuide.KEY_REFRESH_STATUS)?.apply {
            setOnPreferenceClickListener {
                updateBatteryOptimizationStatus()
                true
            }
        }

        updateBatteryOptimizationStatus()
    }

    private fun updateBatteryOptimizationStatus() {
        val ignoring = PowerManagementGuide.isIgnoringBatteryOptimizations(requireContext())
        findPreference<Preference>(PowerManagementGuide.KEY_BATTERY_OPTIMIZATION)?.summary =
            getString(
                if (ignoring) {
                    R.string.battery_optimization_ignored
                } else {
                    R.string.battery_optimization_restricted
                },
            )
    }
}
