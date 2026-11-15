package com.ssilencce.ui.screen.miniplayer

import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.ssilencce.data.model.Song

/**
 * What the mini player should play: an external VIEW intent or a library preview.
 */
sealed interface MiniPlayerRequest {
    data class External(
        val uri: Uri,
    ) : MiniPlayerRequest

    data class Preview(
        val song: Song,
    ) : MiniPlayerRequest

    data object Unsupported : MiniPlayerRequest

    companion object {
        const val EXTRA_SONG = "com.ssilencce.extra.MINI_PLAYER_SONG"

        fun fromIntent(intent: Intent): MiniPlayerRequest {
            val song = IntentCompat.getParcelableExtra(intent, EXTRA_SONG, Song::class.java)
            if (song != null) {
                return Preview(song)
            }
            val uri = intent.data ?: return Unsupported
            return External(uri)
        }
    }
}

data class MiniPlayerTrack(
    val uri: Uri,
    val title: String,
    val artist: String?,
    val album: String?,
    val artworkUri: Uri?,
    val durationMs: Long,
    val song: Song?,
) {
    fun toMediaItem(): MediaItem =
        MediaItem
            .Builder()
            .setUri(uri)
            .setMediaId(song?.id?.toString() ?: uri.toString())
            .setMediaMetadata(
                MediaMetadata
                    .Builder()
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(artworkUri)
                    .setDurationMs(durationMs.coerceAtLeast(0L))
                    .build(),
            ).build()
}

data class MiniPlayerUiState(
    val track: MiniPlayerTrack? = null,
    val isPreview: Boolean = false,
    val isLoading: Boolean = true,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val blockedByMute: Boolean = false,
    val hasError: Boolean = false,
    val canOpenInApp: Boolean = false,
)
