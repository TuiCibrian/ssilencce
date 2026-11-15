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

package com.ssilencce.ui.screen.setup

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.ssilencce.ui.component.base.AbsThemeActivity
import com.ssilencce.ui.screen.MainActivity
import com.ssilencce.ui.theme.SsilencceTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class SetupWizardActivity : AbsThemeActivity() {
    private val viewModel: SetupWizardViewModel by viewModel()

    private val fromSettings: Boolean
        get() = intent.getBooleanExtra(EXTRA_FROM_SETTINGS, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SsilencceTheme {
                val state by viewModel.state.collectAsState()
                LaunchedEffect(state.saveCompleted) {
                    if (state.saveCompleted) {
                        openNextScreen()
                    }
                }
                SetupWizardScreen(
                    state = state,
                    isFirstRun = !fromSettings,
                    onNext = viewModel::next,
                    onBack = { viewModel.previous() },
                    onSkip = ::skipWizard,
                    onFinish = viewModel::save,
                    onSetMainFolder = viewModel::setMainFolder,
                    onResetMainFolder = viewModel::resetMainFolder,
                    onAddInclude = viewModel::addIncludeFolder,
                    onRemoveInclude = viewModel::removeIncludeFolder,
                    onAddExclude = viewModel::addExcludeFolder,
                    onRemoveExclude = viewModel::removeExcludeFolder,
                    onAutoRescanChange = viewModel::setAutoRescan,
                    onRescan = viewModel::rescan,
                )
            }
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (viewModel.state.value.saving) return
                    if (viewModel.previous()) return
                    if (fromSettings) {
                        finish()
                    } else {
                        skipWizard()
                    }
                }
            },
        )
    }

    private fun skipWizard() {
        viewModel.completeWithoutSaving()
        openNextScreen()
    }

    private fun openNextScreen() {
        if (isFinishing || isDestroyed) return
        if (fromSettings) {
            finish()
            return
        }
        startActivity(
            Intent(this, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        )
        finish()
    }

    companion object {
        const val EXTRA_FROM_SETTINGS = "from_settings"
    }
}
