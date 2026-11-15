package com.ssilencce.ui.screen.audiocutter

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssilencce.data.local.audio.clip.AudioClipExportResult
import com.ssilencce.data.local.audio.clip.AudioClipExporter
import com.ssilencce.data.local.audio.clip.AudioClipFailure
import com.ssilencce.data.local.audio.clip.AudioClipInspection
import com.ssilencce.data.local.audio.clip.AudioClipNaming
import com.ssilencce.data.local.audio.clip.AudioClipRange
import com.ssilencce.data.local.audio.clip.ClipToneKind
import com.ssilencce.data.local.audio.clip.RingtoneClipStore
import com.ssilencce.data.local.audio.clip.RingtoneSetResult
import com.ssilencce.data.local.repository.Repository
import com.ssilencce.data.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AudioCutterUiState(
    val loading: Boolean = true,
    val missing: Boolean = false,
    val song: Song? = null,
    val inspection: AudioClipInspection? = null,
    val exporting: Boolean = false,
)

sealed interface AudioCutterEvent {
    data class ClipExported(
        val displayName: String,
    ) : AudioCutterEvent

    data class ToneSet(
        val kind: ClipToneKind,
    ) : AudioCutterEvent

    data class ExportFailed(
        val detail: String? = null,
    ) : AudioCutterEvent

    data class ToneFailed(
        val detail: String? = null,
    ) : AudioCutterEvent

    data object WriteSettingsDenied : AudioCutterEvent

    data object StoragePermissionDenied : AudioCutterEvent
}

class AudioCutterViewModel(
    private val repository: Repository,
    private val exporter: AudioClipExporter,
    private val ringtoneStore: RingtoneClipStore,
    private val contentResolver: ContentResolver,
    private val songId: Long,
) : ViewModel() {
    private val _state = MutableStateFlow(AudioCutterUiState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<AudioCutterEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    private var prepareJob: Job? = null
    private var workJob: Job? = null

    fun prepare() {
        if (prepareJob?.isActive == true || _state.value.song != null) return
        prepareJob =
            viewModelScope.launch {
                _state.update { it.copy(loading = true) }
                try {
                    val song =
                        withContext(Dispatchers.IO) {
                            repository.songById(songId)
                        }
                    if (song == Song.emptySong) {
                        _state.value = AudioCutterUiState(loading = false, missing = true)
                        return@launch
                    }
                    val inspection =
                        withContext(Dispatchers.IO) {
                            exporter.inspect(song.uri)
                        }
                    _state.value =
                        AudioCutterUiState(
                            loading = false,
                            song = song,
                            inspection = inspection,
                        )
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Throwable) {
                    _state.value = AudioCutterUiState(loading = false, missing = true)
                }
            }
    }

    fun exportToDocument(
        uri: Uri,
        startMs: Long,
        endMs: Long,
    ) {
        val song = _state.value.song ?: return
        val format = _state.value.inspection?.format ?: return
        if (workJob?.isActive == true) return
        workJob =
            viewModelScope.launch {
                _state.update { it.copy(exporting = true) }
                val range = AudioClipRange(startMs, endMs)
                val displayName = AudioClipNaming.clipFileName(song.title, startMs, endMs, format.extension)
                val result =
                    try {
                        withContext(Dispatchers.IO) {
                            val output =
                                runCatching { contentResolver.openOutputStream(uri) }.getOrNull()
                            if (output == null) {
                                AudioClipExportResult.Failure(
                                    AudioClipFailure.Io,
                                    "Could not open the destination file",
                                )
                            } else {
                                exporter.export(song.uri, range, output, format)
                            }
                        }
                    } catch (cancellation: CancellationException) {
                        _state.update { it.copy(exporting = false) }
                        throw cancellation
                    } catch (throwable: Throwable) {
                        AudioClipExportResult.Failure(AudioClipFailure.Io, throwable.message)
                    }
                if (result !is AudioClipExportResult.Success) {
                    withContext(Dispatchers.IO) {
                        runCatching { contentResolver.delete(uri, null, null) }
                    }
                }
                _state.update { it.copy(exporting = false) }
                when (result) {
                    is AudioClipExportResult.Success -> {
                        _events.tryEmit(AudioCutterEvent.ClipExported(displayName))
                    }

                    is AudioClipExportResult.Failure -> {
                        _events.tryEmit(AudioCutterEvent.ExportFailed(result.detail))
                    }
                }
            }
    }

    fun setAsTone(
        kind: ClipToneKind,
        startMs: Long,
        endMs: Long,
    ) {
        val song = _state.value.song ?: return
        val format = _state.value.inspection?.format ?: return
        if (workJob?.isActive == true) return
        if (!ringtoneStore.canWriteSettings()) {
            _events.tryEmit(AudioCutterEvent.WriteSettingsDenied)
            return
        }
        workJob =
            viewModelScope.launch {
                _state.update { it.copy(exporting = true) }
                val range = AudioClipRange(startMs, endMs)
                val displayName = AudioClipNaming.clipFileName(song.title, startMs, endMs, format.extension)
                val title = AudioClipNaming.clipTitle(song.title, startMs, endMs)
                val result =
                    try {
                        ringtoneStore.exportAndSet(
                            source = song.uri,
                            range = range,
                            format = format,
                            kind = kind,
                            displayName = displayName,
                            title = title,
                        )
                    } catch (cancellation: CancellationException) {
                        _state.update { it.copy(exporting = false) }
                        throw cancellation
                    } catch (throwable: Throwable) {
                        RingtoneSetResult.Failed(throwable.message)
                    }
                _state.update { it.copy(exporting = false) }
                when (result) {
                    is RingtoneSetResult.Success -> {
                        _events.tryEmit(AudioCutterEvent.ToneSet(kind))
                    }

                    RingtoneSetResult.WriteSettingsDenied -> {
                        _events.tryEmit(AudioCutterEvent.WriteSettingsDenied)
                    }

                    RingtoneSetResult.StoragePermissionDenied -> {
                        _events.tryEmit(AudioCutterEvent.StoragePermissionDenied)
                    }

                    is RingtoneSetResult.Failed -> {
                        _events.tryEmit(AudioCutterEvent.ToneFailed(result.detail))
                    }
                }
            }
    }
}
