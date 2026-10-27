# Killmon Music 0.7.9 (74)

Sin commit, push ni build release. Región CO/es-419, fix ANR 0.7.7 y cambios de 0.7.8 intactos (nada nuevo bloquea el hilo principal; el prefetch corre en `Dispatchers.IO`).

## 1. Swipe «siguiente» en Recientes de Buscar
- `RecentSearchRow` envuelve su contenido en `PlayNextSwipeBox` (el mismo componente de los resultados) cuando recibe `onPlayNext`. Solo las filas con `videoId` (canción/video) se pueden deslizar; artistas, álbumes, playlists y podcasts quedan como fila normal.
- `SearchScreen` pasa `sharedViewModel.playNext`: con cola, aviso «Se reproducirá a continuación» + inserción en actual+1; con cola vacía empieza a sonar (lógica de 0.7.8).
- La fila lleva fondo opaco (`colorScheme.background`) para que la capa verde del swipe no se vea en reposo.

## 2. Colchón offline de las 2 siguientes
Estado previo: ya había 2 ExoPlayers preparados (buffer en RAM de hasta ~200 s por pista, escrito también en el SimpleCache), pero atado al ciclo de vida del player: un error de red o un `release()` lo perdía, sin reintento ni política de datos. Además elegía las pistas por orden de playlist, **no** por orden de shuffle (con shuffle preparaba pistas equivocadas).
- Nuevo `NextTrackPrefetcher` (`media3/service/prefetch/`): resuelve la URL con antelación (formato guardado y vigente, si no `getStream`, que además guarda el formato y su caducidad) y escribe el audio **completo** con `CacheWriter` en el `playerCache`, con la misma clave (media id) que lee el resolver de reproducción. Una pista entera en disco se reproduce sin pedir URL ni red.
- Política (`PrefetchPolicy`, pura): Wi-Fi/sin tarifa hasta 48 MiB por pista; datos móviles hasta 16 MiB; sin red, roaming o Ahorro de datos activo → no descarga. Tope aplicado también durante la escritura (se corta). Streams en vivo se omiten. Hasta 3 intentos (espera 4 s y 12 s). Pista ya completa en caché de descargas o de player → no hace nada.
- Cancelación: `update(ids)` sustituye el conjunto; lo que ya no es una de las 2 siguientes se cancela (cambio de cola, skip, shuffle, repeat). `cancelAll()` en vaciar precache, cast, cola vacía y `release()`.
- El orden de las «siguientes» ahora respeta shuffle y repeat-all (`PrefetchPolicy.upcomingIndices`), también para los ExoPlayers preparados.
- Cableado: `CrossfadeExoPlayerAdapter(prefetcher = …)` desde `Media3ServiceModule`, usando el mismo OkHttp/proxy del player.

## Resiliencia offline: evaluación honesta
- **Cobertura real**: la pista actual (buffer ~200 s + caché) más 2 pistas enteras. Una pista de 3–4 min pesa ~3–7 MB (128–256 kbps), así que son ~7–14 MB y **unos 7–8 minutos de música** sin red, si ambas se completaron.
- **Caducidad de URLs de YouTube**: ~6 h. Importa solo para pistas **no** cacheadas todavía; una pista completa en disco no necesita URL. El formato guardado se reutiliza solo si no ha caducado y no da 403; si no, se re-extrae (requiere red).
- **Límites**: no se prefetch en datos con Ahorro de datos/roaming; en datos móviles se omiten pistas >16 MiB (podcasts, mixes largos). Los reintentos son solo 2 (4 s y 12 s); si la red ya está caída al cambiar de pista, no hay descarga hasta el siguiente cambio de pista (no hay disparador por «volvió la red»). Si el usuario fijó un límite de caché pequeño, el LRU puede expulsar pistas ya prefetcheadas. Con la caché «sin límite» (por defecto) crece sin tope: son ~3–7 MB por pista reproducida.
- **Pistas en cola más allá de las 2**: no hay colchón. Videos (modo video) no se prefetchean (solo audio). Shuffle cambiado a mitad de pista re-apunta el prefetch.
- **No verificado en dispositivo**: las pruebas automáticas cubren política y orden; la descarga real, el corte de red y la reproducción desde caché requieren el guion QA de abajo.

