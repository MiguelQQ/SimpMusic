# Killmon Music 0.7.17 (82) — dispositivos de la cuenta y escritorio morado

## Alcance

Base: 0.7.16 (app `99e4c0c2`, core `0910c21`). Sin commit ni push de app/core. Se mantienen el updater, YouTube directo en cada aparato, el servidor único `https://killmon.seriepokemon.online` y el branding Killmon. `gradle/libs.versions.toml` pasa a 0.7.17 / 82. El User-Agent de la sesión JAM pasa a `Killmon Music/0.7.17`.

**Backend** (`/workspace/killmon-api`): SÍ cambió. Commit local `f0722ec0d5f0223e5703e5e45c018ace5d934db0` ("Connect: same-account devices and playback transfer.", autor `Killmon <killmon@local>`, sobre `6ed294b`). **Hay que desplegarlo.** Sin ese servidor, la app 0.7.17 llama a `/api/me/devices/*` y recibe 404: la lista de la cuenta queda vacía y no hay transferencia.

La presencia y la sesión viven en memoria del proceso. Un reinicio del API las borra; cada app las vuelve a anunciar en el siguiente latido (8 s). No hay tabla nueva en MySQL.

> No hay teléfono ni escritorio gráfico aquí. `:desktopApp:run` no se lanzó. La UI se compiló (`compileReleaseKotlin` y `compileKotlinJvm`); no se recorrió con un puntero.

JAM (sala de varios usuarios) no se toca. El QR de Ajustes → Dispositivos (`simpmusic://login-sync`, cookies de YouTube en la LAN) sigue debajo de la lista nueva. El icono Android verde/cyan no se toca.

---

## 1) Misma cuenta en el teléfono y en el PC (estilo Spotify Connect)

Pedido: con la misma cuenta, en Dispositivos deben salir el Android y el Windows, y la canción del PC no puede ser otra que la del celular. Como Spotify: se ve el otro aparato y el audio pasa a sonar allí.

### Qué había, y qué no es

- **Ajustes → Dispositivos** (`login_sync_section` = "Dispositivos") era el QR de login-sync: el teléfono manda cookies de YouTube al escritorio por la red local. No lista clientes Killmon.
- **La hoja de salida de audio** lista altavoces locales y receptores Google Cast de esa máquina. El celular solo se ve a sí mismo. Cast no es la app de Windows.
- **JAM** es una sala con código, varios usuarios y un reloj compartido. No es la sesión de una sola cuenta.
- **El login Killmon ya es el mismo código** en Android y Desktop (`App.kt`, `KillmonAccountRepository`, `KillmonApi` con base `https://killmon.seriepokemon.online/api`). No había una cuenta distinta en el escritorio. Lo que faltaba era presencia y una sesión de reproducción por `userId`. Las cookies de YouTube siguen siendo por aparato; el QR de la LAN sigue sirviendo para eso.

### Causa

Cada instalación tiene su propio reproductor y su propia cola. Nada anunciaba "este Windows, de este usuario, está en línea" ni guardaba la cola de la cuenta. Por eso el teléfono solo mostraba el celular (o un Cast) y el PC sonaba otra canción.

### Modelo elegido: transferencia, no espejo de audio

Un usuario, un dispositivo activo. El audio de YouTube/mpv suena solo allí. Los demás:

- ven la misma cola, el índice y la posición (la cola se copia a `QueueData` con nombre "Killmon" y el reproductor local se pausa);
- pueden pausar, reanudar, buscar, siguiente y anterior sobre el aparato activo;
- pueden traer el audio con **transferir** ("Aquí", o tocando el otro dispositivo). El destino carga hasta 40 pistas y pasa a ser el activo. El origen se calla porque ya no es el activo.

No se abre un segundo stream en paralelo (eso sería un espejo) y no se entra en una sala JAM.

| Dato | Valor |
| --- | --- |
| Latido | 8 s, o al instante si se abre Dispositivos / se transfiere / se controla |
| En línea | último latido hace ≤ 45 s |
| Sesión | se olvida a los 90 s sin actualización |
| Cola | máximo 40, `videoId` de YouTube de 11 caracteres |
| `deviceId` | 8–80, `[A-Za-z0-9_-]`, guardado en DataStore `killmon_connect_device_id` |
| Nombre | Android: fabricante + modelo ("Pixel 8"). JVM: "Windows · HOST", "Mac · HOST", "Linux · HOST". Windows sin hostname usable: "Killmon Desktop". iOS: "iPhone" |
| Posición | el servidor suma el tiempo transcurrido mientras `is_playing`. El cliente sella `positionAtMs` al recibir y pinta el resto en local, para no contar dos veces |

