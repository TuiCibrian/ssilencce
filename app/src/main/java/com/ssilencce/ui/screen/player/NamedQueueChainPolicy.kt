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

package com.ssilencce.ui.screen.player

/**
 * Pure named-queue chain resolution. [orderedIds] is the stable `created_at ASC, id ASC` order;
 * the chain never wraps and requires a valid active id.
 */
object NamedQueueChainPolicy {
    enum class Direction {
        First,
        Previous,
        Next,
    }

    sealed interface Result {
        data class Target(
            val queueId: String,
        ) : Result

        data object Boundary : Result

        data object NoActive : Result
    }

    fun matchesTimeline(
        namedMediaIds: List<String>,
        timelineMediaIds: List<String>,
    ): Boolean = namedMediaIds == timelineMediaIds

    fun resolve(
        orderedIds: List<String>,
        activeId: String?,
        direction: Direction,
    ): Result {
        if (activeId == null) return Result.NoActive
        val index = orderedIds.indexOf(activeId)
        if (index < 0) return Result.NoActive
        val target =
            when (direction) {
                Direction.First -> orderedIds.firstOrNull()
                Direction.Previous -> orderedIds.getOrNull(index - 1)
                Direction.Next -> orderedIds.getOrNull(index + 1)
            } ?: return Result.Boundary
        return Result.Target(target)
    }
}
