package com.ssilencce.core.model.equalizer

import kotlin.math.log10

/**
 * Interpolates a gain curve between two equalizer band layouts.
 *
 * Gains are interpolated linearly in log-frequency space, which keeps the perceived curve stable
 * when the band count changes (for example 10 -> 31 bands or a legacy 32-band profile -> 31).
 */
object EqBandResampler {
    fun resample(
        levels: FloatArray,
        sourceFrequenciesInHz: IntArray,
        targetFrequenciesInHz: IntArray,
    ): FloatArray {
        if (targetFrequenciesInHz.isEmpty()) return FloatArray(0)
        if (levels.isEmpty() || sourceFrequenciesInHz.isEmpty()) {
            return FloatArray(targetFrequenciesInHz.size)
        }
        if (levels.size == targetFrequenciesInHz.size &&
            sourceFrequenciesInHz.contentEquals(targetFrequenciesInHz)
        ) {
            return levels.copyOf()
        }

        val usableBands =
            sourceFrequenciesInHz.indices
                .filter { it < levels.size && sourceFrequenciesInHz[it] > 0 }
                .sortedBy { sourceFrequenciesInHz[it] }
        if (usableBands.isEmpty()) return FloatArray(targetFrequenciesInHz.size)

        return FloatArray(targetFrequenciesInHz.size) { targetIndex ->
            interpolate(
                levels = levels,
                sourceFrequenciesInHz = sourceFrequenciesInHz,
                usableBands = usableBands,
                targetFrequencyInHz = targetFrequenciesInHz[targetIndex],
            )
        }
    }

    fun resampleProfile(
        profile: EqProfile,
        bandCapabilities: EqBandCapabilities,
        targetBandCount: Int,
    ): EqProfile? {
        if (!profile.isValid) return null
        val targetFrequencies = bandCapabilities.getFrequenciesOrNull(targetBandCount) ?: return null
        if (profile.numberOfBands == targetBandCount) return profile
        val sourceFrequencies = sourceFrequencies(profile.numberOfBands, bandCapabilities)
        return profile.copy(
            levels = resample(profile.levels, sourceFrequencies, targetFrequencies),
        )
    }

    fun sourceFrequencies(
        bandCount: Int,
        bandCapabilities: EqBandCapabilities,
    ): IntArray =
        bandCapabilities.getFrequenciesOrNull(bandCount)
            ?: EqBandFrequencies.forBandCount(bandCount)
            ?: EqBandFrequencies.logSpaced(bandCount)

    private fun interpolate(
        levels: FloatArray,
        sourceFrequenciesInHz: IntArray,
        usableBands: List<Int>,
        targetFrequencyInHz: Int,
    ): Float {
        val firstBand = usableBands.first()
        val lastBand = usableBands.last()
        if (targetFrequencyInHz <= sourceFrequenciesInHz[firstBand]) return levels[firstBand]
        if (targetFrequencyInHz >= sourceFrequenciesInHz[lastBand]) return levels[lastBand]

        for (index in 1 until usableBands.size) {
            val lowerBand = usableBands[index - 1]
            val upperBand = usableBands[index]
            val lowerFrequency = sourceFrequenciesInHz[lowerBand]
            val upperFrequency = sourceFrequenciesInHz[upperBand]
            if (targetFrequencyInHz in lowerFrequency..upperFrequency) {
                if (upperFrequency == lowerFrequency) return levels[lowerBand]
                val logTarget = log10(targetFrequencyInHz.toFloat())
                val logLower = log10(lowerFrequency.toFloat())
                val logUpper = log10(upperFrequency.toFloat())
                val ratio = (logTarget - logLower) / (logUpper - logLower)
                return levels[lowerBand] + (levels[upperBand] - levels[lowerBand]) * ratio
            }
        }
        return levels[lastBand]
    }
}
