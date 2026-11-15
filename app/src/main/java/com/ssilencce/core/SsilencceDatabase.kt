package com.ssilencce.core

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ssilencce.data.local.room.*

@Database(
    entities = [
        PlaylistEntity::class,
        SongEntity::class,
        HistoryEntity::class,
        PlayCountEntity::class,
        QueueEntity::class,
        InclExclEntity::class,
        LyricsEntity::class,
        PlayEventEntity::class,
        TrackPositionEntity::class,
        NamedQueueEntity::class,
        NamedQueueItemEntity::class,
        SongPlaybackParamEntity::class,
    ],
    version = 9,
    exportSchema = false,
)
abstract class SsilencceDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao

    abstract fun playCountDao(): PlayCountDao

    abstract fun historyDao(): HistoryDao

    abstract fun queueDao(): QueueDao

    abstract fun inclExclDao(): InclExclDao

    abstract fun lyricsDao(): LyricsDao

    abstract fun playEventDao(): PlayEventDao

    abstract fun trackPositionDao(): TrackPositionDao

    abstract fun namedQueueDao(): NamedQueueDao

    abstract fun songPlaybackParamDao(): SongPlaybackParamDao

    companion object {
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE PlaylistEntity ADD COLUMN custom_cover_uri TEXT")
                    db.execSQL("ALTER TABLE PlaylistEntity ADD COLUMN description TEXT")
                }
            }

        val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `QueueEntity` (`id` TEXT NOT NULL, `order` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                }
            }

        val MIGRATION_3_4 =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `CanvasEntity` (`id` INT NOT NULL, `canvas_url` TEXT NOT NULL, `fetch_time` INT NOT NULL, PRIMARY KEY(`id`))")
                }
            }

        val MIGRATION_4_5 =
            object : Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("DROP TABLE IF EXISTS CanvasEntity")
                }
            }

        val MIGRATION_5_6 =
            object : Migration(5, 6) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("DROP TABLE IF EXISTS LyricsEntity")
                    db.execSQL(
                        """
                        CREATE TABLE LyricsEntity (
                            id INTEGER PRIMARY KEY NOT NULL,
                            lyrics TEXT,
                            provider TEXT,
                            is_instrumental INTEGER NOT NULL DEFAULT 0
                        )
                        """.trimIndent(),
                    )
                }
            }

        val MIGRATION_6_7 =
            object : Migration(6, 7) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // Queue identity moves from the media id to the queue position so the same
                    // song can appear more than once in the saved queue.
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `QueueEntity_new` (`id` TEXT NOT NULL, `order` INTEGER NOT NULL, PRIMARY KEY(`order`))",
                    )
                    db.execSQL(
                        "INSERT OR REPLACE INTO `QueueEntity_new` (`id`, `order`) SELECT `id`, `order` FROM `QueueEntity` ORDER BY `order`",
                    )
                    db.execSQL("DROP TABLE `QueueEntity`")
                    db.execSQL("ALTER TABLE `QueueEntity_new` RENAME TO `QueueEntity`")
                }
            }

        val MIGRATION_7_8 =
            object : Migration(7, 8) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // Period statistics start recording from this version; existing aggregate
                    // play counts remain available for all-time queries.
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `PlayEventEntity` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `song_id` INTEGER NOT NULL,
                            `artist_name` TEXT NOT NULL,
                            `played_at` INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_PlayEventEntity_song_id` ON `PlayEventEntity` (`song_id`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_PlayEventEntity_played_at` ON `PlayEventEntity` (`played_at`)")

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `TrackPositionEntity` (
                            `media_id` TEXT NOT NULL,
                            `position_ms` INTEGER NOT NULL,
                            `updated_at` INTEGER NOT NULL,
                            PRIMARY KEY(`media_id`)
                        )
                        """.trimIndent(),
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `NamedQueueEntity` (
                            `id` TEXT NOT NULL,
                            `name` TEXT NOT NULL,
                            `created_at` INTEGER NOT NULL,
                            `updated_at` INTEGER NOT NULL,
                            `resume_index` INTEGER NOT NULL,
                            `resume_position_ms` INTEGER NOT NULL,
                            `repeat_mode` INTEGER NOT NULL,
                            `shuffle_enabled` INTEGER NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `NamedQueueItemEntity` (
                            `queue_id` TEXT NOT NULL,
                            `order` INTEGER NOT NULL,
                            `media_id` TEXT NOT NULL,
                            PRIMARY KEY(`queue_id`, `order`)
                        )
                        """.trimIndent(),
                    )
                }
            }

        val MIGRATION_8_9 =
            object : Migration(8, 9) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // Per-song tempo overrides are optional; existing songs keep following the
                    // global speed and pitch.
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `song_playback_params` (
                            `song_id` INTEGER NOT NULL,
                            `speed` REAL NOT NULL,
                            `pitch` REAL NOT NULL,
                            PRIMARY KEY(`song_id`)
                        )
                        """.trimIndent(),
                    )
                }
            }
    }
}
