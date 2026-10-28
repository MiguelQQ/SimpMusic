# Killmon Music 0.7.16 (81) — cola JAM manual en FIFO y Desktop Killmon

## Alcance

Base: 0.7.15 en `killmon-0.7.3-auto` (app `cf71234`, core `c89c91a`). Sin commit ni push de app/core. Se mantienen el updater de 0.7.14+, YouTube directo en cada dispositivo, el servidor de sala único `https://killmon.seriepokemon.online` y la región CO. `gradle/libs.versions.toml` pasa a 0.7.16 / 81. El User-Agent de la sesión JAM pasa a `Killmon Music/0.7.16`.

**Backend** (`/workspace/killmon-api`, `src/jam/rooms.js`): SÍ cambió. Commit local `6ed294b93a1fdd580f7f1e98112dd61bcee0154d` ("JAM: hand-picked songs stay in a FIFO block ahead of the automatic mix.", autor `Killmon <killmon@local>`, sobre `47f983e`). **Hay que desplegarlo.** Sin ese servidor, la app 0.7.16 sigue mandando `automatic: false`, pero un `queue_add` sin `play` sigue yendo al final de la cola (detrás del mix) y el snapshot no lleva el flag.

> Honestidad sobre la evidencia: no hay teléfono ni escritorio gráfico aquí. Las causas se leyeron en el código de 0.7.15 (`47f983e`) y las pruebas reproducen los mensajes del servidor y el decode del cliente. `:desktopApp:run` no se lanzó. No hay `conveyor` ni `appimagetool` en esta máquina, así que no hay AppImage.

---

## A) Cola JAM: las puestas a mano van en bloque FIFO, delante del mix

Pedido: si yo añado una canción queda primera entre las pendientes, si mi amigo añade otra queda segunda, si yo añado otra queda tercera; si nadie añade más, el mix automático sigue detrás de esas, mezclado entre los dos.

### Causa raíz (0.7.15, commit `47f983e`)

1. **`queue_add` sin `play` ni `after` hacía `push` al final.** `rooms.js:405-406`: solo `play === true` insertaba en `currentIndex + 1`. El resto, incluido "añadir a la cola", iba a `room.queue.push` (`:406`), detrás de las automáticas que `fillMix` ya había puesto.
2. **La pista nueva no era explícitamente manual.** El ítem se creaba sin `automatic` (`rooms.js:396`). `undefined` es falso, así que `fillMix` la trataba como manual *solo si volvía a componer*.
3. **`fillMix` no corría con la cola llena.** `fillMix` (`rooms.js:617`) salía en `rooms.js:620` cuando quedaban ≥ 3 próximas y no había `rebalance`. Una manual añadida tarde se quedaba detrás del mix hasta que la cola bajara de 3.
4. **El snapshot no enviaba el flag.** `rooms.js:229-239` omitía `automatic`. El cliente no podía conservarlo.
5. **El cliente no puede marcar el flag.** `checkTrack` (`rooms.js:51-87`) copia solo `videoId`, textos, miniatura, duración e `isVideo`. Un `automatic: true` en el track del mensaje se tira. El servidor tiene que poner el flag.
6. **El cliente no lo mandaba ni lo guardaba.** `ListenTogetherSession.suggestTrack` no incluía `automatic`. `decodeJamState` no leía el campo. El puente del anfitrión (`ListenTogetherPlaybackBridge.kt`, seed `queue_add`) tampoco. El orden lo manda el servidor; el cliente refleja esa cola y no la reordena.

No existe una función `composeQueue`. La composición es `fillMix`.

### Arreglo (commit `6ed294b`)

