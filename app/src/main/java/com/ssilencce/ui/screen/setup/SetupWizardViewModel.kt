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

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssilencce.data.local.LibraryRescan
import com.ssilencce.data.local.repository.Repository
import com.ssilencce.data.local.room.InclExclDao
import com.ssilencce.data.local.room.InclExclEntity
import com.ssilencce.util.FileUtil
import com.ssilencce.util.Preferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class SetupWizardStep {
    Welcome,
    Folders,
    Scanning,
    Finish,
    ;

    fun next(): SetupWizardStep? = entries.getOrNull(ordinal + 1)

    fun previous(): SetupWizardStep? = entries.getOrNull(ordinal - 1)
}

sealed interface SetupScanState {
    data object Idle : SetupScanState

    data object Scanning : SetupScanState

    data class Finished(
        val scannedPaths: Int,
    ) : SetupScanState
}

data class SetupWizardUiState(
    val step: SetupWizardStep = SetupWizardStep.Welcome,
    val loading: Boolean = true,
    val mainFolder: String = "",
    val includeFolders: List<String> = emptyList(),
    val excludeFolders: List<String> = emptyList(),
    val autoRescan: Boolean = false,
    val scanState: SetupScanState = SetupScanState.Idle,
    val saving: Boolean = false,
    val saveCompleted: Boolean = false,
    val saveFailed: Boolean = false,
)

class SetupWizardViewModel(
    private val application: Application,
    private val repository: Repository,
    private val inclExclDao: InclExclDao,
) : ViewModel() {
    private val _state = MutableStateFlow(SetupWizardUiState())
    val state: StateFlow<SetupWizardUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded =
                withContext(Dispatchers.IO) {
                    runCatching {
                        // Seeding the default exclusions here keeps the summary accurate and
                        // mirrors what LibraryViewModel would do on its first load.
                        if (!Preferences.initializedBlacklist) {
                            repository.initializeBlacklist()
                            Preferences.initializedBlacklist = true
                        }
                        LoadedFolders(
                            includeFolders =
                                inclExclDao
                                    .getPaths(InclExclDao.WHITELIST)
                                    .map { it.path }
                                    .sortedWith(String.CASE_INSENSITIVE_ORDER),
                            excludeFolders =
                                inclExclDao
                                    .getPaths(InclExclDao.BLACKLIST)
                                    .map { it.path }
                                    .sortedWith(String.CASE_INSENSITIVE_ORDER),
                            mainFolder = Preferences.startDirectory.path,
                            autoRescan = Preferences.autoRescanOnMediaMount,
                        )
                    }.getOrElse { error ->
                        Log.e(TAG, "Couldn't load the current library folders", error)
                        LoadedFolders(
                            includeFolders = emptyList(),
                            excludeFolders = emptyList(),
                            mainFolder = FileUtil.getDefaultStartDirectory().path,
                            autoRescan = false,
                        )
                    }
                }
            _state.update {
                it.copy(
                    loading = false,
                    includeFolders = loaded.includeFolders,
                    excludeFolders = loaded.excludeFolders,
                    mainFolder = loaded.mainFolder,
                    autoRescan = loaded.autoRescan,
                )
            }
        }
    }

    fun next() {
        val nextStep = _state.value.step.next() ?: return
        _state.update { it.copy(step = nextStep) }
    }

    fun previous(): Boolean {
        val previousStep = _state.value.step.previous() ?: return false
        _state.update { it.copy(step = previousStep) }
        return true
    }

    fun setMainFolder(path: String) {
        _state.update { it.copy(mainFolder = path, scanState = SetupScanState.Idle) }
    }

    fun resetMainFolder() {
        viewModelScope.launch {
            val defaultFolder =
                withContext(Dispatchers.IO) {
                    FileUtil.getDefaultStartDirectory().path
                }
            setMainFolder(defaultFolder)
        }
    }

    fun addIncludeFolder(path: String) {
        _state.update { current ->
            current.copy(
                includeFolders =
                    (current.includeFolders + path)
                        .distinct()
                        .sortedWith(String.CASE_INSENSITIVE_ORDER),
                excludeFolders = current.excludeFolders - path,
                scanState = SetupScanState.Idle,
            )
        }
    }

    fun removeIncludeFolder(path: String) {
        _state.update { it.copy(includeFolders = it.includeFolders - path, scanState = SetupScanState.Idle) }
    }

    fun addExcludeFolder(path: String) {
        _state.update { current ->
            current.copy(
                excludeFolders =
                    (current.excludeFolders + path)
                        .distinct()
                        .sortedWith(String.CASE_INSENSITIVE_ORDER),
                includeFolders = current.includeFolders - path,
                scanState = SetupScanState.Idle,
            )
        }
    }

    fun removeExcludeFolder(path: String) {
        _state.update { it.copy(excludeFolders = it.excludeFolders - path, scanState = SetupScanState.Idle) }
    }

    fun setAutoRescan(enabled: Boolean) {
        _state.update { it.copy(autoRescan = enabled) }
    }

    fun rescan() {
        if (_state.value.scanState is SetupScanState.Scanning) return
        val includeFolders = _state.value.includeFolders
        val mainFolder = _state.value.mainFolder
        _state.update { it.copy(scanState = SetupScanState.Scanning) }
        viewModelScope.launch {
            val scanned =
                try {
                    LibraryRescan.scan(
                        application,
                        includeFolders = includeFolders,
                        mainFolder = mainFolder,
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Couldn't rescan the library", e)
                    0
                }
            _state.update { it.copy(scanState = SetupScanState.Finished(scanned)) }
        }
    }

    fun save() {
        if (_state.value.saving || _state.value.saveCompleted) return
        val snapshot = _state.value
        _state.update { it.copy(saving = true, saveFailed = false) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    persist(snapshot)
                }
                _state.update { it.copy(saving = false, saveCompleted = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Couldn't save the setup wizard choices", e)
                _state.update { it.copy(saving = false, saveFailed = true) }
            }
        }
    }

    fun completeWithoutSaving() {
        Preferences.setupWizardCompleted = true
    }

    private suspend fun persist(state: SetupWizardUiState) {
        val includeEntities = state.includeFolders.map { InclExclEntity(it, InclExclDao.WHITELIST) }
        val excludeEntities = state.excludeFolders.map { InclExclEntity(it, InclExclDao.BLACKLIST) }
        inclExclDao.clearPaths(InclExclDao.WHITELIST)
        if (includeEntities.isNotEmpty()) {
            inclExclDao.insertPaths(includeEntities)
        }
        inclExclDao.clearPaths(InclExclDao.BLACKLIST)
        if (excludeEntities.isNotEmpty()) {
            inclExclDao.insertPaths(excludeEntities)
        }
        if (includeEntities.isNotEmpty()) {
            Preferences.whitelistEnabled = true
        }
        if (excludeEntities.isNotEmpty()) {
            Preferences.blacklistEnabled = true
        }
        if (state.mainFolder.isNotBlank()) {
            Preferences.startDirectory = File(state.mainFolder)
        }
        Preferences.autoRescanOnMediaMount = state.autoRescan
        Preferences.setupWizardCompleted = true
    }

    private data class LoadedFolders(
        val includeFolders: List<String>,
        val excludeFolders: List<String>,
        val mainFolder: String,
        val autoRescan: Boolean,
    )

    private companion object {
        const val TAG = "SetupWizard"
    }
}