El primer aparato que publica una cola, y no hay sesión, queda activo. Un seguidor no pisa esa sesión. Si el activo se cae (deja de estar en línea), el siguiente publish o un transfer sí pueden tomarla. Play local mientras se sigue a otro **no** publica la cola vieja: pide la transferencia hacia sí mismo, con la posición estimada de la sesión remota, y pausa el local hasta que llega. Un comando `transfer` pendiente se aplica y se confirma antes de volver a publicar. Dentro de una sala JAM el puente no publica, no cede, no espeja y no aplica transfer.

Si el activo ya no está en la lista y se pulsa play en la barra, se trae la sesión aquí en lugar de mandar un control a un aparato caído.

### Arreglo — API (`f0722ec`)

Auth Bearer ya existente (`verifyAccess` / `authenticate`). Memoria por `userId`.

- `PUT /api/me/devices/presence` — `device_id`, `platform` (`android|ios|windows|macos|linux|desktop`), `display_name`, `playback` opcional, `ack_transfer`, `ack_control`.
- `GET /api/me/devices?device_id=` — lista.
- `POST /api/me/devices/transfer` — `target_device_id`. 409 `device_offline` si origen o destino no está en línea. 409 `no_playback` si no hay cola.
- `POST /api/me/devices/control` — `play|pause|seek|next|previous`. Actualiza la sesión ya y deja el comando para el activo. `previous` reinicia si la posición es > 3 s; si no, índice − 1.
- `POST /api/me/devices/bye` — al cerrar sesión o borrar la cuenta. `DELETE /api/me` llama a `connect.forget`.

Otro usuario no ve esos dispositivos. El comando de transferencia lo recibe solo el destino, una vez; el ack lo limpia.

### Arreglo — app (sin commit)

- Dominio: `ConnectSession.kt`, `AccountConnectRepository.kt`, test `ConnectSessionTest.kt`.
- Datos: `KillmonApi` (presence/transfer/control/bye), `AccountConnectRepositoryImpl`, `ConnectPlaybackBridge`, `ConnectModule` (arranca el puente; `App.kt` lo inyecta, si no el `loadKoinModules` no lo construye), `Loader.kt` después del reproductor. `killmonDeviceLabel()` en android/jvm/ios. `beforeClear` manda `bye` antes de logout o borrar cuenta. El latido no llama a `/api/me`.
- UI: `KillmonDevicesSettings` arriba del QR en el grupo Dispositivos. Sección "Killmon" en la hoja de salida de audio. `ConnectSessionBar` sustituye al mini player mientras se sigue a otro aparato ("En {nombre} · m:ss", pausa/play, "Aquí").

### Tests

- `npm test` en `/workspace/killmon-api`: **77/77**, 0 fallos (~784 ms). Incluye: dos dispositivos se ven y la transferencia mueve el activo; el seguidor no pisa la sesión; otro usuario aislado; destino caído → 409; pausa remota; `device_id` malo; HTTP 401 sin token y presence + transfer con JWT.
- `:domain:jvmTest` `ConnectSessionTest`: **3/3** (pausa no avanza, play avanza y se capa a la duración, "siguiendo" solo si hay sesión de otro aparato).
- `:listenTogether:jvmTest` `UserAgentHandshakeTest`: pasa, User-Agent `Killmon Music/0.7.17`.
- `:androidApp:compileReleaseKotlin` y `:desktopApp:compileKotlinJvm`: **BUILD SUCCESSFUL** (1 m 20 s en la segunda pasada, configuration cache). La primera falló en `:data:compileKotlinJvm` (clase pública que exponía un tipo `internal`, y smart-cast de `session`); quedó corregido y la segunda pasó.

---

## 2) Tema morado solo en Windows / Desktop JVM

Pedido: mejorar el diseño de la plataforma en Windows, temática morada. El verde/cyan del icono Android se queda.

### Causa

`AppTheme` generaba el esquema desde `seed` = `killmonGreen` (`#39E75F`) en todas las plataformas. La ventana de escritorio era negro/blanco plano y la barra de título de Windows era la nativa del sistema. No había un acento morado de producto en nav, botones ni mini player.

### Arreglo

No hay un `expect/actual` de color. `getPlatform() == Desktop` elige el morado; Android e iOS siguen en verde.

