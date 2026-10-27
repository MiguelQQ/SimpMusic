# Killmon Music 0.7.13 (78) — JAM en el reproductor

## Alcance y entrega

Continuación de los cambios parciales de la ejecución interrumpida, sobre 0.7.12 en `killmon-0.7.3-auto`. Se conserva el trabajo previo y se completa en app, submódulo core y backend. Versión `0.7.13`, versionCode `78` en `gradle/libs.versions.toml`; User-Agent actualizado.

App y core quedan **sin commit ni push**. Backend **sí cambia** en `/workspace/killmon-api`: commit local `b2dce4c` (`Integrate shared JAM controls, member mixes and position-preserving gates`), sin push ni despliegue. El dueño debe desplegarlo para habilitar los mensajes y permisos de esta versión.

## Reproductor y cola compartida

- Mini-player y los tres estilos del full player muestran `En JAM`; durante la barrera muestran `En JAM · Cargando/Sincronizando`.
- Play/pausa, siguiente/anterior y seek pasan por JamPlaybackRouter hacia la sala para anfitrión y miembros. La disponibilidad de anterior/siguiente se calcula con la cola compartida, aunque el reproductor local tenga un único MediaItem.
- La hoja habitual de cola y la pestaña de cola Apple Music muestran la cola compartida, carátula, autor del aporte y acciones reproducir, subir, bajar y quitar. Los índices se convierten en qid del servidor.
- La pantalla JAM conserva creación, ingreso, miembros, salida y transferencia de anfitrión. Se retiran su cola y sus botones de transporte duplicados, y se aclara que todos pueden controlar.
- Al tocar reproducir desde búsqueda, biblioteca, home, álbum, artista o playlist, las rutas comunes `loadMediaItem` y `addMediaItem(playWhenReady=true)` envían `queue_add` con `play=true`. El servidor añade y selecciona atómicamente; si ya existe ese videoId, reproduce su qid sin duplicarlo. La adición simple no cambia la canción actual. Los catálogos de álbum/playlist se siguen encolando de forma serializada a 250 ms por canción.

## Mezcla automática

1. Cada teléfono aporta al entrar y al quedar menos de tres canciones posteriores a la actual. Se consideran likes y hasta cien canciones de biblioteca ordenadas por actividad reciente; se baraja, filtra la cola actual y limita a treinta videoId válidos distintos. Si no hay canciones propias disponibles, se usa la radio/recomendación de la canción actual, con timeout de 15 s.
2. Solo **el servidor** compone la mezcla mediante `queue_contribute {tracks}`. No hay elección de mezclador entre clientes ni carreras entre anfitrión y miembros. El servidor procesa cada comando de manera síncrona.
3. Baraja cada aporte y alterna miembros en round-robin; evita dos aportes automáticos consecutivos de la misma persona mientras haya opciones de otras. Deduplica por videoId y conserva la autoría original si otro miembro aporta una canción automática ya encolada.
4. Conserva pista actual, historial y canciones manuales. Las canciones movidas expresamente pasan a ser manuales, para respetar la edición. Reorganiza las futuras automáticas cuando llega un nuevo aporte; no reinicia la pista ni su generación.
5. La atribución `added_by`/`added_by_id` se toma de la cuenta autenticada. Cada miembro tiene una reserva acotada; los candidatos que no caben se conservan para una renovación posterior. La cola no supera cien canciones. Al alcanzar esa capacidad se libera historial antiguo, conservando veinte entradas anteriores, la actual y todas las siguientes, para que la mezcla continúe tras más de cien reproducciones.
6. Se amplía el límite WebSocket de 4 KB a 64 KB para admitir treinta canciones con metadatos; siguen vigentes los límites de treinta candidatos por aporte, validación de pistas y frecuencia de mensajes.

## Carga y sincronización sin volver al inicio

- Crear una sala con una canción avanzada conserva la posición local al sembrar la primera entrada. El aporte automático del anfitrión espera esa semilla. La espera de respuesta no borra su MediaItem.
- Al entrar otro teléfono o detectar buffering se abre una barrera nueva con la posición común del reloj. Todos pausan; `beginGate` y `releaseGate` conservan esa posición. Un invitado que bufferea desde cero no puede imponer ese cero al resto.
- Si el videoId ya está cargado, el puente conserva el MediaItem y evita clear/set y una preparación redundante; si un error dejó el reproductor en STATE_IDLE, vuelve a prepararlo para permitir recuperación. Se alinea una vez con la posición de la nueva generación si la diferencia supera 0,5 s; no busca cero repetidamente durante loading.
- Se reenvía `ready` para cada generación cuando Media3 llega a STATE_READY. Los mensajes ready/clock de generaciones anteriores no liberan ni alteran la barrera actual. Durante loading no se aplican correcciones periódicas de seek.
- El timeout sigue en 20 s y el anfitrión fuerza automáticamente la liberación tras la señal del servidor. Quien termine después se incorpora al segundo vivo. El servidor libera la posición congelada, sin reiniciarla.
- Todos pueden enviar transporte y seek; solo el anfitrión publica `pos`, comunica `ended`, fuerza el timeout y transfiere host. Los seek compartidos abren una barrera en la posición solicitada. El anfitrión conserva la autoridad de reloj y los miembros corrigen deriva superior a 500 ms.
- Se mantiene reproducción YouTube directa y resolución normal en cada teléfono. La app solo usa `https://killmon.seriepokemon.online`, WebSocket `wss://killmon.seriepokemon.online/api/jam/ws`; no solicita audio S2 al servidor de sala.

