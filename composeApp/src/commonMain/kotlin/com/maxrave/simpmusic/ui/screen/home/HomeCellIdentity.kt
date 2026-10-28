package com.maxrave.simpmusic.ui.screen.home

/**
 * The id the cell plays. Blank is not an id: a row of empty strings would otherwise share one key
 * and one click target.
 */
fun playbackVideoIdForCell(videoId: String?): String? = videoId?.takeIf { it.isNotBlank() }

/** Lazy key. The index keeps two blank or repeated ids from landing on the same slot. */
fun homeContentItemKey(
    videoId: String?,
    browseId: String?,
    playlistId: String?,
    title: String?,
    index: Int,
): String {
    val id = sequenceOf(videoId, browseId, playlistId, title).firstOrNull { !it.isNullOrBlank() } ?: "item"
    return "$id#$index"
}

/** Shelf key for the Home column. Not [Any.hashCode]: collisions reuse another shelf's clicks. */
fun homeShelfKey(
    title: String,
    subtitle: String?,
    channelId: String?,
    index: Int,
): String {
    val id = sequenceOf(title, subtitle, channelId).firstOrNull { !it.isNullOrBlank() } ?: "shelf"
    return "$id#$index"
}

/** Video id of a quick-access tile that plays a song. Null for albums, artists and playlists. */
fun quickTilePlaybackVideoId(target: QuickTarget): String? =
    when (target) {
        is QuickTarget.LocalSong -> playbackVideoIdForCell(target.song.videoId)
        is QuickTarget.HomeSong -> playbackVideoIdForCell(target.content.videoId)
        else -> null
    }
