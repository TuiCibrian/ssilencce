package com.ssilencce.playback

import com.ssilencce.core.model.shuffle.ShuffleModeState

/**
 * Pure shuffle-button cycle. States that the user disabled in the shuffle settings are skipped so
 * the button never reaches a state that is not enabled.
 */
object ShuffleCyclePolicy {
    fun states(
        standardEnabled: Boolean,
        randomizeEnabled: Boolean,
    ): List<ShuffleModeState> =
        buildList {
            add(ShuffleModeState.Off)
            // At least one enabled state must remain reachable.
            if (standardEnabled || !randomizeEnabled) add(ShuffleModeState.Shuffle)
            if (randomizeEnabled) add(ShuffleModeState.Randomize)
        }

    fun next(
        current: ShuffleModeState,
        states: List<ShuffleModeState>,
    ): ShuffleModeState {
        if (states.isEmpty()) return ShuffleModeState.Off
        val index = states.indexOf(current)
        return if (index < 0) states.first() else states[(index + 1) % states.size]
    }
}
