package com.maxrave.simpmusic.ui.screen.home

import com.maxrave.common.Config
import com.maxrave.domain.data.entities.AlbumEntity
import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.entities.PlaylistEntity
import com.maxrave.domain.data.entities.PodcastsEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.home.Content
import com.maxrave.domain.data.model.home.HomeItem
import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.domain.data.model.searchResult.songs.Thumbnail
import com.maxrave.domain.data.type.RecentlyType
import com.maxrave.domain.utils.isRadioPlaylistId

/** What a Home quick-access tile opens. */
sealed interface QuickTarget {
    data class LocalSong(
        val song: SongEntity,
    ) : QuickTarget

    data class HomeSong(
        val content: Content,
    ) : QuickTarget

    data class Album(
        val browseId: String,
    ) : QuickTarget

    data class Artist(
        val channelId: String,
    ) : QuickTarget

    data class Playlist(
        val id: String,
    ) : QuickTarget

    data class Podcast(
        val id: String,
    ) : QuickTarget

    /** The local Favorite (liked songs) list. */
    data object Favorites : QuickTarget
}

data class QuickTile(
    val key: String,
    val title: String,
    val thumbnail: String?,
    /** Artists are drawn round, like Spotify's. */
    val round: Boolean,
    val target: QuickTarget,
)

/** The tile grid is two columns wide, so it always holds an even number. */
const val QUICK_TILE_LIMIT = 8

/** A shelf from the listener's own history needs at least this many cards to be worth a row. */
const val LOCAL_SHELF_MIN_ITEMS = 4
private const val LOCAL_SHELF_MAX_ITEMS = 15

/** [recents] arrives oldest first, as `getAllRecentData` returns it. */
private fun List<RecentlyType>.newestFirst(): List<RecentlyType> = asReversed()

private fun SongEntity.isListed() = inLibrary != Config.REMOVED_SONG_DATE_TIME && title.isNotBlank()

private fun PlaylistEntity.isListed() = title.isNotBlank() && !id.isRadioPlaylistId()

/**
 * The 6-8 small cards under the chips: Favorite first (when the listener has any), then the
 * listener's own recents mixed across kinds so eight songs do not crowd out an artist or a
 * playlist, and finally real cards from YouTube's own Home when the history is too short.
 * Nothing is made up: with neither history nor Home data the result is empty and the grid is hidden.
 */
fun buildQuickTiles(
    recents: List<RecentlyType>,
    favoritesCount: Int,
    favoritesThumbnail: String?,
    favoritesTitle: String,
    homeShelves: List<HomeItem>,
    limit: Int = QUICK_TILE_LIMIT,
): List<QuickTile> {
    val tiles = LinkedHashMap<String, QuickTile>()

    fun add(tile: QuickTile) {
        if (tiles.size < limit && tile.title.isNotBlank()) tiles.putIfAbsent(tile.key, tile)
    }

    if (favoritesCount > 0) {
        add(QuickTile("favorites", favoritesTitle, favoritesThumbnail, false, QuickTarget.Favorites))
    }

    val newest = recents.newestFirst()
    val lanes =
        listOf(
            newest.filterIsInstance<PlaylistEntity>().filter { it.isListed() }.map {
                QuickTile("pl:${it.id}", it.title, it.thumbnails.ifBlank { null }, false, QuickTarget.Playlist(it.id))
            },
            newest.filterIsInstance<ArtistEntity>().map {
                QuickTile("ar:${it.channelId}", it.name, it.thumbnails, true, QuickTarget.Artist(it.channelId))
            },
            newest.filterIsInstance<SongEntity>().filter { it.isListed() }.map {
                QuickTile("so:${it.videoId}", it.title, it.thumbnails, false, QuickTarget.LocalSong(it))
            },
            newest.filterIsInstance<AlbumEntity>().map {
                QuickTile("al:${it.browseId}", it.title, it.thumbnails, false, QuickTarget.Album(it.browseId))
            },
            newest.filterIsInstance<PodcastsEntity>().map {
                QuickTile("po:${it.podcastId}", it.title, it.thumbnail, false, QuickTarget.Podcast(it.podcastId))
            },
        )
    var round = 0
    while (tiles.size < limit && lanes.any { round < it.size }) {
        lanes.forEach { lane -> lane.getOrNull(round)?.let(::add) }
        round++
    }

    if (tiles.size < limit) {
        // Too little history: top up with what YouTube put on this Home. Songs of the first shelf
        // (Quick picks) come first, then one card from each other shelf in turn.
        val shelves = homeShelves.filter { it.contents.isNotEmpty() }
        val homeTiles = shelves.map { shelf -> shelf.contents.filterNotNull().mapNotNull { it.toQuickTile() } }
        var r = 0
        while (tiles.size < limit && homeTiles.any { r < it.size }) {
            homeTiles.forEach { lane -> lane.getOrNull(r)?.let(::add) }
            r++
        }
    }

    // Two columns: a lone last tile would leave a hole.
    return tiles.values.toList().let { if (it.size % 2 == 1) it.dropLast(1) else it }
}

