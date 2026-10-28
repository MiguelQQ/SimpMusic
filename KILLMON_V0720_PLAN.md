# Killmon Music 0.7.20 (85) — volumen sin spike, play remoto y el mismo reproductor

## Pedido

Tres fallos de Killmon Connect (una cuenta, un solo aparato con audio, el otro solo controla):

1. Al cambiar de canción el volumen sube al máximo y luego baja al del usuario. Tiene que salir al volumen del usuario desde el primer frame. Desktop y Android, en local y por Connect.
2. Desde Android, pausar el PC activo sí corta el audio. Reanudar no lo vuelve a poner. Pause y resume remotos tienen que oírse en el aparato activo.
3. Con el PC activo, Android pinta otro reproductor (`ConnectRemotePlayer` / `ConnectSessionBar`). Miguel quiere el mismo mini-player y el mismo Now Playing de siempre, alimentados por la sesión remota, con los controles mandados al aparato que suena.

Sobre **0.7.19 / versionCode 84**, rama de trabajo actual. Subir a **0.7.20 / versionCode 85**. User-Agent JAM: `Killmon Music/0.7.20`.

No es JAM. No cambia el contrato de la sala, ni el updater, ni el `applicationId`.

El API de Connect vive en `/workspace/killmon-api` (`src/connect/devices.js`). El bug 2 no se cierra solo en la app: el ack y el playback van en el mismo latido y el servidor acepta el playback viejo. Hay que desplegar ese cambio del API junto con la app. Sin commit en este plan (solo lectura de código).

---

## 1) Volumen: el primer frame no puede ser 1.0

### Causa en Desktop (mpv)

Un handle nuevo nace a volumen 100/100. El comentario del propio adaptador lo dice, y el campo lo confirma:

- `MpvPlayer.kt:637-640` — `masterPercent`, `fadePercent` y `sleepPercent` arrancan en 100.
- `MpvPlayerAdapter.kt:1144-1156` — `applyPlaybackLevels()` existe justo para reescribir ese 100 con el volumen del usuario (`internalVolume`) antes de que el handle se oiga. Los dos niveles tienen que ir en una sola llamada: si no, el `ao-volume` compartido publica 100 y todos los handles lo oyen.

Eso no se cumple en el cambio de pista:

- El camino normal (`MpvPlayerAdapter.kt:1534-1546`) llama a `applyPlaybackLevels()` antes de `loadFile`. Aun así `ao-volume` no existe hasta que el AO se crea. `MpvPlayer.kt:514-519` (`AUDIO_RECONFIG`) lo reconoce: el `setMasterVolume` anterior cae al volumen de software, y el mixer del dispositivo sigue en el valor por defecto (100) hasta que `applyVolume()` (`MpvPlayer.kt:717-724`) vuelve a escribir. Los primeros buffers salen a 100 y después bajan a `internalVolume`. Es el spike y la bajada.
- El cruce es peor. `MpvPlayerAdapter.kt:1984-2014` hace `loadFile` **antes** de `applyPlaybackLevels()`, y el mute llega después del `loadFile`. En Windows todos los handles comparten un solo `ao-volume` (`MpvPlayerAdapter.kt:1123-1130`, `audio-client-name` = `Killmon Music` en `MpvPlayer.kt:243`). El handle de la pista siguiente (precache en `2894-2899`, o el del crossfade) reinicia esa sesión WASAPI a 100 mientras la pista actual sigue sonando. Luego `applyPlaybackLevels()` la devuelve al nivel del usuario.
- El crossfade de escritorio anima el fade a 100 fijo (`MpvPlayerAdapter.kt:2220-2285`, `targetVolume = 100`) confiando en que el master ya es el del usuario. Si el master acaba de saltar a 100, el fade entero se oye al máximo.

