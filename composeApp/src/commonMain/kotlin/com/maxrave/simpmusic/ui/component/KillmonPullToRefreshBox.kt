package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.getPlatform

/**
 * PullToRefreshBox on touch platforms. On Desktop the mouse wheel overscroll at the top would
 * engage the refresh gesture, so there it is a plain Box (no indicator, no reload).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KillmonPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: PullToRefreshState,
    contentAlignment: Alignment = Alignment.TopStart,
    indicator: @Composable BoxScope.() -> Unit = {
        PullToRefreshDefaults.Indicator(
            modifier = Modifier.align(Alignment.TopCenter),
            isRefreshing = isRefreshing,
            state = state,
        )
    },
    content: @Composable BoxScope.() -> Unit,
) {
    if (getPlatform() == Platform.Desktop) {
        Box(modifier = modifier, contentAlignment = contentAlignment, content = content)
    } else {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = modifier,
            state = state,
            contentAlignment = contentAlignment,
            indicator = indicator,
            content = content,
        )
    }
}
