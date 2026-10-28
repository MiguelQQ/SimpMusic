package com.maxrave.simpmusic.ui.component

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopTitleBarTest {
    @Test
    fun windowsUsesCaptionButtonsAndMacKeepsTrafficLights() {
        assertEquals(DesktopTitleBarStyle.Windows, desktopTitleBarStyle("Windows 11"))
        assertEquals(DesktopTitleBarStyle.Windows, desktopTitleBarStyle("Windows 10"))
        assertEquals(DesktopTitleBarStyle.Mac, desktopTitleBarStyle("Mac OS X"))
        assertEquals(DesktopTitleBarStyle.Mac, desktopTitleBarStyle("Linux"))
    }
}