El volumen del usuario en Desktop sí está guardado y sí se aplica al arrancar (`JvmMediaPlayerHandlerImpl.kt:506`, `dataStoreManager.playerVolume`) y al mover el slider (`1116-1118`). El spike no es el slider: es el handle nuevo a 100 hasta que el master se reescribe.

### Causa en Android (ExoPlayer)

Cada cambio de ítem pasa por `onMediaItemTransition` → `mayBeNormalizeVolume()` (`MediaServiceHandlerImpl.kt:2828-2839`).

Con la normalización apagada (el caso normal) esa función pone el player a 1 y se va:

```kotlin
// MediaServiceHandlerImpl.kt:2444-2454
if (!normalizeVolume) {
    loudnessEnhancer?.enabled = false
    ...
    player.volume = 1f
    return
}
```

El setter del adaptador (`CrossfadeExoPlayerAdapter.kt:1468-1475`) no solo sube el `ExoPlayer` actual: escribe `internalVolume = 1f` y avisa a la UI. A partir de ahí el nivel del usuario ya no existe en el adaptador.

El orden del cambio de pista lo empeora (`CrossfadeExoPlayerAdapter.kt:1704-1784`):

1. Se notifica `onMediaItemTransition` (ahí el handler pone volumen 1 y pisa `internalVolume`).
2. El player nuevo, o el precargado, se promueve. El precache se crea con el volumen por defecto de ExoPlayer, que es 1, y nunca se le estampa el nivel del usuario (`3169-3206`, solo `prepare()`).
3. `player.volume = internalVolume` aplica el 1 que acaba de quedar escrito.

Por eso el primer audio de la pista nueva sale a tope. En Desktop la corrección llega en `AUDIO_RECONFIG` / `applyPlaybackLevels`. En Android no hay esa corrección: `PlayerEvent.UpdateVolume` está vacío (`MediaServiceHandlerImpl.kt:1048`), así que el slider de la cápsula no mueve el `ExoPlayer`, y `playerVolume` del DataStore solo se lee en el arranque de Desktop. El salto se oye igual en local y por Connect porque los dos pasan por el mismo `onMediaItemTransition` (el `seekTo` de next/previous del puente también).

La fila de volumen de Now Playing en Android es otra cosa: `STREAM_MUSIC` (`DeviceVolumeController.android.kt:49-53`). No hay que tocarla al cambiar de pista. El spike a corregir es la ganancia del player (`internalVolume` / `ao-volume`), que tiene que ser la del usuario antes del primer sample.

### Arreglo

Una sola regla en los dos backends: **ningún handle audible puede existir a ganancia 1 si el usuario no está en 1.** El nivel se estampa en el mismo momento en que el handle puede emitir, no en un evento posterior.

Desktop:

- En `MpvPlayer.create`, fijar `volume` y, si la opción existe antes de `mpv_initialize`, el equivalente de master al porcentaje ya conocido. Si `ao-volume` aún no se puede escribir, el software volume del handle nuevo tiene que quedar en 0 hasta `AUDIO_RECONFIG`, y ese evento aplica master y fade juntos (`setVolumeLevels`), nunca un frame a 100.
- `triggerCrossfadeTransition` y `triggerPrecachingInternal`: `applyPlaybackLevels()` (y mute o fade 0 en el que todavía no es el actual) **antes** de `loadFile`. El handle que comparte `ao-volume` no puede inicializar el mixer.
- El fade del crossfade sigue siendo 0..100 sobre el software volume. No se reescribe el master a 100 en ningún paso.

Android:

- `mayBeNormalizeVolume()` con la normalización apagada no asigna `player.volume`. Quitar el `= 1f`. Si hace falta apagar el `LoudnessEnhancer`, se apaga y se deja `internalVolume` como está.
- Al crear cada `ExoPlayer` (`createExoPlayerInstance`) y al precargar, poner `volume = internalVolume` antes de `prepare()` / `play()`. En el camino de `loadTrackInternal`, no disparar un reset a 1 desde el listener que corre antes de esa asignación.
- `MediaServiceHandlerImpl` aplica `PlayerEvent.UpdateVolume` igual que Desktop (`player.volume = newVolume`) y, al crear el handler, lee `playerVolume` del DataStore una vez, como `JvmMediaPlayerHandlerImpl.kt:506`. Así el primer frame y el slider son el mismo número.
- No escribir `STREAM_MUSIC` al cambiar de pista.

