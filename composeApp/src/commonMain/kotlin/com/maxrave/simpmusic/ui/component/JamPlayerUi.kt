package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.domain.repository.ListenTogetherRepository
import org.koin.compose.koinInject

/**
 * The room line above the player in every Now Playing style. While the room holds everybody at the
 * load gate it carries a spinner and says who is still loading: the phone that is already playing
 * pauses at its second, and without this nothing said why.
 */
@Composable
fun JamPlayerStatus(modifier: Modifier = Modifier) {
    val room by koinInject<ListenTogetherRepository>().room.collectAsStateWithLifecycle()
    if (!room.inRoom) return
    val waiting = room.members.filter { it.userId in room.waitingFor }.joinToString { it.username }
    Row(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (room.showsLoading(false)) {
            CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = when {
                !room.showsLoading(false) -> "En JAM"
                waiting.isNotBlank() -> "En JAM · Cargando · esperando a $waiting"
                else -> "En JAM · Cargando/Sincronizando"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