- Cada `queue_add` nuevo lleva `automatic: false` (`rooms.js:412-414`). `set_track` igual (`:597`).
- Sin `play` y con pista en curso, se inserta en `firstAutomaticIndex` (`rooms.js:641-647`, usado en `:425`): justo antes de la primera automática que aún no suena. Orden FIFO global de quién la puso.
- `play === true` sigue en `currentIndex + 1` (`:424`) y esa pista es manual. `after` (cadena de playlist, ventana 1,5 s del cliente) sigue detrás de ese `videoId` (`:422`).
- `prioritizeManuals` (`rooms.js:652-664`) deja el historial y la actual quietos y reagrupa lo que falta: manuales en su orden relativo, luego automáticas. No mueve una sala sin pista actual.
- `fillMix` (`:668`) empieza por ese reagrupado. Si no recompone (≥ 3 próximas y sin `rebalance`, `:672-675`) igual emite estado si movió manuales delante. Cuando recompone, las manuales se conservan como objetos y el round-robin solo añade `automatic: true` detrás (`:682`, `:716`, `:722`).
- Si el `videoId` ya estaba en el mix que falta por sonar, `queue_add` lo saca de ahí, lo marca manual y lo pone al final del bloque manual (`:392-403`). El historial no se mueve. `play` sobre una que ya está sigue haciendo `startTrack` (`:391`).
- `queue_move` marca la fila como manual (`:452`) y luego `prioritizeManuals` (`:455`): una automática arrastrada entra al bloque manual y no se queda entre el mix.
- El snapshot incluye `automatic: item.automatic === true` (`rooms.js:240`).
- Cliente: `suggestTrack` y el seed del anfitrión envían `automatic: false` (`ListenTogetherSession.kt:147`, `ListenTogetherPlaybackBridge.kt:222`). `decodeJamState` guarda `queueAutomatic` en paralelo a la cola (`ListenTogetherSession.kt:99` y `:319`). El cliente no reordena: el espejo usa el orden del servidor.

Al terminar una manual, `advance` (`rooms.js:727-731`) sigue con la siguiente de la cola. Si la siguiente es manual, suena esa; si ya no quedan, suena la primera automática.

### Tests

- `killmon-api` `npm test`: 73/73. Nuevos en `test/rooms.test.js`:
  - `:701` A añade M1, B añade M2, A añade M3, B vuelve a contribuir → próximas `[M1, M2, M3, auto…]`, las tres `automatic: false` y el resto `true`. El snapshot lleva el flag. Tras `ended` de la actual, suena M1; tras otro `ended`, la cola desde la actual es `[M2, M3, auto…]`.
  - `:739` `play: true` inserta M2 justo detrás de la actual (y pasa a ser la actual), `automatic: false`; M1 queda detrás; el mix sigue automático.
  - `:751` una canción que ya estaba en el mix, añadida a mano, pasa al frente del bloque manual y no se duplica. El test manda `automatic: true` en el track y el servidor lo ignora (`checkTrack`).
- Cliente (`:listenTogether:jvmTest`, relanzado): `UserAgentHandshakeTest` comprueba `automatic: false` en el `queue_add`. `LiveRoomSyncTest.kt:33` (campo ausente = manual) y `:35-41` (orden `false, false, false, true`; la primera automática queda después de la última manual).

---

## B) Desktop adaptada a Killmon Music

El módulo `desktopApp` ya estaba en el fork (Compose Multiplatform / JVM, mismo `App.kt` que Android). Quedaban nombre de paquete visible, `fsname` `simpmusic`, esquemas, icono de nota y comentarios que reescribían enlaces a `simpmusic.org`.

El `applicationId` de Android ya es `com.killmon.music` (`androidApp/build.gradle.kts`). Los fuentes siguen en `com.maxrave.simpmusic`: renombrar el paquete Kotlin rompería Android y no cambia el id de la app. `mainClass` del empaquetado sigue siendo `com.maxrave.simpmusic.MainKt`.

### 1. Branding

### Causa raíz
- `desktopApp/build.gradle.kts` generaba `fsname` / `.desktop` / `Exec` / `Icon` / `MimeType` / `StartupWMClass` con `simpmusic`, y el icono de AppImage era `composeApp/appimage/simpmusic.png` (logo de nota, 128 px).
- `conveyor.conf` decía vendor y descripción SimpMusic y registraba el esquema `simpmusic`.
- `DesktopDeepLinkHandler` reescribía `simpmusic://watch` a `https://simpmusic.org/app/...`.
- `WindowsProtocolRegistrar` registraba el esquema `simpmusic`.
- `DesktopApp.kt` usaba `applicationId` y release de Sentry de SimpMusic. `MiniPlayerWindow.kt` guardaba la posición en el nodo Java `SimpMusic/MiniPlayer`.
- El icono grande (`composeApp/icon/circle_app_icon.png`, 1024) y el `ic_launcher` de Android ya eran la K de Killmon.