Connect no lleva volumen en `ConnectPlayback` (`ConnectSession.kt:15-20`). No hace falta: el aparato que suena usa su propio `playerVolume`. El seguidor no abre stream (`silenceLocal`), así que no tiene spike. El activo, al hacer next/previous/transfer, usa los players de arriba y queda cubierto.

---

## 2) Play remoto: la pausa llega, el resume no despierta a mpv

### Qué sí funciona

Android manda `remoteControl("pause"|"play")` (`ConnectSessionBar.kt:130-135`, `ConnectRemotePlayer.kt:193`). El servidor pone `isPlaying` y deja un comando `control` solo para el aparato activo (`devices.js:294-348`, test `pausa remota` en `test/connect.test.js`). El PC lo aplica en `ConnectPlaybackBridge.applyControl` (`224-247`).

La pausa se oye porque `player.pause()` acaba llamando a mpv (`MpvPlayerAdapter.kt:358-364`) y el latido siguiente publica `is_playing: false`.

### Causa

Hay dos agujeros, y con uno solo el resume se pierde.

**El `play()` del estado PLAYING no despierta el backend.**

```kotlin
// MpvPlayerAdapter.kt:335-338
InternalState.PLAYING -> {
    internalPlayWhenReady = true
    cachedIsLoading = false
}
```

Lo mismo en Android (`CrossfadeExoPlayerAdapter.kt:680-683`). Si la máquina ya dice PLAYING, `play()` no llama a `MpvPlayer.play()` ni a `ExoPlayer.play()`.

Ese estado queda mentiroso después de una pausa. `playing()` fuerza PLAYING sin mirar el intent (`MpvPlayerAdapter.kt:1708-1716`). `paused()` solo cambia el enum si ya estaba PLAYING (`1723-1728`) y no vuelve a leer el flag de mpv. Un `PLAYBACK_RESTART` (el propio comentario en `MpvPlayer.kt:522-526` dice que también sale en cada seek) puede dejar `internalState == PLAYING` con el handle en `pause=yes` y `internalPlayWhenReady == false`.

El comando remoto entra por `player.play()`, no por `playWhenReady = true` (`ConnectPlaybackBridge.kt:234-237`). Cae en la rama PLAYING: pone `internalPlayWhenReady = true` y no le dice nada a mpv. El audio sigue parado.

`localPlayback()` publica `isPlaying = playWhenReady` (`ConnectPlaybackBridge.kt:283` y `302`), no el pause real de mpv. El servidor y el teléfono pasan a "sonando". El botón muestra pausa. El PC sigue en silencio. Por eso parece que el pause sincroniza y el play no.

Además `play()` y `pause()` son `launch` en el hilo único `Desktop-Player-Thread` (`DesktopPlayerModule.kt:21-29`), el mismo scope del puente (`ConnectModule.kt:23`). `applyControl` lee el player en el `withContext(Main)` justo después de encolar `play()` (`ConnectPlaybackBridge.kt:229-247` y `157-163`). Si el cuerpo de `play()` todavía no corrió, el ack se va con `is_playing: false`.

**El servidor se come el playback viejo en el mismo request del ack.**

`devices.js:206-258`: `ack()` sube `ackedControl` y **después** mira `controlPending`. En el request que trae `ack_control` y `playback`, el pendiente ya es falso, así que `writeSession` guarda el `is_playing` del snapshot. El comando queda confirmado y no se reintenta. El guard de 0.7.18 (`el latido viejo no pisa una pausa`) no cubre este caso: el test (`connect.test.js:107-132`) manda el playback viejo **sin** ack, y el ack **con** el `is_playing` ya correcto.

