# Killmon Music 0.7.11 (76) — JAM

## Alcance y entrega

Implementación local en `/workspace/killmon-api`, `/workspace/killmon-web` y esta app, incluido **core** (MiguelQQ/core, rama `killmon-0.7.3-auto`). Se revisaron y reutilizaron los cambios pendientes de la sesión anterior del API. No se accedió a servidores, no hubo despliegue, push, APK/AAB ni firma. Solo el API lleva commit local: `4460ba0` (autor técnico Codex; no se cambió la configuración global de Git). App, web y core quedan sin commit para que los revise y guarde el dueño. Al guardar, hacer primero el commit de core y después actualizar su referencia en la app.

Versión: `gradle/libs.versions.toml`: `0.7.11`, código `76`. No se modificó la política de región CO; las pruebas de preferencias regionales siguen pasando.

## Arquitectura

- Se conservan la entrada de Listen Together, el repositorio de dominio, los modelos y el puente al reproductor, adaptados a **JAM**. Se retira el transporte a Metrolist/SimpMusic; el único servidor de producción es `https://killmon.seriepokemon.online`.
- `ListenTogetherSession` usa JSON/Ktor: crear o unirse por HTTP con Bearer; WebSocket `/api/jam/ws`, primer mensaje `auth {token,code,caps:{ready:true}}`. No pone el token de cuenta en la URL del socket. Reconecta hasta 10 intentos, con demora creciente y una nueva lectura de la credencial en cada intento.
- Koin obtiene `KillmonAccountRepository`, que ya estaba registrado, y llama a `jamAccessToken()` asíncrono. Este método usa el almacenamiento seguro y el mecanismo de renovación de la cuenta existente; no se busca una implementación concreta sin registrar.
- La sesión y el puente sobreviven a la pantalla. La red y el almacenamiento se ejecutan fuera de Main; las operaciones sobre Media3 se ejecutan en Main. El camino de audio JAM salta la resolución YouTube y las lecturas bloqueantes de preferencias de la factoría heredada.
- El API mantiene miembros, cola, anfitrión, fase, posición y generación `seq` en memoria. No se necesitan migraciones SQL. Al reiniciar se pierden las salas.

## Uso en Android

1. Iniciar sesión en la cuenta Killmon y abrir **JAM** desde el icono de personas/Home/Biblioteca.
2. Pulsar Conectar, Crear JAM o introducir el código de **6 caracteres** y Entrar a la JAM. El nombre viene de la cuenta, sin una identidad adicional para la sala.
3. Compartir o copiar el código/enlace. Si el anfitrión tenía una canción cargada, se añade al crear la sala. También se puede pegar un enlace/ID en JAM. Al elegir canciones del catálogo o «reproducir después» durante JAM se añaden a la cola compartida.
4. Todos pueden usar Añadir, Quitar, Subir y Bajar desde la cola de JAM. Se permiten IDs repetidos, identificados por `qid`.
5. Solo el anfitrión controla play, pausa, seek, anterior y siguiente. Los comandos de la app y de notificación se enrutan a la sala; un invitado no cambia el reloj. El seek desde el panel del sistema se rechaza porque ese callback no expone el destino; el anfitrión usa el deslizador de la app.
6. Salir de la pantalla conserva la sesión. «Salir de la JAM» abandona la sala y detiene/vacía la reproducción de la sala. Se restauran velocidad, silencio, shuffle y repetición previos; la selección posterior fuera de JAM sigue el flujo normal.

## Puerta de carga y sincronización

Cada pista nueva incrementa `seq` y entra en `loading`. Todos, incluido el anfitrión, preparan **un solo MediaItem** del proxy S2 con `playWhenReady=false`. No se carga una cola local que pueda avanzar por su cuenta. La cola completa vive en el servidor y se presenta en JAM.

La política pura `JamSync.ready` exige Media3 `STATE_READY` y al menos **3 segundos** por delante, o lo que quede si la pista es más corta. Se comprueba cada 150 ms. Se envía `ready` con la generación que realmente se cargó; avisos de una generación antigua no desbloquean otra canción.

El servidor espera a todos los miembros conectados que anuncian audio. La app muestra «Esperando a que los demás carguen…» y Cargando/Listo por miembro. A los **20 s** muestra el aviso y el anfitrión puede pulsar «Forzar reproducción». El servidor rechaza forzar antes de ese plazo. El rezagado permanece como Cargando y se incorpora al tiempo actual al acabar de preparar el audio. Los clientes v1 sin `caps.ready` conservan compatibilidad como observadores; publicar la web nueva para que participe en la barrera.