### Arreglo
- `writeConveyorConfig` escribe `app.fsname = killmonmusic`, `app.display-name = Killmon Music`, `app.rdns-name = com.killmon.music` (`desktopApp/build.gradle.kts:131-133`).
- AppImage: icono `killmonmusic.png` (copia del icono Killmon de 1024), archivo `killmonmusic.desktop`, `Exec=bin/killmonmusic`, `Icon=killmonmusic`, `StartupWMClass=Killmon Music`, `MimeType=x-scheme-handler/killmonmusic;x-scheme-handler/wordbyword;` (`build.gradle.kts:385-411`). `AppRun` instala `~/.local/share/applications/com.killmon.music.desktop` y ejecuta `bin/killmonmusic`. Se borró `composeApp/appimage/simpmusic.png`.
- macOS: `CFBundleURLName` `com.killmon.music.deeplink`, esquemas `killmonmusic` y `wordbyword`.
- `conveyor.conf`: vendor `Killmon`, descripción "Killmon Music — YouTube Music client.", `url-schemes = ["killmonmusic", "wordbyword"]`, icono `composeApp/icon/circle_app_icon.png`.
- `DesktopDeepLinkHandler.kt`: acepta `killmonmusic://` y, en proceso, `simpmusic://`. No reescribe a `simpmusic.org`.
- `App.kt:153` `isKillmonAppScheme`: `killmonmusic` o `simpmusic`. Sigue aceptando un host `simpmusic.org` por si llega un enlace viejo ya convertido; no se llama a esa web.
- `WindowsProtocolRegistrar.kt:22`: esquema `killmonmusic`, descripción "URL:Killmon Music Protocol". Busca `killmonmusic.exe` y `Killmon Music.exe`. Last.fm sigue en `wordbyword` (el callback está registrado así en la cuenta).
- `DesktopApp.kt:203` `applicationId = "com.killmon.music"`. Release Sentry `killmon-music-desktop@…` (`:229`). Título de ventana ya era "Killmon Music".
- `MiniPlayerWindow.kt:47`: prefs `KillmonMusic/MiniPlayer`. La posición de la ventana mini no se migra del nodo viejo.
- `scripts/windows/install.bat` y `scripts/wrap-mac-dmg.sh`: textos y volumen "Killmon Music".

### Qué se dejó a propósito
- **QR Android → Desktop.** `LoginSyncWire.kt:76` sigue siendo `simpmusic://login-sync?...`. El escáner de Android busca ese prefijo. Cambiarlo rompería el login cruzado. No es un MimeType del sistema: el SO registra `killmonmusic`.
- **`~/.simpmusic`.** Biblioteca y ajustes de escritorio (`MusicDatabase.jvm.kt:18`, `DataStoreManagerImpl.jvm.kt:24`, `SettingsViewModel.jvm.kt:50`). Moverla dejaría la biblioteca huérfana. No se muestra en la UI.
- **User-Agent de YouTube** `SimpMusic/$versionName` en `AppIdentity.kt:12`. Es la identidad Innertube, no un texto de pantalla. No se tocó.
- **`conveyor.conf` `vcs-url`** sigue en `https://github.com/maxrave-dev/SimpMusic`. Conveyor lo exige para el tramo open source. No es una API de reproducción.
- **`install.bat:75`** sigue lanzando `shell:appsFolder\Simpmusic_ejp2bhxmz1qq6!Simpmusic`. Ese AUMID es el de la familia ya publicada. Un `rdns` nuevo solo tiene familia nueva después de un build real de Conveyor en Windows. No se inventa un AUMID.
- **`simpmusic.dev`** como system property de desarrollo en Gradle. No la ve el usuario.

### 2. Cuenta, API y JAM

### Causa raíz
El shell de escritorio ya entra por el mismo `App.kt` que Android. El login Killmon (`killmonStart`: Home o `KillmonLoginDestination`) es común. `ListenTogetherSession` tiene `BASE = "https://killmon.seriepokemon.online"` (`ListenTogetherSession.kt:297`). No hay cliente del servidor de Polonia ni de simpmusic.org.

### Arreglo / hueco
No hace falta una pila JAM aparte. En Desktop, si el login Killmon está hecho, Escuchar juntos usa el mismo código que Android: sala en `killmon.seriepokemon.online` y YouTube en el reproductor local (mpv). Lo que no se ha ejecutado aquí es la ventana (no hay sesión gráfica de prueba). El mini player y el QR de login se conservan, con el esquema del QR citado arriba.

### 3. Compilación

- `:desktopApp:compileKotlinJvm` y `:androidApp:compileReleaseKotlin`: BUILD SUCCESSFUL (junto con los tests de abajo, 42 s, 198 tareas). JDK `/home/box/opt/jdk-21.0.13+9`.
- AppImage: no generado. `conveyor` y `appimagetool` no están en el PATH. El script de empaquetado ya apunta a `killmonmusic`; falta la herramienta para producirlo.

