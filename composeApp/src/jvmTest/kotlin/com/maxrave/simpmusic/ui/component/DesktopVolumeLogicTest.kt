package com.maxrave.simpmusic.ui.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopVolumeLogicTest {
    @Test
    fun sliderShowsTheObservedVolumeUnlessAGestureIsInProgress() {
        assertEquals(0.3f, displayedVolume(observed = 0.3f, gesture = null))
        assertEquals(0.8f, displayedVolume(observed = 0.3f, gesture = 0.8f))
        assertTrue(displayedVolume(0.3f, null) != 0f)
        assertTrue(displayedVolume(0.3f, null) != 1f)
    }

    @Test
    fun wheelDownLowersAndWheelUpRaises() {
        assertEquals(0f, volumeAfterWheel(0.25f, scrollDeltaY = 1f, step = 0.25f))
        assertEquals(0.5f, volumeAfterWheel(0.25f, scrollDeltaY = -1f, step = 0.25f))
        assertEquals(0f, volumeAfterWheel(0.02f, scrollDeltaY = 1f))
        assertEquals(1f, volumeAfterWheel(0.99f, scrollDeltaY = -1f))
    }

    @Test
    fun keysStepTheSameWay() {
        assertEquals(0.75f, volumeAfterKey(0.5f, up = true, step = 0.25f))
        assertEquals(0.25f, volumeAfterKey(0.5f, up = false, step = 0.25f))
    }
}
