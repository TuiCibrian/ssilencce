/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ssilencce.playback.extractor

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Converts one DSD channel (1-bit samples) to 88.2 kHz 16-bit PCM.
 *
 * Two cascaded boxcar averages first reduce the 2.8/5.6/11.2 MHz bitstream to 352.8 kHz; a
 * 64-tap Blackman-windowed sinc then removes the out-of-band DSD noise and decimates by four.
 * The whole filter is linear in the input bit count, so the cost per second is constant and
 * independent of the DSD rate (only the first stage length grows with DSD128/DSD256).
 */
internal class DsdChannelDecoder(
    private val decimation: Int,
) {
    private val stageLength = (decimation / 4).coerceAtLeast(1)
    private val stage1Ring = IntArray(stageLength)
    private var stage1Index = 0
    private var stage1Sum = 0
    private var stage1Count = 0

    private val stage2Ring = FloatArray(stageLength)
    private var stage2Index = 0
    private var stage2Sum = 0f

    private val firHistory = FloatArray(FIR_TAPS * 2)
    private var firIndex = 0
    private var firInputCount = 0

    fun reset() {
        stage1Ring.fill(0)
        stage1Index = 0
        stage1Sum = 0
        stage1Count = 0
        stage2Ring.fill(0f)
        stage2Index = 0
        stage2Sum = 0f
        firHistory.fill(0f)
        firIndex = 0
        firInputCount = 0
    }

    /**
     * Consumes one DSD byte and returns one PCM sample when the decimation boundary is reached,
     * or [NO_SAMPLE] otherwise. Bits are read LSB-first for DSF and MSB-first for DSDIFF.
     */
    fun processByte(
        byte: Int,
        msbFirst: Boolean,
    ): Int {
        for (bitIndex in 0 until 8) {
            val shift = if (msbFirst) 7 - bitIndex else bitIndex
            val value = if ((byte shr shift) and 1 == 1) 1 else -1

            stage1Sum += value - stage1Ring[stage1Index]
            stage1Ring[stage1Index] = value
            stage1Index++
            if (stage1Index == stageLength) stage1Index = 0
            stage1Count++
            if (stage1Count < stageLength) continue

            stage1Count = 0
            val stage1Value = stage1Sum.toFloat() / stageLength
            stage2Sum += stage1Value - stage2Ring[stage2Index]
            stage2Ring[stage2Index] = stage1Value
            stage2Index++
            if (stage2Index == stageLength) stage2Index = 0

            firInputCount++
            if (firInputCount < FIR_DECIMATION) continue

            firInputCount = 0
            val firValue = stage2Sum / stageLength
            firHistory[firIndex] = firValue
            firHistory[firIndex + FIR_TAPS] = firValue
            var sum = 0f
            val base = firIndex + 1
            for (tap in 0 until FIR_TAPS) {
                sum += firHistory[base + tap] * COEFFICIENTS[tap]
            }
            firIndex++
            if (firIndex == FIR_TAPS) firIndex = 0
            return (sum * OUTPUT_SCALE).roundToInt().coerceIn(-32768, 32767)
        }
        return NO_SAMPLE
    }

    companion object {
        const val NO_SAMPLE = 32768
        private const val FIR_TAPS = 64
        private const val FIR_DECIMATION = 4
        private const val OUTPUT_SCALE = 0.95f * 32767f

        private val COEFFICIENTS: FloatArray = buildCoefficients()

        private fun buildCoefficients(): FloatArray {
            val coefficients = FloatArray(FIR_TAPS)
            val center = (FIR_TAPS - 1) / 2.0
            // 352.8 kHz input, 40 kHz pass-band edge, 88.2 kHz output.
            val cutoff = 0.45 * 88_200.0 / 352_800.0
            var sum = 0.0
            for (i in 0 until FIR_TAPS) {
                val offset = i - center
                val sinc =
                    if (offset == 0.0) {
                        2.0 * cutoff
                    } else {
                        sin(2.0 * PI * cutoff * offset) / (PI * offset)
                    }
                val window =
                    0.42 -
                        0.5 * cos(2.0 * PI * i / (FIR_TAPS - 1)) +
                        0.08 * cos(4.0 * PI * i / (FIR_TAPS - 1))
                coefficients[i] = (sinc * window).toFloat()
                sum += coefficients[i]
            }
            if (sum != 0.0) {
                for (i in coefficients.indices) {
                    coefficients[i] = (coefficients[i] / sum).toFloat()
                }
            }
            return coefficients
        }
    }
}
