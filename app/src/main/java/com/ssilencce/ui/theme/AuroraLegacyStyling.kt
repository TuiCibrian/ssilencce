package com.ssilencce.ui.theme

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.children
import androidx.core.view.updatePadding
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.RelativeCornerSize
import com.ssilencce.R
import com.ssilencce.extensions.dp
import com.ssilencce.extensions.resources.surfaceColor
import com.ssilencce.util.Preferences

fun styleAuroraHierarchy(root: View) {
    if (!Preferences.isAuroraAppearance) return
    val surface = root.context.surfaceColor()

    fun visit(view: View) {
        if ((view.background as? ColorDrawable)?.color == surface) view.setBackgroundColor(Color.TRANSPARENT)
        when (view) {
            is AppBarLayout -> {
                view.setBackgroundColor(Color.TRANSPARENT)
                view.setStatusBarForegroundColor(Color.TRANSPARENT)
                view.elevation = 0f
            }

            is Toolbar -> {
                view.setBackgroundColor(Color.TRANSPARENT)
                view.elevation = 0f
                if (view is MaterialToolbar) {
                    view.isTitleCentered = false
                    view.isSubtitleCentered = false
                }
            }

            is MaterialCardView -> {
                view.setCardBackgroundColor(0x9E060608.toInt())
                view.strokeColor = 0x18FFFFFF
                view.strokeWidth = 1.dp(view.context)
                view.cardElevation = 0f
            }

            is EditText -> {
                if (view.id == R.id.search_view) {
                    view.background = auroraGlassDrawable(view, 10)
                    view.setTextColor(0xFFEBEBEB.toInt())
                    view.setHintTextColor(0xFFA5A5A5.toInt())
                    view.setPadding(12.dp(view.context), 0, 12.dp(view.context), 0)
                    view.textSize = 14f
                }
            }
        }
        if (view is ViewGroup) view.children.forEach(::visit)
    }
    visit(root)
    styleAuroraHeaderScroll(root)
}

fun styleAuroraMediaRow(root: View) {
    if (!Preferences.isAuroraAppearance) return
    val rowHeight = root.layoutParams.height
    if (rowHeight > 0 && rowHeight - root.paddingTop - root.paddingBottom < 48.dp(root.context)) {
        val padding = ((rowHeight - 48.dp(root.context)) / 2).coerceAtLeast(0)
        root.updatePadding(top = padding, bottom = padding)
    }
    root.findViewById<TextView>(R.id.title)?.textSize = if (root.layoutParams.height > 0) 14.5f else 13.5f
    root.findViewById<TextView>(R.id.text)?.textSize = 12.5f
    root.findViewById<TextView>(R.id.time)?.textSize = 12.5f
    root.findViewById<ShapeableImageView>(R.id.image)?.apply {
        if (shapeAppearanceModel.topLeftCornerSize !is RelativeCornerSize) {
            shapeAppearanceModel = shapeAppearanceModel.toBuilder().setAllCornerSizes(6.dp(context).toFloat()).build()
        }
    }
    root.findViewById<MaterialButton>(R.id.menu)?.apply {
        minimumWidth = 48.dp(context)
        minimumHeight = 48.dp(context)
        isFocusable = true
        cornerRadius = 8.dp(context)
        iconSize = 20.dp(context)
        contentDescription = context.getString(R.string.action_more)
    }
}

fun auroraGlassDrawable(
    view: View,
    radius: Int = 12,
): GradientDrawable =
    GradientDrawable().apply {
        setColor(0x9E060608.toInt())
        cornerRadius = radius.dp(view.context).toFloat()
        setStroke(1.dp(view.context), 0x18FFFFFF)
    }
