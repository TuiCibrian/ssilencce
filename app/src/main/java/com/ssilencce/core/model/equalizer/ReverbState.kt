package com.ssilencce.core.model.equalizer

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.ssilencce.R

/**
 * Environmental reverb presets. [Custom] is stored when the user edits any parameter and is not
 * offered in the preset selector.
 */
enum class ReverbPreset(
    @StringRes val nameRes: Int,
) {
    SmallRoom(R.string.reverb_preset_small_room),
    MediumRoom(R.string.reverb_preset_medium_room),
    LargeRoom(R.string.reverb_preset_large_room),
    MediumHall(R.string.reverb_preset_medium_hall),
    LargeHall(R.string.reverb_preset_large_hall),
    Plate(R.string.reverb_preset_plate),
    Custom(R.string.reverb_preset_custom),
    ;

    val isCustom: Boolean get() = this == Custom

    companion object {
        val Selectable = entries.filterNot { it.isCustom }

        fun fromName(name: String?): ReverbPreset = entries.firstOrNull { it.name == name } ?: MediumRoom
    }
}

/**
 * EnvironmentalReverb parameters. Levels are stored in millibels, times in milliseconds, the
 * decay HF ratio and diffusion/density in the platform's per-mille units.
 */
@Immutable
data class ReverbSettings(
    val roomLevel: Int,
    val roomHfLevel: Int,
    val decayTimeMs: Int,
    val decayHfRatio: Int,
    val reflectionsLevel: Int,
    val reflectionsDelayMs: Int,
    val reverbLevel: Int,
    val reverbDelayMs: Int,
    val diffusion: Int,
    val density: Int,
) {
    companion object {
        val MediumRoom =
            ReverbSettings(
                roomLevel = -600,
                roomHfLevel = -300,
                decayTimeMs = 2100,
                decayHfRatio = 500,
                reflectionsLevel = -1300,
                reflectionsDelayMs = 5,
                reverbLevel = -100,
                reverbDelayMs = 15,
                diffusion = 1000,
                density = 1000,
            )

        fun forPreset(preset: ReverbPreset): ReverbSettings =
            when (preset) {
                ReverbPreset.SmallRoom -> {
                    MediumRoom.copy(
                        roomLevel = -1000,
                        roomHfLevel = -600,
                        decayTimeMs = 1100,
                        reflectionsLevel = -1200,
                        reverbLevel = -200,
                        reverbDelayMs = 11,
                    )
                }

                ReverbPreset.LargeRoom -> {
                    MediumRoom.copy(
                        roomLevel = -500,
                        decayTimeMs = 3100,
                        reflectionsLevel = -1400,
                        reverbLevel = 0,
                        reverbDelayMs = 20,
                    )
                }

                ReverbPreset.MediumHall -> {
                    MediumRoom.copy(
                        roomLevel = -1000,
                        roomHfLevel = -600,
                        decayTimeMs = 3600,
                        decayHfRatio = 700,
                        reverbLevel = 500,
                        reverbDelayMs = 30,
                    )
                }

                ReverbPreset.LargeHall -> {
                    MediumRoom.copy(
                        roomLevel = -1000,
                        roomHfLevel = -600,
                        decayTimeMs = 5100,
                        decayHfRatio = 700,
                        reflectionsLevel = -1400,
                        reflectionsDelayMs = 10,
                        reverbLevel = 500,
                        reverbDelayMs = 40,
                    )
                }

                ReverbPreset.Plate -> {
                    MediumRoom.copy(
                        roomLevel = 0,
                        roomHfLevel = 0,
                        decayTimeMs = 1400,
                        decayHfRatio = 700,
                        reflectionsLevel = -1000,
                        reflectionsDelayMs = 0,
                        reverbLevel = 500,
                        reverbDelayMs = 0,
                    )
                }

                ReverbPreset.MediumRoom, ReverbPreset.Custom -> {
                    MediumRoom
                }
            }
    }
}

@Immutable
data class ReverbState(
    val supported: Boolean,
    val enabled: Boolean,
    val preset: ReverbPreset,
    val settings: ReverbSettings,
) {
    val isUsable = supported && enabled

    companion object {
        val Unspecified =
            ReverbState(
                supported = false,
                enabled = false,
                preset = ReverbPreset.MediumRoom,
                settings = ReverbSettings.MediumRoom,
            )
    }
}
