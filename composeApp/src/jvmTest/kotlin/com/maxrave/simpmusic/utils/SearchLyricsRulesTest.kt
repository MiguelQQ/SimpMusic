package com.maxrave.simpmusic.utils

import com.maxrave.simpmusic.ui.screen.other.SearchUIType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchLyricsRulesTest {
    @Test
    fun typed_text_always_shows_results_never_suggestions() {
        assertEquals(SearchUIType.SEARCH_RESULTS, searchUiTypeFor("bless", isFocused = true, isExpanded = true))
        assertEquals(SearchUIType.SEARCH_RESULTS, searchUiTypeFor("bless", isFocused = false, isExpanded = false))
    }

    @Test
    fun history_only_fills_the_focused_empty_bar() {
        assertEquals(SearchUIType.SEARCH_HISTORY, searchUiTypeFor("", isFocused = true, isExpanded = true))
        assertEquals(SearchUIType.EMPTY, searchUiTypeFor("", isFocused = false, isExpanded = false))
    }

    @Test
    fun lyrics_fetch_repeats_for_another_track_or_a_different_length_only() {
        assertTrue(lyricsAttemptIsStale(null, "a", 200))
        assertFalse(lyricsAttemptIsStale("a" to 200, "a", 201))
        assertFalse(lyricsAttemptIsStale("a" to 200, "a", 204))
        assertTrue(lyricsAttemptIsStale("a" to 200, "a", 205))
        assertTrue(lyricsAttemptIsStale("a" to 200, "b", 200))
    }
}