- Seed de escritorio `#7C4DFF`, secundario `#B388FF`, gradiente hacia `#9B59B6` (`Color.kt`: `killmonBrand()`, `killmonBrandSecondary()`, `killmonBrandGradient()`).
- Tema oscuro de escritorio: ventana `#100818`, superficie `#1A1028`, paneles `#1C1230` / `#26183E` (`withDesktopPurpleSurfaces`). El color personalizado y el del wallpaper no pisan el morado en desktop.
- Panel de contenido: borde 1 dp `#7C4DFF` al 45 %, esquina 14 dp, padding 8 dp (el hueco del now playing sigue contando 8 dp).
- Windows: barra de título propia (macOS ya la tenía; Linux sigue nativa porque undecorated+transparent rompe algunos gestores). Fondo `#1A1030` (oscuro) o `#F3E9FF` (claro), título lavanda, línea inferior 2 dp `#7C4DFF`.
- Nav lateral: fila 52 dp, un poco más de aire. Botón primario, chips, avatar, campos de login y el progreso del mini player usan el brand de la plataforma (verde en Android, morado en desktop).

### Test

Compilación JVM y Android release (arriba). No hay prueba de píxeles: no hay pantalla. En un Windows real hay que mirar barra, sidebar, panel y mini player en tema oscuro y claro, y confirmar que el Android sigue verde.

---

## 3) Calidad de audio: dejar de quedarse en ~150 kbps

Pedido: en PC y Android suena a ~148/158 kbps. Tiene que ir el itag de audio de mayor bitrate (Opus o AAC high), no medium/low. El default es MAX/HIGH sin que haya que abrir Ajustes. Si lo guardado es low o medium, se migra a max.

### Causa

YouTube reparte el audio adaptativo así:

| itag | Códec | Nominal | Qué pinta la UI |
| --- | --- | --- | --- |
| 250 | Opus | ~70 kbps | Low |
| 251 | Opus | ~160 kbps | **~148–158 kbps** (el `bitrate` medio que manda el player) |
| 774 | Opus | 256 kbps | High, cuenta con derecho a alta calidad |
| 141 / 327 | AAC | 256 kbps | El mismo piso, en MP4 |

`StreamRepositoryImpl.getStream` pedía **un solo itag**, el de la etiqueta guardada (`QUALITY.itagOf`). "Medium - 129kps" es itag **251**. Si 251 venía en la respuesta, se usaba **aunque 774 o 141 estuvieran al lado**. El default de una instalación nueva ya era "High Opus - 256kps", pero un aparato que guardó Medium (o lo sincronizó la cuenta Killmon en `quality`) se quedaba ahí para siempre. La fila de formato en base de datos reutilizaba esa URL hasta que caducaba, así que cambiar el ajuste no se oía en la canción en curso.

Ni MPV ni ExoPlayer/Media3 bajan el bitrate: los dos reproducen la URL que devuelve `getStream` (MPV como `edl://` con el audio aparte; Media3 con `ResolvingDataSource`). No hay tope de ABR en el player. El techo de ~160 kbps, cuando 774/141 no vienen en la respuesta, es el máximo que YouTube entrega a esa sesión (sin el tier alto). No se inventa un stream que el player no mandó.

`newPipePlayer` solo rellenaba la URL de los itag que WEB_REMIX ya había listado. Un 774 que solo traía PipePipe se tiraba.

### Arreglo

- `ITAG.bestAudio`: si hay itag 774, 141 o 327 (o `audioQuality = AUDIO_QUALITY_HIGH`), gana ese, con bitrate nominal 256 kbps cuando el campo viene a 0. Si no hay ninguno, el de mayor bitrate (251 por delante de 250). A igualdad, Opus 774 antes que AAC.
- Reproducción (`isDownloading = false`) y una descarga cuya etiqueta ya es high usan `bestAudio`. Low/Medium ya no nombran el itag de playback.
- `QUALITY.normalize` convierte Low, Medium, vacío y desconocido en "High Opus - 256kps (YT Premium)". High AAC se queda. Se reescribe en DataStore al reproducir, al abrir Ajustes y al subir/bajar las preferencias de la cuenta, para que el otro aparato no vuelva a empujar Medium.
- Una fila cacheada con itag que no es high no se reutiliza hasta que este proceso haya resuelto esa canción con `bestAudio`. Así no sigue sonando el 251 guardado.
- Las URL de audio que PipePipe trae y WEB_REMIX no listó se añaden a `adaptiveFormats` (774/141 incluidos).

### Cómo verificar

