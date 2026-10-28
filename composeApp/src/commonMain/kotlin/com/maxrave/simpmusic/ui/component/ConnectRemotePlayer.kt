package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.maxrave.domain.repository.AccountConnectRepository
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.Pause
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.SkipNext
import com.maxrave.simpmusic.ui.icon.SkipPrevious
import com.maxrave.simpmusic.ui.theme.killmonBrand
import com.maxrave.simpmusic.ui.theme.typo
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * El mismo Now Playing de la cuenta, sin audio local. Play, pausa, seek y
 * siguiente/anterior mandan al aparato activo. "Escuchar en este dispositivo" trae el audio aquí.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun ConnectRemotePlayerSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val hide = {
        scope.launch {
            sheetState.hide()
            onDismiss()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Black,
        dragHandle = {},
        modifier = Modifier.fillMaxWidth(),
    ) {
        ConnectRemotePlayer(onDismiss = { hide() }, modifier = Modifier.fillMaxWidth())
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun ConnectRemotePlayer(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val repo = koinInject<AccountConnectRepository>()
    val state by repo.state.collectAsStateWithLifecycle()
    val session = state.session
    val track = session?.currentTrack()
    if (!state.isFollowingRemote() || session == null || track == null) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("No hay reproducción en otro dispositivo", color = MaterialTheme.colorScheme.onSurface)
        }
        return
    }
    val now = rememberConnectNow(session)
    val duration = track.durationMs
    val position = session.estimatedPositionMs(now)
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    val shownFraction =
        if (dragging) {
            dragFraction
        } else if (duration > 0L) {
            (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    val shownMs = if (duration > 0L) (shownFraction * duration).toLong() else position
    val deviceName = connectActiveDeviceName(state)
    val art = track.thumbnail.ifBlank { "https://i.ytimg.com/vi/${track.videoId}/hqdefault.jpg" }
    val brand = killmonBrand()
    val onBrand = if (getPlatform() == Platform.Desktop) Color.White else Color.Black
    Column(
        modifier
            .background(Color.Black)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDismiss) {
                Icon(SimpIcons.Close, contentDescription = "Cerrar", tint = Color.White)
            }
            Text(
                text = "En $deviceName",
                style = typo().labelLarge,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        AsyncImage(
            model = art,
            contentDescription = null,
            placeholder = rememberHolderPainter(),
            error = rememberHolderPainter(),
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .padding(top = 8.dp)
                    .size(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .align(Alignment.CenterHorizontally),
        )
        Text(
            text = track.title.ifBlank { "Killmon" },
            style = typo().titleMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
        Text(
            text = track.artist,
            style = typo().bodyMedium,
            color = Color.White.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Slider(
            value = shownFraction,
            onValueChange = {
                dragging = true
                dragFraction = it.coerceIn(0f, 1f)
            },
            onValueChangeFinished = {
                val target = if (duration > 0L) (dragFraction * duration).toLong() else 0L
                dragging = false
                repo.remoteControl("seek", target)
            },
            enabled = duration > 0L,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatConnectClock(shownMs), color = Color.White.copy(alpha = 0.7f), style = typo().bodySmall)
            Text(
                if (duration > 0L) formatConnectClock(duration) else "",
                color = Color.White.copy(alpha = 0.7f),
                style = typo().bodySmall,
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { repo.remoteControl("previous") }) {
                Icon(SimpIcons.SkipPrevious, contentDescription = "Anterior", tint = Color.White, modifier = Modifier.size(36.dp))
            }
            IconButton(onClick = { repo.remoteControl(if (session.isPlaying) "pause" else "play") }) {
                Icon(
                    imageVector = if (session.isPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                    contentDescription = if (session.isPlaying) "Pausar" else "Reproducir",
                    tint = brand,
                    modifier = Modifier.size(48.dp),
                )
            }
            IconButton(onClick = { repo.remoteControl("next") }) {
                Icon(SimpIcons.SkipNext, contentDescription = "Siguiente", tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }
        Button(
            onClick = { repo.transferTo(state.deviceId) },
            colors = ButtonDefaults.buttonColors(containerColor = brand, contentColor = onBrand),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(CONNECT_LISTEN_HERE)
        }
        Text(
            text = "El audio suena solo en $deviceName",
            style = typo().bodySmall,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 8.dp),
        )
        LazyColumn(Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(max = 240.dp)) {
            itemsIndexed(session.queue) { index, item ->
                val current = index == session.index
                Text(
                    text = "${index + 1}.  ${item.title.ifBlank { item.videoId }}",
                    color = if (current) brand else Color.White.copy(alpha = 0.8f),
                    style = if (current) typo().labelLarge else typo().bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                )
            }
        }
    }
}
