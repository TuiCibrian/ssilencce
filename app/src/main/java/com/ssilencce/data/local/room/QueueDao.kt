package com.ssilencce.data.local.room

import androidx.room.*

@Dao
interface QueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveQueue(items: List<QueueEntity>)

    @Query("SELECT * FROM QueueEntity ORDER BY `order`")
    suspend fun savedItems(): List<QueueEntity>

    @Query("DELETE FROM QueueEntity")
    suspend fun clearQueue()

    @Transaction
    suspend fun replaceQueue(newQueue: List<QueueEntity>) {
        clearQueue()
        saveQueue(newQueue)
    }
}
