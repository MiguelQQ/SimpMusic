package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.domain.data.entities.RecentSearchItem
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.searchResult.albums.AlbumsResult
import com.maxrave.domain.data.model.searchResult.artists.ArtistsResult
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.domain.data.model.searchResult.songs.SongsResult
import com.maxrave.domain.data.model.searchResult.songs.Thumbnail
import com.maxrave.domain.data.model.searchResult.videos.VideosResult
import com.maxrave.domain.data.type.SearchResultType
import com.maxrave.domain.utils.connectArtists
import com.maxrave.simpmusic.ui.icon.Add
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.Song_and_artist_name
import simpmusic.composeapp.generated.resources.album
import simpmusic.composeapp.generated.resources.album_and_artist_name
import simpmusic.composeapp.generated.resources.artist
import simpmusic.composeapp.generated.resources.cd_add_to_playlist
import simpmusic.composeapp.generated.resources.cd_remove_recent
import simpmusic.composeapp.generated.resources.podcast
import simpmusic.composeapp.generated.resources.recent_kind_playlist
import simpmusic.composeapp.generated.resources.song

fun SearchResultType.toRecentSearchItem(): RecentSearchItem? =
    when (this) {
        is SongsResult ->
            RecentSearchItem(
                id = "song:$videoId",
                kind = RecentSearchItem.SONG,
                title = title.orEmpty(),
                subtitle = artists?.map { it.name }?.connectArtists().orEmpty(),
                thumbnail = thumbnails?.lastOrNull()?.url,
                explicit = isExplicit == true,
                videoId = videoId,
            )

        is VideosResult ->
            RecentSearchItem(
                id = "song:$videoId",
                kind = RecentSearchItem.SONG,
                title = title.orEmpty(),
                subtitle = artists?.map { it.name }?.connectArtists().orEmpty(),
                thumbnail = thumbnails?.lastOrNull()?.url,
                explicit = false,
                videoId = videoId,
            )

        is ArtistsResult ->
            RecentSearchItem(
                id = "artist:$browseId",
                kind = RecentSearchItem.ARTIST,
                title = artist,
                thumbnail = thumbnails.lastOrNull()?.url,
                browseId = browseId,
            )

        is AlbumsResult ->
            RecentSearchItem(
                id = "album:$browseId",
                kind = RecentSearchItem.ALBUM,
                title = title,
                subtitle = artists.map { it.name }.connectArtists(),
                thumbnail = thumbnails.lastOrNull()?.url,
                explicit = isExplicit,
                browseId = browseId,
            )

        is PlaylistsResult -> {
            val podcast = resultType == "Podcast"
            RecentSearchItem(
                id = "${if (podcast) "podcast" else "playlist"}:$browseId",
                kind = if (podcast) RecentSearchItem.PODCAST else RecentSearchItem.PLAYLIST,
                title = title,
                subtitle = author,
                thumbnail = thumbnails.lastOrNull()?.url,
                browseId = browseId,
            )
        }

        else -> null
    }

fun RecentSearchItem.toPlayableTrack(): Track? {
    val id = videoId ?: return null
    return Track(
        album = null,
        artists = subtitle.takeIf { it.isNotBlank() }?.let { listOf(Artist(id = null, name = it)) },
        duration = null,
        durationSeconds = null,
        isAvailable = true,
        isExplicit = explicit,
        likeStatus = null,
        thumbnails = thumbnail?.let { listOf(Thumbnail(height = 544, url = it, width = 544)) },
        title = title,
        videoId = id,
        videoType = null,
        category = null,
        feedbackTokens = null,
        resultType = null,
    )
}

fun RecentSearchItem.toSongEntity(): SongEntity? {
    val id = videoId ?: return null
    return SongEntity(
        videoId = id,
        artistName = subtitle.takeIf { it.isNotBlank() }?.let { listOf(it) },
        duration = "",
        durationSeconds = 0,
        isAvailable = true,
        isExplicit = explicit,
        likeStatus = "",
        thumbnails = thumbnail,
        title = title,
        videoType = "",
        category = null,
        resultType = null,
    )
}

@Composable
fun RecentSearchItem.kindSubtitle(): String {
    val credit = subtitle
    return when (kind) {
        RecentSearchItem.ARTIST -> stringResource(Res.string.artist)
        RecentSearchItem.ALBUM ->
            if (credit.isBlank()) {
                stringResource(Res.string.album)
            } else {
                stringResource(Res.string.album_and_artist_name, credit)
            }
        RecentSearchItem.PLAYLIST -> stringResource(Res.string.recent_kind_playlist)
        RecentSearchItem.PODCAST -> stringResource(Res.string.podcast)
        else ->
            if (credit.isBlank()) {
                stringResource(Res.string.song)
            } else {
                stringResource(Res.string.Song_and_artist_name, credit)
            }
    }
}

@Composable
fun RecentSearchRow(
    item: RecentSearchItem,
    onClick: () -> Unit,
    onAdd: (() -> Unit)?,
    onRemove: () -> Unit,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val titleColor = if (dark) Color.White else MaterialTheme.colorScheme.onBackground
    val subtitleColor = if (dark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model =
                ImageRequest
                    .Builder(LocalPlatformContext.current)
                    .data(item.thumbnail)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .diskCacheKey(item.thumbnail)
                    .crossfade(true)
                    .build(),
            placeholder = rememberHolderPainter(),
            error = rememberHolderPainter(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(if (item.kind == RecentSearchItem.ARTIST) CircleShape else RoundedCornerShape(4.dp)),
        )
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
        ) {
            Text(
                text = item.title,
                color = titleColor,
                fontWeight = FontWeight.Bold,
                style = typo().titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.explicit) {
                    ExplicitBadge(modifier = Modifier.size(18.dp).padding(end = 4.dp))
                }
                Text(
                    text = item.kindSubtitle(),
                    color = subtitleColor,
                    style = typo().bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (onAdd != null) {
            IconButton(onClick = onAdd) {
                Icon(
                    imageVector = SimpIcons.Add,
                    contentDescription = stringResource(Res.string.cd_add_to_playlist),
                    tint = titleColor,
                )
            }
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = SimpIcons.Close,
                contentDescription = stringResource(Res.string.cd_remove_recent),
                tint = titleColor,
            )
        }
    }
}
