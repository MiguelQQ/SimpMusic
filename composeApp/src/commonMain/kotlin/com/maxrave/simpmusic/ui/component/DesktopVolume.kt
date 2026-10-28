package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import kotlin.math.abs

/** What the slider draws: the drag or the wheel step until the player reports that same level. */
fun displayedVolume(
    observed: Float,
    gesture: Float?,
): Float = (gesture ?: observed).coerceIn(0f, 1f)

/** Wheel down (positive y) lowers the volume. One notch is [step]. */
fun volumeAfterWheel(
    current: Float,
    scrollDeltaY: Float,
    step: Float = 0.05f,
): Float {
    if (scrollDeltaY == 0f) return current.coerceIn(0f, 1f)
    val direction = if (scrollDeltaY > 0f) -1f else 1f
    return (current + direction * step).coerceIn(0f, 1f)
}

/** Up is higher. Shared by the arrow keys, +/- and the volume keys. */
fun volumeAfterKey(
    current: Float,
    up: Boolean,
    step: Float = 0.05f,
): Float = (current + if (up) step else -step).coerceIn(0f, 1f)

class VolumeControl(
    val shown: Float,
    val dragging: Boolean,
    val onDrag: (Float) -> Unit,
    val onDragFinished: () -> Unit,
    val nudge: (Float) -> Unit,
)

/**
 * [observed] is the player volume. [gesture] covers a drag or a key/wheel step so the thumb
 * does not snap back to a stale [observed] when the event that publishes it is still in flight.
 */
@Composable
fun rememberVolumeControl(
    observed: Float,
    onVolume: (Float) -> Unit,
): VolumeControl {
    var gesture by remember { mutableStateOf<Float?>(null) }
    var dragging by remember { mutableStateOf(false) }
    val shown = displayedVolume(observed, gesture)
    LaunchedEffect(observed, gesture, dragging) {
        val held = gesture ?: return@LaunchedEffect
        if (!dragging && abs(observed - held) < 0.004f) gesture = null
    }
    val publish by rememberUpdatedState(onVolume)
    return VolumeControl(
        shown = shown,
        dragging = dragging,
        onDrag = { next ->
            dragging = true
            val level = next.coerceIn(0f, 1f)
            gesture = level
            publish(level)
        },
        onDragFinished = { dragging = false },
        nudge = { next ->
            dragging = false
            val level = next.coerceIn(0f, 1f)
            gesture = level
            publish(level)
        },
    )
}

/** Wheel and keys on the same control that draws [VolumeControl.shown]. */
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun Modifier.volumeGestures(control: VolumeControl): Modifier {
    val shown by rememberUpdatedState(control.shown)
    val nudge by rememberUpdatedState(control.nudge)
    return this
        .onPointerEvent(PointerEventType.Scroll) { event ->
            val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
            if (delta == 0f) return@onPointerEvent
            nudge(volumeAfterWheel(shown, delta))
            event.changes.forEach { it.consume() }
        }.onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            val up =
                when (event.key) {
                    Key.DirectionUp, Key.Equals, Key.NumPadAdd, Key.VolumeUp -> true
                    Key.DirectionDown, Key.Minus, Key.NumPadSubtract, Key.VolumeDown -> false
                    else -> return@onPreviewKeyEvent false
                }
            nudge(volumeAfterKey(shown, up))
            true
        }.focusable()
}
