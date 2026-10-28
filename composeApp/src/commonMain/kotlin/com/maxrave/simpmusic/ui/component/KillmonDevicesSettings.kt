package com.maxrave.simpmusic.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.domain.data.model.connect.connectPickerRows
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
    connectPickerRows(state.devices, state.session).forEach { device ->
        SettingItem(
            title = device.displayName,
            subtitle = device.status,
            onClick = {
                if (!device.selected && state.session?.queue?.isNotEmpty() == true) repo.transferTo(device.deviceId)
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
