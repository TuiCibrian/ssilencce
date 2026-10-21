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

package com.ssilencce.ui.screen.settings.backup

import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

/**
 * One-call integration for a plain `<Preference key="scheduled_backup">` entry.
 * Call once after the preference screen has been inflated.
 */
fun PreferenceFragmentCompat.bindScheduledBackupPreference(key: String = "scheduled_backup") {
    findPreference<Preference>(key)?.setOnPreferenceClickListener {
        if (!isAdded || childFragmentManager.isStateSaved) {
            return@setOnPreferenceClickListener false
        }
        ScheduledBackupDialogFragment().show(
            childFragmentManager,
            ScheduledBackupDialogFragment.TAG,
        )
        true
    }
}
