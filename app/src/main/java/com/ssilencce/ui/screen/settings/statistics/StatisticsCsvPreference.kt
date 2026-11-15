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

package com.ssilencce.ui.screen.settings.statistics

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.util.AttributeSet
import androidx.fragment.app.FragmentActivity
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import com.ssilencce.R
import com.ssilencce.util.statistics.StatisticsCsvCadence
import com.ssilencce.util.statistics.StatisticsCsvLastResult
import com.ssilencce.util.statistics.StatisticsCsvStore

/**
 * Advanced-settings entry that opens the Compose automatic-statistics-CSV sheet.
 */
class StatisticsCsvPreference
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = androidx.preference.R.attr.preferenceStyle,
    ) : Preference(context, attrs, defStyleAttr) {
        private val preferences =
            context.applicationContext.getSharedPreferences(
                StatisticsCsvStore.PREFERENCES_NAME,
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
            val state = StatisticsCsvStore(context).read()
            if (!state.enabled) {
                val lastResult =
                    when (state.lastResult) {
                        StatisticsCsvLastResult.Success -> R.string.automatic_statistics_csv_success
                        StatisticsCsvLastResult.NoData -> R.string.automatic_statistics_csv_no_data
                        StatisticsCsvLastResult.Failure -> R.string.automatic_statistics_csv_failure
                        StatisticsCsvLastResult.None -> R.string.automatic_statistics_csv_never_run
                    }
                return listOf(
                    context.getString(R.string.automatic_statistics_csv_off),
                    context.getString(R.string.automatic_statistics_csv_summary),
                    context.getString(lastResult),
                ).joinToString(" • ")
            }
            val parts = mutableListOf(context.getString(state.cadence.labelRes()))
            parts += state.destinationName ?: context.getString(R.string.automatic_statistics_csv_folder_none)
            when (state.lastResult) {
                StatisticsCsvLastResult.Success -> {
                    parts += context.getString(R.string.automatic_statistics_csv_success)
                }

                StatisticsCsvLastResult.NoData -> {
                    parts += context.getString(R.string.automatic_statistics_csv_no_data)
                }

                StatisticsCsvLastResult.Failure -> {
                    parts += context.getString(R.string.automatic_statistics_csv_failure)
                }

                StatisticsCsvLastResult.None -> {
                    Unit
                }
            }
            return parts.joinToString(" • ")
        }

        private fun showDialog() {
            val activity = context.findFragmentActivity() ?: return
            if (activity.supportFragmentManager.isStateSaved) return
            StatisticsCsvDialogFragment().show(
                activity.supportFragmentManager,
                StatisticsCsvDialogFragment.TAG,
            )
        }
    }

internal fun StatisticsCsvCadence.labelRes(): Int =
    when (this) {
        StatisticsCsvCadence.Weekly -> R.string.weekly
        StatisticsCsvCadence.Monthly -> R.string.monthly
        StatisticsCsvCadence.Yearly -> R.string.yearly
    }

private fun Context.findFragmentActivity(): FragmentActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return null
}
