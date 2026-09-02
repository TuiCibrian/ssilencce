package com.ssilencce.ui.component.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.ssilencce.R
import kotlin.math.min

/**
 * Small white caret drawn over the active item of the bottom navigation bar (Aurora Glass).
 */
class AuroraNavigationCaretView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : View(context, attrs, defStyleAttr) {
        private val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ContextCompat.getColor(context, R.color.aurora_nav_item_checked)
                setShadowLayer(
                    context.resources.displayMetrics.density * 5f,
                    0f,
                    0f,
                    0x8CFFFFFF.toInt(),
                )
            }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            if (w == 0 || h == 0) {
                return
            }
            paint.setShadowLayer(
                min(h * 1f, context.resources.displayMetrics.density * 6f) * 0.9f,
                0f,
                0f,
                0x8CFFFFFF.toInt(),
            )
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val radius = height.coerceAtMost(width / 2).toFloat()
            canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), radius, radius, paint)
        }
    }
