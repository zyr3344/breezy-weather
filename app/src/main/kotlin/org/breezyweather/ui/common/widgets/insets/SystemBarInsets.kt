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

package org.breezyweather.ui.common.widgets.insets

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.breezyweather.common.extensions.resolveSystemBarInsets

@Composable
fun systemBarsWithStatusBarFallback(
    symmetricLandscapeHorizontalInsets: Boolean? = null,
): WindowInsets {
    val view = LocalView.current
    val density = LocalDensity.current
    val applySymmetricInsets = symmetricLandscapeHorizontalInsets
        ?: (view.parent !is DialogWindowProvider)
    var insets by remember(view, applySymmetricInsets) {
        mutableStateOf(
            view.resolveSystemBarInsets(
                symmetricLandscapeHorizontalInsets = applySymmetricInsets
            )
        )
    }

    DisposableEffect(view, applySymmetricInsets) {
        val updateInsets = { rootInsets: WindowInsetsCompat? ->
            insets = view.resolveSystemBarInsets(
                rootInsets = rootInsets,
                symmetricLandscapeHorizontalInsets = applySymmetricInsets
            )
        }
        val layoutChangeListener = android.view.View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateInsets(ViewCompat.getRootWindowInsets(view))
        }
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, rootInsets ->
            updateInsets(rootInsets)
            rootInsets
        }
        view.addOnLayoutChangeListener(layoutChangeListener)
        view.requestApplyInsets()
        onDispose {
            view.removeOnLayoutChangeListener(layoutChangeListener)
            ViewCompat.setOnApplyWindowInsetsListener(view, null)
        }
    }

    return WindowInsets(
        left = with(density) { insets.left.toDp() },
        top = with(density) { insets.top.toDp() },
        right = with(density) { insets.right.toDp() },
        bottom = with(density) { insets.bottom.toDp() }
    )
}
