# Killmon Music 0.7.8 (73)

Sin commit, push ni build release. Región CO/es-419 y el arreglo de ANR de 0.7.7 intactos (no se añadió ningún acceso bloqueante al hilo principal).

## 1. Gapless / precarga
Ya existía precarga (`CrossfadeExoPlayerAdapter.triggerPrecachingInternal`: ExoPlayers preparados para las 2 pistas siguientes, que resuelven la URL y llenan buffer). El hueco restante estaba en el cambio sin crossfade: esperaba `STATE_ENDED`, saltar al scope y hacer el swap.
- Nuevo `maybeStartGaplessHandoff` en el bucle de posición (50 ms): si faltan ≤120 ms, la siguiente pista tiene player precargado en `STATE_READY` sin error, y no hay crossfade/cast/live/repeat-one, se hace el traspaso en ese momento. Una sola vez por pista (`gaplessHandoffFor`, se reinicia al pasar 1.5 s). Si la siguiente no está lista, se conserva la ruta normal por ENDED, así que solo puede adelantar un cambio, nunca saltarlo.
- Costo: se pierden como máximo ~120 ms del final de la pista (normalmente silencio).

## 2. Letras
La validación de 0.7.4 (título/artista/duración) ya estaba en los proveedores; se cerraron las carreras restantes en `SharedViewModel`:
- El observador de duración reiniciaba la búsqueda cada vez que cambiaba el total en milisegundos con la letra aún nula (p. ej. tras «Letra no disponible»), reapareciendo el parpadeo y repitiendo peticiones. Ahora `lyricsAttemptIsStale` solo repite si es otra pista o la duración usada difería >4 s (metadatos → duración exacta del player).
- `LyricsData` lleva `videoId`; una traducción solo se adjunta a las letras de su propia pista.
- Se usa `lyricsStillFor` (estado del ViewModel Y del reproductor) también en la rama de escritura final de `updateLyrics`.
- Se conservan: cancelación del Job al cambiar de pista, descarte de respuestas tardías, «Letra no disponible», rechazo de filas antiguas de la tabla `lyrics`.
- Test extra de «versiones» (Live, Acoustic, otro artista, artista vacío).

## 3. Swipe → siguiente
El gesto ya existía (`PlayNextSwipeBox` → `SharedViewModel.playNext`). Fallaba con la cola vacía: el handler no insertaba nada pero mostraba el aviso. Ahora:
- Sin pista/cola, la canción empieza a sonar (con radio, igual que un toque).
- Con cola, el aviso «Se reproducirá a continuación» sale al instante (el handler puede tardar por red para el artista) y luego inserta en posición actual+1.

## 4. Búsqueda
- Texto escrito ⇒ siempre cuerpo de resultados musicales (`searchUiTypeFor`); el historial solo llena la barra vacía con foco. Las sugerencias de texto ya no reemplazan resultados (ni se piden).
- Búsqueda en vivo con debounce de 450 ms (`searchLive`) además de enviar; un enlace de YouTube pegado sigue yendo al router.
- `SearchViewModel`: un solo `searchJob` (cada búsqueda cancela la anterior) y `lastSearch` evita repetir lo ya cargado; el estado `Empty` ya no muestra «sin resultados» antes de buscar.

## Archivos
- `gradle/libs.versions.toml` (0.7.8 / 73)
- `composeApp/.../viewModel/SharedViewModel.kt`, `SearchViewModel.kt`
- `composeApp/.../ui/screen/other/SearchScreen.kt`
- `composeApp/.../utils/SearchLyricsRules.kt` (nuevo)
- `composeApp/src/jvmTest/.../utils/SearchLyricsRulesTest.kt` (nuevo)
- **core cambió** (submódulo): `media/media3/.../CrossfadeExoPlayerAdapter.kt`, `service/lyricsService/.../LyricsMatchTest.kt`. Quedan sin commit; el puntero del submódulo en el repo principal hay que actualizarlo al commitear.

## Validación
- `./gradlew :androidApp:compileReleaseKotlin -PisFullBuild=false --no-daemon`: BUILD SUCCESSFUL (primer intento falló por un `QueueData?`, corregido).
- `:composeApp:jvmTest :lyricsService:jvmTest :kotlinYtmusicScraper:jvmTest :domain:jvmTest :data:jvmTest`: BUILD SUCCESSFUL. composeApp 47, lyricsService 6, scraper 16, domain 20, data 11; 0 fallos, 0 errores.
- Logs: /tmp/killmon-078-compile.log, /tmp/killmon-078-tests.log.
- No hay tests automáticos del gapless ni del swipe (requieren dispositivo).

## Guion de Pruebas QA
1. Versión 0.7.8 (73) en Ajustes; sin branding SimpMusic; Home/charts en región Colombia; arranque en frío sin ANR (repetir 5 veces).
2. Gapless: cola de 3+ canciones continuas (álbum en vivo/mezcla), crossfade apagado. Escuchar los cambios con audífonos: sin silencio audible ni recorte notable. Repetir con pantalla apagada, con Bluetooth, y con repetir-uno (no debe adelantar) y repetir-todo.
3. Gapless con red lenta/modo avión parcial: si la siguiente no cargó, debe cambiar igual al terminar (ruta normal). Con crossfade activado el comportamiento no cambia.
4. Letras: reproducir «Palabras Sobran» de Blessd; debe coincidir. Saltar rápido A→B→C y A→B→A: la letra anterior desaparece al instante y no vuelve una tardía. Probar títulos similares/remix/en vivo: letra correcta o «Letra no disponible» (sin parpadeo repetido).
5. Letras con traducción automática activada y cambio de pista durante la traducción; cambiar de proveedor con una petición pendiente; letras a pantalla completa.
6. Swipe a la derecha en Buscar (canción y video), Recientes, listas, álbum y playlist: aviso «Se reproducirá a continuación» y la canción suena justo después de la actual (no al final).
7. Swipe con nada reproduciéndose: la canción empieza a sonar. Con shuffle activo y con radio: sigue sonando como siguiente.
8. Buscar: escribir «blessd» — tras ~0.5 s salen canciones/artistas/álbumes de YouTube Music, sin lista de sugerencias tipo Google; tocar el campo vacío muestra historial; enviar con el teclado no duplica la búsqueda; cambiar chips (Canciones/Álbumes…) funciona; borrar con la X vuelve a categorías.
9. Escribir rápido varias palabras: solo se ven los resultados de la última consulta (sin saltos a resultados viejos). Pegar un enlace de YouTube sigue abriendo/reproduciendo.
