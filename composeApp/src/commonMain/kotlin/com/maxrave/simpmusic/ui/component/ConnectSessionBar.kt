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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

/**
 * Lo que está sonando en el otro aparato de la cuenta. El audio no sale de aquí:
 * play/pause manda al dispositivo activo y "Aquí" trae la sesión a este.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun ConnectSessionBar(modifier: Modifier = Modifier) {
    val repo = koinInject<AccountConnectRepository>()
    val state by repo.state.collectAsStateWithLifecycle()
    val session = state.session
    val track = session?.currentTrack()
    if (!state.isFollowingRemote() || session == null || track == null) return
    var now by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(session.revision, session.isPlaying, session.positionMs, session.index) {
        while (isActive) {
            now = Clock.System.now().toEpochMilliseconds()
            delay(500)
        }
    }
    val activeDevice = state.devices.firstOrNull { it.deviceId == session.activeDeviceId }
    val deviceName = activeDevice?.displayName ?: "otro dispositivo"
    val position = session.estimatedPositionMs(now)
    val duration = track.durationMs
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = track.title.ifBlank { "Killmon" },
                style = typo().labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "En $deviceName · ${formatClock(position)}" + if (duration > 0) " / ${formatClock(duration)}" else "",
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
                        if (activeDevice == null) {
                            repo.transferTo(state.deviceId)
                        } else {
                            repo.remoteControl(if (session.isPlaying) "pause" else "play")
                        }
                    },
        )
        Text(
            text = "Aquí",
            style = typo().labelLarge,
            color = killmonBrand(),
            modifier = Modifier.clickable { repo.transferTo(state.deviceId) },
        )
    }
}

private fun formatClock(ms: Long): String {
    val total = (ms / 1000L).coerceAtLeast(0L)
    val minutes = total / 60L
    val seconds = total % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
