/*
 * This file is part of Breezy Weather.
 *
 * Breezy Weather is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, version 3 of the License.
 *
 * Breezy Weather is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Breezy Weather. If not, see <https://www.gnu.org/licenses/>.
 */

package org.breezyweather.common.extensions

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.ViewConfiguration
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener
import androidx.core.view.WindowInsetsCompat
import kotlin.math.max

/*
 * Source: Android Developers, Chris Banes
 * https://medium.com/androiddevelopers/windowinsets-listeners-to-layouts-8f9ccc8fa4d1
 */

/**
 * Apply window insets for system bars. Display cutouts are intentionally ignored
 * so landscape content can draw through camera areas.
 */
fun View.doOnApplyWindowInsets(f: (View, Insets) -> Unit) {
    // Set an actual OnApplyWindowInsetsListener which proxies to the given lambda
    setOnApplyWindowInsetsListener(this) { v, insets ->
        val i = v.resolveSystemBarInsets(insets)
        f(v, i)
        // Always return the insets, so that children can also use them
        insets
    }
    // request some insets
    requestApplyInsetsWhenAttached()
}

fun View.requestApplyInsetsWhenAttached() {
    if (isAttachedToWindow) {
        // We're already attached, just request as normal
        requestApplyInsets()
    } else {
        // We're not attached to the hierarchy, add a listener to request when we are
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.removeOnAttachStateChangeListener(this)
                v.requestApplyInsets()
            }

            override fun onViewDetachedFromWindow(v: View) = Unit
        })
    }
}

val Context.legacyStatusBarHeight: Int
    get() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return 0
        }
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }

val Context.legacyNavigationBarHeight: Int
    get() = getLegacySystemBarDimension("navigation_bar_height")

val Context.legacyNavigationBarHeightLandscape: Int
    get() = getLegacySystemBarDimension("navigation_bar_height_landscape")

val Context.legacyNavigationBarWidth: Int
    get() = getLegacySystemBarDimension("navigation_bar_width")

val Context.hasLegacyNavigationBar: Boolean
    get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.R && !ViewConfiguration.get(this).hasPermanentMenuKey()

val Context.isLegacyThreeButtonNavigation: Boolean
    get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
        hasLegacyNavigationBar &&
        runCatching {
            Settings.Secure.getInt(contentResolver, "navigation_mode", -1) == 0
        }.getOrDefault(false)

fun View.getStableSystemBarInsets(): Insets {
    return ViewCompat.getRootWindowInsets(this)
        ?.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars())
        ?: Insets.NONE
}

fun View.resolveSystemBarInsets(
    rootInsets: WindowInsetsCompat? = ViewCompat.getRootWindowInsets(this),
    symmetricLandscapeHorizontalInsets: Boolean = true,
): Insets {
    val stableInsets = rootInsets?.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars())
        ?: Insets.NONE
    val systemBarInsets = (
        rootInsets?.getInsets(WindowInsetsCompat.Type.systemBars())
            ?: Insets.NONE
        )
        .withLegacySystemBarFallback(this, stableInsets)
    return if (symmetricLandscapeHorizontalInsets) {
        systemBarInsets.withLandscapeSymmetricHorizontalInsets(context)
    } else {
        systemBarInsets
    }
}

fun Insets.withLegacySystemBarFallback(view: View, stableInsets: Insets = view.getStableSystemBarInsets()): Insets {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        return this
    }
    val context = view.context
    val statusBarHeight = context.legacyStatusBarHeight
    val newLeft = if (left == 0) stableInsets.left else left
    val newTop = if (top == 0) stableInsets.top.takeIf { it > 0 } ?: statusBarHeight else top
    val newRight = if (right == 0) stableInsets.right else right
    var newBottom = if (bottom == 0) stableInsets.bottom else bottom

    if (context.isLegacyThreeButtonNavigation) {
        if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
            newLeft == 0 &&
            newRight == 0
        ) {
            val navBarWidth = context.legacyNavigationBarWidth.takeIf { it > 0 }
                ?: context.legacyNavigationBarHeightLandscape
            return Insets.of(
                newLeft,
                newTop,
                navBarWidth,
                newBottom
            )
        } else if (context.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE &&
            newBottom == 0
        ) {
            newBottom = context.legacyNavigationBarHeight
        }
    }

    return Insets.of(newLeft, newTop, newRight, newBottom)
}

fun Insets.withLandscapeSymmetricHorizontalInsets(context: Context): Insets {
    if (context.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE) {
        return this
    }
    val horizontal = max(left, right)
    return Insets.of(horizontal, top, horizontal, bottom)
}

private fun Context.getLegacySystemBarDimension(name: String): Int {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        return 0
    }
    val resourceId = resources.getIdentifier(name, "dimen", "android")
    return if (resourceId > 0) {
        resources.getDimensionPixelSize(resourceId)
    } else {
        0
    }
}
