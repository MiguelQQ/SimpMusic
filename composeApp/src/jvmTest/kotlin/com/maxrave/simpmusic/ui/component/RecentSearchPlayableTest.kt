package com.maxrave.simpmusic.ui.component

import com.maxrave.domain.data.entities.RecentSearchItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RecentSearchPlayableTest {
    @Test
    fun songRecentBecomesAPlayableTrack() {
        val item =
            RecentSearchItem(
                id = "song:abc",
                kind = RecentSearchItem.SONG,
                title = "Palabras Sobran",
                subtitle = "Blessd",
                videoId = "abc",
            )
        val track = item.toPlayableTrack()
        assertNotNull(track)
        assertEquals("abc", track.videoId)
        assertEquals("Palabras Sobran", track.title)
    }

    @Test
    fun nonPlayableRecentsCannotBeSwiped() {
        val artist = RecentSearchItem(id = "artist:x", kind = RecentSearchItem.ARTIST, title = "Blessd", browseId = "UCx")
        val album = RecentSearchItem(id = "album:y", kind = RecentSearchItem.ALBUM, title = "Álbum", browseId = "MPREy")
        assertNull(artist.toPlayableTrack())
        assertNull(album.toPlayableTrack())
    }
}