La pausa sobrevive porque mpv sí recibe `pause` y el latido de después publica false. El play no, porque mpv no recibió `play` y el comando ya está ack.

`wasPlaying = session.isPlaying` (`ConnectPlaybackBridge.kt:160`) pisa el `wasPlaying = true` de la rama play (`236`). No es la causa del silencio, pero deja el flanco del paso siguiente (`97-104`) mirando un valor que no es el del player.

### Arreglo

App:

- En los dos adaptadores, la rama PLAYING de `play()` llama al backend (`MpvPlayer.play()` / `currentPlayer.play()`). Es idempotente si ya sonaba, y es lo único que despierta un handle pausado con el enum mal.
- `playing()` no pasa a PLAYING si `internalPlayWhenReady == false`. Quien manda es el intent, no un evento atrasado.
- `applyControl` aplica el intent de forma síncrona: `playWhenReady = true` en play y `playWhenReady = false` en pause (el setter ya llama a `play()`/`pause()`). En play, `audioFocusLost = false`, igual que JAM (`ListenTogetherPlaybackBridge.kt:117`), para que un foco viejo no se trague el resume en Android cuando el activo es el teléfono.
- No publicar playback en el mismo `exchange` que el `ack_control`. Orden: aplicar, esperar a que `playWhenReady` (y, en Desktop, el estado PAUSED/PLAYING) coincida con la acción, ack, y el playback del latido siguiente. El `isPlaying` publicado tiene que ser el del backend después de aplicar, no el leído antes del `launch`.
- `wasPlaying` después de un control sale del player, no de `session.isPlaying`.

API (`devices.js`), y test nuevo en `test/connect.test.js`:

- Si el body trae `ack_control` de la revisión pendiente, no dejar que `playback.is_playing` contradiga la acción que se acaba de confirmar. Opción mínima: mientras el request es el ack, ignorar `playback` (la posición buena llega en el latido siguiente, que ya no tiene comando pendiente). No volver a invertir el orden "ack y luego mirar el pendiente" sin ese corte: si no, el snapshot falso sigue ganando.

Sin el cambio del API, un cliente viejo puede seguir pisando el play. La app nueva no debe mandar ese snapshot; el servidor nuevo no debe creerlo.

---

## 3) El mismo mini-player y el mismo Now Playing

### Causa

`App.kt` trata "estoy siguiendo a otro aparato" como otra UI.

- `connectFollows` (`App.kt:176`) es `connectState.isFollowingRemote()`.
- Teléfono, barra de abajo: si `connectFollows`, no se monta `MiniPlayer` (`591`, `610-617`). En su lugar, `ConnectSessionBar`.
- Tablet: la cápsula también se cambia por `ConnectSessionBar` (`686-693`).
- Now Playing de tablet landscape: `ConnectRemotePlayer` (`911-915`) en vez de `NowPlayingScreenContent` (`918-927`).
- Teléfono: la hoja es `ConnectRemotePlayerSheet` (`936-937`) en vez de `NowPlayingScreen` (`939-946`).

`ConnectRemotePlayer.kt` es otra columna (carátula 220 dp, slider, tres botones, cola en texto). No es el Now Playing del estilo que el usuario tenga puesto (Spotify, M3 o Apple Music). `ConnectSessionBar` tampoco es la cápsula ni la barra de 72 dp.

El puente ya apaga el audio local una vez (`ConnectPlaybackBridge.kt:184-198`, `silenceLocal`) y copia cola e índice al handler (`254-260`, `mirrorQueue`). Al vaciar los media items, `onMediaItemTransition(null)` borra el now-playing local. Por eso la UI de siempre se queda vacía y se sustituyó por el reproductor remoto. El audio no tiene que volver; los datos de la pantalla sí.

