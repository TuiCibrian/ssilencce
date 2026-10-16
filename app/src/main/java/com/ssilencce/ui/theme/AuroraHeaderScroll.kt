package com.ssilencce.ui.theme

import android.content.Context
import android.content.res.ColorStateList
import android.database.ContentObserver
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL
import com.google.android.material.appbar.MaterialToolbar
import com.ssilencce.R
import com.ssilencce.ui.component.views.TopAppBarLayout
import com.ssilencce.util.Preferences
import kotlin.math.abs

/**
 * Reference scroll treatment for the Aurora header (ssilencce-auroras.html CSS 927-998 and
 * JS 2609-2737): the title fades, translates and blurs away while the hamburger/search/
 * overflow actions stay on screen as floating glass pills over the collapsing header space.
 * Reduced motion snaps between the two states instead of interpolating, like the reference's
 * prefers-reduced-motion branch. Two compatible modes:
 * - Pinned bars: Home (`TopAppBarLayout`), the main lists and album/artist detail (plain
 *   `AppBarLayout`) pin their toolbar, fade the title and collapse the header space through
 *   the scrolling-view overlay.
 * - Collapsing bars (`R.id.app_bar` with `CollapsingToolbarLayout`, Top Tracks / Recently
 *   Added / playlist detail / search) keep their collapse behaviour and pinned toolbar; the
 *   action pills and the content title (`R.id.title` / `R.id.subtitle`) are driven with the
 *   same progress formula.
 */
fun styleAuroraHeaderScroll(root: View) {
    if (!Preferences.isAuroraAppearance) return
    val pinnedRoot = root.findViewById<View>(R.id.appBarLayout)
    val appBar = (pinnedRoot ?: root.findViewById<View>(R.id.app_bar)) as? AppBarLayout ?: return
    val collapsing = pinnedRoot == null
    val toolbar =
        (appBar as? TopAppBarLayout)?.toolbar
            ?: appBar.findViewById<MaterialToolbar>(R.id.toolbar)
            ?: return
    // Home scrolls in the NestedScrollView, list screens in recycler_view, detail screens in recyclerView.
    val scrollView =
        root.findViewById<View>(R.id.container)
            ?: root.findViewById<View>(R.id.recycler_view)
            ?: root.findViewById<View>(R.id.recyclerView)
            ?: return
    if (scrollView.getTag(R.id.aurora_header_scroll_styled) == true) return
    scrollView.setTag(R.id.aurora_header_scroll_styled, true)
    val context = root.context
    val density = context.resources.displayMetrics.density
    val threshold = 6f * density
    val range = 28f * density

    // Keep the actions visible instead of scrolling the whole bar away (floating state).
    // Collapsing bars already pin their toolbar, so their flags stay untouched.
    if (!collapsing) {
        if (appBar is TopAppBarLayout) {
            appBar.pinWhenScrolled()
        } else {
            toolbar.updateLayoutParams<AppBarLayout.LayoutParams> { scrollFlags = SCROLL_FLAG_NO_SCROLL }
        }
    }

    val titleViews = toolbar.children.filterIsInstance<TextView>().toMutableList()
    if (collapsing) {
        root.findViewById<TextView>(R.id.title)?.let(titleViews::add)
        root.findViewById<TextView>(R.id.subtitle)?.let(titleViews::add)
    }
    val actionViews = mutableListOf<View>()
    val originalBackgrounds = mutableListOf<Drawable?>()
    val floatingBackgrounds = mutableListOf<Drawable>()

    fun registerAction(view: View) {
        actionViews += view
        originalBackgrounds += view.background
        floatingBackgrounds += auroraFloatingActionDrawable(density)
    }
    toolbar.children.forEach { child ->
        when (child) {
            is ActionMenuView -> child.children.forEach { registerAction(it) }
            is AppCompatImageButton -> registerAction(child)
            is ImageButton -> registerAction(child)
        }
    }
    val overlayBehavior =
        if (collapsing) {
            null
        } else {
            (scrollView.layoutParams as? CoordinatorLayout.LayoutParams)?.behavior as? AppBarLayout.ScrollingViewBehavior
        }

    var reducedMotion = isAuroraReducedMotion(context)
    val motionObserver =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reducedMotion = isAuroraReducedMotion(context)
            }
        }

    var lastBlurRadius = -1f

    fun apply() {
        // View.computeVerticalScrollOffset is protected; RecyclerView exposes it publicly,
        // while Home's NestedScrollView reports its position through scrollY.
        val offset =
            when (scrollView) {
                is RecyclerView -> scrollView.computeVerticalScrollOffset()
                else -> scrollView.scrollY
            }.toFloat()
        var progress = ((offset - threshold) / range).coerceIn(0f, 1f)
        if (reducedMotion) progress = if (progress > .02f) 1f else 0f
        val floating = progress > .01f

        titleViews.forEach { title ->
            title.alpha = 1f - progress
            if (!collapsing) title.translationY = -8f * density * progress
            title.importantForAccessibility =
                if (progress >= .55f) View.IMPORTANT_FOR_ACCESSIBILITY_NO else View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val blurRadius = if (!reducedMotion && progress > .12f) 4f * density * progress else 0f
            if (abs(blurRadius - lastBlurRadius) >= .5f) {
                lastBlurRadius = blurRadius
                titleViews.forEach { title ->
                    title.setRenderEffect(
                        if (blurRadius > 0f) {
                            RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP)
                        } else {
                            null
                        },
                    )
                }
            }
        }
        actionViews.forEachIndexed { index, action ->
            action.background = if (floating) floatingBackgrounds[index] else originalBackgrounds[index]
            action.elevation = if (floating) 2f * density else 0f
        }
        if (!collapsing) {
            // Collapse the header placeholder like the reference: the content slides under the
            // pinned action row while the title area disappears.
            val toolbarHeight = toolbar.height.takeIf { it > 0 } ?: appBar.height
            overlayBehavior?.overlayTop = (toolbarHeight * progress).toInt()
            appBar.translationZ = if (floating) 1f else 0f
        }
    }

    val motionSettingsUri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
    context.contentResolver.registerContentObserver(motionSettingsUri, false, motionObserver)
    scrollView.addOnAttachStateChangeListener(
        object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                // Re-register after a detach that cleared the marker, so reduced-motion
                // updates keep working for a re-attached, already-styled view.
                if (view.getTag(R.id.aurora_header_scroll_styled) != true) {
                    view.setTag(R.id.aurora_header_scroll_styled, true)
                    context.contentResolver.registerContentObserver(motionSettingsUri, false, motionObserver)
                    view.post { apply() }
                }
            }

            override fun onViewDetachedFromWindow(view: View) {
                context.contentResolver.unregisterContentObserver(motionObserver)
                view.setTag(R.id.aurora_header_scroll_styled, null)
            }
        },
    )

    scrollView.setOnScrollChangeListener { _, _, _, _, _ -> apply() }
    appBar.post { apply() }
}

private fun isAuroraReducedMotion(context: Context): Boolean = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

private fun auroraFloatingActionDrawable(density: Float): RippleDrawable {
    // No backdrop blur is available for View content; the translucent fill plus stroke is the
    // native equivalent of the reference pill (documented approximation).
    val pill =
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0xC716161A.toInt())
            setStroke(density.toInt().coerceAtLeast(1), 0x17FFFFFF)
        }
    return RippleDrawable(ColorStateList.valueOf(0x1FFFFFFF), pill, null)
}
