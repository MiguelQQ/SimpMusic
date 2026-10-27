# Killmon Music 0.7.15 (80) — JAM: sincronía sin eco, cola normal, letra, video, foco y continuidad

## Alcance

Base: 0.7.14 ya commiteado en `killmon-0.7.3-auto` (app + submódulo `core`). Sin commit ni push en app/core. Se mantienen el updater de 0.7.14, YouTube directo en cada teléfono y el servidor de sala único `https://killmon.seriepokemon.online`. `gradle/libs.versions.toml` pasa a 0.7.15 / 80 y el User-Agent de la sesión a `Killmon Music/0.7.15`.

**Backend** (`/workspace/killmon-api`, `src/jam/rooms.js`): SÍ cambió (ítems 4 y 6). Commit local `47f983e` ("JAM: manual play inserts behind the current track…", autor `Killmon <killmon@local>`). **Hay que desplegarlo**; sin él la app 0.7.15 sigue funcionando, pero el ítem 6 no se corrige y el video (ítem 4) no se propaga a otros teléfonos (el servidor descartaba el campo `isVideo`).

> Honestidad sobre la evidencia: no hay teléfono físico aquí. Las causas se establecieron leyendo el código y las pruebas reproducen la secuencia de mensajes real del servidor y las decisiones del cliente como funciones puras; el puente Android en sí (corrutinas + adaptador Media3) no se ha ejecutado en dispositivo. El ítem 3 (letra) tiene causas concretas encontradas pero la coincidencia exacta con el caso del dueño debe confirmarse en el guion QA.

---

## 1) Sync: la canción no se reinicia ni repite el intro; carga visible

### Causa raíz (3 mecanismos que se sumaban)

1. **Eco de transporte hacia el servidor.** `ListenTogetherPlaybackBridge` convertía `MediaPlayerListener.onIsPlayingChanged` y `onSeeked` en comandos `play` / `pause` / `seek` de la sala, protegidos por un flag `applying` puesto *alrededor* de las llamadas propias. Pero `CrossfadeExoPlayerAdapter` ejecuta `pause()`, `playWhenReady=`, `seekTo()` y `setMediaItem()` dentro de `coroutineScope.launch { … }` (`CrossfadeExoPlayerAdapter.kt:685` pause, `:758` seekTo, `:917` setMediaItem, `:1409` playWhenReady), y el `onPositionDiscontinuity` → `onSeeked` (`:1976`) llega cuando el flag ya está bajado. El eco de NUESTRO pause/play/seek se mandaba al servidor como si fuera una persona:
   - `pause` durante la puerta de carga → el servidor pone `wantPlay=false` (`rooms.js` case `pause`, fase `loading`) y la sala **queda pausada** al liberarse (reproducido por el test "el eco de un pause durante la puerta…").
   - `play` con la posición local atrasada → `room.position = msg.position` (`rooms.js` case `play`) **pisa el reloj de la sala** y el anfitrión salta atrás: la canción se repite.
   - `seek` (incluidos los `SEEK_ADJUSTMENT` de webm/cue points) → `beginGate` nuevo → todos pausan y recargan: el ciclo "se pausa y reanuda como cargando".
2. **Seeks que se alimentan entre sí.** El bucle de seguimiento de 150 ms hacía `seekTo` a cualquier deriva >500 ms sin límite; un seek sobre un stream remoto provoca BUFFERING, y el listener convertía ese BUFFERING en `buffering` → otra puerta para todos.
3. **Recargas evitables.** `setMediaItem` + `prepare()` + `seekTo(target)` eran tres tareas del adaptador: la carga arrancaba en 0 (`loadAndPlayTrackInternal(0, 0, …)`), `prepare()` construía **otro** ExoPlayer en 0, y el seek llegaba después — el intro se oía antes de saltar. Además `play()` en estado ENDED hace `seekTo(0)` (`CrossfadeExoPlayerAdapter.kt:~655`): con la sala aún "playing" el bucle forzaba `playWhenReady=true` sobre un player terminado y la canción **volvía a empezar**.

### Arreglo

