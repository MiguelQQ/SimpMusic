# Killmon Music 0.7.12 (77) — JAM con YouTube directo

## Alcance

Continuación de los cambios locales sin commit de la ejecución interrumpida, revisados contra KILLMON_V0711_PLAN.md y el protocolo actual de `/workspace/killmon-api/src/jam/{rooms,routes}.js` (solo lectura). Este documento sustituye para Android las instrucciones de streaming S2 y de pegar enlaces del plan 0.7.11.

- Versión `0.7.12`, versionCode `77` en `gradle/libs.versions.toml`.
- Cambian la app y **core sí**. Sin commit, push, despliegue, APK/AAB ni build release. `compileReleaseKotlin` solo compila Kotlin.
- Backend intacto: el endpoint de audio S2 puede seguir disponible para otros clientes; Android no lo solicita.
- Se conserva región CO y la obtención asíncrona del token desde el tipo `KillmonAccountRepository` registrado en Koin.

## Reproducción y sincronización

1. Cada teléfono prepara un único MediaItem con el videoId de YouTube, URL `https://music.youtube.com/watch?v=VIDEO_ID`, descripción SONG y clave de caché igual al videoId. Usa la factoría y el resolver de streams normales de la app (incluida la caché local habitual).
2. Se eliminan de Android la petición y respuesta WebSocket `audio`, los tokens/URL S2, la detección de esas URL en JamPlaybackRouter y las excepciones que impedían resolver YouTube durante JAM. JamPlaybackRouter y JamSessionCommands permanecen para compartir cola y controles; no deciden el origen del audio.
3. El puente carga con `playWhenReady=false`. Envía `ready {seq}` cuando el reproductor local llega a `STATE_READY` para la pista y generación actuales, mediante evento y comprobación de respaldo cada 150 ms. Ya no exige tres segundos adicionales de buffer ni una duración conocida.
4. Al desconectar se pausa y se invalidan loadedSeq/readySeq. Al reconectar se vuelve a preparar la pista y a informar ready aunque seq sea el mismo. Una señal para otro videoId o seq se descarta.
5. El anfitrión mantiene el reloj real: `pos` cada segundo, con posición en segundos y `t` en milisegundos del servidor. Los invitados saltan al tiempo compensado del anfitrión cuando la deriva supera 500 ms; también al incorporarse tarde.
6. Ante `gate.timed_out=true`, el anfitrión envía automáticamente `force_play` una vez por generación y conexión y muestra aviso. Se mantiene el botón manual. S2 establece ese indicador después de 20 s; no se envía un forzado anticipado.
7. Un error de YouTube marca como fallido al miembro **en su propio teléfono**, invalida su ready y muestra cómo reintentar. No se finge que esté listo. El forzado libera al resto a los 20 s aunque siga fallando; si el teléfono recupera STATE_READY, vuelve a informar y se incorpora al tiempo actual.
8. Límite del protocolo sin cambiar backend: `rooms.js` solo publica `loading` por miembro, no tiene `failed`/`load_error`. Por ello otros teléfonos ven Cargando hasta el forzado; no pueden ver el detalle de fallo remoto. No se inventa un mensaje incompatible ni se usa ready para ocultar un error.
9. La app espera `welcome` antes del primer ping. Evita la carrera con la consulta asíncrona de la cuenta durante `auth`, que podía causar el rechazo «primero envía auth».

Las operaciones del reproductor y las variables del puente se confinan a Main; la red, el almacenamiento y el token se gestionan en corrutinas fuera de Main. La construcción del MediaSource SONG evita la lectura bloqueante de preferencias de vídeo; la resolución de red sigue en el hilo de carga de Media3. Se conservan la supresión de crossfade/SponsorBlock/silencio saltado, velocidad 1, autoridad del anfitrión y transferencia de anfitrión.

## Cola y UI

- Se elimina el campo y el método de pegar enlaces/ID de YouTube en JAM.
- Se añaden canciones desde búsqueda, biblioteca, playlists y los controles normales. En sala, el menú dice «Añadir a la JAM»; las restricciones de la cola local de radio/infinita no bloquean esa acción.
- SharedViewModel deriva la selección hacia la sala antes de limpiar el reproductor. El handler tampoco permite que una limpieza de cola normal borre la pista de JAM; el puente controla directamente su único MediaItem.
- Las selecciones de playlist/álbum añaden la elegida y el resto del catálogo cargado. Las adiciones se serializan con 250 ms entre canciones, dejando margen para pos/ping dentro del límite S2 de 60 mensajes/10 s. La cola del servidor conserva su máximo de 100 pistas y sus límites de duración.
- Todos siguen pudiendo quitar, subir y bajar elementos por qid, incluidos duplicados. Los controles de transporte y la transferencia siguen siendo del anfitrión.
- Se sustituyen «Audio desde el servidor Killmon» y los créditos heredados de protocolo por YouTube directo y Killmon. Las traducciones antiguas de esos créditos usan el texto base de Killmon.

## Servidor de salas y migración

Producción usa exclusivamente `https://killmon.seriepokemon.online`; WebSocket `wss://killmon.seriepokemon.online/api/jam/ws`. El constructor público de ListenTogetherSession fija esa base; solo el constructor interno admite una base de fixture para pruebas locales.