El anfitrión envía su posición real cada **1 s** con `seq` y hora de captura estimada en el reloj del servidor. Ping/pong calibra ese reloj sobre tiempo monotónico y RTT; el servidor retransmite `clock` con hora de servidor. Los invitados calculan la posición compensando el tiempo en tránsito y hacen seek si el error es **mayor de 500 ms**. Llegar tarde o reconectar exige cargar y después saltar a esa posición viva. La desconexión pausa el reproductor local hasta recuperar el estado.

Se suprimen crossfade, silencio saltado, SponsorBlock, cambios de velocidad y avance de radio durante JAM. El anfitrión avisa `ended {seq}` para avanzar y repetir la barrera; hay un respaldo del servidor si conoce la duración. El relevo pasa al siguiente conectado al salir explícitamente, o después de **8 s** desconectado; los miembros ausentes se eliminan a los 45 s.

## Streaming desde S2

`audio {video_id}` devuelve una ruta de `killmon.seriepokemon.online` y un token HMAC-SHA256 corto, de **1 hora**, ligado a usuario/sala/videoId y derivado de `JWT_SECRET`. Se solicita un token al cargar cada pista o reconectar. El MediaItem utiliza esa URL; no una URL googlevideo resuelta en el teléfono. El servidor vuelve a comprobar pertenencia y presencia de la pista en la cola para cada petición. Admite `?t=` o `Authorization: Bearer`.

`yt-dlp` se invoca con `execFile`, sin shell, videoId validado, solo audio, metadata y URL. El proxy descarga desde la misma IP S2 que resolvió la URL y retransmite sin guardar audio en disco. Soporta `Range`, 206, `Content-Range`, `Accept-Ranges`, y vuelve a resolver una vez al recibir 403/410/401. Valida también los destinos de redirección.

Límites actuales: **15 min** por pista, sin directos ni duración desconocida; 8 miembros, 100 elementos de cola, 3 salas por dueño, 500 salas; 3 procesos yt-dlp simultáneos y 3 esperando, 4 streams por usuario y 40 totales. Caché de resolución 2 h, caché negativa 20 s, timeout de yt-dlp 30 s, apertura total 75 s y cabeceras upstream 20 s; stream máximo 20 min. API y nginx omiten las queries de los logs de acceso para no registrar el token de audio.

Limitaciones: no se ha reproducido audio real desde S2 ni usado dos dispositivos físicos en esta sesión. YouTube puede requerir actualizar yt-dlp o ajustes de runtime/cookies que el dueño debe verificar en S2. Las salas no persisten y cada cuenta se representa como un miembro (para QA usar cuentas distintas). No se añade Chromecast para JAM ni expulsión/bloqueo/solicitudes de aprobación heredadas. La cola se edita desde la pantalla JAM. El streaming tiene coste de salida de S2 por cada oyente.

## Archivos principales

- API: `src/jam/rooms.js`, `routes.js`, `audio.js`, `audio-token.js`, `src/app.js`, `Dockerfile`, `.env.example`, `README.md`, `DEPLOY_JAM.md`, `test/{rooms,jam-api,audio}.test.js`.
- App: versión, traducciones del nombre JAM, `ListenTogetherScreen.kt`, `ListenTogetherSettingsScreen.kt`, `ListenTogetherViewModel.kt` y este documento.
- **Core sí cambió**: `service/listenTogether` (sesión JSON, reloj/política y pruebas; se retira el transporte anterior), `data/listentogether` (puente y repositorio), `data/di/ListenTogetherModule.kt`, token de cuenta, interfaz de cuenta, modelos/repositorio de sala, `domain/.../JamPlaybackRouter.kt`, handler Android, factorías de Media3 y callback de sesión/notificación.
- Web: `js/jam.js`, `jam/index.html`. Usa HTMLAudioElement, S2, barrera, reloj del anfitrión y cola compartida. Los navegadores pueden pedir un toque para autorizar audio.

## Validación local

