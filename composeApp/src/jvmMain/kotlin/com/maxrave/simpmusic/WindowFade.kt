package com.maxrave.simpmusic

import java.awt.GraphicsDevice
import java.awt.GraphicsEnvironment
import java.awt.IllegalComponentStateException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext

/**
 * Per-window opacity fade for the desktop shell.
 *
 * Linux keeps the native title bar, where [java.awt.Window.setOpacity] is unreliable, so
 * [supported] is false there and callers keep the previous instant show/hide.
 * A failed opacity write disables the fade for the rest of the process.
 */
object WindowFade {
    const val HIDE_MS = 160
    const val RESTORE_MS = 200
    const val STARTUP_MS = 220
    const val EXIT_MS = 180

    private const val STEP_MS = 16L

    /** Mirrors `nativeTitleBar` in the desktop shell (`true` on Linux). */
    @Volatile
    var nativeTitleBar: Boolean = false

    @Volatile
    private var disabledByFailure: Boolean = false

    private val translucencySupported: Boolean =
        runCatching {
            GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .defaultScreenDevice
                .isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT)
        }.getOrDefault(false)

    val supported: Boolean
        get() = translucencySupported && !nativeTitleBar && !disabledByFailure

    /** Cubic ease-out from [from] to [to]. About one step per frame. No-op when [supported] is false. */
    suspend fun fade(
        window: java.awt.Window,
        from: Float,
        to: Float,
        durationMs: Int,
    ) {
        if (!supported) return
        val safeFrom = from.coerceIn(0f, 1f)
        val safeTo = to.coerceIn(0f, 1f)
        withContext(Dispatchers.Swing) {
            try {
                val steps = (durationMs / STEP_MS).toInt().coerceAtLeast(1)
                for (i in 0..steps) {
                    val t = i / steps.toFloat()
                    val inv = 1f - t
                    val eased = 1f - inv * inv * inv
                    window.opacity = safeFrom + (safeTo - safeFrom) * eased
                    if (i < steps) delay(STEP_MS)
                }
                window.opacity = safeTo
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (error is IllegalComponentStateException || error is UnsupportedOperationException) {
                    disabledByFailure = true
                }
                runCatching { window.opacity = safeTo }
            }
        }
    }
}