### Arquitectura

Un solo sitio pinta. Dos sitios producen el estado.

```text
ConnectSession (otro aparato suena)
        │
        ▼
SharedViewModel  ──proyecta──►  el mismo ControlState / now playing / progreso
        │                       que ya leen MiniPlayer y NowPlaying*
        │
        └─ onUIEvent de transporte ──► remoteControl(play|pause|seek|next|previous)
                                      si isFollowingRemote()
                                      si no, MediaPlayerHandler como hoy

MediaPlayerHandler (este aparato suena)
        │
        └─ igual que hoy, y el player mantiene el volumen del apartado 1
```

Reglas:

- `App.kt` deja de bifurcar. `MiniPlayer` y `NowPlayingScreen` / `NowPlayingScreenContent` se muestran con la misma condición de siempre (`isShowMiniPlayer`, hoja, panel de tablet). `connectFollows` no los esconde.
- La proyección vive en `SharedViewModel`, que ya es el dueño de `controllerState` (`SharedViewModel.kt:235-248`) y de `onUIEvent` (`1116` es el volumen; el transporte pasa por `mediaPlayerHandler.onPlayerEvent`). Con `state.isFollowingRemote()`:
  - título, artista, carátula y duración salen de `ConnectSession.currentTrack()` (`ConnectSession.kt:31`);
  - play/pausa de `session.isPlaying`;
  - posición de `estimatedPositionMs` (el tick de 250 ms que hoy está en `rememberConnectNow`, `ConnectSessionBar.kt:154-164`), escrita al mismo flujo de progreso que ya consume el slider;
  - la cola sigue siendo la que `mirrorQueue` deja en `queueData`. No hace falta otra lista.
- Transportes que se redirigen, y solo esos: play/pausa, next, previous, seek (slider y ±10 s). Van a `AccountConnectRepository.remoteControl` con las acciones que el servidor ya acepta (`play`, `pause`, `seek`, `next`, `previous`). No pasan por `player.play()` local: el seguidor no tiene stream (`silenceLocal` se queda).
- Volumen, like, letra, cola visual, estilo de Now Playing y cápsula de Desktop no cambian de widget. El volumen sigue siendo el de **este** aparato (apartado 1). No se añade `set_volume` al protocolo.
- Una línea dentro del reproductor que ya existe, no un reproductor nuevo: "En {displayName}" con `connectActiveDeviceName` (`ConnectSessionBar.kt:53-56`). "Escuchar en este dispositivo" sigue en Ajustes → Dispositivos (`KillmonDevicesSettings.kt:50-55`) y en la hoja de salida (`AppleMusicOutputSheet.kt:209`). Se puede repetir como acción de esa hoja; no como pantalla distinta.
- `ConnectRemotePlayer`, `ConnectRemotePlayerSheet` y el uso de `ConnectSessionBar` como barra de reproducción se dejan de montar. El formato de reloj y el tick de posición se mueven al view model (o se quedan como funciones internas usadas por la proyección). No queda una segunda composición de carátula y controles.

`silenceLocal` puede seguir vaciando MPV/ExoPlayer. No puede ser la fuente de la UI cuando hay sesión remota: la proyección va encima y gana mientras `isFollowingRemote()` sea true. Al transferir a este aparato, la proyección se apaga y el handler local (ya cargado por `applyTransfer`, `ConnectPlaybackBridge.kt:201-221`) vuelve a ser la fuente. El volumen de esa carga es el del apartado 1, desde el primer frame.

---

## Archivos

