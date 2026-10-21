package com.ssilencce.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class QueueEntity(
    val id: String,
    @PrimaryKey
    val order: Int,
)
