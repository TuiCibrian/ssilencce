package com.ssilencce.playback

import androidx.media3.common.Player

/**
 * Single owner of the player output volume. Combines the user/equalizer volume, the
 * audio-focus ducking multiplier and the active playback fade multiplier so those
 * features never overwrite each other.
 */
class PlaybackVolumeController(
    private val player: Player,
) {
    private var baseVolume = 1f
    private var duckMultiplier = 1f
    private var fadeMultiplier = 1f
    private var crossfadeMultiplier = 1f

    val currentFadeMultiplier: Float
        get() = fadeMultiplier

    /** Volume produced by the main player without fade/crossfade ramps. */
    val currentOutputVolume: Float
        get() = (baseVolume * duckMultiplier).coerceIn(0f, 1f)

    fun setBaseVolume(volume: Float) {
        baseVolume = volume.coerceIn(0f, 1f)
        applyVolume()
    }

    fun setDucked(ducked: Boolean) {
        duckMultiplier = if (ducked) DUCK_MULTIPLIER else 1f
        applyVolume()
    }

    fun setFadeMultiplier(multiplier: Float) {
        fadeMultiplier = multiplier.coerceIn(0f, 1f)
        applyVolume()
    }

    fun clearFade() = setFadeMultiplier(1f)

    fun setCrossfadeMultiplier(multiplier: Float) {
        crossfadeMultiplier = multiplier.coerceIn(0f, 1f)
        applyVolume()
    }

    fun clearCrossfade() = setCrossfadeMultiplier(1f)

    private fun applyVolume() {
        player.volume = (baseVolume * duckMultiplier * fadeMultiplier * crossfadeMultiplier).coerceIn(0f, 1f)
    }

    private companion object {
        const val DUCK_MULTIPLIER = 0.2f
    }
}
