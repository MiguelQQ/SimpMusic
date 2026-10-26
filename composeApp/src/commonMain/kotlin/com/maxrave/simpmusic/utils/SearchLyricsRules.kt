package com.maxrave.simpmusic.utils

import com.maxrave.simpmusic.ui.screen.other.SearchUIType
import kotlin.math.abs

/**
 * Which body the Search tab shows. Typed text ALWAYS shows music results (live search fills them
 * in); the history list only fills the focused, still empty bar. Text suggestions never take the
 * results' place.
 */
internal fun searchUiTypeFor(
    searchText: String,
    isFocused: Boolean,
    isExpanded: Boolean,
): SearchUIType =
    when {
        searchText.isNotEmpty() -> SearchUIType.SEARCH_RESULTS
        isFocused && isExpanded -> SearchUIType.SEARCH_HISTORY
        else -> SearchUIType.EMPTY
    }

/** Same bar as the catalog match in lyricsService, which this module cannot see. */
internal const val LYRICS_RETRY_DURATION_DELTA_SECONDS = 4

/**
 * A lyrics fetch that already ran is worth repeating only for another track, or when the length it
 * used was off by more than the catalog tolerance (metadata first, then the player's exact one).
 */
internal fun lyricsAttemptIsStale(
    last: Pair<String, Int>?,
    videoId: String,
    durationSec: Int,
): Boolean {
    if (last == null) return true
    return last.first != videoId || abs(last.second - durationSec) > LYRICS_RETRY_DURATION_DELTA_SECONDS
}
