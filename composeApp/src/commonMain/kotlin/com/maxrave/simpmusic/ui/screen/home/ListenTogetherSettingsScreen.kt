package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun ListenTogetherSettingsScreen(navController: NavController, innerPadding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(innerPadding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("JAM · Killmon Music", style = MaterialTheme.typography.titleLarge)
        Text("Servidor Killmon (AWS): killmon.seriepokemon.online")
        Text("Usa tu cuenta Killmon. Comparte el código de 6 caracteres para invitar a tus amigos.")
        Text("Todos pueden añadir, quitar y ordenar canciones. Solo el anfitrión controla el reloj. Se espera a que todos estén listos; después de 20 segundos la sala continúa automáticamente y avisa al anfitrión.")
        Text("Cada teléfono reproduce directamente desde YouTube y avisa cuando su reproductor está listo. La sala se pierde si se reinicia el servidor.")
        TextButton(onClick = { navController.navigateUp() }) { Text("Volver") }
    }
}
