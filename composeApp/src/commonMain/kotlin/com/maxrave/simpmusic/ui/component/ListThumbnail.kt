package com.maxrave.simpmusic.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Precision

/**
 * Pixel size of a list thumbnail. Remembered so a scroll recomposition does not
 * redo the density conversion for every row.
 */
@Composable
fun rememberThumbPx(size: Dp): Int {
    val density = LocalDensity.current
    return remember(size, density) { with(density) { size.roundToPx() }.coerceAtLeast(1) }
}

/**
 * Decode a list image at the size it is drawn, not at the file's full resolution.
 * Memory and disk cache stay on. Crossfade is left off: it keeps a second bitmap
 * and an animation running while the row is scrolling in.
 * [Precision.INEXACT] lets Coil reuse a cached bitmap that is already close enough.
 */
fun ImageRequest.Builder.boundedDecode(
    widthPx: Int,
    heightPx: Int = widthPx,
): ImageRequest.Builder =
    this
        .size(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1))
        .precision(Precision.INEXACT)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