- `ListenTogetherPlaybackBridge.kt`: el listener solo **observa** (buffering real y errores). Todo el transporte del usuario ya sale por `JamPlaybackRouter` (scrubber, play/pause, siguiente/anterior, notificación, auriculares), así que no hace falta eco. `applying` eliminado.
- `JamSync.planStep(...)`: un cambio de generación hace **como mucho una acción**: `Keep` (misma grabación y a ≤1 s de la sala → no se toca nada, quien suena pausa en su segundo), `Seek` (misma grabación, >1 s) o `Load(startMs)` (otra grabación). Nunca se vuelve a poner un `MediaItem` con el mismo videoId (se compara con y sin prefijo `Video`).
- `MediaPlayerInterface.setMediaItem(item, startPositionMs)` (nuevo, con implementación por defecto) y override en `CrossfadeExoPlayerAdapter`: **una sola carga ya posicionada y pausada**. Sin `prepare()` ni `seekTo` posterior; `pendingLoad` evita una segunda carga mientras el adaptador aplica la primera.
- `JamSync.mayCorrectDrift` (≥2,5 s entre seeks correctivos, y nunca sobre ENDED) y `JamSync.isRealStall` (un BUFFERING <4 s después de nuestro propio seek no abre puerta). El `buffering` solo lo manda quien ya estaba en paso (`readySeq == seq`): uno que aún se pone al día no detiene otra vez a todos.
- El bucle de seguimiento no fuerza `play` sobre un player ENDED; el anfitrión manda `ended` al instante (antes esperaba hasta 1 s).

### Rastreo de todo `seekTo(0)` / `setMediaItem` / `prepare` / `clear`

| Momento | Antes (0.7.14) | Ahora |
|---|---|---|
| Entrar a la sala (`lastRoomCode != code`) | `pause()` ×3 | `pause()` ×1; el anfitrión que ya sonaba siembra su pista (con `isVideo`) |
| Estado/gate/cambio de generación, misma pista | `pause()`, `prepare()` si IDLE, `seekTo` si >500 ms | `playWhenReady=false` + `Keep`/`Seek` (>1 s); IDLE → `Load` posicionado |
| Estado/gate, otra pista | `clearMediaItems` + `setMediaItem` + `prepare` + `seekTo` | un `setMediaItem(item, startMs)` |
| `queue`, `clock`, `welcome` | no tocan el player | igual (solo actualizan estado y espejo de cola) |
| Reconexión | `pause()` | igual |
| `ready` tardío | `seekTo(target)` siempre | solo si >1 s de la sala |
| Salir de la sala | `pause` + `clearMediaItems` | igual (+ se vacía el espejo de cola) |
| `play()` del adaptador en ENDED | `seekTo(0)` forzado por el bucle | el bucle ya no lo invoca |

### Indicador de carga (verificado hasta los composables)

- `ListenTogetherRoom.showsLoading(playerLoading)` (domain) = spinner del player **o** `room.loading`.
- `SharedViewModel.timeline` = `combine(_timeline, listenTogetherRepository.room)` con `loading` ya mezclado. Todos los consumidores leen `sharedViewModel.timeline`: `MiniPlayer.kt:260/331`, `NowPlayingScreen.kt:194` → `state.timelineState` → Spotify (`NowPlayingContentSpotify.kt:1730,1965`), M3E (`NowPlayingContentM3Expressive.kt:947`, `NowPlayingExpressiveCards.kt:1002`), `FullscreenPlayer.kt:126/548`, `FullscreenLyricsContent.kt:183/522`.
- `JamPlayerStatus` (shell de los tres estilos, incluido Apple Music) muestra spinner y "esperando a <nombre>".

### Tests

- `core/service/listenTogether/.../JamSyncSequenceTest.kt` (7): reproduce los snapshots reales del servidor (entrada a mitad de canción con puerta y liberación, rebuffer en video con prefijo, cambio de pista, entrada tardía, tormenta de seeks, spinner del room, orden de playlist) y comprueba que nunca hay `Load` ni `Seek(0)` para el mismo videoId.
- `killmon-api/test/rooms.test.js`: entrada de invitado a mitad de canción (UNA puerta, posición 32 s antes y después, misma `seq` al liberar), rebuffer en 45,5 s, y el eco `pause` que dejaba la sala pausada.
- `core/domain/.../JamPlayerStateTest.kt` (3): `showsLoading`.

---

## 2) La cola en JAM es la cola normal

