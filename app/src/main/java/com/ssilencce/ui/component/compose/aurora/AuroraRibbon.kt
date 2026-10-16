package com.ssilencce.ui.component.compose.aurora

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
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
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

// The reference (ssilencce-auroras.html, .aurora) keeps an opaque near-black
// base, a static white veil, two S layers with their own geometry, a horizon
// and top glow, and an eight-stop dark overlay on top. All percentages below
// are taken from that file's CSS; the path uses its 400x900 user space.
private const val USER_WIDTH = 400f
private const val USER_HEIGHT = 900f
private const val RASTER_SCALE = .64f
private const val USER_MIN_X = -24f
private const val USER_MAX_X = 424f
private const val USER_MIN_Y = -72f
private const val USER_MAX_Y = 972f
private const val MESH_ROWS = 48

// x' = x + A*sin(K*y + phase) from the reference script (serpiente-2d).
private const val WAVE_AMPLITUDE = 40f
private const val WAVE_FREQUENCY = .0105f
private const val WAVE_SPEED = .55f

// CSS aurora-undulate / aurora-undulate-slow keyframes, approximated by a
// sine with the same periods (the keyframe leg is 4.2s / 6.2s, alternating)
// and peak amplitudes.
private const val WIGGLE_PERIOD_MAIN = 8.4f
private const val WIGGLE_PERIOD_BACK = 12.4f
private const val WIGGLE_COMMON_PERIOD = 260.4f // 31 x 8.4s and 21 x 12.4s
private const val WIGGLE_SHIFT_MAIN = .0085f
private const val WIGGLE_SHIFT_BACK = .0065f
private const val WIGGLE_SKEW_MAIN = .0157f
private const val WIGGLE_SKEW_BACK = .0130f

private const val MAIN_OVERSCAN_X = .16f
private const val MAIN_OVERSCAN_Y = .08f
private const val BACK_OVERSCAN_X = .20f
private const val BACK_OVERSCAN_Y = .10f
private const val BACK_LAYER_ALPHA = .32f
private const val BACK_WAVE_PHASE = PI.toFloat()

private class RibbonStroke(
    val width: Float,
    val stdDeviation: Float,
    val alpha: Float,
)

// Main layer: ambient veil, outer halo, mid glow, luminous core, filament.
private val MAIN_STROKES =
    arrayOf(
        RibbonStroke(240f, 118f, .18f * .34f),
        RibbonStroke(180f, 88f, .26f * .48f),
        RibbonStroke(96f, 64f, .48f * .72f),
        RibbonStroke(32f, 36f, .88f * .88f),
        RibbonStroke(3.2f, 18f, 1f * .42f),
    )

// Back layer: ambient veil, strong glow, soft core. It is drawn with a
// further .32 opacity, matching .aurora-snake-wrap.back.
private val BACK_STROKES =
    arrayOf(
        RibbonStroke(190f, 110f, .22f * .30f),
        RibbonStroke(110f, 76f, .38f * .52f),
        RibbonStroke(28f, 40f, .72f * .82f * .95f),
    )

private class RibbonLayers(
    val back: Bitmap,
    val main: Bitmap,
)

