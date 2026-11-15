/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.ssilencce.playback

/**
 * Pure block-move planner for the queue multi-selection.
 *
 * Keys are [QueueSong.key] values (`media id` + occurrence), unique even when the same media id
 * appears more than once. The planner operates on the **timeline** order; callers resolve selected
 * effective/shuffle positions to timeline indices through `QueuePosition`.
 */
object QueueBatchMovePolicy {
    data class Move(
        val from: Int,
        val to: Int,
    )

    data class Result(
        val moves: List<Move>,
        val finalTimelineKeys: List<Pair<Long, Int>>,
        val oldToNewIndex: IntArray,
        val blockStart: Int,
        val blockEndExclusive: Int,
    ) {
        val isNoOp: Boolean
            get() = moves.isEmpty()
    }

    data class StopAfterRemap(
        val stopIndex: Int,
        val pauseAtEndOfMediaItems: Boolean,
    )

    /**
     * Applies the same stop-after rule as SET_STOP_POSITION: only a target that is now the current
     * item uses `pauseAtEndOfMediaItems`; a future target stays represented by `stopIndex` and is
     * armed by the normal media-item transition.
     */
    fun remapStopAfter(
        oldStopIndex: Int,
        oldToNewIndex: IntArray,
        currentMediaItemIndex: Int,
    ): StopAfterRemap? {
        if (oldStopIndex !in oldToNewIndex.indices) return null
        val newStopIndex = oldToNewIndex[oldStopIndex]
        return if (newStopIndex == currentMediaItemIndex) {
            StopAfterRemap(stopIndex = -1, pauseAtEndOfMediaItems = true)
        } else {
            StopAfterRemap(stopIndex = newStopIndex, pauseAtEndOfMediaItems = false)
        }
    }

    fun isDropInsideSelection(
        selectedEffectiveIndices: List<Int>,
        targetEffectiveIndex: Int,
    ): Boolean {
        if (selectedEffectiveIndices.isEmpty()) return false
        return targetEffectiveIndex in selectedEffectiveIndices.min()..selectedEffectiveIndices.max()
    }

    fun isDropAfterSelection(
        selectedEffectiveIndices: List<Int>,
        targetEffectiveIndex: Int,
    ): Boolean {
        if (selectedEffectiveIndices.isEmpty()) return false
        return targetEffectiveIndex > selectedEffectiveIndices.max()
    }

    /**
     * Plans the single-item moves that turn [timelineKeys] into the requested block order.
     *
     * [targetTimelineIndex] is the timeline index of the drop row. [dropAfter] follows the queue
     * reorder convention: the block is inserted after the target when moving downward and before
     * it when moving upward (the target is evaluated after removing the selected block).
     */
    fun move(
        timelineKeys: List<Pair<Long, Int>>,
        selectedTimelineIndices: Set<Int>,
        targetTimelineIndex: Int,
        dropAfter: Boolean,
    ): Result {
        val size = timelineKeys.size
        val identityMap = IntArray(size) { it }
        val selected = selectedTimelineIndices.filter { it in 0 until size }.sorted()
        val noOp = Result(emptyList(), timelineKeys, identityMap, -1, -1)
        if (selected.size < 2 || targetTimelineIndex !in 0 until size) return noOp
        if (targetTimelineIndex in selectedTimelineIndices) return noOp

        val selectedSet = selected.toSet()
        val block = selected.map { timelineKeys[it] }
        val remaining = timelineKeys.filterIndexed { index, _ -> index !in selectedSet }
        val removedBeforeTarget = selected.count { it < targetTimelineIndex }
        val targetRemaining = (targetTimelineIndex - removedBeforeTarget).coerceIn(0, remaining.size)
        val insertAt =
            (if (dropAfter) targetRemaining + 1 else targetRemaining).coerceIn(0, remaining.size)
        val finalKeys =
            buildList {
                addAll(remaining.subList(0, insertAt))
                addAll(block)
                addAll(remaining.subList(insertAt, remaining.size))
            }
        if (finalKeys == timelineKeys) return noOp

        val current = timelineKeys.toMutableList()
        val movingDown = insertAt > selected.first()
        val selectedOrder = if (movingDown) block.asReversed() else block
        val moves = mutableListOf<Move>()
        for (key in selectedOrder) {
            val from = current.indexOf(key)
            val to = finalKeys.indexOf(key)
            if (from != to) {
                moves += Move(from, to)
                current.add(to, current.removeAt(from))
            }
        }
        if (current != finalKeys) {
            // Defensive fallback for an unexpected key layout: preserve the requested final order.
            current.clear()
            current.addAll(timelineKeys)
            moves.clear()
            for (index in finalKeys.indices) {
                val from = current.indexOf(finalKeys[index])
                if (from != index) {
                    moves += Move(from, index)
                    current.add(index, current.removeAt(from))
                }
            }
        }

        val indexByKey = finalKeys.withIndex().associate { it.value to it.index }
        val oldToNewIndex = IntArray(size) { oldIndex -> indexByKey.getValue(timelineKeys[oldIndex]) }
        return Result(
            moves = moves,
            finalTimelineKeys = finalKeys,
            oldToNewIndex = oldToNewIndex,
            blockStart = insertAt,
            blockEndExclusive = insertAt + block.size,
        )
    }
}