### Causa raíz
`QueueBottomSheet` (`ModalBottomSheet.kt`) y `AppleMusicQueueView.kt` bifurcaban a `JamPlayerQueue`, una lista propia con botones "Subir/Bajar/Quitar" y sin arrastre ni la hoja de acciones.

### Arreglo
El puente **refleja la cola de la sala** en `MediaPlayerHandler.setQueueData` (`mirrorOf`, `Track` con título/artistas/duración/miniatura reales) y el handler ya enruta a la sala todos los gestos (`playMediaItemInMediaSource` → `play_index`, `swap`/`moveQueueItem` → `queue_move`, `removeMediaItem` → `queue_remove`, `moveItemToPlayNext` vía `currentOrderIndex`). Se eliminan los desvíos a `JamPlayerQueue` y el composable. Resultado: mismo componente, mantener pulsado + arrastrar (`queue_move`), hoja ⋯ con Subir/Bajar/Siguiente/Quitar. `currentOrderIndex()` y la disponibilidad de siguiente/anterior (`updateNextPreviousTrackAvailability`) leen la cola de la sala en JAM (`MediaServiceHandlerImpl.kt`). `queueData.playlistId = null`, así que ni "cargar más" ni el recorte de radio actúan sobre la sala.

---

## 3) Letras de la JAM

### Causa raíz (concretas, por código)
1. `GenericMediaItem.toSongEntity()` (`AllExt.kt:49`) hacía `metadata.artist.toString()` / `albumTitle.toString()`: un item de sala sin artista se convertía en un artista llamado **"null"**; con artista vacío, `getLyricsFromFormat` usaba `""` en vez del respaldo. Con artista inválido, SimpMusic/LRCLIB emparejan solo por título y devuelven otra canción (la inglesa homónima).
2. Las pistas añadidas a la sala viajaban con `artist`/`duration` vacíos cuando el `MediaItem` original no los traía, y el espejo de cola no existía, por lo que `getDataOfNowPlayingState` nunca encontraba el `Track` de la sala.

### Arreglo
- `toSongEntity()`: ausente sigue ausente (sin "null"). `getLyricsFromFormat` trata artista vacío o `"null"` como ausente y usa el mismo respaldo que fuera de JAM.
- `ListenTogetherPlaybackBridge.outgoing`: al añadir/reproducir, completa título, artista y **duración** desde la biblioteca local (misma fuente que fuera de JAM) antes de enviar; los datos viajan en el servidor.
- El espejo de cola entrega `Track` completos al handler: `getDataOfNowPlayingState` construye el `SongEntity` con duración y artistas de la sala. El refresco por cambio de pista ya existía (`nowPlayingState.distinctUntilChangedBy(videoId)` reinicia letra) y se conserva.
- Tests: `JamPlayerStateTest.aRoomTrackWithoutArtistOrAlbum…`.

> A confirmar en QA: el caso del dueño exacto (canción en español → letra inglesa) con una pista añadida desde búsqueda por el otro teléfono.

---

## 4) Pistas con video

### Causa raíz
`ListenTogetherPlaybackBridge` forzaba `description = MERGING_DATA_TYPE.SONG` en el MediaItem de sala y `actions.add/play` descartaban el tipo. `MergingMediaSourceFactory` solo une el stream de video cuando `description == VIDEO` (`MergingMediaSourceFactory.kt:~62`), así que en JAM nunca se pedía video: imagen estática.

### Arreglo
El tipo viaja como `isVideo` (cliente → `queue_add.track.isVideo` → servidor `checkTrack` → snapshot `is_video` → `ListenTogetherState.queueIsVideo`). `mediaItemFor` fija `description = VIDEO` o `SONG` según la pista, de modo que el resto es el camino normal (respeta "ver video en lugar de audio"). La comparación de pista se hace con y sin prefijo `Video`, de modo que no se recarga. Servidor: `checkTrack` valida booleano.
Límite: las pistas aportadas automáticamente (biblioteca/relacionadas) siguen siendo audio.

---

## 5) Foco de audio