@Composable
fun AuroraRibbon(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
) {
    val motionScale = rememberAuroraMotionScale(visible)
    val animated = motionScale > 0f
    val phase = remember { mutableFloatStateOf(0f) }
    val wiggleTime = remember { mutableFloatStateOf(0f) }
    val layers by produceState<RibbonLayers?>(null) {
        value = withContext(Dispatchers.Default) { RibbonLayers(renderRibbon(BACK_STROKES), renderRibbon(MAIN_STROKES)) }
    }
    LaunchedEffect(motionScale) {
        if (motionScale > 0f) {
            var previousFrame = 0L
            while (isActive) {
                val frame = withFrameNanos { it }
                if (previousFrame != 0L) {
                    val elapsedSeconds = (frame - previousFrame) / 1_000_000_000f / motionScale.coerceAtLeast(.1f)
                    phase.floatValue = (phase.floatValue + elapsedSeconds * WAVE_SPEED) % (2f * PI.toFloat())
                    wiggleTime.floatValue = (wiggleTime.floatValue + elapsedSeconds) % WIGGLE_COMMON_PERIOD
                }
                previousFrame = frame
                delay(30)
            }
        }
    }
    RibbonCanvas(modifier, layers, animated, { phase.floatValue }, { wiggleTime.floatValue })
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
    layers: RibbonLayers?,
    animated: Boolean,
    phase: () -> Float,
    wiggleTime: () -> Float,
) {
    Spacer(
        modifier.drawWithCache {
            val veil =
                radialGlowPaint(
                    centerX = .5f * size.width,
                    centerY = .4536f * size.height,
                    radiusX = .8352f * size.width,
                    radiusY = .7192f * size.height,
                    stops = floatArrayOf(0f, .46f, .78f, 1f),
                    alphas = floatArrayOf(.034f * .32f, .011f * .32f, 0f, 0f),
                )
            val horizon =
                radialGlowPaint(
                    centerX = .5f * size.width,
                    centerY = .8088f * size.height,
                    radiusX = .9472f * size.width,
                    radiusY = .2016f * size.height,
                    stops = floatArrayOf(0f, .32f, .68f, 1f),
                    alphas = floatArrayOf(.095f * .44f, .034f * .44f, 0f, 0f),
                )
            val top =
                radialGlowPaint(
                    centerX = .4728f * size.width,
                    centerY = .0596f * size.height,
                    radiusX = .9248f * size.width,
                    radiusY = .1976f * size.height,
                    stops = floatArrayOf(0f, .62f, 1f),
                    alphas = floatArrayOf(.062f * .34f, 0f, 0f),
                )
            // .aurora-dark-overlay, all eight stops with their exact colors.
            val scrim =
                Brush.verticalGradient(
                    0f to Color(0f, 0f, 0f, .36f),
                    .14f to Color(2f / 255f, 2f / 255f, 4f / 255f, .42f),
                    .28f to Color(4f / 255f, 4f / 255f, 7f / 255f, .51f),
                    .44f to Color(6f / 255f, 6f / 255f, 10f / 255f, .60f),
                    .60f to Color(8f / 255f, 8f / 255f, 12f / 255f, .69f),
                    .74f to Color(9f / 255f, 9f / 255f, 13f / 255f, .78f),
                    .86f to Color(10f / 255f, 10f / 255f, 14f / 255f, .86f),
                    1f to Color(5f / 255f, 5f / 255f, 7f / 255f, .92f),
                )
            val backPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply { alpha = (BACK_LAYER_ALPHA * 255f).roundToInt() }
            val mainPaint = Paint(Paint.FILTER_BITMAP_FLAG)
            val vertices = FloatArray((MESH_ROWS + 1) * 4)
            onDrawBehind {
                drawRect(AuroraDesign.background)
                drawCanvasRect(veil)
                val wavePhase = phase()
                val wiggle = wiggleTime()
                layers?.let {
                    drawRibbonLayer(
                        bitmap = it.back,
                        paint = backPaint,
                        overscanX = BACK_OVERSCAN_X,
                        overscanY = BACK_OVERSCAN_Y,
                        waveformPhase = (wavePhase + BACK_WAVE_PHASE) % (2f * PI.toFloat()),
                        wigglePeriod = WIGGLE_PERIOD_BACK,
                        wiggleShift = WIGGLE_SHIFT_BACK,
                        wiggleSkew = WIGGLE_SKEW_BACK,
                        wiggleTime = wiggle,
                        animated = animated,
                        vertices = vertices,
                    )
                    drawRibbonLayer(
                        bitmap = it.main,
                        paint = mainPaint,
                        overscanX = MAIN_OVERSCAN_X,
                        overscanY = MAIN_OVERSCAN_Y,
                        waveformPhase = wavePhase,
                        wigglePeriod = WIGGLE_PERIOD_MAIN,
                        wiggleShift = WIGGLE_SHIFT_MAIN,
                        wiggleSkew = WIGGLE_SKEW_MAIN,
                        wiggleTime = wiggle,
                        animated = animated,
                        vertices = vertices,
                    )
                }
                drawCanvasRect(horizon)
                drawCanvasRect(top)
                drawRect(scrim)
            }
        },
    )
}