- API: `npm test`: **55 pruebas, 55 pasan**. Salas, barrera, timeout/forzado, autoridad/posición, cola, relevo, tokens, resolución simulada, caché/concurrencia, proxy Range/403, timeout upstream y redirecciones no permitidas.
- Web: `npm test`: **2 pasan**; `node --check js/jam.js` pasa. No se realizó prueba interactiva de navegador.
- Kotlin: `JAVA_HOME=~/opt/jdk-21.0.13+9 ANDROID_HOME=~/android-sdk ./gradlew :androidApp:compileReleaseKotlin -PisFullBuild=false --no-daemon`: pasa (también se ejecutó como parte de la validación conjunta).
- `:composeApp:jvmTest`: **56**; `:listenTogether:jvmTest`: **16**; `:data:jvmTest`: **11**; `:domain:jvmTest`: **20**; `:media3:testDebugUnitTest`: **26**. Total **129**, sin fallos ni omitidas. Incluye la conexión HTTP/WS real contra un fixture local, codec JSON de cola/fase/anfitrión, umbral de deriva, ready y comandos de MediaSession. Las pruebas heredadas de protobuf permanecen como regresión; la app ya no usa protobuf en la red.
- Comando conjunto: `JAVA_HOME=~/opt/jdk-21.0.13+9 ANDROID_HOME=~/android-sdk ./gradlew :androidApp:compileReleaseKotlin :composeApp:jvmTest :listenTogether:jvmTest :data:jvmTest :domain:jvmTest :media3:testDebugUnitTest -PisFullBuild=false --no-daemon`. BUILD SUCCESSFUL.

## Despliegue por el dueño

Ver `/workspace/killmon-api/DEPLOY_JAM.md`: contiene **el vhost completo**, basado en `/tmp/kvhost.conf`, la ruta WebSocket con Upgrade y la ruta de audio sin buffering/caché y con timeouts largos. También contiene actualización de yt-dlp y comprobaciones manuales.

Orden: reconstruir y levantar imagen del API en S2 → publicar web actualizada → validar y recargar nginx → probar el audio y la sala → distribuir app 0.7.11. No cambiar `JWT_SECRET` salvo que se quiera invalidar tokens. Una imagen antigua sin yt-dlp no sirve para esta app.

## Guion de Pruebas QA

1. Dos teléfonos y dos cuentas distintas: abrir JAM sin crash Koin, comprobar versión 0.7.11 (76), región CO y login persistente.
2. Crear sala en A con canción ya cargada y entrar desde B usando el código. Verificar mismo nombre/pista/cola y audio solicitado a `/api/jam/audio/...` en ambos; ninguna resolución local de audio YouTube.
3. Limitar la red de B y cambiar pista en A. Ambos deben permanecer en silencio, con «Esperando…», A Listo y B Cargando. Con buffer suficiente en B, ambos arrancan.
4. Mantener B cargando más de 20 s: mostrar aviso sin inicio automático. A fuerza; B sigue Cargando y al cargar salta al segundo actual. Comprobar que un ready atrasado no libera la siguiente canción.
5. A pausa, reanuda y arrastra el deslizador. B sigue a A; introducir deriva mayor de 500 ms y verificar corrección. Un invitado no debe alterar play/pause/seek de la sala desde app ni notificación.
6. Desde B añadir canciones del catálogo/enlace, quitar una y reordenar. Verificar orden en A y web; probar duplicados. Al terminar/avanzar, una única siguiente pista y nueva barrera. Quitar la actual debe seleccionar la siguiente.
7. Unir un tercero a mitad de canción; debe preparar y saltar al segundo vivo. Cortar/reponer red a B: pausa local, reconexión y resincronización.
8. Salir A explícitamente: B recibe el mando. Repetir matando la conexión de A y esperar unos 8 s. Verificar que el nuevo anfitrión emite su reloj real.
9. Audio: Range produce 206 y longitud/rango coherentes; sin token, caducado, usuario ajeno o pista fuera de cola se rechaza; >15 min y directos no cargan y muestran error. Verificar que actualizar yt-dlp recupera una resolución rota.
10. Abrir web actualizada junto a Android, añadir/quitar/mover y probar anfitrión web. Si el navegador bloquea autoplay, usar «Toca para activar el audio».
11. Cambiar de pantalla y apagar pantalla manteniendo la sesión; comprobar notificación/controles y ausencia de ANR. Desactivar ahorro de batería si el sistema suspende la red durante esta prueba.
12. Salir de JAM y reproducir normalmente: sin URL de sala, con preferencias de reproducción previas. Reiniciar el API en una prueba controlada debe cerrar las salas con aviso.
