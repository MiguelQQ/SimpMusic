package com.maxrave.simpmusic.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.domain.repository.AccountConnectRepository
import org.koin.compose.koinInject

/** Filas de la cuenta dentro de Ajustes → Dispositivos. El QR de login sigue debajo. */
@Composable
fun KillmonDevicesSettings() {
    val repo = koinInject<AccountConnectRepository>()
    val state by repo.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { repo.refresh() }
    if (!state.signedIn) {
        SettingItem(
            title = "Mis dispositivos",
            subtitle = "Inicia sesión en la cuenta Killmon para ver el teléfono y el escritorio.",
            isEnable = false,
        )
        return
    }
    if (state.devices.isEmpty()) {
        SettingItem(
            title = "Buscando dispositivos…",
            subtitle = "Abre Killmon en el otro aparato con la misma cuenta.",
            isEnable = false,
        )
        return
    }
    val track = state.session?.currentTrack()?.title
    state.devices.forEach { device ->
        val kind =
            when (device.platform) {
                "android" -> "Teléfono"
                "ios" -> "iPhone"
                "windows" -> "PC"
                "macos" -> "Mac"
                "linux" -> "Linux"
                else -> "Dispositivo"
            }
        val status =
            when {
                device.isActive && !track.isNullOrBlank() -> "Reproduciendo · $track"
                device.isActive -> "Reproduciendo"
                device.isSelf -> "Este dispositivo · $kind"
                else -> "$kind · En línea · Toca para escuchar aquí"
            }
        SettingItem(
            title = device.displayName,
            subtitle = status,
            onClick = {
                if (!device.isActive && state.session?.queue?.isNotEmpty() == true) repo.transferTo(device.deviceId)
            },
        )
    }
    if (state.devices.none { !it.isSelf }) {
        SettingItem(
            title = "Ningún otro dispositivo en línea",
            subtitle = "Abre Killmon en el teléfono o en el escritorio con esta misma cuenta.",
            isEnable = false,
        )
    }
}