## 3. Home estilo Spotify
Se mantienen los chips (Todo/Música/Pódcasts). Todo sale de datos reales: historial local y los shelves que ya trae el Home de YouTube Music (región CO); nada se inventa.
- **Cuadrícula de acceso rápido** (`HomeQuickAccessGrid`, justo bajo los chips, dentro del primer ítem para conservar el brillo de fondo): 2 columnas, 6-8 tiles de 56 dp (carátula cuadrada a la izquierda, artistas en círculo, título en negrita a la derecha, fondo gris oscuro redondeado, `onBackground` al 10 %). Sustituye a las tarjetas altas de «Selección rápida», que sale de la lista de filas (se detecta por forma —`itemsPerColumn`— además de por título, porque el título se traduce).
  - Contenido (`buildQuickTiles`, puro y testeado): Favoritos primero si hay canciones con me gusta; luego recientes locales (`getAllRecentData`) mezclados por tipo (playlists, artistas, canciones, álbumes, pódcasts; sin canciones eliminadas ni radios) para que ocho canciones no tapen todo; si el historial es corto se completa con tarjetas reales del Home de YouTube. Siempre cantidad par; sin datos, la cuadrícula se oculta.
  - Toque: canción → radio como en Selección rápida; álbum/artista/playlist/pódcast → su pantalla; Favoritos → lista Favoritos. Oculta en el chip Pódcasts y con un mood (params) activo.
- **Más carruseles, densos**:
  - Se intercalan hasta 4 filas de tu historial (`buildLocalShelves`): «Tus playlists», «Tus artistas», «Tus álbumes», «Escuchado recientemente» (mín. 4 elementos; se omite si no los hay), una tras el primer shelf y luego cada dos (`interleaveShelves`). Usan las mismas tarjetas grandes que el resto.
  - `HomeViewModel` carga solas hasta 3 páginas de continuación mientras haya menos de 12 shelves (solo Home sin mood); el scroll sigue paginando como antes.
  - Títulos de sección ya eran negrita (headlineMedium Bold); acentos verde/cian de Killmon sin tocar; sin branding Spotify.
- Strings nuevas: `home_recent_songs`, `home_your_artists`, `home_your_albums`, `home_your_playlists` (base inglés + `values-es`).
- Limitaciones honestas: las secciones por género/«Corridos» como en la captura dependen de lo que YouTube devuelva para tu cuenta/región (la parte de moods y géneros sigue al final como antes); «recientes» = lo que la app guardó al reproducir/abrir (hasta 30 por tipo). No hay swipe en los tiles ni long-press.

## Archivos
- `gradle/libs.versions.toml` (0.7.9 / 74)
- `composeApp/.../ui/component/RecentSearchRow.kt`, `ui/screen/other/SearchScreen.kt`
- Home: `ui/screen/home/HomeScreen.kt`, `ui/screen/home/HomeQuickAccess.kt` (nuevo), `ui/component/HomeQuickAccessGrid.kt` (nuevo), `viewModel/HomeViewModel.kt`, `di/ViewModelModule.kt`, `composeResources/values/strings.xml`, `values-es/strings.xml`
- test nuevo `composeApp/src/jvmTest/.../ui/screen/home/HomeQuickAccessTest.kt` (7 casos)
- `composeApp/src/jvmTest/.../ui/component/RecentSearchPlayableTest.kt` (nuevo)
- **core cambió** (submódulo): `media/media3/.../service/prefetch/PrefetchPolicy.kt` y `NextTrackPrefetcher.kt` (nuevos), `exoplayer/CrossfadeExoPlayerAdapter.kt`, `di/Media3ServiceModule.kt`, test `service/prefetch/PrefetchPolicyTest.kt` (nuevo). Sin commit: hay que commitear en core y actualizar el puntero del submódulo.

