/*
 * Copyright (c) 2024 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ssilencce.ui.component.base

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewConfiguration
import java.lang.ref.WeakReference

class SkipButtonTouchHandler(
    private val direction: Int,
    private val callback: Callback,
) : OnTouchListener {
    interface Callback {
        fun onSkipButtonHold(direction: Int)

        fun onSkipButtonTap(direction: Int)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var isHolding = false
    private var touchedViewRef: WeakReference<View>? = null
    private var touchSlop: Int = -1

    private val repeatRunnable =
        object : Runnable {
            override fun run() {
                val view = touchedViewRef?.get()
                if (view != null && view.isEnabled) {
                    isHolding = true
                    callback.onSkipButtonHold(direction)
                    handler.postDelayed(this, SKIP_TRIGGER_NORMAL_INTERVAL_MILLIS)
                } else {
                    cancel()
                }
            }
        }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(
        view: View,
        event: MotionEvent,
    ): Boolean {
        if (touchSlop < 0) {
            touchSlop = ViewConfiguration.get(view.context).scaledTouchSlop
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchedViewRef = WeakReference(view)
                isHolding = false
                view.isPressed = true
                handler.postDelayed(repeatRunnable, SKIP_TRIGGER_INITIAL_INTERVAL_MILLIS)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isPointInsideView(event.x, event.y, view)) {
                    cancel()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(repeatRunnable)
                val wasPressed = view.isPressed
                view.isPressed = false

                if (event.action == MotionEvent.ACTION_UP && !isHolding && wasPressed) {
                    if (isPointInsideView(event.x, event.y, view)) {
                        callback.onSkipButtonTap(direction)
                    }
                }

                touchedViewRef?.clear()
                touchedViewRef = null
                isHolding = false
                return true
            }
        }
        return false
    }

    private fun isPointInsideView(
        x: Float,
        y: Float,
        view: View,
    ): Boolean =
        x >= -touchSlop && x <= view.width + touchSlop &&
            y >= -touchSlop && y <= view.height + touchSlop

    private fun cancel() {
        handler.removeCallbacks(repeatRunnable)
        touchedViewRef?.get()?.isPressed = false
        touchedViewRef = null
        isHolding = false
    }

    companion object {
        private const val SKIP_TRIGGER_INITIAL_INTERVAL_MILLIS = 1000L
        private const val SKIP_TRIGGER_NORMAL_INTERVAL_MILLIS = 250L

        const val DIRECTION_NEXT = 1
        const val DIRECTION_PREVIOUS = 2
    }
}
