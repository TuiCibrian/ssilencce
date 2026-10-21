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

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.util.AttributeSet
import androidx.fragment.app.FragmentActivity
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import com.ssilencce.R
import com.ssilencce.util.backup.ScheduledBackupLastResult
import com.ssilencce.util.backup.ScheduledBackupStore

/**
 * Advanced-settings entry that opens the Compose scheduled-backup configuration sheet.
 */
class ScheduledBackupPreference
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = androidx.preference.R.attr.preferenceStyle,
    ) : Preference(context, attrs, defStyleAttr) {
        private val preferences =
            context.applicationContext.getSharedPreferences(
                ScheduledBackupStore.PREFERENCES_NAME,
                Context.MODE_PRIVATE,
            )
        private val changeListener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                summary = buildSummary()
            }

        init {
            setOnPreferenceClickListener {
                showDialog()
                true
            }
        }

        override fun onAttachedToHierarchy(preferenceManager: PreferenceManager) {
            super.onAttachedToHierarchy(preferenceManager)
            summary = buildSummary()
            preferences.registerOnSharedPreferenceChangeListener(changeListener)
        }

        override fun onPrepareForRemoval() {
            preferences.unregisterOnSharedPreferenceChangeListener(changeListener)
            super.onPrepareForRemoval()
        }

        private fun buildSummary(): CharSequence {
            val state = ScheduledBackupStore(context).read()
            if (!state.enabled) return context.getString(R.string.scheduled_backup_summary)
            val parts = mutableListOf(context.getString(state.frequency.labelRes))
            parts += state.destinationName ?: context.getString(R.string.scheduled_backup_folder_none)
            if (state.lastResult == ScheduledBackupLastResult.Failure) {
                parts += context.getString(R.string.scheduled_backup_failure)
            }
            return parts.joinToString(" • ")
        }

        private fun showDialog() {
            val activity = context.findFragmentActivity() ?: return
            if (activity.supportFragmentManager.isStateSaved) return
            ScheduledBackupDialogFragment().show(
                activity.supportFragmentManager,
                ScheduledBackupDialogFragment.TAG,
            )
        }
    }

private fun Context.findFragmentActivity(): FragmentActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return null
}