## Artwork Killmon

`rememberHolderPainter` usa el vector gris `killmon_gray.xml`, derivado del logo Killmon existente. Así se sustituye el placeholder heredado de SimpMusic en reproductor y miniaturas que usan ese pintor. El mini-player prioriza la carátula de la canción de JAM y usa `https://i.ytimg.com/vi/VIDEO_ID/hqdefault.jpg` cuando no viene en metadata. Se corrige además la creación de miniaturas de now-playing para canciones que aún no existen en la base local: utiliza metadata/videoId real y evita la URL con ID nulo. El fallback de error es el logo gris.

## Validación

Comando de compilación y suites del cliente:

```sh
JAVA_HOME=$HOME/opt/jdk-21.0.13+9 ANDROID_HOME=$HOME/android-sdk ./gradlew :androidApp:compileReleaseKotlin :composeApp:jvmTest :listenTogether:jvmTest :data:jvmTest :domain:jvmTest :media3:testDebugUnitTest -PisFullBuild=false --no-daemon
```

Resultado: **BUILD SUCCESSFUL**. Suites: composeApp 56, listenTogether 17, data 12, domain 20, media3 26: **131 tests, cero fallos, errores u omitidos**. Se verifican transporte HTTP/WebSocket real contra fixture local, User-Agent, selección con `play=true` frente a adición simple, aportes válidos deduplicados, posición avanzada y atribución durante loading, gate, timeout, reconexión y umbral de deriva.

Backend: `npm test`, **61 tests aprobados**, incluidos controles de invitados, selección duplicada sin duplicación, mezcla/atribución, reserva y continuidad tras cien canciones, edición manual, lote WebSocket de treinta canciones, barrera de entrada tardía, buffering desde cero, generación obsoleta y posición avanzada conservada tras ready/timeout. `git diff --check` se verifica en los tres repositorios.

No se ha desplegado ni contactado producción ni probado reproducción en dos teléfonos físicos. La sincronización audible, redes móviles y layouts deben validarse con el siguiente guion.

## Guion de Pruebas QA

1. Desplegar el commit local de killmon-api antes de probar la app 0.7.13 (78). Actualizar dos teléfonos con cuentas distintas desde 0.7.12; conservar cuenta, biblioteca y región CO. Verificar servidor Killmon fijo y YouTube directo en ambos.
2. En A reproducir hasta 60–90 s y crear JAM; unir B con red lenta. Ambos muestran Cargando/Sincronizando en mini y full. A pausa cerca de su posición actual y conserva su MediaItem. Cuando B queda listo, ambos continúan desde ese segundo, sin repetir el inicio. Repetir estando en pausa, al reconectar y durante buffering a mitad de canción.
3. Mantener B sin poder cargar durante más de 20 s: A continúa desde la posición congelada tras timeout; B se incorpora al segundo vivo al recuperarse. Comprobar ausencia de bucles de seek-a-0 y que los ready viejos no liberan una canción nueva.
4. En cada teléfono, desde mini-player, full player en los tres estilos y controles de sistema disponibles: play/pausa, siguiente/anterior y seek. El cambio afecta a todos; un miembro no recibe not_host por transporte. Introducir deriva mayor de 0,5 s y comprobar corrección.
5. Abrir la cola habitual desde ambos teléfonos: mismas entradas, títulos, carátulas y aportantes. Reproducir una fila, quitar actual/no actual y subir/bajar. Verificar que la edición se propaga y que la pantalla JAM solo gestiona la sala y sus miembros.
6. Estando en sala, buscar una canción y tocar reproducir: se añade y suena para todos después de ready. Repetir con una canción ya encolada: se reproduce sin duplicarla. Repetir desde biblioteca, home, artista, playlist y álbum; añadir a cola desde menú debe conservar la reproducción actual.
7. Con bibliotecas/likes distintos, crear/unirse y comprobar aportes de ambas cuentas alternados, sin duplicados por videoId ni dos automáticas del mismo miembro si hay alternativas. Quitar hasta dejar menos de tres futuras sin cambiar de canción y comprobar renovación. Avanzar suficientes canciones para probar varias renovaciones y más de cien entradas acumuladas.
8. Usar una cuenta sin likes ni biblioteca y comprobar aporte mediante radio de la canción actual. Probar aporte vacío/error de red y recuperación al cambiar pista. Verificar que un nuevo aporte no reinicia la canción ni deshace movimientos manuales.
9. Verificar portada de JAM en mini y full con canción nueva para la biblioteca local, metadata sin thumbnail y URL de imagen rota. Debe aparecer thumbnail YouTube o logo gris Killmon; nunca branding SimpMusic como placeholder. Repetir fuera de JAM.
10. Transferir host; salir o desconectar al anfitrión durante más de 8 s. Verificar reloj, timeout y controles del nuevo host y miembros. Navegar fuera de JAM, apagar pantalla y volver. Salir de sala y comprobar reproducción normal y restauración de preferencias.
