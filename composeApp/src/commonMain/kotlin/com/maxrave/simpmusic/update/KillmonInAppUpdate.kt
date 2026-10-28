package com.maxrave.simpmusic.update

import androidx.compose.runtime.Composable

/**
 * On cold launch and foreground resumes, look for a newer Killmon release on GitHub.
 * Desktop has nothing to install, so its actual is empty.
 */
@Composable
expect fun KillmonInAppUpdate(active: Boolean)
