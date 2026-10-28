package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.maxrave.domain.data.model.connect.ConnectSession
import com.maxrave.domain.data.model.connect.ConnectState
import com.maxrave.domain.repository.AccountConnectRepository
import com.maxrave.simpmusic.ui.icon.Pause
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.killmonBrand
import com.maxrave.simpmusic.ui.theme.typo
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.koin.compose.koinInject

internal const val CONNECT_LISTEN_HERE = "Escuchar en este dispositivo"

internal fun formatConnectClock(ms: Long): String {
    val total = (ms / 1000L).coerceAtLeast(0L)
    val minutes = total / 60L
    val seconds = total % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

internal fun connectActiveDeviceName(state: ConnectState): String {
    val id = state.session?.activeDeviceId ?: return "otro dispositivo"
    return state.devices.firstOrNull { it.deviceId == id }?.displayName ?: "otro dispositivo"
}

/**
 * Mini reproductor de la sesión de la cuenta. El audio no sale de aquí:
 * play/pausa manda al dispositivo activo y el botón trae la sesión a este.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun ConnectSessionBar(
    modifier: Modifier = Modifier,
    onOpen: () -> Unit = {},
) {
    val repo = koinInject<AccountConnectRepository>()
    val state by repo.state.collectAsStateWithLifecycle()
    val session = state.session
    val track = session?.currentTrack()
    if (!state.isFollowingRemote() || session == null || track == null) return
    val now = rememberConnectNow(session)
    val position = session.estimatedPositionMs(now)
    val duration = track.durationMs
    val fraction = if (duration > 0L) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
    val deviceName = connectActiveDeviceName(state)
    val art = track.thumbnail.ifBlank { "https://i.ytimg.com/vi/${track.videoId}/hqdefault.jpg" }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onOpen)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = art,
                contentDescription = null,
                placeholder = rememberHolderPainter(),
                error = rememberHolderPainter(),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = track.title.ifBlank { "Killmon" },
                    style = typo().labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track.artist.ifBlank { "En $deviceName" },
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "En $deviceName · ${formatConnectClock(position)}" + if (duration > 0) " / ${formatConnectClock(duration)}" else "",
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = if (session.isPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                contentDescription = if (session.isPlaying) "Pausar" else "Reproducir",
                tint = killmonBrand(),
                modifier =
                    Modifier
                        .size(36.dp)
                        .clickable {
                            if (state.devices.none { it.deviceId == session.activeDeviceId }) {
                                repo.transferTo(state.deviceId)
                            } else {
                                repo.remoteControl(if (session.isPlaying) "pause" else "play")
                            }
                        },
            )
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp)),
            color = killmonBrand(),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Text(
            text = CONNECT_LISTEN_HERE,
            style = typo().labelLarge,
            color = killmonBrand(),
            modifier = Modifier.clickable { repo.transferTo(state.deviceId) },
        )
    }
}

@OptIn(ExperimentalTime::class)
@Composable
internal fun rememberConnectNow(session: ConnectSession): Long {
    var now by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(session.revision, session.isPlaying, session.positionMs, session.index) {
        while (isActive) {
            now = Clock.System.now().toEpochMilliseconds()
            delay(250)
        }
    }
    return now
}
