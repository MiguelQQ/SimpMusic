package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.maxrave.common.Config
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDynamicPlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PodcastDestination
import com.maxrave.simpmusic.ui.screen.home.QuickTarget
import com.maxrave.simpmusic.ui.screen.home.QuickTile
import com.maxrave.simpmusic.ui.screen.home.quickTilePlaybackVideoId
import com.maxrave.simpmusic.ui.screen.library.LibraryDynamicPlaylistType
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.HomeViewModel

private val TileHeight = 56.dp
private val TileShape = RoundedCornerShape(6.dp)

/**
 * Spotify's quick-access grid: two columns of small tiles, the square cover on the left and the
 * title beside it on a dark rounded card. Songs play like a Quick picks tap; everything else opens
 * its page. [tiles] has an even size (see `buildQuickTiles`).
 */
@Composable
fun HomeQuickAccessGrid(
    tiles: List<QuickTile>,
    navController: NavController,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
) {
    if (tiles.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { tile ->
                    key(tile.key) {
                        QuickTileCard(
                            tile = tile,
                            modifier = Modifier.weight(1f),
                            onClick = { openQuickTile(tile.target, navController, viewModel) },
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuickTileCard(
    tile: QuickTile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .height(TileHeight)
                .clip(TileShape)
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.10f))
                .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model =
                ImageRequest
                    .Builder(LocalPlatformContext.current)
                    .data(tile.thumbnail)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .diskCacheKey(tile.thumbnail)
                    .boundedDecode(rememberThumbPx(TileHeight))
                    .build(),
            placeholder = rememberHolderPainter(),
            error = rememberHolderPainter(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(TileHeight).clip(if (tile.round) CircleShape else RoundedCornerShape(0.dp)),
        )
        Text(
            text = tile.title,
            style = typo().labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
        )
    }
}

private fun openQuickTile(
    target: QuickTarget,
    navController: NavController,
    viewModel: HomeViewModel,
) {
    when (target) {
        is QuickTarget.Album -> navController.navigate(AlbumDestination(target.browseId))
        is QuickTarget.Artist -> navController.navigate(ArtistDestination(channelId = target.channelId))
        is QuickTarget.Playlist -> navController.navigate(PlaylistDestination(playlistId = target.id))
        is QuickTarget.Podcast -> navController.navigate(PodcastDestination(podcastId = target.id))
        QuickTarget.Favorites ->
            navController.navigate(
                LibraryDynamicPlaylistDestination(type = LibraryDynamicPlaylistType.Favorite.toStringParams()),
            )
        is QuickTarget.LocalSong -> {
            val id = quickTilePlaybackVideoId(target) ?: return
            playSingle(viewModel, target.song.toTrack().copy(videoId = id))
        }
        is QuickTarget.HomeSong -> {
            val id = quickTilePlaybackVideoId(target) ?: return
            playSingle(viewModel, target.content.toTrack().copy(videoId = id))
        }
    }
}

private fun playSingle(
    viewModel: HomeViewModel,
    track: com.maxrave.domain.data.model.browse.album.Track,
) {
    viewModel.setQueueData(
        QueueData.Data(
            listTracks = arrayListOf(track),
            firstPlayedTrack = track,
            playlistId = "RDAMVM${track.videoId}",
            playlistName = "\"${track.title}\" Radio",
            playlistType = PlaylistType.RADIO,
            continuation = null,
        ),
    )
    viewModel.loadMediaItem(track, type = Config.SONG_CLICK)
}
