package com.ssilencce.core.model.equalizer

import androidx.compose.runtime.Immutable
import kotlin.math.abs

@Immutable
class EqBandCapabilities(
    /**
     * Supported band level range, in decibels.
     */
    val bandRange: ClosedFloatingPointRange<Float>,
    /**
     * Supported band configurations.
     */
    val bandConfigurations: Set<BandConfiguration>,
    /**
     * How many bands the engine can handle when Pro Mode isn't enabled.
     */
    val maxBandCountInNormalMode: Int,
) {
    val hasMultipleBandConfigurations: Boolean = bandConfigurations.size > 1

    fun getAvailableBandCounts(isProMode: Boolean) =
        bandConfigurations
            .filter { it.bandCount <= maxBandCountInNormalMode || isProMode }
            .map { it.bandCount }
            .sorted()

    fun isBandCountSupported(bandCount: Int) = bandConfigurations.any { it.bandCount == bandCount }

    fun getConfiguration(bandCount: Int): BandConfiguration? = bandConfigurations.firstOrNull { it.bandCount == bandCount }

    fun getFrequenciesOrNull(bandCount: Int): IntArray? = getConfiguration(bandCount)?.bandFrequenciesInHz

    fun getFrequencies(bandCount: Int): IntArray =
        requireNotNull(getFrequenciesOrNull(bandCount)) {
            "Unsupported band count: $bandCount"
        }

    /**
     * The device-fixed count is authoritative for single-configuration engines (Legacy Equalizer).
     */
    fun getClosestBandCount(
        bandCount: Int,
        isProMode: Boolean,
    ): Int? =
        getAvailableBandCounts(isProMode).minByOrNull { abs(it - bandCount) }
            ?: bandConfigurations.minByOrNull { abs(it.bandCount - bandCount) }?.bandCount

    fun getBands(
        profile: EqProfile,
        bandCount: Int,
    ): List<EqBand> {
        if (bandConfigurations.isEmpty()) {
            return emptyList()
        }

        val configuration =
            getConfiguration(bandCount)
                ?: bandConfigurations.minByOrNull { abs(it.bandCount - bandCount) }
                ?: return emptyList()
        val levels =
            if (profile.isValid && profile.numberOfBands == configuration.bandCount) {
                profile.levels
            } else if (profile.isValid) {
                EqBandResampler.resample(
                    levels = profile.levels,
                    sourceFrequenciesInHz = EqBandResampler.sourceFrequencies(profile.numberOfBands, this),
                    targetFrequenciesInHz = configuration.bandFrequenciesInHz,
                )
            } else {
                FloatArray(configuration.bandCount)
            }
        return (0 until configuration.bandCount).map {
            EqBand(
                index = it,
                value = levels[it],
                valueRange = bandRange,
                frequencyInHz = configuration.bandFrequenciesInHz[it],
            )
        }
    }

    class BandConfiguration(
        val bandCount: Int,
        val bandFrequenciesInHz: IntArray,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as BandConfiguration

            if (bandCount != other.bandCount) return false
            if (!bandFrequenciesInHz.contentEquals(other.bandFrequenciesInHz)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = bandCount
            result = 31 * result + bandFrequenciesInHz.contentHashCode()
            return result
        }
    }

    companion object {
        val Empty = EqBandCapabilities(-1f..0f, emptySet(), -1)
    }
}
