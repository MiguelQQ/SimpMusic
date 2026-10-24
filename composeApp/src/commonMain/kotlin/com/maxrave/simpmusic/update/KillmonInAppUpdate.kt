package com.maxrave.simpmusic.update

import androidx.compose.runtime.Composable

/**
 * After a Killmon session is ready, look for a newer arm64 APK on GitHub.
 * Desktop has nothing to install, so its actual is empty.
 */
@Composable
expect fun KillmonInAppUpdate(active: Boolean)
