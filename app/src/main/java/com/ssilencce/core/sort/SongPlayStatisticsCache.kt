package com.ssilencce.core.sort

import android.os.SystemClock
import com.ssilencce.data.local.room.PlayCountDao
import com.ssilencce.data.local.room.SongPlayStatistics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/**
 * Short-lived, in-memory cache of the aggregate play statistics used by the play-count sort
 * keys. The whole table is joined once per snapshot instead of querying per song.
 */
object SongPlayStatisticsCache : KoinComponent {
    private const val CACHE_TTL_MS = 5_000L

    private val lock = Any()

    @Volatile
    private var cached: Map<Long, SongPlayStatistics> = emptyMap()

    @Volatile
    private var hasSnapshot = false

    @Volatile
    private var loadedAt = 0L

    fun snapshot(): Map<Long, SongPlayStatistics> {
        val now = SystemClock.elapsedRealtime()
        if (hasSnapshot && now - loadedAt <= CACHE_TTL_MS) {
            return cached
        }
        synchronized(lock) {
            val current = SystemClock.elapsedRealtime()
            if (hasSnapshot && current - loadedAt <= CACHE_TTL_MS) {
                return cached
            }
            val statistics =
                runBlocking(Dispatchers.IO) {
                    get<PlayCountDao>()
                        .allPlayStatistics()
                        .associateBy { it.id }
                }
            cached = statistics
            loadedAt = SystemClock.elapsedRealtime()
            hasSnapshot = true
            return statistics
        }
    }
}