## Validación
- `./gradlew :androidApp:compileReleaseKotlin -PisFullBuild=false --no-daemon`: BUILD SUCCESSFUL (/tmp/killmon-079-compile.log).
- `:composeApp:jvmTest :lyricsService:jvmTest :kotlinYtmusicScraper:jvmTest :domain:jvmTest :data:jvmTest :media3:testDebugUnitTest`: BUILD SUCCESSFUL (/tmp/killmon-079-tests.log, rehecho tras el Home: /tmp/killmon-079-compile2.log y /tmp/killmon-079-tests2.log). composeApp 56, lyricsService 6, scraper 16, domain 20, data 11, media3 23; 0 fallos, 0 errores.

## Guion de Pruebas QA
1. Versión 0.7.9 (74); sin branding SimpMusic; región Colombia; arranque en frío sin ANR (5 veces).
2. Buscar con campo vacío → Recientes: deslizar a la derecha una canción: aviso «Se reproducirá a continuación» y suena justo después de la actual. Con nada sonando: empieza a sonar. En una fila de artista/álbum/playlist el swipe no hace nada; tocar, «+» y «X» siguen funcionando; sin franja verde en reposo, en tema claro y oscuro.
3. Prefetch en Wi-Fi: poner un álbum/playlist, esperar ~1 min en la primera pista. En logcat (tag `NextTrackPrefetcher`) deben aparecer «written to the player cache» para las 2 siguientes.
4. Offline real: tras el punto 3, activar modo avión durante la pista 1: debe pasar a la 2 y a la 3 sin cortes. Verificar que la 4 falla/se queda cargando (esperado).
5. Cambio de cola: durante el prefetch, saltar varias pistas o cambiar de playlist: no hay descargas de pistas viejas (log de cancelación), sin crash.
6. Shuffle: con shuffle activo, las pistas prefetcheadas son las que de verdad suenan después (modo avión tras la primera: siguen 2). Repeat-all en la última pista: precarga las primeras.
7. Datos móviles (sin Wi-Fi): pistas normales se prefetchean; con Ahorro de datos activado no hay prefetch; un podcast/mix largo (>16 MiB) no se descarga entera.
8. Red caída al cambiar de pista: no hay crash ni bucle de reintentos (máx. 3 intentos); al volver la red, la siguiente pista reproduce con normalidad.
9. Gapless/crossfade de 0.7.8 siguen igual; letras y búsqueda en vivo sin regresiones.
10. Ajustes → caché con límite pequeño: confirmar que el uso de disco respeta el límite y que no hay errores en log.
11. Home (comparar con la captura Spotify): bajo los chips, cuadrícula de 2 columnas con 6-8 tiles pequeños (carátula izq., título bold der., fondo gris); artistas con carátula redonda; toque en canción reproduce, en álbum/artista/playlist abre su pantalla, «Favoritos» abre Favoritos. Usuario nuevo (sin historial): tiles salen del Home de YouTube; sin internet ni historial no aparece cuadrícula ni hueco.
12. Home: ya no existe la fila alta «Selección rápida»; debajo hay muchas filas de carruseles con tarjetas grandes y títulos en negrita; intercaladas «Tus playlists/artistas/álbumes/Escuchado recientemente» según tu historial (solo si hay ≥4). Con shuffle/otros chips: Música mantiene cuadrícula; Pódcasts la oculta; elegir un mood la oculta.
13. Home: tras abrir, sin hacer scroll, aparecen ≥8-12 filas (carga automática de páginas); al hacer scroll sigue cargando; pull-to-refresh y cambio de región/idioma recargan sin duplicar filas ni crash. Probar tema claro (tiles legibles) y pantalla ancha/landscape.
14. Regresión: swipe en Recientes de Buscar, prefetch de 2 pistas (puntos 2-8), región CO, sin ANR al arranque en frío, sin branding SimpMusic.
