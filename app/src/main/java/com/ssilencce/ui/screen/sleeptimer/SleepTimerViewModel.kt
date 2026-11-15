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

enum class SleepTimerMode {
    Duration,
    EndAt,
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
    ) = viewModelScope.launch(Dispatchers.IO) {
        getApplication<App>().sleepTimerDataStore.edit {
            it[Keys.TIMER_VALUE] = value
            it[Keys.IS_FINISH_MUSIC] = isFinishMusic
            it[Keys.IS_FADE_OUT] = isFadeOut
            it[Keys.FADE_OUT_DURATION] = fadeOutDuration
        }
    }

    fun startTimer(
        mode: SleepTimerMode,
        endAtHour: Int,
        endAtMinute: Int,
    ) = viewModelScope.launch(Dispatchers.IO) {
        if (!sleepTimer.canScheduleExactAlarm()) {
            sleepTimer.launchExactAlarmPermissionRequest()
            return@launch
        }
        val delayMillis =
            when (mode) {
                SleepTimerMode.Duration -> uiState.value.timerValue.toLong() * 60 * 1000
                SleepTimerMode.EndAt -> SleepTimerEndTime.delayMillis(ZonedDateTime.now(), endAtHour, endAtMinute)
            }
        sleepTimer.set(
            millisInFuture = delayMillis,
            allowPendingQuit = uiState.value.isFinishMusic,
            fadeOut = uiState.value.isFadeOut,
            fadeDuration = uiState.value.fadeOutDuration.toLong() * 1000,
        )
        getApplication<App>().sleepTimerDataStore.edit { prefs ->
            prefs[Keys.TIMER_VALUE] = uiState.value.timerValue
            prefs[Keys.IS_FINISH_MUSIC] = uiState.value.isFinishMusic
            prefs[Keys.IS_FADE_OUT] = uiState.value.isFadeOut
            prefs[Keys.FADE_OUT_DURATION] = uiState.value.fadeOutDuration
            prefs[Keys.TIMER_MODE] = mode.name
            if (mode == SleepTimerMode.EndAt) {
                prefs[Keys.END_AT_HOUR] = endAtHour
                prefs[Keys.END_AT_MINUTE] = endAtMinute
            }
        }
        _sleepTimerEvent.send(SleepTimerEvent.Set)
    }

    fun cancelTimer() =
        viewModelScope.launch(Dispatchers.IO) {
            if (sleepTimer.cancel()) {
                _sleepTimerEvent.send(SleepTimerEvent.Canceled)
            }
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
        }
    }
}