- Now Playing (estilo Apple y la cápsula del escritorio) ya muestra `toAudioQualityLabel`: **"256 kbps · 48 kHz"** si llegó el tier alto, o **"~160 kbps"** si la respuesta solo traía 251. No hace falta un ajuste nuevo.
- Log `Stream`, una línea por canción, sin la URL: `itag 774 bitrate … avg … audio/webm` (o `itag 141` / `audio/mp4`). En logcat (Android) o en la consola del desktop. "Audio from format itag N bitrate M" es la fila cacheada; si N es 250 o 251 la primera vez tras esta versión, el log siguiente debe ser la resolución nueva.
- Si después de una cuenta con YouTube en alta calidad el log sigue en itag 251, esa respuesta no trajo 774 ni 141: el máximo real de esa sesión es ~160 kbps.

### Test

`:domain:jvmTest` `AudioQualitySelectionTest`: **4/4**. 774 con bitrate 0 gana a 251 a 158 kbps; 141 gana a 251; sin high queda 251; "Medium - 129kps" y "Low - 66kps" normalizan a High Opus e `itagOf` devuelve 774. Compilan `:androidApp:compileReleaseKotlin`, `:desktopApp:compileKotlinJvm`, `:media3:compileReleaseKotlin` y `:media-jvm:compileKotlin`.

---

## 4) Versión

- `gradle/libs.versions.toml`: `version-name = "0.7.17"`, `version-code = "82"`.
- `ListenTogetherSession` agent `Killmon Music/0.7.17`, y el assert del test.
- Updater sin cambios.

---

## Guion de Pruebas QA

Hace falta el API `f0722ec` desplegado en `https://killmon.seriepokemon.online`. Misma cuenta en el teléfono y en el PC. YouTube con sesión útil en el aparato que vaya a sonar (el QR de Dispositivos sigue sirviendo para copiar cookies).

1. **Lista.** Inicia sesión en Android y en Windows. Espera unos 10 s. Ajustes → Dispositivos en los dos: salen el teléfono (modelo) y el PC ("Windows · …" o "Killmon Desktop"), cada uno marcado "Este dispositivo" en sí mismo. El QR de emparejar YouTube sigue debajo.
2. **Un solo audio.** Reproduce una canción en el teléfono. En el PC, a los pocos segundos, la barra inferior muestra el mismo título y "En {teléfono}", y el PC no suena. La posición avanza en la barra sin saltar de golpe cada 8 s.
3. **Traer al PC.** En el PC pulsa "Aquí" (o el teléfono dentro de Dispositivos, o la fila Killmon de la hoja de salida). El audio pasa al PC en la misma canción y cerca de la misma posición. El teléfono deja de sonar y muestra "En {Windows}".
4. **Control remoto.** Con el audio en el PC, pausa y reanuda desde la barra del teléfono. La pausa se oye en el PC. Siguiente / anterior no están en la barra; si se añaden a mano por API, el activo obedece.
5. **Cola.** En el activo, encola 2–3 canciones. El otro aparato muestra la misma cola en Now Playing (nombre "Killmon"), sin reproducirlas en local.
6. **Play local mientras sigues.** En el seguidor, pulsa play en el reproductor del sistema. No debe sonar la canción vieja de ese aparato: o se pausa y trae la sesión, o sigue en silencio hasta que el transfer carga la cola remota.
7. **Caída.** Cierra la app del activo. En menos de un minuto el otro deja de listarlo. "Aquí" o play en la barra trae la última sesión (si no pasaron 90 s).
8. **Cuentas distintas.** Otra cuenta Killmon no ve esos dispositivos.
9. **JAM.** Entra en una sala JAM. La sync de cuenta no debe pisar la cola de la sala ni pausarla por seguir a otro aparato.
10. **Logout.** Cierra sesión en un aparato. Desaparece de la lista del otro en el siguiente latido (≤ 45 s, o al abrir Dispositivos).
11. **Tema.** En Windows oscuro: ventana violeta oscura, no negro plano; barra con línea morada; nav, botón de login y progreso del mini player en `#7C4DFF`. En Android el acento sigue verde `#39E75F` y el icono no cambia.
12. **Versión.** Ajustes o el paquete: 0.7.17 (82). El updater anterior sigue ofreciendo esta build; no se reescribió.
13. **Bitrate.** Reproduce una canción nueva (no la que ya estaba en caché de antes, o espera a que el log diga que resuelve otra vez). En Now Playing la línea de calidad debe subir de ~148/158 kbps a ~256 kbps si la sesión de YouTube trae itag 774 o 141. En el log `Stream`: `itag 774` o `itag 141`, no `itag 251`. Ajustes → calidad de audio muestra High Opus aunque antes dijera Medium; no hay que tocarlo. Si el log se queda en 251, esa respuesta no incluyó el tier alto y ~160 kbps es el máximo de esa cuenta.
