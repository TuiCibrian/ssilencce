package com.ssilencce.playback

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.ShuffleOrder
import kotlin.random.Random

/**
 * A dynamic shuffle order: instead of materializing a permutation, every navigation picks a random
 * destination. Adjacent random choices are cached per source index so repeated calls during one
 * transition (hasNext followed by seekToNext) agree.
 *
 * With [belowCurrentOnly] the walk only moves forward, mirroring the "only shuffle songs below the
 * current song" behavior; the queue then ends naturally when the last timeline item is reached.
 */
@UnstableApi
class RandomizeShuffleOrder(
    private val length: Int,
    private val belowCurrentOnly: Boolean = false,
    private val random: Random = Random.Default,
) : ShuffleOrder {
    private val lock = Any()
    private var nextSource = C.INDEX_UNSET
    private var nextTarget = C.INDEX_UNSET
    private var previousSource = C.INDEX_UNSET
    private var previousTarget = C.INDEX_UNSET

    override fun getLength(): Int = length

    override fun getNextIndex(index: Int): Int =
        synchronized(lock) {
            if (length <= 1) {
                C.INDEX_UNSET
            } else if (index == nextSource) {
                nextTarget
            } else {
                val target =
                    if (belowCurrentOnly) {
                        pickAfter(index)
                    } else {
                        pickDifferent(index)
                    }
                nextSource = index
                nextTarget = target
                target
            }
        }

    override fun getPreviousIndex(index: Int): Int =
        synchronized(lock) {
            if (length <= 1) {
                C.INDEX_UNSET
            } else if (index == previousSource) {
                previousTarget
            } else {
                val target =
                    if (belowCurrentOnly) {
                        pickBefore(index)
                    } else {
                        pickDifferent(index)
                    }
                previousSource = index
                previousTarget = target
                target
            }
        }

    override fun getLastIndex(): Int = if (length > 0) length - 1 else C.INDEX_UNSET

    override fun getFirstIndex(): Int = synchronized(lock) { if (length > 0) random.nextInt(length) else C.INDEX_UNSET }

    override fun cloneAndInsert(
        insertionIndex: Int,
        insertionCount: Int,
    ): ShuffleOrder = RandomizeShuffleOrder(length + insertionCount, belowCurrentOnly, random)

    override fun cloneAndRemove(
        indexFrom: Int,
        indexToExclusive: Int,
    ): ShuffleOrder {
        val removed = indexToExclusive - indexFrom
        return RandomizeShuffleOrder((length - removed).coerceAtLeast(0), belowCurrentOnly, random)
    }

    override fun cloneAndMove(
        indexFrom: Int,
        indexToExclusive: Int,
        newIndexFrom: Int,
    ): ShuffleOrder = RandomizeShuffleOrder(length, belowCurrentOnly, random)

    override fun cloneAndClear(): ShuffleOrder = RandomizeShuffleOrder(0, belowCurrentOnly, random)

    override fun cloneAndSet(
        insertionCount: Int,
        startIndex: Int,
    ): ShuffleOrder = RandomizeShuffleOrder(insertionCount, belowCurrentOnly, random)

    private fun pickDifferent(index: Int): Int {
        if (index !in 0 until length) return random.nextInt(length)
        // Guarantees a different item without unbounded retries.
        return (index + 1 + random.nextInt(length - 1)) % length
    }

    private fun pickAfter(index: Int): Int {
        if (index !in 0 until length) return random.nextInt(length)
        val remaining = length - index - 1
        return if (remaining > 0) index + 1 + random.nextInt(remaining) else C.INDEX_UNSET
    }

    private fun pickBefore(index: Int): Int {
        if (index !in 0 until length) return random.nextInt(length)
        return if (index > 0) random.nextInt(index) else C.INDEX_UNSET
    }
}
