package com.ssilencce.data.local.replaygain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.tan

/**
 * Result of streaming PCM through the K-weighting filters. [blockEnergies] are the mean-square
 * values of 400 ms blocks and are kept so album loudness can be gated from the combined blocks.
 */
data class ReplayGainAnalysis(
    val blockEnergies: DoubleArray,
    val peak: Float,
)

/**
 * Android-free BS.1770-4 / EBU R128 math. Kept separate from the analyzer so it can be exercised
 * by a pure probe without loading native or framework classes.
 */
object ReplayGainMath {
    const val REFERENCE_LUFS = -18.0
    const val ABSOLUTE_GATE_LUFS = -70.0
    const val RELATIVE_GATE_LU = -10.0
    const val MIN_GAIN_DB = -24f
    const val MAX_GAIN_DB = 24f

    fun integratedLufs(blockEnergies: DoubleArray): Double? {
        val finiteBlocks = blockEnergies.filter { it.isFinite() && it > 0.0 }
        if (finiteBlocks.isEmpty()) return null

        val absoluteGated = finiteBlocks.filter { loudness(it) > ABSOLUTE_GATE_LUFS }
        if (absoluteGated.isEmpty()) return null

        val absoluteMean = absoluteGated.average()
        val relativeThreshold = loudness(absoluteMean) + RELATIVE_GATE_LU
        val relativeGated = absoluteGated.filter { loudness(it) > relativeThreshold }
        if (relativeGated.isEmpty()) return null

        return loudness(relativeGated.average())
    }

    fun loudness(meanSquare: Double): Double = -0.691 + 10.0 * log10(meanSquare)

    fun trackGainDb(
        lufs: Double,
        peak: Float,
    ): Float = gainDb(lufs, peak)

    fun albumGainDb(
        lufs: Double,
        peak: Float,
    ): Float = gainDb(lufs, peak)

    fun clampGain(
        gainDb: Float,
        peak: Float,
    ): Float = limitByPeak(gainDb, peak).coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)

    /**
     * Limits gain by a linear peak. Any finite positive peak is enforced, including values above
     * 1.0 from a malformed or externally written tag; zero, null and non-finite peaks do not limit.
     */
    fun limitByPeak(
        gainDb: Float,
        peak: Float?,
    ): Float =
        if (peak != null && peak > 0f && peak.isFinite()) {
            min(gainDb, -20f * log10(peak.toDouble()).toFloat())
        } else {
            gainDb
        }

    private fun gainDb(
        lufs: Double,
        peak: Float,
    ): Float = clampGain((REFERENCE_LUFS - lufs).toFloat(), peak)
}

/**
 * Streaming K-weighting loudness meter. Feed interleaved float PCM with [accept] and call
 * [finish] once. The analyzer supports mono/stereo; wider layouts are rejected by the decoder so
 * an incorrect downmix is never reported as a valid measurement.
 */
class ReplayGainAnalyzer(
    private val sampleRate: Int,
    private val channelCount: Int,
) {
    private val blockSize = (sampleRate * 0.4).roundToInt().coerceAtLeast(1)
    private val hop = (sampleRate * 0.1).roundToInt().coerceIn(1, blockSize)

    private val shelf = Array(channelCount) { Biquad.highShelf(sampleRate) }
    private val highPass = Array(channelCount) { Biquad.highPass(sampleRate) }
    private val channelWeights = DoubleArray(channelCount) { 1.0 }

    private val window = DoubleArray(blockSize)
    private var windowHead = 0
    private var windowCount = 0
    private var windowSum = 0.0
    private var peak = 0f

    private val blockEnergies = ArrayList<Double>()

    init {
        require(sampleRate > 0) { "Invalid sample rate: $sampleRate" }
        require(channelCount in 1..2) { "Unsupported channel count: $channelCount" }
    }

    fun accept(
        samples: FloatArray,
        frameCount: Int,
    ) {
        require(frameCount >= 0)
        require(samples.size >= frameCount * channelCount) {
            "Expected ${frameCount * channelCount} samples, got ${samples.size}"
        }

        var sampleIndex = 0
        repeat(frameCount) {
            var frameEnergy = 0.0
            for (channel in 0 until channelCount) {
                val sample = samples[sampleIndex++]
                val magnitude = abs(sample)
                if (magnitude > peak) {
                    peak = magnitude
                }
                val weighted = highPass[channel].process(shelf[channel].process(sample.toDouble()))
                frameEnergy += channelWeights[channel] * weighted * weighted
            }

            window[windowHead] = frameEnergy
            windowHead = (windowHead + 1) % blockSize
            windowSum += frameEnergy
            windowCount++

            if (windowCount == blockSize) {
                blockEnergies += windowSum / blockSize
                repeat(hop) {
                    val oldest = (windowHead - windowCount + blockSize) % blockSize
                    windowSum -= window[oldest]
                    window[oldest] = 0.0
                    windowCount--
                }
            }
        }
    }

    fun finish(): ReplayGainAnalysis = ReplayGainAnalysis(blockEnergies.toDoubleArray(), peak)

    private class Biquad(
        private val b0: Double,
        private val b1: Double,
        private val b2: Double,
        private val a1: Double,
        private val a2: Double,
    ) {
        private var x1 = 0.0
        private var x2 = 0.0
        private var y1 = 0.0
        private var y2 = 0.0

        fun process(x: Double): Double {
            val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1
            x1 = x
            y2 = y1
            y1 = y
            return y
        }

        companion object {
            fun highShelf(sampleRate: Int): Biquad {
                val f0 = 1681.974450955533
                val gainDb = 3.999843853973347
                val q = 0.7071752369554196
                val k = tan(PI * f0 / sampleRate)
                val vh = 10.0.pow(gainDb / 20.0)
                val vb = vh.pow(0.4996667741545416)
                val a0 = 1.0 + k / q + k * k
                return Biquad(
                    b0 = (vh + vb * k / q + k * k) / a0,
                    b1 = 2.0 * (k * k - vh) / a0,
                    b2 = (vh - vb * k / q + k * k) / a0,
                    a1 = 2.0 * (k * k - 1.0) / a0,
                    a2 = (1.0 - k / q + k * k) / a0,
                )
            }

            fun highPass(sampleRate: Int): Biquad {
                val f0 = 38.13547087602444
                val q = 0.5003270373238773
                val k = tan(PI * f0 / sampleRate)
                val a0 = 1.0 + k / q + k * k
                return Biquad(
                    b0 = 1.0,
                    b1 = -2.0,
                    b2 = 1.0,
                    a1 = 2.0 * (k * k - 1.0) / a0,
                    a2 = (1.0 - k / q + k * k) / a0,
                )
            }
        }
    }
}
