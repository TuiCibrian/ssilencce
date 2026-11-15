package com.ssilencce.coil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.content.ContextCompat
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.toBitmap
import com.google.common.util.concurrent.ListenableFuture
import com.ssilencce.ui.component.views.getPlaceholderDrawable
import com.ssilencce.util.Preferences

@UnstableApi
class CoilBitmapLoader(
    private val context: Context,
) : BitmapLoader {
    override fun supportsMimeType(mimeType: String): Boolean = Util.isBitmapFactorySupportedMimeType(mimeType)

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> = if (Preferences.disableAlbumArt) loadPlaceholder() else loadImageUsingData(data)

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> = if (Preferences.disableAlbumArt) loadPlaceholder() else loadImageUsingData(uri)

    private fun loadPlaceholder(): ListenableFuture<Bitmap> =
        CallbackToFutureAdapter.getFuture { completer ->
            val drawable = context.getPlaceholderDrawable(DEFAULT_SONG_IMAGE)
            val bitmap = Bitmap.createBitmap(PLACEHOLDER_SIZE, PLACEHOLDER_SIZE, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, PLACEHOLDER_SIZE, PLACEHOLDER_SIZE)
            drawable.draw(Canvas(bitmap))
            completer.set(bitmap)
        }

    private fun loadImageUsingData(data: Any): ListenableFuture<Bitmap> =
        CallbackToFutureAdapter.getFuture { completer ->
            SingletonImageLoader
                .get(context)
                .enqueue(
                    ImageRequest
                        .Builder(context)
                        .data(data)
                        .target(
                            onError = {
                                completer.setException(Exception("Coil failed to load the image"))
                            },
                            onSuccess = {
                                completer.set(it.toBitmap())
                            },
                        ).memoryCachePolicy(CachePolicy.DISABLED)
                        .size(MAX_BITMAP_SIZE)
                        .build(),
                ).also {
                    completer.addCancellationListener(
                        { it.dispose() },
                        ContextCompat.getMainExecutor(context),
                    )
                }
        }

    companion object {
        private const val MAX_BITMAP_SIZE = 1024
        private const val PLACEHOLDER_SIZE = 512
    }
}
