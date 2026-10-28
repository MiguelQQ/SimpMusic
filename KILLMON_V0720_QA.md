# Killmon Music 0.7.20 — guion QA (Grokbot)

Misma cuenta Killmon en el teléfono (Android) y en el PC (Desktop). El API de Connect tiene que estar corriendo con el arreglo del ack (`devices.js`: el presence que confirma un control no guarda el `playback` viejo). Volumen del usuario a la mitad antes de cada caso de audio (en el PC, el altavoz de la cápsula; en el teléfono, el salto a vigilar es el del audio de la pista, no el de `STREAM_MUSIC`). Anotar 0.7.20 (versionCode 85).

Build de esta rama: `feature/killmon-0.7.20-connect-fixes` en el repo y en `core/`. Sin commit, push, merge ni release en este cambio.

## 1. Volumen en el PC, local

Inicio → cápsula de abajo (mini-player Desktop). Icono de altavoz, popup, dejar el número cerca de 30. Reproducir una canción de Inicio. Al terminar, o con Siguiente en la cápsula, la pista nueva entra a ese mismo nivel desde el primer momento audible. No hay un golpe a tope ni una bajada después. Repetir con Anterior.

## 2. Volumen en el teléfono, local

Inicio → barra mini-player de abajo (la de siempre). Abrir Now Playing tocando la barra. Siguiente dentro de Now Playing: la pista nueva no entra más fuerte que la anterior y no baja sola un segundo después. El volumen de `STREAM_MUSIC` no se mueve al cambiar de canción.

## 3. Volumen por Connect

El PC es el que suena (Ajustes → Dispositivos: el PC sale como `Reproduciendo · <canción>`). En el teléfono, Siguiente del mismo mini-player de siempre. El audio del PC cambia de pista sin el golpe a volumen máximo. Repetir con el teléfono como aparato activo y el Siguiente hecho en el PC: el teléfono no pega el golpe. Cada aparato usa su propio volumen; Connect no lo transporta.

## 4. Pausa y resume Android → PC

PC sonando. En el teléfono, el botón de pausa del mini-player (el de la barra de siempre). El PC calla en alrededor de un segundo y el icono pasa a play en los dos. Pulsar play en el teléfono, en el mini-player y otra vez desde Now Playing (tocar la barra abre la hoja de Now Playing de siempre). El PC vuelve a sonar desde el mismo sitio. El icono queda en pausa solo mientras no hay audio. Probar también el play del centro de Now Playing, no solo el de la barra.

## 5. Seek y saltos mientras el PC suena

En el Now Playing del teléfono, arrastrar la barra de progreso, ±10 s si el estilo los tiene, Siguiente y Anterior. El PC obedece. La carátula, el título y el reloj del teléfono son los de esa pista, los mismos campos que cuando el teléfono es el que suena.

## 6. La UI es la misma

Con el PC activo, el teléfono no muestra la columna negra de `ConnectRemotePlayer` (carátula pequeña centrada, botón «Escuchar en este dispositivo» como botón ancho debajo de los tres iconos, cola numerada en texto). Muestra el mini-player normal y, al tocarlo, el Now Playing del estilo elegido en Ajustes → Interfaz (Spotify, M3 o Apple Music). Se lee `En {nombre del PC}`. En el PC, mientras el teléfono es el activo, la cápsula de siempre muestra la misma canción y `En {nombre del teléfono}`, no una tarjeta `ConnectSessionBar` distinta. Play, pausa, seek y skip de esa cápsula mueven el teléfono y el PC no suena. Like, letras, volumen y estilo de Now Playing siguen siendo locales.

## 7. Traer el audio desde Ajustes

Ajustes → Dispositivos (sección Dispositivos, encima del QR). Tocar la fila del aparato que no está reproduciendo. El audio pasa a ese aparato, cerca de la misma posición, al volumen que ese aparato ya tenía, sin spike. El otro deja de sonar y sigue mostrando el mismo reproductor, ahora como control remoto.

## 8. JAM no se mezcla

Entrar en una sala JAM. Connect no sustituye la cola de la sala. El User-Agent de la sala es `Killmon Music/0.7.20`.

## 9. Versión

La pantalla de ajustes o el log de la app dice 0.7.20. `versionCode` 85.

## 10. Salida de audio → Killmon lista los dos (requisito 4)

PC sonando una canción con título reconocible.

1. En el PC, abrir Now Playing y el icono de auriculares. Se abre la hoja «Salida de audio».
2. Bajar hasta la sección **Killmon** (debajo de las salidas del sistema; la hoja hace scroll si no cabe).
3. Tienen que verse las dos filas de la cuenta: el PC (por ejemplo `Windows · Miguel`) y el teléfono Android, con su nombre.
4. El que suena lleva el check y el estado `Reproduciendo · <canción>`. Si se pausa, esa fila pasa a `En pausa · <canción>` y el check se queda.
5. La otra fila no lleva check. Su estado es `En línea` o `Este dispositivo`.
6. Falla si solo aparece `Windows · Miguel · Reproduciendo · …` con check y el teléfono no está en la lista ni al hacer scroll.

## 11. Transferir desde el picker del PC

En esa misma sección **Killmon**, tocar la fila del teléfono.

- El check pasa al teléfono.
- El PC deja de sonar. El audio sale solo del teléfono, la misma canción y cerca de la misma posición, al volumen que el teléfono ya tenía (sin spike).
- El PC queda de controlador: el mismo mini-player y el mismo Now Playing, con `En {nombre del teléfono}`.
- Play, pausa, siguiente, anterior y seek de la cápsula del PC mueven el teléfono. El PC no vuelve a sonar hasta que se le transfiera otra vez.

## 12. El selector del teléfono hace lo mismo

En el teléfono:

1. Now Playing → «Salida de audio» → sección **Killmon**.
2. Ajustes → Dispositivos.

En los dos sitios están el PC y el teléfono. El check está solo en el que suena. Tocar la fila del PC.

- El audio vuelve al PC, misma canción y posición, volumen propio del PC.
- El check queda en el PC en la hoja y en Ajustes.
- El teléfono no suena. Su mini-player y Now Playing siguen esa pista (`En {nombre del PC}`) y sus controles de transporte van al PC.

## Hecho en máquina, sin los dos aparatos

- `./gradlew :composeApp:compileKotlinJvm :domain:jvmTest :data:jvmTest :listenTogether:jvmTest` — BUILD SUCCESSFUL. `ConnectPlaybackPolicyTest` 5/5, `ConnectSessionTest` 4/4, `UserAgentHandshakeTest` 1/1, domain 36, data 12, listenTogether 25, sin fallos.
- `node --test test/connect.test.js` en `killmon-api` — 6/6, incluido «el ack de un play no deja que el is_playing viejo deshaga el resume».
- No se abrió la app en un PC ni en un Android. Los pasos 1–12 hay que oírlos en los dos aparatos.
