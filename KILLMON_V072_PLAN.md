# Killmon Music v0.7.2

`versionName` 0.7.2 y `versionCode` 67.

## Mini reproductor

En el teléfono, encima de la barra, flota una tarjeta gris oscuro (`#3A3A3A`), esquinas de 14 dp, con margen lateral de 12 dp y 8 dp de separación respecto a la barra. No toma el color de la portada y no lleva sombra.

La portada es un cuadrado de 52 dp a la izquierda, con esquinas de 4 dp. El título va en blanco y negrita; el artista, debajo, en gris (`#B3B3B3`). Si el título o el artista no caben, se desplazan en horizontal con `basicMarquee`. Si caben, se quedan quietos.

A la derecha, tres controles con área táctil de 48 dp y el icono centrado, sin ir pegados: dispositivos (Cast, o el icono de altavoz si Cast no está), añadir (círculo con más) o guardado (disco verde con check blanco), y reproducir o pausa. Abajo, dentro de la tarjeta, una línea de progreso blanca de 2 dp.

Siguen igual el toque para abrir Ahora suena, el deslizamiento horizontal para anterior y siguiente, y el deslizamiento hacia abajo para cerrar. La cápsula de escritorio no cambia.

## Guion de Pruebas QA

1. Instala el APK 0.7.2 (versionCode 67). Reproduce una canción. Encima de la barra debe verse una tarjeta gris oscura, redondeada, separada de los bordes y de la barra. La portada es cuadrada, a la izquierda, con las esquinas un poco redondeadas.
2. El título se lee en blanco y negrita. El artista va debajo, en gris. Elige una canción con título largo y otra con varios artistas: el texto que no cabe se desplaza solo, de lado a lado. Un título corto no se mueve.
3. A la derecha hay tres botones, con aire entre ellos, fáciles de pulsar: dispositivos, añadir y play. Pulsa play y pasa a pausa, y al revés. La música sigue el botón.
4. Pulsa añadir: el icono pasa a un círculo verde con un check blanco. Púlsalo otra vez: vuelve el círculo con el signo más. Cada toque acierta el botón, no abre Ahora suena.
5. Toca el título o la portada: se abre Ahora suena. Desliza la zona del título a la izquierda y suena la siguiente; a la derecha, la anterior. Desliza la tarjeta hacia abajo: el reproductor se cierra. La línea blanca de abajo avanza con la canción y no se sale de la tarjeta.
6. En Ajustes, la versión es 0.7.2. Con sesión y red no debe salir "Nueva versión … disponible" mientras en GitHub no haya un tag mayor que 0.7.2, o el mismo 0.7.2 con `versionCode` mayor que 67, y un APK `KillmonMusic-*-arm64-release.apk`.