La preferencia heredada `lt_server_url` se elimina mediante JamServerPreferencesMigration tanto en Android/createDataStore como en JVM, sin tocar cuenta ni región. Aunque hubiera un valor antiguo, no participa en el enrutamiento. Se retiran del ViewModel los lectores/escritores de servidor personalizado y la pantalla de ajustes no ofrece esa opción. Se muestra «Servidor Killmon (AWS)».

## Protocolo verificado (solo lectura del backend)

| Mensaje | Campos enviados por Android |
| --- | --- |
| auth | token, code, caps: {ready: true} |
| ready | seq |
| pos | seq, position (segundos), t (ms del servidor) |
| queue_add | track: {videoId, title, artist, thumbnail, durationMs} |
| queue_remove | qid |
| queue_move | qid, to (índice desde cero) |
| play / pause / seek | position (segundos) cuando corresponde |
| next / prev / force_play | sin campos adicionales |
| ended | seq |
| transfer_host | user_id |
| ping / sync / leave | sin campos adicionales |

La cola recibida usa duration_ms, current_qid y qid; el anfitrión usa host_id. Se mantienen todos esos nombres exactamente como S2 los publica. Android no envía `audio` ni `buffer_ready`.

## Archivos principales

App: `gradle/libs.versions.toml`, recursos strings (base/es y limpieza de créditos traducidos), `ListenTogetherScreen.kt`, `ListenTogetherSettingsScreen.kt`, `ListenTogetherSettingsViewModel.kt`, `ListenTogetherViewModel.kt`, `SharedViewModel.kt`, `ui/component/ModalBottomSheet.kt` y este plan.

Core: `data/.../ListenTogetherPlaybackBridge.kt`, `ListenTogetherRepositoryImpl.kt`, `ListenTogetherPrefs.kt`, `MediaServiceHandlerImpl.kt`, `JamServerPreferencesMigration.kt`, creadores de DataStore Android/JVM; modelos de sala y `JamPlaybackRouter.kt` en domain; `Media3ServiceModule.kt` y `MergingMediaSourceFactory.kt`; `ListenTogetherSession.kt`, `JamSync.kt` y pruebas de sincronización/transporte/migración.

## Validación local

Comando obligatorio, con los tests en la misma ejecución:

```sh
JAVA_HOME=~/opt/jdk-21.0.13+9 ANDROID_HOME=~/android-sdk ./gradlew :androidApp:compileReleaseKotlin :composeApp:jvmTest :listenTogether:jvmTest :data:jvmTest :domain:jvmTest :media3:testDebugUnitTest -PisFullBuild=false --no-daemon
```

Resultado final: **BUILD SUCCESSFUL**, compilación Kotlin Android aprobada. Tests: composeApp 56, listenTogether 16, data 12, domain 20, media3 26: **130 en total, cero fallos, cero errores y cero omitidas**. `git diff --check` pasa tanto en la app como en core. Las pruebas comprueban STATE_READY, umbral de deriva, codec de cola, transferencia, migración de cualquier override sin alterar CO/cuenta, y transporte HTTP/WebSocket contra fixture local: auth sin ping prematuro, ready sin audio S2 previo, rechazo de ready obsoleto, campos de comandos, error local, forzado tras timeout y ready de la misma generación tras reconectar. No se ha reproducido YouTube en dos teléfonos físicos ni contactado el servidor de producción en esta sesión.

## Guion de Pruebas QA

1. Actualizar una instalación con servidor antiguo/personalizado guardado: versión 0.7.12 (77), región CO, cuenta conservada, servidor Killmon AWS y ninguna opción para cambiarlo. Abrir JAM sin crash Koin.
2. Con dos cuentas y dos teléfonos, crear/unirse a una sala. Comprobar resolución YouTube normal en cada teléfono y ninguna petición Android a `/api/jam/audio` ni mensaje `audio`. No deben aparecer textos de audio S2 ni campo de pegar enlaces.
3. Desde búsqueda, biblioteca, playlist/álbum y menú de canción añadir/reproducir/encolar. La pista actual no se interrumpe al añadir. Ambos ven la misma cola; comprobar duplicados, quitar y reordenar desde cualquier miembro. Probar playlist de más de 60 canciones dentro del máximo de sala y comprobar que no aparece rate_limited.
4. Cambiar pista con red lenta en B: A permanece en silencio hasta que B llega a STATE_READY, sin exigir tres segundos extra. Probar pistas cortas y duración desconocida aceptadas por la cola.
5. Mantener B cargando o provocar fallo de YouTube: B muestra fallo local si hay error; después de unos 20 s A avisa y fuerza automáticamente el inicio. A continúa sin ERROR_UNKNOWN causado por la barrera. Si B recupera STATE_READY, salta al segundo actual.
6. Pausar, reanudar, avanzar, retroceder y mover el deslizador en A; B sigue. Introducir deriva mayor de 0,5 s y verificar salto. Un invitado no cambia el reloj desde pantalla, notificación ni controles del sistema.
7. Entrar tarde y cortar/reponer la conexión de B durante carga y durante reproducción: pausa local, auth correcto, nueva preparación y ready reenviado para el mismo seq, incorporación al segundo vivo.
8. Transferir el anfitrión manualmente; salir A y repetir con desconexión de A durante más de 8 s. El nuevo anfitrión mantiene reloj, cola y liberación de la barrera.
9. Navegar fuera de JAM y apagar pantalla: sesión conservada y sin ANR. Salir de JAM y reproducir normalmente: preferencias restauradas, YouTube normal y ninguna adición pendiente destinada a otra sala.
