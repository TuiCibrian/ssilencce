package com.ssilencce.ui.component.compose.aurora

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.preference.PreferenceManager
import com.ssilencce.extensions.observeKeyAsState
import com.ssilencce.ui.theme.AuroraDesign
import com.ssilencce.util.AURORA_MOTION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun AuroraRibbon(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
) {
    val motionScale = rememberAuroraMotionScale(visible)
    val phase = remember { mutableFloatStateOf(0f) }
    val ribbon by produceState<Bitmap?>(null) {
        value = withContext(Dispatchers.Default) { renderRibbon() }
    }
    LaunchedEffect(motionScale) {
        if (motionScale > 0f) {
            var previousFrame = 0L
            while (isActive) {
                val frame = withFrameNanos { it }
                if (previousFrame != 0L) {
                    val elapsedSeconds = (frame - previousFrame) / 1_000_000_000f
                    phase.floatValue = (phase.floatValue + elapsedSeconds * (2f / 3f) / motionScale.coerceAtLeast(.1f)) % (2f * PI.toFloat())
                }
                previousFrame = frame
                delay(30)
            }
        }
    }
    RibbonCanvas(modifier, ribbon) { phase.floatValue }
}

@Composable
internal fun rememberAuroraMotionScale(
    visible: Boolean,
    preferenceKey: String = AURORA_MOTION,
    preferenceDefault: Boolean = true,
): Float {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val preferences = remember(context) { PreferenceManager.getDefaultSharedPreferences(context) }
    val motion by preferences.observeKeyAsState(preferenceKey, preferenceDefault)
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    var animationScale by remember { mutableFloatStateOf(readAnimationScale(context)) }
    var batterySaver by remember { mutableStateOf(context.getSystemService<PowerManager>()?.isPowerSaveMode == true) }

    DisposableEffect(context, lifecycle) {
        val lifecycleObserver =
            LifecycleEventObserver { _, _ ->
                resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            }
        val settingsObserver =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    animationScale = readAnimationScale(context)
                }
            }
        val powerReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent,
                ) {
                    batterySaver = context.getSystemService<PowerManager>()?.isPowerSaveMode == true
                }
            }
        lifecycle.addObserver(lifecycleObserver)
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            settingsObserver,
        )
        ContextCompat.registerReceiver(
            context,
            powerReceiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
            context.contentResolver.unregisterContentObserver(settingsObserver)
            context.unregisterReceiver(powerReceiver)
        }
    }

    return if (visible && resumed && motion && !batterySaver) animationScale.coerceAtLeast(0f) else 0f
}

@Composable
private fun RibbonCanvas(
    modifier: Modifier,
    ribbon: Bitmap?,
    phase: () -> Float,
) {
    Spacer(
        modifier.drawWithCache {
            val overscan = size.width * .16f
            val vertices = FloatArray(49 * 4)
            val paints =
                arrayOf(.22f, .65f).map { alpha ->
                    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                        this.alpha = (alpha * 255).toInt()
                    }
                }
            val scrim =
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = .36f),
                    .28f to Color(0xFF040407).copy(alpha = .51f),
                    .60f to Color(0xFF08080C).copy(alpha = .69f),
                    1f to Color(0xFF050507).copy(alpha = .92f),
                )
            onDrawBehind {
                drawRect(AuroraDesign.background)
                ribbon?.let { image ->
                    val currentPhase = phase()
                    drawIntoCanvas { canvas ->
                        // Two cached meshes replace 96 strip draws without blurring foreground content.
                        for (layer in paints.indices) {
                            for (row in 0..48) {
                                val offset = sin(row * .13f + currentPhase + layer * PI.toFloat()) * size.width * .035f
                                val index = row * 4
                                val y = size.height * row / 48
                                vertices[index] = -overscan + offset
                                vertices[index + 1] = y
                                vertices[index + 2] = size.width + overscan + offset
                                vertices[index + 3] = y
                            }
                            canvas.nativeCanvas.drawBitmapMesh(image, 1, 48, vertices, 0, null, 0, paints[layer])
                        }
                    }
                }
                drawRect(scrim)
            }
        },
    )
}

private fun readAnimationScale(context: Context) = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)

private fun renderRibbon(): Bitmap {
    val bitmap = Bitmap.createBitmap(256, 576, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.scale(.64f, .64f)
    val path =
        Path().apply {
            moveTo(200f, -40f)
            cubicTo(360f, 70f, -10f, 170f, 200f, 300f)
            cubicTo(410f, 430f, -10f, 570f, 200f, 700f)
            cubicTo(410f, 830f, 40f, 880f, 200f, 940f)
        }
    val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = android.graphics.Color.WHITE
        }
    val widths = floatArrayOf(240f, 180f, 96f, 32f, 3.2f)
    val blurs = floatArrayOf(118f, 88f, 64f, 36f, 18f)
    val alphas = floatArrayOf(.18f * .34f, .26f * .48f, .48f * .72f, .88f * .88f, .42f)
    for (index in widths.indices) {
        paint.strokeWidth = widths[index]
        paint.alpha = (255 * alphas[index]).toInt()
        paint.maskFilter = BlurMaskFilter(blurs[index], BlurMaskFilter.Blur.NORMAL)
        canvas.drawPath(path, paint)
    }
    return bitmap
}

@Preview(widthDp = 392, heightDp = 842)
@Composable
private fun AuroraRibbonPreview() {
    AuroraRibbon(Modifier, visible = false)
}
