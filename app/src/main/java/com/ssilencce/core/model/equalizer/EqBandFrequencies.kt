package com.ssilencce.core.model.equalizer

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * Center frequencies used by the equalizer engines.
 *
 * The tables are ascending so [EqBandResampler] can interpolate between any two layouts. The
 * 31-band list is the standard 1/3-octave ISO series (without the unusual 16 Hz band).
 */
object EqBandFrequencies {
    val SUPPORTED_BAND_COUNTS: List<Int> = listOf(3, 5, 10, 15, 31)

    fun isSupported(bandCount: Int): Boolean = bandCount in SUPPORTED_BAND_COUNTS

    fun closestBandCount(bandCount: Int): Int = SUPPORTED_BAND_COUNTS.minByOrNull { abs(it - bandCount) } ?: bandCount

    fun forBandCount(bandCount: Int): IntArray? =
        when (bandCount) {
            3 -> BAND_FREQUENCIES_IN_HZ_3
            5 -> BAND_FREQUENCIES_IN_HZ_5
            10 -> BAND_FREQUENCIES_IN_HZ_10
            15 -> BAND_FREQUENCIES_IN_HZ_15
            31 -> BAND_FREQUENCIES_IN_HZ_31
            32 -> LEGACY_BAND_FREQUENCIES_IN_HZ_32
            else -> null
        }

    fun requireForBandCount(bandCount: Int): IntArray = requireNotNull(forBandCount(bandCount)) { "Unsupported band count: $bandCount" }

    /**
     * Log-spaced fallback for layouts produced by other equalizer implementations.
     */
    fun logSpaced(
        bandCount: Int,
        minFrequencyInHz: Int = 20,
        maxFrequencyInHz: Int = 20000,
    ): IntArray {
        if (bandCount <= 0) return IntArray(0)
        if (bandCount == 1) return intArrayOf(minFrequencyInHz)
        val minLog = ln(minFrequencyInHz.toDouble())
        val maxLog = ln(maxFrequencyInHz.toDouble())
        val step = (maxLog - minLog) / (bandCount - 1)
        return IntArray(bandCount) { index ->
            exp(minLog + step * index).roundToInt()
        }
    }

    private val BAND_FREQUENCIES_IN_HZ_3 =
        intArrayOf(
            100,
            1000,
            10000,
        )

    private val BAND_FREQUENCIES_IN_HZ_5 =
        intArrayOf(
            60,
            230,
            910,
            3600,
            14000,
        )

    private val BAND_FREQUENCIES_IN_HZ_10 =
        intArrayOf(
            31,
            63,
            125,
            250,
            500,
            1000,
            2000,
            4000,
            8000,
            16000,
        )

    private val BAND_FREQUENCIES_IN_HZ_15 =
        intArrayOf(
            25,
            40,
            63,
            100,
            160,
            250,
            400,
            630,
            1000,
            1600,
            2500,
            4000,
            6300,
            10000,
            16000,
        )

    private val BAND_FREQUENCIES_IN_HZ_31 =
        intArrayOf(
            20,
            25,
            31,
            40,
            50,
            63,
            80,
            100,
            125,
            160,
            200,
            250,
            315,
            400,
            500,
            630,
            800,
            1000,
            1250,
            1600,
            2000,
            2500,
            3150,
            4000,
            5000,
            6300,
            8000,
            10000,
            12500,
            16000,
            20000,
        )

    /**
     * Last layout used before 31 bands replaced it; only needed to migrate stored profiles.
     */
    private val LEGACY_BAND_FREQUENCIES_IN_HZ_32 =
        intArrayOf(
            16,
            20,
            25,
            31,
            40,
            50,
            63,
            80,
            100,
            125,
            160,
            200,
            250,
            315,
            400,
            500,
            630,
            800,
            1000,
            1250,
            1600,
            2000,
            2500,
            3150,
            4000,
            5000,
            6300,
            8000,
            10000,
            12500,
            16000,
            20000,
        )
}