private fun Content.toQuickTile(): QuickTile? {
    val thumb = thumbnails.lastOrNull()?.url
    val video = videoId
    val browse = browseId
    val playlist = playlistId
    return when {
        !video.isNullOrEmpty() -> QuickTile("so:$video", title, thumb, false, QuickTarget.HomeSong(this))
        browse != null && browse.startsWith("UC") -> QuickTile("ar:$browse", title, thumb, true, QuickTarget.Artist(browse))
        browse != null && browse.startsWith("MPRE") -> QuickTile("al:$browse", title, thumb, false, QuickTarget.Album(browse))
        playlist != null && !playlist.startsWith("UC") -> QuickTile("pl:$playlist", title, thumb, false, QuickTarget.Playlist(playlist))
        else -> null
    }
}

/**
 * Carousels built from the listener's own history, so Home has rows that follow their taste on top
 * of whatever YouTube sends. Each is an ordinary [HomeItem], drawn by the same cards as the rest.
 * A row with fewer than [LOCAL_SHELF_MIN_ITEMS] cards is dropped rather than padded.
 */
fun buildLocalShelves(
    recents: List<RecentlyType>,
    songsTitle: String,
    artistsTitle: String,
    albumsTitle: String,
    playlistsTitle: String,
): List<HomeItem> {
    val newest = recents.newestFirst()
    val songs =
        newest.filterIsInstance<SongEntity>().filter { it.isListed() }.map { s ->
            Content(
                album = null,
                artists = s.artistName?.map { Artist(id = null, name = it) },
                description = null,
                isExplicit = s.isExplicit,
                playlistId = null,
                browseId = null,
                thumbnails = s.thumbnails.thumbs(),
                title = s.title,
                videoId = s.videoId,
                views = null,
            )
        }
    val artists =
        newest.filterIsInstance<ArtistEntity>().map { a ->
            Content(
                album = null,
                artists = null,
                description = null,
                isExplicit = null,
                playlistId = null,
                browseId = a.channelId,
                thumbnails = a.thumbnails.thumbs(),
                title = a.name,
                videoId = null,
                views = null,
            )
        }
    val albums =
        newest.filterIsInstance<AlbumEntity>().map { a ->
            Content(
                album = null,
                artists = a.artistName?.map { Artist(id = null, name = it) },
                description = null,
                isExplicit = null,
                playlistId = null,
                browseId = a.browseId,
                thumbnails = a.thumbnails.thumbs(),
                title = a.title,
                videoId = null,
                views = null,
            )
        }
    val playlists =
        newest.filterIsInstance<PlaylistEntity>().filter { it.isListed() }.map { p ->
            Content(
                album = null,
                artists = p.author?.takeIf { it.isNotBlank() }?.let { listOf(Artist(id = null, name = it)) },
                description = null,
                isExplicit = null,
                playlistId = p.id,
                browseId = null,
                thumbnails = p.thumbnails.thumbs(),
                title = p.title,
                videoId = null,
                views = null,
            )
        }
    return listOf(
        playlistsTitle to playlists,
        artistsTitle to artists,
        albumsTitle to albums,
        songsTitle to songs,
    ).filter { (_, items) -> items.size >= LOCAL_SHELF_MIN_ITEMS }
        .map { (title, items) -> HomeItem(contents = items.take(LOCAL_SHELF_MAX_ITEMS), title = title) }
}

private fun String?.thumbs(): List<Thumbnail> = if (isNullOrBlank()) emptyList() else listOf(Thumbnail(height = 544, url = this, width = 544))

/**
 * Puts [extra] rows between YouTube's: the first after the first shelf, the rest every second
 * shelf, and any left over at the end, so the page alternates instead of ending in a block.
 */
fun interleaveShelves(
    shelves: List<HomeItem>,
    extra: List<HomeItem>,
): List<HomeItem> {
    if (extra.isEmpty()) return shelves
    val out = ArrayList<HomeItem>(shelves.size + extra.size)
    val pending = ArrayDeque(extra)
    shelves.forEachIndexed { index, shelf ->
        out.add(shelf)
        if (index % 2 == 0 && pending.isNotEmpty()) out.add(pending.removeFirst())
    }
    out.addAll(pending)
    return out
}