### 4. Features de escritorio que se conservan

Comparado con el módulo que ya traía el fork (mismo origen que el `desktopApp` de maxrave-dev/SimpMusic): ventana principal, mini player, login por QR desde Android, deep links, callback Last.fm `wordbyword`, empaquetado Conveyor (Windows / macOS / AppImage). Se renombró lo visible. No se quitó el mini player ni el QR.

---

## C) Versión y lo que no se tocó

- `gradle/libs.versions.toml`: `version-name = "0.7.16"`, `version-code = "81"`. Android y Desktop leen el catálogo.
- Updater (`KillmonRelease`, `KillmonUpdatePolicy`, `KillmonInAppUpdate`) sin cambios. Los tests que citan 0.7.15 / 80 son datos de fixture, no la versión de la app.
- YouTube sigue saliendo del dispositivo (Innertube local). La sala solo sincroniza.
- Región por defecto CO, sin cambio.

### Verificación en esta máquina

| Qué | Resultado |
|---|---|
| `killmon-api` `npm test` | 73/73, exit 0 |
| `:androidApp:compileReleaseKotlin` | BUILD SUCCESSFUL |
| `:desktopApp:compileKotlinJvm` | BUILD SUCCESSFUL |
| `:listenTogether:jvmTest` (relanzado con `--rerun-tasks`) | BUILD SUCCESSFUL, 6 s |
| `:composeApp:jvmTest`, `:data:jvmTest`, `:domain:jvmTest` | BUILD SUCCESSFUL en la misma corrida |
| `:desktopApp:run` | no lanzado (hace falta escritorio) |
| AppImage | no generado (sin conveyor / appimagetool) |

App y core quedan sin commit. El backend sí está commiteado en local y no está pusheado.

---

## Guion de Pruebas QA

Preparación: dos teléfonos con 0.7.16 (A y B, cuentas Killmon distintas) y el backend **desplegado** con `6ed294b93a1fdd580f7f1e98112dd61bcee0154d`. Región CO. Si hay un escritorio, el mismo build Desktop (`com.killmon.music`), con login Killmon. Sin ese deploy, el punto 1 falla aunque la app sea 0.7.16.

1. **FIFO manual.** A crea la sala y deja sonar una canción (el mix ya habrá rellenado varias). A añade M1 a la cola (no "Reproducir ahora"). B añade M2. A añade M3. Esperado, en la cola de A y de B, justo después de la que suena: M1, M2, M3, y solo detrás el mix. Ninguna automática entre M1 y M3. Dejar de añadir: al acabar la actual suena M1, luego M2, luego M3, y después el mix alternando biblioteca de A y de B.
2. **Reproducir ahora.** Con M1 ya en el bloque manual y el mix detrás, B elige otra canción y "Reproducir ahora". Esperado: suena esa al momento; al terminar sigue M1 (no una automática); esa pista cuenta en el bloque de las puestas a mano.
3. **Ya estaba en el mix.** Añadir a la cola una canción que ya aparece más abajo como automática. Esperado: sube al final del bloque manual y desaparece de en medio del mix. No se duplica.
4. **Playlist.** Reproducir una playlist desde la biblioteca estando en la sala. Esperado: el primer tema suena ya; el resto queda junto, en orden, dentro del bloque manual, delante del mix. Al terminar uno sigue el siguiente de la playlist.
5. **Escritorio.** Abrir Killmon Music en Linux/Windows/macOS. Esperado: título e icono Killmon (la K), login Killmon obligatorio, reproducción de un video de YouTube en el propio equipo. Crear o unirse a una sala con el teléfono: la cola manual del punto 1 se ve igual. Mini player abre como "Killmon Music". El QR de login desde el teléfono sigue funcionando (el código lleva `simpmusic://login-sync`; es compatibilidad, no la marca de la ventana). No debe aparecer el nombre SimpMusic en la ventana, el instalador ni el acceso directo. `killmonmusic://` abre la app; Last.fm sigue volviendo por `wordbyword://`.
6. **Regresiones.** Updater sigue ofreciendo desde 0.7.14 (no se reescribió). Fuera de la sala, YouTube suena en el teléfono. Ajustes de región siguen en CO. Tres personas en la sala: una manual de cada una respeta el orden en que se pusieron, y el mix no se cuela delante. Arrastrar una automática en la cola la trata como puesta a mano y la agrupa con las otras manuales.

Si el punto 1 falla, mirar que el servidor en producción sea `6ed294b` y no `47f983e`. Adjutar el `code` de la sala.
