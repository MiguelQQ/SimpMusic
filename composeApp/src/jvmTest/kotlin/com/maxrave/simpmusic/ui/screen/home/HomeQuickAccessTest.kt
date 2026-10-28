package com.maxrave.simpmusic.ui.screen.home

import com.maxrave.common.Config
import com.maxrave.domain.data.entities.AlbumEntity
import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.entities.PlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.home.Content
import com.maxrave.domain.data.model.home.HomeItem
import com.maxrave.domain.data.model.searchResult.songs.Thumbnail
import com.maxrave.domain.data.type.RecentlyType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomeQuickAccessTest {
    private fun song(
        id: String,
        removed: Boolean = false,
    ) = SongEntity(
        videoId = id,
        duration = "",
        durationSeconds = 0,
        isAvailable = true,
        isExplicit = false,
        likeStatus = "",
        thumbnails = "https://t/$id",
        title = "Song $id",
        videoType = "",
        category = null,
        resultType = null,
    ).let { if (removed) it.copy(inLibrary = Config.REMOVED_SONG_DATE_TIME) else it }

    private fun artist(id: String) = ArtistEntity(channelId = "UC$id", name = "Artist $id", thumbnails = "https://t/$id")

    private fun playlist(id: String) = PlaylistEntity(id = id, title = "Playlist $id", thumbnails = "https://t/$id")

    private fun album(id: String) =
        AlbumEntity(
            browseId = "MPRE$id",
            audioPlaylistId = "OLAK$id",
            description = "",
            duration = null,
            durationSeconds = 0,
            thumbnails = "https://t/$id",
            title = "Album $id",
            trackCount = 0,
            type = "Album",
            year = null,
        )

    private fun content(
        title: String,
        videoId: String? = null,
        browseId: String? = null,
        playlistId: String? = null,
    ) = Content(
        album = null,
        artists = null,
        description = null,
        isExplicit = null,
        playlistId = playlistId,
        browseId = browseId,
        thumbnails = listOf(Thumbnail(544, "https://t/$title", 544)),
        title = title,
        videoId = videoId,
        views = null,
    )

    private fun tiles(
        recents: List<RecentlyType>,
        favorites: Int = 0,
        shelves: List<HomeItem> = emptyList(),
    ) = buildQuickTiles(recents, favorites, null, "Favoritos", shelves)

    @Test
    fun emptyHistoryAndHomeGivesNoTiles() {
        assertTrue(tiles(emptyList()).isEmpty())
    }

    @Test
    fun eachCellKeepsItsOwnVideoIdWhenTheListShifts() {
        val morena = song("bbbbbbbbbbb").copy(title = "Morena")
        val mero = song("aaaaaaaaaaa").copy(title = "Mero tote")
        fun ids(oldestFirst: List<SongEntity>) =
            tiles(oldestFirst)
                .filter { it.target is QuickTarget.LocalSong }
                .associate { it.title to quickTilePlaybackVideoId(it.target) }
        assertEquals("aaaaaaaaaaa", ids(listOf(morena, mero))["Mero tote"])
        assertEquals("bbbbbbbbbbb", ids(listOf(morena, mero))["Morena"])
        assertEquals("aaaaaaaaaaa", ids(listOf(mero, morena))["Mero tote"])
        assertEquals("bbbbbbbbbbb", ids(listOf(mero, morena))["Morena"])
        val shifted =
            listOf(
                content("Morena", videoId = "bbbbbbbbbbb"),
                content("Mero tote", videoId = "aaaaaaaaaaa"),
            )
        val keys = shifted.mapIndexed { index, cell -> homeContentItemKey(cell.videoId, cell.browseId, cell.playlistId, cell.title, index) }
        assertEquals(keys.toSet().size, keys.size)
        assertEquals("aaaaaaaaaaa", playbackVideoIdForCell(shifted.first { it.title == "Mero tote" }.videoId))
        assertTrue(keys.first { it.startsWith("aaaaaaaaaaa") }.startsWith("aaaaaaaaaaa"))
        val blanks =
            listOf(
                homeContentItemKey("", null, null, "Mero tote", 0),
                homeContentItemKey("", null, null, "Morena", 1),
            )
        assertEquals(blanks.toSet().size, blanks.size)
    }

    @Test
    fun favoritesComeFirstAndAreHiddenWithNoLikedSongs() {
        assertEquals("Favoritos", tiles(listOf(song("a"), song("b")), favorites = 3).first().title)
        assertTrue(tiles(listOf(song("a"), song("b"))).none { it.target is QuickTarget.Favorites })
    }

    @Test
    fun recentsAreMixedAcrossKindsNewestFirstAndEven() {
        // Oldest first, as the repository returns them.
        val recents: List<RecentlyType> =
            listOf(album("1"), song("1"), song("2"), song("3"), song("4"), artist("1"), playlist("1"))
        val result = tiles(recents)
        assertEquals(6, result.size)
        // One of each kind leads before any lane gives a second tile.
        assertEquals(
            setOf("Playlist 1", "Artist 1", "Song 4", "Album 1"),
            result.take(4).map { it.title }.toSet(),
        )
        assertTrue(result.first { it.target is QuickTarget.Artist }.round)
    }

    @Test
    fun removedSongsAndRadioPlaylistsAreSkipped() {
        val result = tiles(listOf(song("x", removed = true), playlist("RDAMVMabc"), song("ok"), song("ok2")))
        assertEquals(listOf("so:ok2", "so:ok"), result.map { it.key })
    }

    @Test
    fun shortHistoryIsToppedUpFromHomeWithoutInventingAnything() {
        val shelves =
            listOf(
                HomeItem(listOf(content("Quick1", videoId = "q1"), content("Quick2", videoId = "q2")), "Quick", itemsPerColumn = 4),
                HomeItem(listOf(content("Mix", playlistId = "RDCLAKx"), content("Art", browseId = "UCz")), "Mixes"),
            )
        val result = tiles(listOf(song("a")), shelves = shelves)
        // 5 candidates, and the grid keeps an even count.
        assertEquals(4, result.size)
        assertTrue(result.all { it.title.isNotBlank() })
        assertEquals(result.map { it.key }.distinct(), result.map { it.key })
    }

    @Test
    fun localShelvesNeedFourCardsAndInterleave() {
        val recents: List<RecentlyType> = (1..5).map { artist("$it") } + (1..2).map { album("$it") }
        val local = buildLocalShelves(recents, "Songs", "Artists", "Albums", "Playlists")
        assertEquals(listOf("Artists"), local.map { it.title })
        val yt = (1..4).map { HomeItem(listOf(content("c$it", videoId = "v$it")), "YT$it") }
        val merged = interleaveShelves(yt, local + local.map { it.copy(title = "More") })
        assertEquals(listOf("YT1", "Artists", "YT2", "YT3", "More", "YT4"), merged.map { it.title })
    }

    @Test
    fun interleaveWithNothingExtraKeepsOrder() {
        val yt = listOf(HomeItem(emptyList(), "A"), HomeItem(emptyList(), "B"))
        assertEquals(yt, interleaveShelves(yt, emptyList()))
    }
}