private fun DrawScope.drawRibbonLayer(
    bitmap: Bitmap,
    paint: Paint,
    overscanX: Float,
    overscanY: Float,
    waveformPhase: Float,
    wigglePeriod: Float,
    wiggleShift: Float,
    wiggleSkew: Float,
    wiggleTime: Float,
    animated: Boolean,
    vertices: FloatArray,
) {
    val layerLeft = -overscanX * size.width
    val layerTop = -overscanY * size.height
    val layerWidth = (1f + 2f * overscanX) * size.width
    val layerHeight = (1f + 2f * overscanY) * size.height
    val scaleX = layerWidth / USER_WIDTH
    val scaleY = layerHeight / USER_HEIGHT
    val centerY = layerTop + layerHeight / 2f
    val amplitude = if (animated) WAVE_AMPLITUDE else 0f
    val wiggleWave = if (animated) sin(wiggleTime * 2f * PI.toFloat() / wigglePeriod) else 0f
    val shift = wiggleWave * wiggleShift * layerWidth
    val tanSkew = tan(wiggleWave * wiggleSkew)
    for (row in 0..MESH_ROWS) {
        val userY = USER_MIN_Y + (USER_MAX_Y - USER_MIN_Y) * row / MESH_ROWS
        val displacement = amplitude * sin(WAVE_FREQUENCY * userY + waveformPhase)
        val layerY = layerTop + userY * scaleY
        val xOffset = layerLeft + (layerY - centerY) * tanSkew + shift
        val index = row * 4
        vertices[index] = xOffset + (USER_MIN_X + displacement) * scaleX
        vertices[index + 1] = layerY
        vertices[index + 2] = xOffset + (USER_MAX_X + displacement) * scaleX
        vertices[index + 3] = layerY
    }
    drawIntoCanvas { it.nativeCanvas.drawBitmapMesh(bitmap, 1, MESH_ROWS, vertices, 0, null, 0, paint) }
}

private fun DrawScope.drawCanvasRect(paint: Paint) {
    drawIntoCanvas { it.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint) }
}

// White radial glow with elliptical radii; the reference blurs the gradient
// box, which this keeps as the already-soft ramp.
private fun radialGlowPaint(
    centerX: Float,
    centerY: Float,
    radiusX: Float,
    radiusY: Float,
    stops: FloatArray,
    alphas: FloatArray,
): Paint {
    val shader =
        RadialGradient(
            centerX,
            centerY,
            radiusX.coerceAtLeast(1f),
            IntArray(alphas.size) { glowColor(alphas[it]) },
            stops,
            Shader.TileMode.CLAMP,
        )
    shader.setLocalMatrix(Matrix().apply { setScale(1f, (radiusY / radiusX).coerceAtLeast(.01f), centerX, centerY) })
    return Paint(Paint.ANTI_ALIAS_FLAG).apply { setShader(shader) }
}

private fun glowColor(alpha: Float) = android.graphics.Color.argb((alpha * 255f).roundToInt().coerceIn(0, 255), 255, 255, 255)

private fun readAnimationScale(context: Context) = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)

private fun renderRibbon(strokes: Array<RibbonStroke>): Bitmap {
    val width = ((USER_MAX_X - USER_MIN_X) * RASTER_SCALE).roundToInt()
    val height = ((USER_MAX_Y - USER_MIN_Y) * RASTER_SCALE).roundToInt()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.translate(-USER_MIN_X * RASTER_SCALE, -USER_MIN_Y * RASTER_SCALE)
    canvas.scale(RASTER_SCALE, RASTER_SCALE)
    val path = referencePath()
    val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = android.graphics.Color.WHITE
        }
    for (stroke in strokes) {
        paint.strokeWidth = stroke.width
        paint.alpha = (stroke.alpha * 255f).roundToInt().coerceIn(0, 255)
        // Skia turns a BlurMaskFilter radius r into a sigma of r * .57735 + .5
        // and then scales that sigma by the drawing CTM (SkMaskFilter respects
        // the CTM by default). The reference blur is an SVG stdDeviation in
        // user units, so aim the sigma at user units: the canvas scale(.64)
        // and the mesh stretch then reproduce the SVG viewport's anisotropic
        // raster sigma instead of double-applying the raster scale.
        val radius = (stroke.stdDeviation - .5f) / .57735f
        paint.maskFilter = BlurMaskFilter(radius.coerceAtLeast(.1f), BlurMaskFilter.Blur.NORMAL)
        canvas.drawPath(path, paint)
    }
    return bitmap
}

private fun referencePath(): Path =
    Path().apply {
        moveTo(200f, -40f)
        cubicTo(360f, 70f, -10f, 170f, 200f, 300f)
        cubicTo(410f, 430f, -10f, 570f, 200f, 700f)
        cubicTo(410f, 830f, 40f, 880f, 200f, 940f)
    }

@Preview(widthDp = 392, heightDp = 842)
@Composable
private fun AuroraRibbonPreview() {
    AuroraRibbon(Modifier, visible = false)
}
