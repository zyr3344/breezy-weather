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

import android.os.Build
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import org.breezyweather.common.extensions.legacyStatusBarHeight

@Composable
fun systemBarsWithStatusBarFallback(): WindowInsets {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        return WindowInsets.systemBars
    }

    val context = LocalContext.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
    val top = systemBarsPadding.calculateTopPadding().takeIf { it.value != 0f } ?: with(density) {
        context.legacyStatusBarHeight.toDp()
    }

    return WindowInsets(
        left = systemBarsPadding.calculateLeftPadding(layoutDirection),
        top = top,
        right = systemBarsPadding.calculateRightPadding(layoutDirection),
        bottom = systemBarsPadding.calculateBottomPadding()
    )
}
