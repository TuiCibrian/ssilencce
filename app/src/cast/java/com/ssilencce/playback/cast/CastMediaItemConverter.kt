package com.ssilencce.playback.cast

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.annotation.OptIn
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.DefaultMediaItemConverter
import androidx.media3.cast.MediaItemConverter
import androidx.media3.cast.RemoteCastPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.google.android.gms.cast.MediaQueueItem
import com.ssilencce.coil.CoverProvider
import java.util.concurrent.ConcurrentHashMap

/**
 * Rewrites local media items to HTTP URLs served by [CastMediaServer] before they are sent to the
 * Cast receiver, and restores the original local items when the receiver timeline is read back.
 * [DefaultMediaItemConverter] is reused for the actual Cast metadata/custom-data serialization.
 */
@OptIn(UnstableApi::class)
internal class CastMediaItemConverter(
    private val context: Context,
    private val serverProvider: () -> CastMediaServer?,
) : MediaItemConverter {
    private val delegate = DefaultMediaItemConverter()
    private val localItems = ConcurrentHashMap<String, MediaItem>()

    override fun toMediaQueueItem(mediaItem: MediaItem): MediaQueueItem {
        localItems[mediaItem.mediaId] = mediaItem
        val server = serverProvider() ?: return delegate.toMediaQueueItem(mediaItem)
        val remoteItem = mediaItem.toRemoteMediaItem(server) ?: return delegate.toMediaQueueItem(mediaItem)
        return delegate.toMediaQueueItem(remoteItem)
    }

    override fun toMediaItem(mediaQueueItem: MediaQueueItem): MediaItem {
        val item = delegate.toMediaItem(mediaQueueItem)
        localItems[item.mediaId]?.let { return it }
        // The process may have been recreated while a receiver was connected, so the original
        // local item map can be empty. Rebuild a playable local item from the media id instead of
        // keeping the HTTP URL that only works while the embedded server is running.
        val localItem = item.toLocalItemFromMediaId() ?: return item
        localItems[item.mediaId] = localItem
        return localItem
    }

    private fun MediaItem.toLocalItemFromMediaId(): MediaItem? {
        val songId = mediaId.toLongOrNull() ?: return null
        val localUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)
        val coverUri = CoverProvider.getImageUri(CoverProvider.SONG_COVER_PATH, songId)
        return buildUpon()
            .setUri(localUri)
            .setMediaMetadata(
                mediaMetadata
                    .buildUpon()
                    .setArtworkUri(coverUri)
                    .build(),
            ).build()
    }

    private fun MediaItem.toRemoteMediaItem(server: CastMediaServer): MediaItem? {
        val configuration = localConfiguration ?: return null
        val mimeType = normalizeMimeType(configuration.mimeType ?: resolveMimeType(configuration.uri))
        val audioPath = server.registerAudio(configuration.uri, mimeType) ?: return null
        val audioUrl = server.urlFor(audioPath) ?: return null
        val artworkUrl =
            mediaMetadata.artworkUri?.let { artworkUri ->
                server.registerArtwork(artworkUri)?.let { path -> server.urlFor(path) }
            }
        return buildUpon()
            .setUri(audioUrl)
            .setMimeType(mimeType)
            .setMediaMetadata(
                mediaMetadata
                    .buildUpon()
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setArtworkUri(artworkUrl?.let(Uri::parse))
                    .build(),
            ).build()
    }

    private fun resolveMimeType(uri: Uri): String {
        val resolved =
            if (uri.scheme == "content") {
                runCatching { context.contentResolver.getType(uri) }.getOrNull()
            } else {
                val extension =
                    uri.path
                        ?.substringAfterLast('.', "")
                        ?.lowercase()
                        .orEmpty()
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            }
        if (resolved.isNullOrBlank()) {
            Log.w(TAG, "Unknown MIME type for $uri, assuming audio/mpeg")
        }
        return resolved?.takeIf { it.isNotBlank() } ?: MimeTypes.AUDIO_MPEG
    }

    /** The default receiver expects the RFC MIME types, not the legacy `audio/x-*` aliases. */
    private fun normalizeMimeType(mimeType: String): String =
        when (mimeType.lowercase()) {
            "audio/x-flac" -> MimeTypes.AUDIO_FLAC
            "audio/x-wav", "audio/wave" -> MimeTypes.AUDIO_WAV
            "audio/x-matroska" -> MimeTypes.AUDIO_MATROSKA
            else -> mimeType
        }

    companion object {
        private const val TAG = "CastMediaItemConverter"
    }
}