| Archivo | Cambio |
| --- | --- |
| `core/media/media-jvm/.../mpv/MpvPlayer.kt` | Handle nuevo sin frame a 100. `AUDIO_RECONFIG` aplica master y fade juntos. |
| `core/media/media-jvm/.../mpv/MpvPlayerAdapter.kt` | Niveles antes de `loadFile` en carga, precache y crossfade. `play()` en PLAYING despierta mpv. `playing()` respeta `internalPlayWhenReady`. |
| `core/media/media3/.../CrossfadeExoPlayerAdapter.kt` | Volumen del usuario al crear y al precargar, antes de `prepare`/`play`. `play()` en PLAYING llama a ExoPlayer. |
| `core/data/.../androidMain/.../MediaServiceHandlerImpl.kt` | Quitar `player.volume = 1f`. Aplicar `UpdateVolume` y el `playerVolume` guardado al arrancar. |
| `core/data/.../killmon/ConnectPlaybackBridge.kt` | Play/pause por `playWhenReady`. Ack sin playback adelantado. `wasPlaying` desde el player. |
| `core/data/.../killmon/AccountConnectRepositoryImpl.kt` | Solo si hace falta separar el ack del report. La regla puede quedarse en el puente. |
| `/workspace/killmon-api/src/connect/devices.js` | El ack no acepta un `is_playing` que deshaga el control recién confirmado. |
| `/workspace/killmon-api/test/connect.test.js` | El caso nuevo del apartado Tests. |
| `composeApp/.../viewModel/SharedViewModel.kt` | Proyección de la sesión remota y desvío de play/pausa/seek/next/previous. |
| `composeApp/.../App.kt` | Quitar la rama `connectFollows` que cambia de reproductor. |
| `composeApp/.../ui/component/ConnectRemotePlayer.kt` | Deja de ser la pantalla. Tick y reloj se reutilizan o se mueven. |
| `composeApp/.../ui/component/ConnectSessionBar.kt` | Deja de sustituir al mini-player. La línea "En {nombre}" vive en el mini-player / Now Playing. |
| `composeApp/.../ui/screen/MiniPlayer.kt` y Now Playing (`NowPlayingScreen.kt`, contenidos en `ui/screen/player/content/`) | Leen el estado proyectado. No hace falta otro layout. Subtítulo "En {dispositivo}" cuando se sigue a otro. |
| `gradle/libs.versions.toml` | `version-name = "0.7.20"`, `version-code = "85"`. |
| `core/service/listenTogether/.../ListenTogetherSession.kt:121` | `agent = "Killmon Music/0.7.20"`. |
| `core/service/listenTogether/.../UserAgentHandshakeTest.kt:54` y `:62` | El mismo string. |

`androidApp/build.gradle.kts:26-31`, `vivoAndroidApp/build.gradle.kts` y `composeApp/build.gradle.kts` ya leen el toml. No hace falta un segundo número suelto.

Fuera de esto: no renombrar el repo, no tocar la URL del updater, no cambiar JAM salvo el User-Agent y el `audioFocusLost = false` que el puente de Connect copia del de JAM.

---

## Pasos

1. Volumen Desktop. Reproducir en un test de la política (abajo) el orden "niveles y mute antes de loadFile". Implementar en `MpvPlayer` / `MpvPlayerAdapter`. Un cambio de pista con volumen 0.3 no escribe 100 en el master en ningún momento observable.
2. Volumen Android. Quitar el `= 1f`. Estampar `internalVolume` al construir y al precargar. Conectar `UpdateVolume` y la lectura inicial del DataStore. El listener de transición no puede correr antes y dejar el gain en 1.
3. Play remoto en los adaptadores y en el puente. Ack separado del snapshot. `wasPlaying` desde el player.
4. API: ignorar playback en el request que confirma el control, con el test de Node en rojo antes del cambio.
5. Proyección en `SharedViewModel` y quitar la bifurcación de `App.kt`. Los controles del mini-player y de Now Playing, en los estilos que ya existen, llaman a `remoteControl` mientras `isFollowingRemote()`.
6. Bump 0.7.20 / 85 y el User-Agent en los dos archivos de JAM.
7. Tests de abajo. Compilar `:composeApp:compileKotlinJvm`, `:desktopApp:compileKotlinJvm` y el test de dominio. El APK release solo si hay memoria.

