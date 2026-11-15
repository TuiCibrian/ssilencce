package com.ssilencce.ui.screen.player

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.ssilencce.extensions.files.getContentUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Creates a temporary, FileProvider-backed JPEG of the player view for sharing.
 */
object PlayerScreenshot {
    private const val TAG = "PlayerScreenshot"
    private const val CACHE_FOLDER = "now_playing_share"
    private const val JPEG_QUALITY = 95

    suspend fun createShareUri(
        context: Context,
        source: Bitmap,
        square: Boolean,
    ): Uri? =
        withContext(Dispatchers.IO) {
            runCatching {
                val output = if (square) cropSquare(source) else source
                try {
                    val folder = File(context.cacheDir, CACHE_FOLDER)
                    if (!folder.isDirectory && !folder.mkdirs()) {
                        throw IOException("Could not create the screenshot folder")
                    }
                    folder.listFiles()?.forEach { it.delete() }
                    val file = File(folder, "now_playing_${System.currentTimeMillis()}.jpg")
                    file.outputStream().use { stream ->
                        if (!output.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)) {
                            throw IOException("Could not compress the screenshot")
                        }
                    }
                    file.getContentUri(context)
                } finally {
                    if (output !== source) output.recycle()
                }
            }.onFailure { error ->
                Log.e(TAG, "Could not create the Now Playing screenshot", error)
            }.getOrNull()
        }

    fun shareIntent(
        context: Context,
        uri: Uri,
        title: String,
    ): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, title, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    private fun cropSquare(source: Bitmap): Bitmap {
        val size = minOf(source.width, source.height)
        val x = (source.width - size) / 2
        val y = (source.height - size) / 2
        return Bitmap.createBitmap(source, x, y, size, size)
    }
}
