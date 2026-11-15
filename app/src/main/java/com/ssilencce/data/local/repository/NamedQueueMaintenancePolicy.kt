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

package com.ssilencce.data.local.repository

import com.ssilencce.data.local.room.NamedQueueEntity

/**
 * Pure maintenance rules for saved queues: append-merge order and least-recently-used eviction.
 */
object NamedQueueMaintenancePolicy {
    /**
     * The target keeps its order and its resume state; source songs are appended. Duplicate
     * skipping compares media ids against the already merged list, so repeated songs inside the
     * source are also skipped once.
     */
    fun mergeMediaIds(
        targetMediaIds: List<String>,
        sourceMediaIds: List<String>,
        skipDuplicates: Boolean,
    ): List<String> {
        if (!skipDuplicates) return targetMediaIds + sourceMediaIds
        val seen = targetMediaIds.toHashSet()
        return buildList(targetMediaIds.size + sourceMediaIds.size) {
            addAll(targetMediaIds)
            sourceMediaIds.forEach { mediaId ->
                if (seen.add(mediaId)) add(mediaId)
            }
        }
    }

    /**
     * Least recently used first. [protectedQueueId] is skipped so an eviction never removes the
     * queue the user is currently bound to.
     */
    fun leastRecentlyUsed(
        queues: List<NamedQueueEntity>,
        protectedQueueId: String? = null,
    ): NamedQueueEntity? =
        queues
            .asSequence()
            .filterNot { it.id == protectedQueueId }
            .minWithOrNull(
                compareBy<NamedQueueEntity> { it.updatedAt }
                    .thenBy { it.createdAt }
                    .thenBy { it.id },
            )
}
