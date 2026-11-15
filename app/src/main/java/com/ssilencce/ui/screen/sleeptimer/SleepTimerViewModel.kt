package com.ssilencce.ui.screen.sleeptimer

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ssilencce.App
import com.ssilencce.playback.SleepTimer
import com.ssilencce.playback.SleepTimerEndTime
import com.ssilencce.util.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

private val Context.sleepTimerDataStore by preferencesDataStore("sleep_timer")

private const val DEFAULT_SONG_COUNT = 3

enum class SleepTimerMode {
    Duration,
    EndAt,
    Songs,
}

class SleepTimerViewModel(
    application: Application,
    private val sleepTimer: SleepTimer,
) : AndroidViewModel(application) {
    private val initialEndAt = SleepTimerEndTime.nextFullHour(ZonedDateTime.now())

    private val _uiState: Flow<SleepTimerUiState> =
        combine(
            sleepTimer.isRunning,
            sleepTimer.waitingFor,
            application.sleepTimerDataStore.data,
        ) { isRunning, waitingFor, prefs ->
            val mode =
                prefs[Keys.TIMER_MODE]
                    ?.let { name -> SleepTimerMode.entries.firstOrNull { it.name == name } }
                    ?: SleepTimerMode.Duration
            SleepTimerUiState(
                isRunning = isRunning,
                waitingFor = waitingFor,
                isFinishMusic = prefs[Keys.IS_FINISH_MUSIC] ?: false,
                isFadeOut = prefs[Keys.IS_FADE_OUT] ?: false,
                fadeOutDuration = prefs[Keys.FADE_OUT_DURATION] ?: 5f,
                timerValue = prefs[Keys.TIMER_VALUE] ?: 5f,
                timerMode = mode,
                endAtHour = prefs[Keys.END_AT_HOUR] ?: initialEndAt.first,
                endAtMinute = prefs[Keys.END_AT_MINUTE] ?: initialEndAt.second,
                songCount = prefs[Keys.SONG_COUNT] ?: DEFAULT_SONG_COUNT,
                history = SleepTimerHistoryEntry.decode(prefs[Keys.HISTORY]),
            )
        }

    val uiState =
        _uiState.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue =
                SleepTimerUiState(
                    isRunning = false,
                    waitingFor = null,
                    isFinishMusic = false,
                    isFadeOut = false,
                    fadeOutDuration = 5f,
                    timerValue = 5f,
                    timerMode = SleepTimerMode.Duration,
                    endAtHour = initialEndAt.first,
                    endAtMinute = initialEndAt.second,
                    songCount = DEFAULT_SONG_COUNT,
                    history = emptyList(),
                ),
        )

    private val _sleepTimerEvent = Channel<SleepTimerEvent>(Channel.BUFFERED)
    val sleepTimerEvent = _sleepTimerEvent.receiveAsFlow()

    init {
        sleepTimer.createTimerUpdater()
    }

    override fun onCleared() {
        super.onCleared()
        sleepTimer.cancelTimerUpdater()
    }

    fun setTimerState(
        value: Float = uiState.value.timerValue,
        isFinishMusic: Boolean = uiState.value.isFinishMusic,
        isFadeOut: Boolean = uiState.value.isFadeOut,
        fadeOutDuration: Float = uiState.value.fadeOutDuration,
        songCount: Int = uiState.value.songCount,
    ) = viewModelScope.launch(Dispatchers.IO) {
        getApplication<App>().sleepTimerDataStore.edit {
            it[Keys.TIMER_VALUE] = value
            it[Keys.IS_FINISH_MUSIC] = isFinishMusic
            it[Keys.IS_FADE_OUT] = isFadeOut
            it[Keys.FADE_OUT_DURATION] = fadeOutDuration
            it[Keys.SONG_COUNT] = songCount.coerceAtLeast(1)
        }
    }

    fun startTimer(
        mode: SleepTimerMode,
        endAtHour: Int,
        endAtMinute: Int,
        songCount: Int = uiState.value.songCount,
    ) = viewModelScope.launch(Dispatchers.IO) {
        val state = uiState.value
        if (mode != SleepTimerMode.Songs && !sleepTimer.canScheduleExactAlarm()) {
            sleepTimer.launchExactAlarmPermissionRequest()
            return@launch
        }
        startTimerInternal(
            mode = mode,
            durationMinutes = state.timerValue.toInt().coerceAtLeast(1),
            endAtHour = endAtHour,
            endAtMinute = endAtMinute,
            songCount = songCount,
            isFinishMusic = state.isFinishMusic,
            isFadeOut = state.isFadeOut,
            fadeOutDuration = state.fadeOutDuration,
        )
    }

    /** One-tap "stop after this song": the current track is the first and only counted song. */
    fun startStopAfterThisSong() =
        viewModelScope.launch(Dispatchers.IO) {
            val state = uiState.value
            startTimerInternal(
                mode = SleepTimerMode.Songs,
                durationMinutes = state.timerValue.toInt().coerceAtLeast(1),
                endAtHour = state.endAtHour,
                endAtMinute = state.endAtMinute,
                songCount = 1,
                isFinishMusic = state.isFinishMusic,
                isFadeOut = state.isFadeOut,
                fadeOutDuration = state.fadeOutDuration,
            )
        }

    /** Reuses a persisted time-based timer immediately. */
    fun reuseHistory(entry: SleepTimerHistoryEntry) =
        viewModelScope.launch(Dispatchers.IO) {
            if (!sleepTimer.canScheduleExactAlarm()) {
                sleepTimer.launchExactAlarmPermissionRequest()
                return@launch
            }
            val state = uiState.value
            when (entry) {
                is SleepTimerHistoryEntry.Duration -> {
                    startTimerInternal(
                        mode = SleepTimerMode.Duration,
                        durationMinutes = entry.minutes,
                        endAtHour = state.endAtHour,
                        endAtMinute = state.endAtMinute,
                        songCount = state.songCount,
                        isFinishMusic = state.isFinishMusic,
                        isFadeOut = state.isFadeOut,
                        fadeOutDuration = state.fadeOutDuration,
                    )
                }

                is SleepTimerHistoryEntry.EndAt -> {
                    startTimerInternal(
                        mode = SleepTimerMode.EndAt,
                        durationMinutes = state.timerValue.toInt().coerceAtLeast(1),
                        endAtHour = entry.hour,
                        endAtMinute = entry.minute,
                        songCount = state.songCount,
                        isFinishMusic = state.isFinishMusic,
                        isFadeOut = state.isFadeOut,
                        fadeOutDuration = state.fadeOutDuration,
                    )
                }
            }
        }

    fun clearHistory() =
        viewModelScope.launch(Dispatchers.IO) {
            getApplication<App>().sleepTimerDataStore.edit { it.remove(Keys.HISTORY) }
        }

    fun setStopMusicOnlyInForeground(enabled: Boolean) {
        Preferences.sleepTimerStopMusicOnlyInForeground = enabled
    }

    fun cancelTimer() =
        viewModelScope.launch(Dispatchers.IO) {
            if (sleepTimer.cancel()) {
                _sleepTimerEvent.send(SleepTimerEvent.Canceled)
            }
        }

    private suspend fun startTimerInternal(
        mode: SleepTimerMode,
        durationMinutes: Int,
        endAtHour: Int,
        endAtMinute: Int,
        songCount: Int,
        isFinishMusic: Boolean,
        isFadeOut: Boolean,
        fadeOutDuration: Float,
    ) {
        val fadeDurationMillis = fadeOutDuration.toLong() * 1000
        if (mode == SleepTimerMode.Songs) {
            sleepTimer.setSongCount(
                songs = songCount.coerceAtLeast(1),
                fadeOut = isFadeOut,
                fadeDuration = fadeDurationMillis,
            )
        } else {
            val delayMillis =
                when (mode) {
                    SleepTimerMode.Duration -> durationMinutes.toLong() * 60 * 1000
                    SleepTimerMode.EndAt -> SleepTimerEndTime.delayMillis(ZonedDateTime.now(), endAtHour, endAtMinute)
                    SleepTimerMode.Songs -> 0L
                }
            sleepTimer.set(
                millisInFuture = delayMillis,
                allowPendingQuit = isFinishMusic,
                fadeOut = isFadeOut,
                fadeDuration = fadeDurationMillis,
            )
        }
        getApplication<App>().sleepTimerDataStore.edit { prefs ->
            prefs[Keys.TIMER_VALUE] = durationMinutes.toFloat()
            prefs[Keys.SONG_COUNT] = songCount.coerceAtLeast(1)
            prefs[Keys.IS_FINISH_MUSIC] = isFinishMusic
            prefs[Keys.IS_FADE_OUT] = isFadeOut
            prefs[Keys.FADE_OUT_DURATION] = fadeOutDuration
            prefs[Keys.TIMER_MODE] = mode.name
            if (mode == SleepTimerMode.EndAt) {
                prefs[Keys.END_AT_HOUR] = endAtHour
                prefs[Keys.END_AT_MINUTE] = endAtMinute
            }
            val historyEntry =
                when (mode) {
                    SleepTimerMode.Duration -> SleepTimerHistoryEntry.Duration(durationMinutes)
                    SleepTimerMode.EndAt -> SleepTimerHistoryEntry.EndAt(endAtHour, endAtMinute)
                    SleepTimerMode.Songs -> null
                }
            if (historyEntry != null) {
                prefs[Keys.HISTORY] =
                    SleepTimerHistoryEntry.encode(
                        SleepTimerHistoryEntry.add(
                            SleepTimerHistoryEntry.decode(prefs[Keys.HISTORY]),
                            historyEntry,
                        ),
                    )
            }
        }
        _sleepTimerEvent.send(SleepTimerEvent.Set)
    }

    private interface Keys {
        companion object {
            val IS_FINISH_MUSIC = booleanPreferencesKey("finish_music")
            val IS_FADE_OUT = booleanPreferencesKey("fade_out")
            val FADE_OUT_DURATION = floatPreferencesKey("fade_out_duration")
            val TIMER_VALUE = floatPreferencesKey("timer_value")
            val TIMER_MODE = stringPreferencesKey("timer_mode")
            val END_AT_HOUR = intPreferencesKey("end_at_hour")
            val END_AT_MINUTE = intPreferencesKey("end_at_minute")
            val SONG_COUNT = intPreferencesKey("song_count")
            val HISTORY = stringPreferencesKey("timer_history")
        }
    }
}
