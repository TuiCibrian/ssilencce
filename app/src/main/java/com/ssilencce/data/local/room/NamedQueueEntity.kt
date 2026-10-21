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

package com.ssilencce.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-named, persistent queue. The active playback queue remains [QueueEntity]; a named
 * queue is a snapshot that can be loaded into it.
 */
@Entity(tableName = "NamedQueueEntity")
data class NamedQueueEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "resume_index")
    val resumeIndex: Int,
    @ColumnInfo(name = "resume_position_ms")
    val resumePositionMs: Long,
    @ColumnInfo(name = "repeat_mode")
    val repeatMode: Int,
    @ColumnInfo(name = "shuffle_enabled")
    val shuffleEnabled: Boolean,
)
