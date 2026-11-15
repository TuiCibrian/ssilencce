package com.ssilencce.playback

import com.ssilencce.core.model.shuffle.ShuffleModeState

/**
 * Process-wide shuffle state shared by the playback service and the UI. The service is the only
 * writer; readers use it to distinguish randomize from a fixed shuffled order without IPC.
 */
object ShuffleStateHolder {
    @Volatile
    var state: ShuffleModeState = ShuffleModeState.Off
        private set

    fun update(value: ShuffleModeState) {
        state = value
    }
}
