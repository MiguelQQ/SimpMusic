# Killmon Music v0.7.2

`versionName` 0.7.2 y `versionCode` 67.

## Mini reproductor

En el teléfono, encima de la barra, flota una tarjeta gris oscuro (`#3A3A3A`), esquinas de 14 dp, con margen lateral de 12 dp. Entre la tarjeta y la fila de la barra hay 12 dp; cada elemento añade 10 dp por encima del icono, así que el hueco que se ve hasta el icono es de unos 22 dp. No toma el color de la portada y no lleva sombra.

La portada es un cuadrado de 52 dp a la izquierda, con esquinas de 4 dp. El título va en blanco y negrita; el artista, debajo, en gris (`#B3B3B3`). Si el título o el artista no caben, se desplazan en horizontal con `basicMarquee`. Si caben, se quedan quietos.

A la derecha, tres controles con área táctil de 48 dp, el icono centrado y 6 dp entre un control y el siguiente: dispositivos (Cast, o el icono de altavoz si Cast no está), añadir (círculo con más) o guardado (disco verde con check blanco), y reproducir o pausa. Abajo, dentro de la tarjeta, una línea de progreso blanca de 2 dp.

Siguen igual el toque para abrir Ahora suena, el deslizamiento horizontal para anterior y siguiente, y el deslizamiento hacia abajo para cerrar. La cápsula de escritorio no cambia.

## Barra inferior

En el teléfono hay cuatro elementos del mismo ancho: Inicio, Buscar, Tu biblioteca y Crear. Los iconos son los de siempre (casa, lupa, biblioteca, más), dibujados a 28 dp, con la etiqueta debajo a 12 sp. El seleccionado es blanco y negrita. El resto es gris (`#B3B3B3`), sin negrita. No hay píldora ni fondo de indicador.

Encima del conjunto hay una franja de 72 dp que pasa de transparente a negro. Desde la tarjeta hacia abajo el fondo es negro opaco: los márgenes de la tarjeta, el hueco de 12 dp, los iconos, las etiquetas y la zona del gesto del sistema. El contenido que se desplaza por detrás se desvanece en esa franja y no se ve a través de la barra.

Crear no cambia de pantalla: abre la hoja de crear que ya existe (lista local o Escuchar juntos). La barra lateral de la tableta y el escritorio no cambian. Si Mix o Analytics están activos, entran en la misma fila y ocupan el mismo ancho que el resto.

## Guion de Pruebas QA

1. Instala el APK 0.7.2 (versionCode 67). Reproduce una canción. Encima de la barra debe verse una tarjeta gris oscura, redondeada, separada de los bordes laterales y con un hueco claro respecto a los iconos. La portada es cuadrada, a la izquierda, con las esquinas un poco redondeadas. El fondo de la barra es negro opaco. Justo encima de la tarjeta, el contenido se desvanece de transparente a negro y no se lee a través de los iconos.
2. El título se lee en blanco y negrita. El artista va debajo, en gris. Elige una canción con título largo y otra con varios artistas: el texto que no cabe se desplaza solo, de lado a lado. Un título corto no se mueve.
3. A la derecha hay tres botones, con aire entre ellos, fáciles de pulsar: dispositivos, añadir y play. Pulsa play y pasa a pausa, y al revés. La música sigue el botón.
4. Pulsa añadir: el icono pasa a un círculo verde con un check blanco. Púlsalo otra vez: vuelve el círculo con el signo más. Cada toque acierta el botón, no abre Ahora suena.
5. Toca el título o la portada: se abre Ahora suena. Desliza la zona del título a la izquierda y suena la siguiente; a la derecha, la anterior. Desliza la tarjeta hacia abajo: el reproductor se cierra. La línea blanca de abajo avanza con la canción y no se sale de la tarjeta.
6. La barra tiene cuatro elementos repartidos por igual en todo el ancho: Inicio, Buscar, Tu biblioteca y Crear. Los iconos son los de esta versión (no los de Spotify), grandes, con la etiqueta debajo. Inicio se ve blanco y en negrita. Los otros tres se ven grises y sin negrita. No hay píldora ni recuadro detrás del icono elegido.
7. Pulsa Buscar: se abre Buscar y ese elemento pasa a blanco y negrita. Pulsa Tu biblioteca: se abre la biblioteca y queda en blanco y negrita. Vuelve a Inicio: Inicio queda seleccionado otra vez.
8. Pulsa Crear. No cambia la pestaña: se abre la hoja Crear, con Lista local y Escuchar juntos. Elige Lista local, pon un nombre y créala: la lista queda en la biblioteca. Cierra la hoja sin elegir: Crear deja de verse seleccionado y sigue marcada la pestaña en la que estabas.
9. Desplázate por Inicio hasta que las tarjetas lleguen abajo. Deben fundirse en el degradado y desaparecer en el negro de la barra, sin verse mezcladas con los iconos ni en los huecos de los lados de la tarjeta.
10. En Ajustes, la versión es 0.7.2. Con sesión y red no debe salir "Nueva versión … disponible" mientras en GitHub no haya un tag mayor que 0.7.2, o el mismo 0.7.2 con `versionCode` mayor que 67, y un APK `KillmonMusic-*-arm64-release.apk`.
