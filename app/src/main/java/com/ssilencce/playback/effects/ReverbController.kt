package com.ssilencce.playback.effects

import android.media.audiofx.EnvironmentalReverb
import android.util.Log
import com.ssilencce.core.model.equalizer.ReverbSettings

/**
 * Owns the [EnvironmentalReverb] attached to the player audio session.
 *
 * The effect is only created while it is enabled and effects are allowed; it is released when
 * the audio session changes, when effects are disabled (bit-perfect/offload) or when playback
 * service shuts down.
 */
class ReverbController {
    private var reverb: EnvironmentalReverb? = null
    private var sessionId: Int = NO_SESSION_ID
    private var effectsAllowed: Boolean = false
    private var enabled: Boolean = false
    private var settings: ReverbSettings = ReverbSettings.MediumRoom

    @Synchronized
    fun update(
        sessionId: Int,
        effectsAllowed: Boolean,
        enabled: Boolean,
        settings: ReverbSettings,
    ) {
        val sessionChanged = sessionId != this.sessionId
        this.sessionId = sessionId
        this.effectsAllowed = effectsAllowed
        this.enabled = enabled
        this.settings = settings

        if (sessionChanged) {
            release()
        }

        if (!effectsAllowed || !enabled || sessionId == NO_SESSION_ID) {
            reverb?.let { effect ->
                runCatching { effect.enabled = false }
            }
            if (!effectsAllowed || sessionId == NO_SESSION_ID) {
                release()
            }
            return
        }

        val effect = reverb ?: createEffect()
        if (effect == null) {
            return
        }
        reverb = effect
        runCatching {
            applySettings(effect, settings)
            effect.enabled = true
        }.onFailure {
            Log.e(TAG, "Failed to apply reverb settings", it)
        }
    }

    @Synchronized
    fun release() {
        reverb?.let { effect ->
            runCatching { effect.enabled = false }
            runCatching { effect.release() }
        }
        reverb = null
    }

    private fun createEffect(): EnvironmentalReverb? =
        runCatching {
            EnvironmentalReverb(0, sessionId).also { effect ->
                if (!effect.hasControl()) {
                    effect.release()
                    throw IllegalStateException("Reverb effect has no control")
                }
            }
        }.onFailure {
            Log.e(TAG, "Failed to create EnvironmentalReverb", it)
        }.getOrNull()

    private fun applySettings(
        effect: EnvironmentalReverb,
        settings: ReverbSettings,
    ) {
        effect.setRoomLevel(settings.roomLevel.coerceIn(ROOM_MIN, ROOM_MAX).toShort())
        effect.setRoomHFLevel(settings.roomHfLevel.coerceIn(ROOM_MIN, ROOM_MAX).toShort())
        effect.setDecayTime(settings.decayTimeMs.coerceIn(DECAY_MIN, DECAY_MAX))
        effect.setDecayHFRatio(settings.decayHfRatio.coerceIn(DECAY_RATIO_MIN, DECAY_RATIO_MAX).toShort())
        effect.setReflectionsLevel(settings.reflectionsLevel.coerceIn(REFLECTIONS_MIN, REFLECTIONS_MAX).toShort())
        effect.setReflectionsDelay(settings.reflectionsDelayMs.coerceIn(0, REFLECTIONS_DELAY_MAX))
        effect.setReverbLevel(settings.reverbLevel.coerceIn(REVERB_MIN, REVERB_MAX).toShort())
        effect.setReverbDelay(settings.reverbDelayMs.coerceIn(0, REVERB_DELAY_MAX))
        effect.setDiffusion(settings.diffusion.coerceIn(DIFFUSION_MIN, DIFFUSION_MAX).toShort())
        effect.setDensity(settings.density.coerceIn(DENSITY_MIN, DENSITY_MAX).toShort())
    }

    private companion object {
        const val TAG = "ReverbController"
        const val NO_SESSION_ID = 0

        const val ROOM_MIN = -9000
        const val ROOM_MAX = 0
        const val REFLECTIONS_MIN = -9000
        const val REFLECTIONS_MAX = 1000
        const val REVERB_MIN = -9000
        const val REVERB_MAX = 2000
        const val DECAY_MIN = 100
        const val DECAY_MAX = 20000
        const val DECAY_RATIO_MIN = 100
        const val DECAY_RATIO_MAX = 2000
        const val REFLECTIONS_DELAY_MAX = 65
        const val REVERB_DELAY_MAX = 100
        const val DIFFUSION_MIN = 0
        const val DIFFUSION_MAX = 1000
        const val DENSITY_MIN = 0
        const val DENSITY_MAX = 1000
    }
}
