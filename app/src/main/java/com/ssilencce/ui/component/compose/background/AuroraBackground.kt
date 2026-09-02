package com.ssilencce.ui.component.compose.background

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ssilencce.util.BACKGROUND_ANIMATION
import com.ssilencce.util.Preferences
import kotlin.math.PI
import kotlin.math.sin

private const val VB_WIDTH = 400f
private const val VB_HEIGHT = 900f
private const val POSITION_X = 200f
private const val AMPLITUDE = 40f
private const val WAVE_NUMBER = 0.0105f
private const val SPEED = 0.55f
private const val SAMPLE_POINTS = 60

private data class CubicSegment(
    val start: Offset,
    val control1: Offset,
    val control2: Offset,
    val end: Offset,
)

private val baseSegments =
    listOf(
        CubicSegment(
            Offset(POSITION_X, -40f),
            Offset(360f, 70f),
            Offset(-10f, 170f),
            Offset(POSITION_X, 300f),
        ),
        CubicSegment(
            Offset(POSITION_X, 300f),
            Offset(410f, 430f),
            Offset(-10f, 570f),
            Offset(POSITION_X, 700f),
        ),
        CubicSegment(
            Offset(POSITION_X, 700f),
            Offset(410f, 830f),
            Offset(40f, 880f),
            Offset(POSITION_X, 940f),
        ),
    )

private val basePoints: List<Offset> =
    baseSegments.flatMap { segment ->
        (0 until SAMPLE_POINTS).map { i ->
            val t = i / SAMPLE_POINTS.toFloat()
            val u = 1f - t
            val x =
                u * u * u * segment.start.x +
                    3f * u * u * t * segment.control1.x +
                    3f * u * t * t * segment.control2.x +
                    t * t * t * segment.end.x
            val y =
                u * u * u * segment.start.y +
                    3f * u * u * t * segment.control1.y +
                    3f * u * t * t * segment.control2.y +
                    t * t * t * segment.end.y
            Offset(x, y)
        }
    } + listOf(Offset(baseSegments.last().end.x, baseSegments.last().end.y))

private fun isAnimatorScaleZero(context: Context): Boolean =
    runCatching {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }.getOrDefault(false)

/**
 * Aurora Glass background: dark base gradient, soft white radial veil, vertical scrim and an
 * optional animated white serpentine (two layers phase-offset by PI). Renders the serpentine
 * with layered strokes to fake the reference's Gaussian blur without blur filters (battery-safe).
 */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    var animationEnabled by remember { mutableStateOf(Preferences.backgroundAnimation) }
    var lifecycleStarted by remember { mutableStateOf(false) }
    var phase by remember { mutableFloatStateOf(0f) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> lifecycleStarted = true
                    Lifecycle.Event.ON_STOP -> lifecycleStarted = false
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(Unit) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == BACKGROUND_ANIMATION) {
                animationEnabled = Preferences.backgroundAnimation
            }
        }
        Preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { Preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val animate = animationEnabled && lifecycleStarted && !isAnimatorScaleZero(context)

    LaunchedEffect(animate) {
        if (!animate) {
            phase = 0f
            return@LaunchedEffect
        }
        var lastFrame = 0L
        while (true) {
            withFrameNanos { frameTime ->
                if (lastFrame != 0L) {
                    phase += (frameTime - lastFrame) / 1_000_000_000f * SPEED
                    if (phase > 2f * PI.toFloat()) {
                        phase -= (2f * PI).toFloat()
                    }
                }
                lastFrame = frameTime
            }
        }
    }

    Canvas(modifier) {
        drawBase(isDark)
        drawVeil(isDark)
        if (animate) {
            drawSerpentine(isDark, phase)
        }
        drawScrim(isDark)
    }
}

private fun DrawScope.drawBase(isDark: Boolean) {
    val colors =
        if (isDark) listOf(Color(0xFF0D0D0F), Color(0xFF0A0A0C), Color(0xFF070708)) else
            listOf(Color(0xFFFAFAFB), Color(0xFFF7F7F8), Color(0xFFF1F1F3))
    drawRect(
        brush =
            Brush.verticalGradient(
                colors = colors,
                startY = 0f,
                endY = size.height,
            ),
    )
}

private fun DrawScope.drawVeil(isDark: Boolean) {
    val center = Offset(size.width * 0.5f, size.height * 0.46f)
    val radius = size.maxDimension * 0.78f
    val topAlpha = if (isDark) 0.034f else 0.030f
    drawCircle(
        brush =
            Brush.radialGradient(
                colors =
                    listOf(
                        Color.White.copy(alpha = topAlpha),
                        Color.White.copy(alpha = topAlpha * 0.32f),
                        Color.Transparent,
                    ),
                center = center,
                radius = radius,
            ),
        radius = radius,
        center = center,
    )
}

private fun DrawScope.drawScrim(isDark: Boolean) {
    val colors =
        if (isDark) {
            listOf(
                Color.Black.copy(alpha = 0.26f),
                Color.Black.copy(alpha = 0.40f),
                Color.Black.copy(alpha = 0.58f),
                Color.Black.copy(alpha = 0.76f),
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.10f),
                Color.White.copy(alpha = 0.20f),
                Color.White.copy(alpha = 0.34f),
                Color.White.copy(alpha = 0.50f),
            )
        }
    drawRect(
        brush =
            Brush.verticalGradient(
                colors = colors,
                startY = 0f,
                endY = size.height,
            ),
    )
}

private fun DrawScope.drawSerpentine(isDark: Boolean, phase: Float) {
    val scaleX = size.width / VB_WIDTH
    val scaleY = size.height / VB_HEIGHT
    val strokeScale = scaleX
    val accent = if (isDark) Color.White else Color(0xFF141416)

    fun serpentinePath(offsetPhase: Float): Path {
        val path = Path()
        basePoints.forEachIndexed { index, point ->
            val displacedX = point.x + AMPLITUDE * sin(WAVE_NUMBER * point.y + offsetPhase)
            val x = displacedX * scaleX
            val y = point.y * scaleY
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        return path
    }

    val backPhase = phase + PI.toFloat()
    val backTranslucent = accent.copy(alpha = if (isDark) 0.30f else 0.16f)
    val backCore = accent.copy(alpha = if (isDark) 0.62f else 0.34f)
    val mainAmbient = accent.copy(alpha = if (isDark) 0.05f else 0.028f)
    val mainOuter = accent.copy(alpha = if (isDark) 0.09f else 0.05f)
    val mainStrong = accent.copy(alpha = if (isDark) 0.16f else 0.10f)
    val mainCore = accent.copy(alpha = if (isDark) 0.42f else 0.26f)

    fun stroke(path: Path, widthUnits: Float, color: Color) {
        drawPath(path, color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(widthUnits * strokeScale, cap = StrokeCap.Round))
    }

    val back = serpentinePath(backPhase)
    stroke(back, 190f, backTranslucent.copy(alpha = backTranslucent.alpha * 0.5f))
    stroke(back, 110f, backTranslucent.copy(alpha = backTranslucent.alpha * 0.75f))
    stroke(back, 26f, backCore.copy(alpha = backCore.alpha * 0.9f))

    val main = serpentinePath(phase)
    stroke(main, 190f, mainAmbient)
    stroke(main, 110f, mainOuter)
    stroke(main, 56f, mainStrong)
    stroke(main, 22f, mainCore)
}
