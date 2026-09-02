package com.ssilencce.ui.component.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import kotlin.math.max
import kotlin.math.sin

/**
 * Small three-bar equalizer indicator shown on the currently playing song row (Aurora Glass).
 * Animates only while its row is the playing row and the view is attached to the window.
 */
class EqMiniView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : View(context, attrs, defStyleAttr) {
        private val density = context.resources.displayMetrics.density
        private val barWidth = 2.5f * density
        private val barGap = 2.5f * density
        private val minBarHeight = 5f * density
        private val maxBarHeight = 15f * density

        private val scrimPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x9E0A0A0C.toInt()
            }

        private val barPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ContextCompat.getColor(context, com.ssilencce.R.color.aurora_nav_item_checked)
                setShadowLayer(3f * density, 0f, 0f, 0x8CFFFFFF.toInt())
            }

        private var phase = 0f
        private var animator: ValueAnimator? = null
        private var playing = false

        fun setPlaying(playing: Boolean) {
            if (this.playing == playing) {
                return
            }
            this.playing = playing
            updateAnimator()
        }

        private fun updateAnimator() {
            val shouldRun = playing && isAttachedToWindow && visibility == VISIBLE
            if (shouldRun && animator?.isRunning != true) {
                animator =
                    ValueAnimator.ofFloat(0f, 1f).apply {
                        duration = 1100
                        repeatCount = ValueAnimator.INFINITE
                        interpolator = LinearInterpolator()
                        addUpdateListener {
                            phase = it.animatedValue as Float
                            invalidate()
                        }
                        start()
                    }
            } else if (!shouldRun && animator?.isRunning == true) {
                animator?.cancel()
                animator = null
                invalidate()
            }
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            updateAnimator()
        }

        override fun onDetachedFromWindow() {
            animator?.cancel()
            animator = null
            super.onDetachedFromWindow()
        }

        override fun onVisibilityChanged(
            changedView: View,
            visibility: Int,
        ) {
            super.onVisibilityChanged(changedView, visibility)
            updateAnimator()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val barAreaWidth = barWidth * 3f + barGap * 2f
            val barAreaHeight = maxBarHeight + 2f * density
            val startX = (width - barAreaWidth) / 2f
            val startY = (height - barAreaHeight) / 2f
            canvas.drawRoundRect(
                startX - barGap,
                startY - 2f * density,
                startX + barAreaWidth + barGap,
                startY + barAreaHeight + 2f * density,
                4f * density,
                4f * density,
                scrimPaint,
            )
            for (i in 0 until 3) {
                val centerY = startY + barAreaHeight / 2f
                val sine = sin((phase * 2f * Math.PI).toFloat() - i * 2.1f - 0.9f)
                val height = max(minBarHeight, (barAreaHeight / 2f) * (0.42f + 0.52f * (sine + 1f) / 2f))
                val left = startX + i * (barWidth + barGap)
                canvas.drawRoundRect(
                    left,
                    centerY - height / 2f,
                    left + barWidth,
                    centerY + height / 2f,
                    barWidth / 2f,
                    barWidth / 2f,
                    barPaint,
                )
            }
        }
    }