---

## Tests unitarios

Dominio, junto a `ConnectSessionTest.kt`:

- La proyección de una `ConnectSession` (función pura nueva, por ejemplo `ConnectSession.toDisplayPlayback(nowMs)`) da título, artista, carátula, `isPlaying`, posición estimada y duración. En pausa la posición no avanza. En play avanza y se capa a la duración. Esto ya está a medias en `estimatedPositionMs`; el test nuevo fija el objeto que va a pintar el view model, para que la UI no vuelva a leer otro modelo.
- `publishesLocalAudio` sigue en false para el seguidor (`ConnectSession.kt:133-137`). El seguidor no es quien manda el volumen ni el `is_playing`.

Puente (extraer la decisión a una función pura testeable, sin MPV):

- Un comando `play` con el player en "estado PLAYING pero pause real" produce la intención `playWhenReady = true` **y** una llamada al backend. No basta con el flag.
- El report que acompaña al ack de un control no lleva playback. El playback del paso siguiente lleva el `isPlaying` de después de aplicar.
- `wasPlaying` tras play queda true aunque `session.isPlaying` del snapshot venga false.

Volumen (función pura o el setter si se puede fingir):

- Con volumen de usuario 0.3 y normalización apagada, una transición no produce el valor 1. El valor que se estampa en un handle nuevo es 0.3, o 0 si todavía no puede sonar, nunca 1.
- Orden de precache/crossfade: la lista de operaciones del handle nuevo es `applyLevels` (y mute/fade 0 si no es el actual) y después `loadFile`. Un test del colaborador que hoy está en `MpvPlayerAdapter.kt:1984-2014` falla si `loadFile` va antes.

API, `test/connect.test.js`:

- Activo en play. El otro manda `play` después de una pausa (o manda `play` con el activo ya en pausa). El presence del activo llega con `ack_control` de esa revisión y `playback.is_playing: false` (el snapshot viejo). La sesión tiene que seguir `is_playing: true` y el comando no puede reaparecer como pause. El presence siguiente, ya sin comando, con `is_playing: true`, se guarda.
- El caso viejo se mantiene: un presence **sin** ack no pisa una pausa pendiente.

JAM:

- `UserAgentHandshakeTest` espera `Killmon Music/0.7.20` en el POST y en el WebSocket.

---

## Versión

`gradle/libs.versions.toml`: `version-name = "0.7.20"`, `version-code = "85"`.

`ListenTogetherSession.kt` línea del `UserAgent`: `Killmon Music/0.7.20`.

`UserAgentHandshakeTest.kt` las dos aserciones de `user-agent`, el mismo string.

---

## Guion de Pruebas QA para Grokbot

Misma cuenta Killmon en el teléfono (Android) y en el PC (Desktop). API de Connect desplegado con el arreglo del ack. Volumen del usuario bajado a la mitad antes de cada caso (en el PC, el altavoz de la cápsula; en el teléfono, la fila de volumen de Now Playing no hace falta tocarla: el salto a vigilar es el del audio, no el del sistema). Anotar 0.7.20 (85).

