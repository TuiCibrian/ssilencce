package com.ssilencce.ui.screen.player.art

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.StringRes
import androidx.annotation.WorkerThread
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.toBitmap
import com.ssilencce.R
import com.ssilencce.coil.CoverProvider
import com.ssilencce.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Loads and exports the artwork of a song without touching the main thread.
 */
object ArtworkStore {
    private const val TAG = "ArtworkStore"
    private const val MAX_ARTWORK_SIZE = 2048
    private const val GALLERY_FOLDER = "ssilencce"

    enum class WallpaperTarget(
        @param:StringRes val titleRes: Int,
    ) {
        Home(R.string.wallpaper_target_home),
        Lock(R.string.wallpaper_target_lock),
        Both(R.string.wallpaper_target_both),
    }

    @WorkerThread
    suspend fun loadArtwork(
        context: Context,
        song: Song,
    ): Bitmap? =
        withContext(Dispatchers.IO) {
            if (song.id == -1L) return@withContext null
            try {
                val result =
                    SingletonImageLoader.get(context).execute(
                        ImageRequest
                            .Builder(context)
                            .data(song)
                            .size(MAX_ARTWORK_SIZE)
                            .precision(Precision.EXACT)
                            .build(),
                    )
                result.image?.toBitmap()
                    ?: CoverProvider.getImageUri(CoverProvider.SONG_COVER_PATH, song.id)?.let { fallback ->
                        SingletonImageLoader
                            .get(context)
                            .execute(
                                ImageRequest
                                    .Builder(context)
                                    .data(fallback)
                                    .size(MAX_ARTWORK_SIZE)
                                    .precision(Precision.EXACT)
                                    .build(),
                            ).image
                            ?.toBitmap()
                    }
            } catch (error: Exception) {
                Log.e(TAG, "Could not load artwork for ${song.id}", error)
                null
            }
        }

    @WorkerThread
    suspend fun saveToGallery(
        context: Context,
        bitmap: Bitmap,
        song: Song,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val displayName = artworkFileName(song)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveWithMediaStore(context, bitmap, displayName)
                } else {
                    saveLegacy(context, bitmap, displayName)
                }
            }.onFailure { error ->
                Log.e(TAG, "Could not save artwork to the gallery", error)
            }.isSuccess
        }

    @WorkerThread
    suspend fun setAsWallpaper(
        context: Context,
        bitmap: Bitmap,
        target: WallpaperTarget,
    ): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val manager = android.app.WallpaperManager.getInstance(context)
                val flags =
                    when (target) {
                        WallpaperTarget.Home -> android.app.WallpaperManager.FLAG_SYSTEM
                        WallpaperTarget.Lock -> android.app.WallpaperManager.FLAG_LOCK
                        WallpaperTarget.Both -> android.app.WallpaperManager.FLAG_SYSTEM or android.app.WallpaperManager.FLAG_LOCK
                    }
                manager.setBitmap(bitmap, null, true, flags)
            }.onFailure { error ->
                Log.e(TAG, "Could not set artwork as wallpaper", error)
            }.isSuccess
        }

    private fun artworkFileName(song: Song): String {
        val base = "${song.title} - ${song.artistName}".ifBlank { "artwork" }
        val safe = base.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_").take(120).trim()
        return "ssilencce_${safe.ifEmpty { "artwork" }}.jpg"
    }

    private fun saveWithMediaStore(
        context: Context,
        bitmap: Bitmap,
        displayName: String,
    ) {
        val resolver = context.contentResolver
        val values =
            ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$GALLERY_FOLDER")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        val uri =
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("MediaStore refused the image insert")
        try {
            resolver.openOutputStream(uri)?.use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                    throw IOException("Could not compress artwork")
                }
            } ?: throw IOException("Could not open the image output stream")
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(
        context: Context,
        bitmap: Bitmap,
        displayName: String,
    ) {
        val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), GALLERY_FOLDER)
        if (!folder.isDirectory && !folder.mkdirs()) {
            throw IOException("Could not create the gallery folder")
        }
        val file = File(folder, displayName)
        file.outputStream().use { stream ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                throw IOException("Could not compress artwork")
            }
        }
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/jpeg"), null)
    }
}
