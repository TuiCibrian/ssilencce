package com.ssilencce.ui.screen.replaygain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssilencce.data.local.replaygain.ReplayGainScanFailure
import com.ssilencce.data.local.replaygain.ReplayGainScanner
import com.ssilencce.data.local.repository.Repository
import com.ssilencce.data.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ReplayGainScanUiState {
    data object Loading : ReplayGainScanUiState

    data class Ready(
        val songs: List<Song>,
    ) : ReplayGainScanUiState

    data class Running(
        val completed: Int,
        val total: Int,
        val currentTitle: String?,
        val failed: Int,
    ) : ReplayGainScanUiState

    data class Finished(
        val written: Int,
        val failed: Int,
        val cancelled: Boolean,
        val failures: List<ReplayGainScanFailure>,
    ) : ReplayGainScanUiState

    data object Empty : ReplayGainScanUiState

    data class Error(
        val error: ReplayGainScanError,
        val detail: String? = null,
    ) : ReplayGainScanUiState
}

enum class ReplayGainScanError {
    PermissionDenied,
    Unexpected,
}

class ReplayGainScanViewModel(
    private val repository: Repository,
    private val scanner: ReplayGainScanner,
) : ViewModel() {
    private val _state = MutableStateFlow<ReplayGainScanUiState>(ReplayGainScanUiState.Loading)
    val state: StateFlow<ReplayGainScanUiState> = _state.asStateFlow()

    private var prepareJob: Job? = null
    private var scanJob: Job? = null

    fun prepare(songIds: LongArray) {
        prepareJob?.cancel()
        if (songIds.isEmpty()) {
            _state.value = ReplayGainScanUiState.Empty
            return
        }
        prepareJob =
            viewModelScope.launch {
                val songs =
                    withContext(Dispatchers.IO) {
                        songIds
                            .distinct()
                            .mapNotNull { id ->
                                repository.songById(id).takeIf { it != Song.emptySong }
                            }
                    }
                _state.value =
                    if (songs.isEmpty()) {
                        ReplayGainScanUiState.Empty
                    } else {
                        ReplayGainScanUiState.Ready(songs)
                    }
            }
    }

    fun start() {
        val ready = _state.value as? ReplayGainScanUiState.Ready ?: return
        if (scanJob?.isActive == true) return
        scanJob =
            viewModelScope.launch {
                try {
                    val result =
                        scanner.scan(ready.songs) { progress ->
                            _state.value =
                                ReplayGainScanUiState.Running(
                                    completed = progress.completed,
                                    total = progress.total,
                                    currentTitle = progress.currentTitle,
                                    failed = progress.failed,
                                )
                        }
                    _state.value =
                        ReplayGainScanUiState.Finished(
                            written = result.written,
                            failed = result.failed,
                            cancelled = result.cancelled,
                            failures = result.failures,
                        )
                } catch (cancellation: CancellationException) {
                    val lastRunning = _state.value as? ReplayGainScanUiState.Running
                    val failed = lastRunning?.failed ?: 0
                    val completed = lastRunning?.completed ?: 0
                    _state.value =
                        ReplayGainScanUiState.Finished(
                            written = (completed - failed).coerceAtLeast(0),
                            failed = failed,
                            cancelled = true,
                            failures = emptyList(),
                        )
                    throw cancellation
                } catch (throwable: Throwable) {
                    _state.value =
                        ReplayGainScanUiState.Error(
                            error = ReplayGainScanError.Unexpected,
                            detail = throwable.message,
                        )
                }
            }
    }

    fun cancel() {
        scanJob?.cancel()
    }

    fun onWritePermissionDenied() {
        _state.value = ReplayGainScanUiState.Error(ReplayGainScanError.PermissionDenied)
    }

    override fun onCleared() {
        prepareJob?.cancel()
        scanJob?.cancel()
        super.onCleared()
    }
}