1. **Volumen en el PC, local.** Inicio → cápsula de abajo (mini-player Desktop). Icono de altavoz, popup, dejar el número cerca de 30. Reproducir una canción de Inicio. Al terminar, o con Siguiente en la cápsula, la pista nueva entra a ese mismo nivel. No hay un golpe a tope ni una bajada después. Repetir con Anterior.
2. **Volumen en el teléfono, local.** Inicio → barra mini-player de abajo (72 dp, la de siempre). Abrir Now Playing tocando la barra. Bajar el volumen del reproductor si el estilo muestra slider de app; si solo está el de `STREAM_MUSIC`, bajar ese y además dejar el del PC en 30 para el caso 3. Siguiente dentro de Now Playing: la pista nueva no entra más fuerte que la anterior y no baja sola un segundo después.
3. **Volumen por Connect.** PC es el que suena (Ajustes → Dispositivos: el PC sale como Reproduciendo). En el teléfono, Siguiente del mismo mini-player de siempre. El audio del PC cambia de pista sin el golpe a volumen máximo. Repetir con el teléfono como aparato activo y el Siguiente hecho en el PC: el teléfono no pega el golpe.
4. **Pausa y resume Android → PC.** PC sonando. En el teléfono, el botón de pausa del mini-player (el de la barra de siempre, no una barra gris distinta). El PC calla en alrededor de un segundo y el icono pasa a play en los dos. Pulsar play en el teléfono, en el mini-player y otra vez desde Now Playing (tocar la barra abre la hoja de Now Playing de siempre). El PC vuelve a sonar desde el mismo sitio. El icono queda en pausa solo mientras hay audio. Probar también el play del centro de Now Playing, no solo el de la barra.
5. **Seek y saltos mientras el PC suena.** En el Now Playing del teléfono, arrastrar la barra de progreso, Siguiente y Anterior. El PC obedece. La carátula, el título y el reloj del teléfono son los de esa pista, los mismos campos que cuando el teléfono es el que suena.
6. **La UI es la misma.** Con el PC activo, el teléfono no muestra la columna negra de `ConnectRemotePlayer` (carátula pequeña centrada, botón "Escuchar en este dispositivo" como botón ancho debajo de los tres iconos, cola numerada en texto). Muestra el mini-player normal y, al tocarlo, el Now Playing del estilo elegido en Ajustes → Interfaz (Spotify, M3 o Apple Music). Se puede leer "En {nombre del PC}". En el PC, mientras el teléfono es el activo, la cápsula de siempre muestra la misma canción y "En {nombre del teléfono}", no una tarjeta `ConnectSessionBar` distinta. Play, pausa, seek y skip de esa cápsula mueven el teléfono y el PC no suena.
7. **Traer el audio.** Ajustes → Dispositivos (sección Dispositivos, encima del QR). Tocar la fila del aparato que no está reproduciendo. El audio pasa a ese aparato, cerca de la misma posición, al volumen que ese aparato ya tenía, sin spike. El otro deja de sonar y sigue mostrando el mismo reproductor, ahora como control remoto.
8. **JAM no se mezcla.** Entrar en una sala JAM. Connect no sustituye la cola de la sala y el User-Agent de la sala es `Killmon Music/0.7.20`.
9. **Versión.** La pantalla de ajustes o el log de la app dice 0.7.20. `versionCode` 85.
10. **Salida de audio → Killmon lista los dos.** PC sonando una canción con título reconocible. En el PC, Now Playing → auriculares, hoja «Salida de audio». En la sección **Killmon** (la lista hace scroll; la tarjeta ya no recorta la segunda fila) están el PC y el teléfono. El activo lleva check y el estado `Reproduciendo · <canción>` (en pausa: `En pausa · <canción>`). El otro sale como `En línea` o `Este dispositivo`, sin check. No vale que solo se vea `Windows · Miguel · Reproduciendo · …`.
11. **Transferir desde el picker del PC.** En esa sección, tocar la fila del teléfono. El check pasa al teléfono. El audio deja el PC y suena solo en el teléfono, la misma canción y cerca de la misma posición, al volumen propio del teléfono. El PC queda de controlador: el mismo mini-player y Now Playing, línea `En {nombre del teléfono}`, y play, pausa, siguiente y seek de esa cápsula mueven el teléfono.
12. **Lo mismo en el teléfono.** Now Playing del teléfono → «Salida de audio» → **Killmon**, y también Ajustes → Dispositivos: las dos filas, check solo en el que suena. Tocar el PC. El audio vuelve al PC en la misma canción y posición. El check queda en el PC. El teléfono no suena y su mini-player sigue esa pista.
