package com.ssilencce.ui.screen.player.art

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.BundleCompat
import androidx.fragment.app.DialogFragment
import com.ssilencce.R
import com.ssilencce.data.model.Song
import com.ssilencce.extensions.EXTRA_SONG
import com.ssilencce.extensions.showToast
import com.ssilencce.extensions.withArgs
import com.ssilencce.ui.screen.player.art.ArtworkStore.WallpaperTarget
import com.ssilencce.ui.theme.SsilencceTheme
import kotlinx.coroutines.launch

/**
 * Full-screen album-art viewer with pinch-to-zoom and artwork export options.
 */
class FullScreenArtDialogFragment : DialogFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val song =
            BundleCompat.getParcelable(requireArguments(), EXTRA_SONG, Song::class.java)
                ?: Song.emptySong
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SsilencceTheme {
                    FullScreenArtScreen(
                        song = song,
                        onClose = { dismissAllowingStateLoss() },
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK))
            setDimAmount(0f)
        }
    }

    companion object {
        const val TAG = "FullScreenArtDialogFragment"

        fun newInstance(song: Song) =
            FullScreenArtDialogFragment().withArgs {
                putParcelable(EXTRA_SONG, song)
            }
    }
}

@Composable
private fun FullScreenArtScreen(
    song: Song,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var wallpaperMenuExpanded by remember { mutableStateOf(false) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(song.id) {
        loading = true
        bitmap = ArtworkStore.loadArtwork(context, song)
        loading = false
    }

    val transformableState =
        rememberTransformableState { centroid, zoomChange, panChange, _ ->
            val newScale = (scale * zoomChange).coerceIn(1f, 6f)
            // Keep the gesture centroid stable while zooming.
            val focus = if (centroid == Offset.Unspecified) Offset.Zero else centroid
            offset =
                if (newScale <= 1f) {
                    Offset.Zero
                } else {
                    (offset - focus) * zoomChange + focus + panChange
                }
            scale = newScale
        }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val artwork = bitmap
        if (artwork != null) {
            Image(
                bitmap = artwork.asImageBitmap(),
                contentDescription = stringResource(R.string.album_art_preview_content_description),
                contentScale = ContentScale.Fit,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }.transformable(transformableState),
            )
        } else if (loading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = .42f))
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(R.drawable.ic_close_24dp),
                    contentDescription = stringResource(R.string.close_action),
                    tint = Color.White,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (song.artistName.isNotBlank()) {
                    Text(
                        text = song.artistName,
                        color = Color.White.copy(alpha = .72f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Row(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = .42f))
                    .navigationBarsPadding()
                    .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                enabled = artwork != null && !saving,
                onClick = {
                    val art = bitmap ?: return@IconButton
                    saving = true
                    scope.launch {
                        val saved = ArtworkStore.saveToGallery(context, art, song)
                        saving = false
                        context.showToast(if (saved) R.string.artwork_saved_to_gallery else R.string.artwork_save_failed)
                    }
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_save_24dp),
                    contentDescription = stringResource(R.string.save_to_gallery),
                    tint = Color.White,
                )
            }
            Box {
                IconButton(
                    enabled = artwork != null,
                    onClick = { wallpaperMenuExpanded = true },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_image_24dp),
                        contentDescription = stringResource(R.string.set_as_wallpaper),
                        tint = Color.White,
                    )
                }
                DropdownMenu(
                    expanded = wallpaperMenuExpanded,
                    onDismissRequest = { wallpaperMenuExpanded = false },
                ) {
                    WallpaperTarget.entries.forEach { target ->
                        DropdownMenuItem(
                            text = { Text(stringResource(target.titleRes)) },
                            onClick = {
                                wallpaperMenuExpanded = false
                                val art = bitmap ?: return@DropdownMenuItem
                                scope.launch {
                                    val applied = ArtworkStore.setAsWallpaper(context, art, target)
                                    context.showToast(if (applied) R.string.wallpaper_set_success else R.string.wallpaper_set_failed)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
