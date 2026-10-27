package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.maxrave.domain.data.player.JamPlaybackRouter
import com.maxrave.domain.repository.ListenTogetherRepository
import org.koin.compose.koinInject

@Composable
fun JamPlayerStatus(modifier: Modifier = Modifier) {
    val room by koinInject<ListenTogetherRepository>().room.collectAsStateWithLifecycle()
    if (room.inRoom) Text(
        text = if (room.loading) "En JAM · Cargando/Sincronizando" else "En JAM",
        modifier = modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

/** The shared queue body used by the normal player's sheet and embedded queue tab. */
@Composable
fun JamPlayerQueue(modifier: Modifier = Modifier) {
    val room by koinInject<ListenTogetherRepository>().room.collectAsStateWithLifecycle()
    Column(modifier.fillMaxWidth()) {
        JamPlayerStatus()
        Text("Cola compartida · añade canciones desde búsqueda o biblioteca", Modifier.padding(12.dp))
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(room.queue, key = { index, _ -> room.queueIds.getOrNull(index) ?: index.toLong() }) { index, track ->
                Column(Modifier.fillMaxWidth().padding(8.dp)) {
                    Row(Modifier.clickable { JamPlaybackRouter.actions?.queue("play_index", index) }) {
                        AsyncImage(
                            model = track.thumbnail.ifBlank { "https://i.ytimg.com/vi/${track.id}/hqdefault.jpg" },
                            contentDescription = track.title,
                            placeholder = rememberHolderPainter(), error = rememberHolderPainter(),
                            contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp),
                        )
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text((if (track.id == room.currentTrack?.id) "▶ " else "") + track.title, maxLines = 1)
                            Text("${track.artist} · ${room.queueContributors.getOrNull(index).orEmpty()}", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
                    }
                    Row {
                        TextButton(enabled = index > 0, onClick = { JamPlaybackRouter.actions?.queue("queue_move", index, index - 1) }) { Text("Subir") }
                        TextButton(enabled = index < room.queue.lastIndex, onClick = { JamPlaybackRouter.actions?.queue("queue_move", index, index + 1) }) { Text("Bajar") }
                        TextButton(onClick = { JamPlaybackRouter.actions?.queue("queue_remove", index) }) { Text("Quitar") }
                    }
                }
            }
        }
    }
}