### Causa raíz
El adaptador gestiona el foco **a propósito a nivel de adaptador** (`CrossfadeExoPlayerAdapter.kt:~226-310`, issue #2155): `ExoPlayer.setAudioAttributes(attrs, false)` en `createExoPlayerInstance` porque un ExoPlayer por pista con `handleAudioFocus=true` abandona el foco al liberarse y detiene la música entre pistas. Ya pide `AUDIOFOCUS_GAIN` con `USAGE_MEDIA`/`CONTENT_TYPE_MUSIC` y hace `pause()` en `LOSS`/`LOSS_TRANSIENT`; eso equivale a lo pedido. **El fallo estaba en JAM**: el bucle de 150 ms hacía `if (player.playWhenReady != shouldPlay) player.playWhenReady = shouldPlay`, es decir, volvía a pedir `play()` (y por tanto el foco) a los milisegundos de que YouTube lo tomara; y el listener enviaba el `pause` a toda la sala.

### Arreglo / decisión
- `MediaPlayerInterface.audioFocusLost` (nuevo); el adaptador lo activa en `LOSS`/`LOSS_TRANSIENT`, lo baja en `GAIN` y al conseguir foco de nuevo.
- JAM: **la pérdida de foco solo pausa ese teléfono** (el listener ya no envía nada) y el bucle no vuelve a forzar `play` mientras `audioFocusLost`. Pulsar play en la app llega por el router (`control("play")` baja el flag) y retoma en el segundo de la sala. El anfitrión sin foco deja de emitir `pos`; el servidor sigue extrapolando para los demás. Documentado: si el anfitrión sale mucho tiempo, el servidor traspasa el mando tras 8 s desconectado; con foco perdido sigue conectado.
- No se cambia `handleAudioFocus` del ExoPlayer ni se añade ajuste: sería regresión de #2155.

---

## 6) Tras una canción manual, la sala sigue con la playlist

### Causa raíz (servidor)
`rooms.js` case `queue_add`: la pista se hacía `push` **al final** de la cola y `startTrack` la volvía actual. Todo lo que estaba entre la pista que sonaba y ella (la playlist pendiente) quedaba **antes** de la actual en el array, y `advance` (`queue[index + 1]`) y `fillMix` (`prefix = queue.slice(0, index + 1)`) lo tratan como historial ya sonado. Al terminar la manual, la sala pasaba a la mezcla automática o a `idle`: ignoraba la playlist.

### Arreglo
- `queue_add` con `play:true` en una sala activa inserta la pista **justo detrás de la actual** (`splice(currentIndex + 1, 0, item)`); al terminar, `advance` continúa con lo que seguía. Sala parada: sigue empezando sola.
- `after` (videoId): inserta justo detrás de esa pista. El cliente lo usa para encadenar una playlist (play + resto) sin que el resto quede detrás de la mezcla automática (ventana de 1,5 s entre elementos, serializada en una cola `outbox` para conservar el orden).
- Pista ya en cola con `play:true`: solo la salta, sin duplicar.
- Tests (`rooms.test.js`): inserción detrás de la actual y continuación (`ended` → siguiente de la playlist), `after` encadenado, sala parada, pista duplicada, y "al terminar la manual la sala sigue con la playlist y no se queda en reposo".

---

## 7) Versión y validación

`gradle/libs.versions.toml`: `version-name = "0.7.15"`, `version-code = "80"`.

Resultados (JDK 21 + Android SDK locales):

- Backend: `npm test` → **70 tests, 0 fallos** (61 previos + 9 nuevos).
- `./gradlew :androidApp:compileReleaseKotlin -PisFullBuild=false` → **BUILD SUCCESSFUL**.
- `./gradlew :composeApp:jvmTest :listenTogether:jvmTest :domain:jvmTest :data:jvmTest -PisFullBuild=false` → **BUILD SUCCESSFUL**: composeApp 61, listenTogether 24 (7 nuevos), domain 23 (3 nuevos), data 12; 0 fallos/errores.
- `./gradlew :media3:testDebugUnitTest -PisFullBuild=false` → **BUILD SUCCESSFUL** (incluye `JamSessionCommandsTest`).
- No ejecutado: nada en teléfono real; el puente Android y el adaptador Media3 no tienen test automatizado de integración.

## Archivos tocados

Backend: `src/jam/rooms.js`, `test/rooms.test.js`.
Core: `service/listenTogether` (`ListenTogetherSession.kt`, `JamSync.kt`, tests), `data` (`ListenTogetherPlaybackBridge.kt`, `MediaServiceHandlerImpl.kt`), `domain` (`MediaPlayerInterface.kt`, `ListenTogetherModels.kt`, `AllExt.kt`, test), `media/media3` (`CrossfadeExoPlayerAdapter.kt`).
App: `SharedViewModel.kt`, `ModalBottomSheet.kt`, `AppleMusicQueueView.kt`, `JamPlayerUi.kt`, `libs.versions.toml`.

## Riesgos conocidos

- Desktop: el router de JAM solo está cableado en el handler de Android; Desktop no se tocó.
- El servidor trata cada reconexión de un teléfono como entrada (abre puerta para todos); no se cambió.
- Si el servidor no se despliega: video no se propaga a otros teléfonos y el ítem 6 sigue igual.

## Guion de Pruebas QA

Preparación: dos teléfonos con 0.7.15 (A anfitrión, B invitado) y el backend desplegado con el commit `47f983e`. Abrir en cada uno Ajustes → Developer option → registro de la app para ver el tag `JAM`. Cuenta Killmon iniciada en ambos.

1. **Entrada sin reinicio.** A crea la sala y reproduce una canción larga; esperar a 1:30. B entra con el código. Esperado: A se pausa en ~1:30 (no vuelve a 0:00) y muestra spinner + "esperando a B" en mini-player y reproductor completo (probar los tres estilos de Now Playing); al cargar B, ambos reanudan en el mismo segundo (±1 s) sin oír el intro. Repetir con B saliendo y entrando 3 veces.
2. **Rebuffer.** Con A y B sonando, poner B en modo avión 5 s y volver. Esperado: una sola pausa de la sala, spinner en ambos, reanudan en el mismo segundo; la canción no vuelve a empezar y no queda pausada para siempre.
3. **Sin eco.** Con la sala sonando, bloquear y desbloquear B, cambiar de app y volver, y arrastrar la barra en B una vez. Esperado: un solo salto (el del arrastre); no aparecen pausas/gates extra en el log del servidor (`seq` no sube sin causa).
4. **Cola normal.** En B abrir la cola desde el reproductor (Spotify, M3E y Apple Music). Esperado: es el mismo diseño que fuera de JAM; mantener pulsada una fila y arrastrar la reordena en A y B (`queue_move`); ⋯ → Subir/Bajar/Reproducir siguiente/Quitar funcionan; tocar una fila salta a ella en ambos.
5. **Letra.** Añadir desde búsqueda en A una canción en español con letra conocida (también una sin artista visible) y reproducirla. Esperado: en A y B la letra es la de esa canción, en español; al saltar a la siguiente pista la letra se renueva. Probar con SimpMusic y LRCLIB como proveedor.
6. **Video.** En A buscar un video musical (MV/UGC) con "Ver video en lugar de audio" activado, y "Reproducir" en JAM. Esperado: se reproduce el video en A y B (no imagen estática); con el ajuste desactivado suena solo audio. Comprobar que cambiar de generación (entrada de otro teléfono) no reinicia el video.
7. **Foco de audio.** Con B en la sala y sonando, abrir la app real de YouTube y reproducir un video. Esperado: Killmon en B se pausa, **A sigue sonando y la sala no se pausa**. Cerrar el video y pulsar play en Killmon: B retoma en el segundo actual de la sala. Repetir fuera de JAM: también se pausa. Repetir con una llamada entrante.
8. **Canción manual y playlist.** Con la sala sonando una playlist de 5 temas (tema 1 en curso), en B buscar otra canción y "Reproducir ahora". Esperado: suena la manual y, al terminar, la sala sigue con el tema 2 de la playlist (no se detiene ni salta a otra mezcla). Luego reproducir una playlist completa desde biblioteca: sus temas quedan en orden detrás del primero. Repetir con "Añadir a la cola" (va al final).
9. **Regresiones.** Updater 0.7.14 (diálogo con un release 0.7.16 de prueba), reproducción fuera de JAM, cola normal fuera de JAM con arrastre, sala con tres teléfonos, anfitrión que sale (traspaso de mando) y reconexión tras apagar el Wi-Fi 10 s.

Si algo falla, adjuntar el registro de la app (Developer option → exportar) de los dos teléfonos y el `code` de la sala.
