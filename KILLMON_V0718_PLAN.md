# Killmon Music 0.7.18 (83) — barra Windows y Connect de verdad

## Alcance

Sobre 0.7.17, rama `killmon-0.7.3-auto`. Sin commit ni push. `gradle/libs.versions.toml`: **0.7.18 / versionCode 83**. User-Agent JAM: `Killmon Music/0.7.18`.

El updater no se reescribe. El icono Android verde no se toca. JAM no se toca.

**Backend** (`/workspace/killmon-api`): sí cambia, sin commit. Un latido del aparato que suena ya no pisa una pausa o un seek que el otro aparato mandó y el activo todavía no confirmó. Hay que desplegar este cambio encima de `f0722ec`. Sin el API de Connect, la lista de dispositivos sigue vacía.

> No hay teléfono ni un Windows gráfico aquí. No se abrió la ventana. Compilan `:composeApp:compileKotlinJvm` y `:desktopApp:compileKotlinJvm`.

---

## 1) Windows ya no parece macOS

### Causa

`CustomTitleBar` dibujaba siempre los tres círculos de macOS (cerrar, minimizar, maximizar) a la izquierda. En Windows la ventana es undecorated a propósito (`DesktopApp`: `nativeTitleBar` solo en Linux). Por eso Miguel veía la app "como Linux o Mac".

### Arreglo

`desktopTitleBarStyle(os.name)`:

| `os.name` | Barra |
| --- | --- |
| contiene `win` | Botones rectangulares a la derecha: minimizar, maximizar/restaurar, cerrar. Título a la izquierda. Cerrar en rojo `#E81123` al pasar el ratón; minimizar y maximizar solo se aclaran. El glifo se ve siempre. |
| Mac | Círculos de tráfico a la izquierda, como antes. |
| Linux | Sigue la barra nativa. Si la custom se montara, se quedaría el estilo Mac. |

Se mantienen arrastrar la barra y el doble clic para maximizar. Windows no usa la barra nativa de Linux.

---

## 2) Misma sesión en el teléfono y en el PC

Pedido: el reproductor se ve igual en los dos, se puede adelantar, pausar, siguiente y anterior, y el audio suena solo en el aparato elegido. El otro muestra el reproductor remoto y **Escuchar en este dispositivo**.

### Qué fallaba

- La barra remota solo tenía play/pausa y "Aquí". No había seek, anterior, siguiente ni el reproductor.
- Now Playing seguía siendo el player local (otra canción, o silencio).
- El primer latido esperaba 8 s, y cada uno 8 s. Una pausa tardaba eso en llegar al que suena.
- El activo publicaba su posición vieja en el mismo latido que recibía el comando, y el servidor la escribía encima del seek o de la pausa.
- El seguidor podía seguir con MPV o ExoPlayer cargados.

### Modelo (el de 0.7.17, ahora usable)

Un usuario, un aparato con audio. Los demás ven la misma cola (hasta 40), el índice y la posición, y mandan play, pause, seek, next y previous. No hay segundo stream.

| Dato | Valor |
| --- | --- |
| Latido con sesión u otro aparato en línea | 1 s |
| Latido en reposo | 8 s |
| Primer latido | al arrancar el puente, sin espera |
| Cambio local (play, pausa, seek, cambio de pista) | publica enseguida (como mucho cada 400 ms) |
| En línea / sesión | igual que 0.7.17: 45 s / 90 s |

El seguidor vacía el player local una vez (no suena). La UI pinta la sesión. "Escuchar en este dispositivo" (también en la barra y al tocar el otro aparato en Dispositivos) transfiere: el destino carga la cola en ExoPlayer o en MPV y pasa a ser el activo.

Ajustes → Dispositivos lista cada aparato con tipo (Teléfono, PC, Mac, Linux) y "Este dispositivo" en el propio. El PC sale en el celular y al revés, con la misma cuenta y el API desplegado.

El reproductor remoto se abre al tocar la barra (teléfono, tablet y la cápsula de escritorio) y sustituye al Now Playing local mientras se sigue a otro aparato.

### Servidor

`presence` ignora `playback` mientras el activo tiene un control sin `ack_control`. El ack y el playback nuevo, en el mismo latido de después de aplicarlo, sí se guardan.

### Tests

- `:domain:jvmTest` `ConnectSessionTest`: pausa no avanza; play avanza; controles remotos (pausa, play, seek con tope, next, previous a los 3 s); el seguidor no publica; el activo sí; el intervalo de latido.
- `:composeApp:jvmTest` `DesktopTitleBarTest`: Windows 10/11 → caption; Mac y Linux → tráfico.
- `npm test` en `/workspace/killmon-api`: el latido viejo no pisa pausa ni seek; el ack posterior sí guarda la posición nueva.
- User-Agent `Killmon Music/0.7.18`.

---

## Guion de Pruebas QA

Hace falta el API desplegado (0.7.17 más este arreglo de `presence`). Misma cuenta en el teléfono y en el PC.

1. **Windows.** La ventana no lleva círculos de colores a la izquierda. A la derecha: raya, cuadrado, X. La X se pone roja al pasar el ratón. Arrastrar mueve. Doble clic en la barra maximiza. Linux sigue con la barra del sistema.
2. **Lista.** Ajustes → Dispositivos en los dos: salen el teléfono y el PC ("Windows · …" o "Killmon Desktop"), cada uno como "Este dispositivo" en sí mismo. El QR de YouTube sigue debajo.
3. **Un solo audio.** Reproduce en el teléfono. En el PC, en torno a un segundo, la barra muestra la misma canción, la posición y "En {teléfono}". El PC no suena.
4. **Reproductor.** Toca esa barra. Salen carátula, título, artista, cola, slider, anterior, pausa/play y siguiente, y el botón "Escuchar en este dispositivo". Adelantar en el teléfono mueve el slider del PC, y al revés. La pausa se oye en el que tiene el audio.
5. **Traer el audio.** "Escuchar en este dispositivo" (o tocar el otro aparato en Dispositivos). Suena en ese aparato, cerca de la misma posición. El otro deja de sonar y pasa a mostrar el reproductor remoto.
6. **JAM.** Dentro de una sala, Connect no pisa la cola de la sala.
7. **Versión.** 0.7.18 (83).
