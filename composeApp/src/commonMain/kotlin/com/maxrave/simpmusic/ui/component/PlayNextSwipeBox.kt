package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.offset
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.SkipNext
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.play_next
import kotlin.math.roundToInt

/** Spotify-green revealed by a completed right swipe. */
val PlayNextGreen = Color(0xFF39E75F)

/** Shared by the quick-pick card and the swipe clip behind it. */
val QuickPicksCardShape: Shape = RoundedCornerShape(6.dp)

/**
 * Right-swipe a song row to play it next. The row slides back after the gesture,
 * whether or not the swipe was far enough to count.
 */
@Composable
fun PlayNextSwipeBox(
    enabled: Boolean,
    onPlayNext: () -> Unit,
    modifier: Modifier = Modifier,
    clipShape: Shape? = null,
    content: @Composable () -> Unit,
) {
    val maxOffsetPx = 112.dp
    val offsetX = remember { Animatable(0f) }
    // The green layer is the swipe reveal. Drawn at rest it shows through the
    // anti-aliased rounded corners of the card on top.
    val revealGreen by remember { derivedStateOf { offsetX.value > 0f } }
    val scope = rememberCoroutineScope()
    val playNext by rememberUpdatedState(onPlayNext)
    val description = stringResource(Res.string.play_next)
    Box(if (clipShape != null) modifier.clip(clipShape) else modifier) {
        if (revealGreen) {
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .background(PlayNextGreen)
                        .padding(start = 20.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(
                    imageVector = SimpIcons.SkipNext,
                    contentDescription = description,
                    tint = Color.Black,
                )
            }
        }
        Box(
            modifier =
                Modifier
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .pointerInput(enabled) {
                        if (!enabled) return@pointerInput
                        val maxOffset = maxOffsetPx.toPx()
                        detectHorizontalDragGestures(
                            onDragCancel = {
                                scope.launch { offsetX.animateTo(0f) }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                val next = offsetX.value + dragAmount
                                if (next > 0f) {
                                    change.consume()
                                    scope.launch { offsetX.snapTo(next.coerceAtMost(maxOffset)) }
                                }
                            },
                            onDragEnd = {
                                val done = offsetX.value >= maxOffset * 0.72f
                                scope.launch {
                                    if (done) playNext()
                                    offsetX.animateTo(0f)
                                }
                            },
                        )
                    },
        ) {
            content()
        }
    }
}
