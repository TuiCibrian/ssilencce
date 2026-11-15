package com.ssilencce.data.local.room

/**
 * Projection used by in-memory song sorting; it joins the aggregate play statistics without
 * loading the full [PlayCountEntity] row.
 */
data class SongPlayStatistics(
    val id: Long,
    val playCount: Int,
    val timePlayed: Long,
)
