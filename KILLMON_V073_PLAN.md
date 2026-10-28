# Killmon Music v0.7.3

`versionName` 0.7.3 y `versionCode` 68.

Referencia del punto 1: `/workspace/simpmusic/miniplayer-ref2.jpg`.

## Método

La tarjeta del mini reproductor es gris plana y opaca. No lleva elevación, sombra ni degradado oscuro encima: el contenido que se desplaza se ve nítido hasta el borde superior. En 0.7.2 la barra pinta una franja de 72 dp, de transparente a negro, como hermana encima de la tarjeta, y desde el borde superior de la tarjeta hacia abajo pinta negro opaco (márgenes laterales, hueco de 12 dp e iconos). Ese tramo negro queda pegado al borde superior y se lee como una sombra, y además tapa lo que en Spotify se ve a los lados de la tarjeta y en el hueco.

En 0.7.3, con la tarjeta visible, no se dibuja esa franja ni un fondo sólido ni un scrim detrás de la tarjeta, en los márgenes laterales ni en el hueco de 12 dp. El contenido pasa por detrás y alrededor de la tarjeta y se sigue viendo en el hueco entre la tarjeta y la barra. El fundido a negro empieza solo debajo de la tarjeta, detrás de la barra inferior. La barra no es un bloque negro: lleva un degradado vertical suave y translúcido, más claro en su borde superior y más oscuro hacia el gesto del sistema, de modo que el contenido se sigue viendo levemente al pasar detrás de los iconos. `Card` sigue a 0 dp, sin `elevation` ni sombra. Iconos, áreas de 48 dp, márgenes y huecos no se tocan.

Para que se vea así, el scroll de las pestañas (Inicio, Buscar, Tu biblioteca) tiene que seguir por debajo de la tarjeta y de la barra. El `Scaffold` no recorta el contenido en el borde superior de la tarjeta ni reserva todo el alto del `bottomBar` como un bloque opaco. La tarjeta gris tapa solo su propio rectángulo; lo que no tapa queda a la vista.

Sin tarjeta, encima de Inicio / Buscar / Tu biblioteca / Crear vuelve la franja de 72 dp, de transparente hacia negro, para que las estanterías se fundan antes de los iconos. Esa franja tampoco termina en un bloque negro sólido: enlaza con el mismo degradado suave de la barra, y el contenido se sigue viendo levemente detrás de los iconos.

En Android Auto el inicio muestra cada módulo de Home (`Listas de reproducción de la comunidad popular`, `Temas relajantes`) como fila pequeña porque el árbol de `MediaLibraryService` no declara estilo y la carátula del módulo va vacía o a 60 px. El host solo pinta mosaico si los extras de la raíz y de cada carpeta piden `CONTENT_STYLE_* = 2` (`GRID_ITEM`) y `CONTENT_STYLE_SUPPORTED`. No hace falta cambiar la forma del árbol: la app del coche ya abre cada módulo y dibuja a sus hijos; si los hijos pasaran a ser la lista de Home, esa cuadrícula se vaciaría. `CONTENT_STYLE_GROUP_TITLE_HINT` agrupa hermanos bajo una cabecera dentro de una misma lista. Aquí cada módulo ya es su propia carpeta, y ponerle el hint duplicaría el título encima del mosaico, así que no se usa.

## Mini reproductor

Igual que en 0.7.2: tarjeta `#3A3A3A` opaca, esquinas de 14 dp, margen lateral de 12 dp, 12 dp entre la tarjeta y la fila de la barra (cada elemento de la barra añade 10 dp por encima del icono). Portada de 52 dp con esquinas de 4 dp, título blanco en negrita y artista `#B3B3B3` con `basicMarquee`, tres controles a 48 dp con 6 dp entre ellos, línea de progreso blanca de 2 dp. Sin degradado, sin sombra y sin elevación en la tarjeta.

Con la tarjeta visible no hay franja ni scrim encima, a los lados ni en el hueco de 12 dp. El contenido se ve nítido hasta el borde superior, alrededor de la tarjeta y en ese hueco. Detrás de la barra, solo el degradado suave translúcido.

Sin tarjeta, la franja de 72 dp (transparente hacia negro, suave, sin corte opaco) sigue encima de la barra, y la barra mantiene el mismo degradado translúcido.

## Android Auto

Raíz (`onGetLibraryRoot`) y cada carpeta (`MediaMetadata.extras`, que Media3 copia al `MediaDescription` que lee el host):

- `android.media.browse.CONTENT_STYLE_BROWSABLE_HINT` = 2
- `android.media.browse.CONTENT_STYLE_PLAYABLE_HINT` = 2
- en la raíz, `android.media.browse.CONTENT_STYLE_SUPPORTED` = true y pista de arte a 512 px

Cada `MediaItem` lleva `artworkUri` grande: `=w60` / `=w120` (y el resto de `=wN-hN`) pasan a `=w544-h544`, `sddefault` a `maxresdefault`, `default.jpg` de `i.ytimg.com` a `hqdefault`. El módulo de Home usa la carátula más grande del primer contenido, no un icono de 60 px ni una uri vacía. Los dibujos locales (`android.resource`) no se reescriben.

## Letra de la canción que suena

A veces la letra sincronizada es de otra canción. En `/workspace/simpmusic/lyrics-bug.jpg` suena «Palabras Sobran» de Blessd (3:36) y la letra es otra («Terciado tengo el Scorpion…», «Se operó en Colombia…»). Pasa cuando el proveedor se queda con el primer resultado de una búsqueda laxa por título, o cuando la petición de la pista anterior termina después del cambio.

Cada proveedor externo tiene que coincidir en título, artista y duración. Si las dos duraciones se conocen y difieren en más de 4 segundos, ese resultado se descarta. No se usa el primer elemento de la lista. LRCLIB busca por `track_name` y `artist_name` y solo entonces, si hace falta, por texto libre, siempre con el mismo filtro. Spotify compara la duración en segundos (el valor del catálogo llega en milisegundos). BetterLyrics solo vale si el TTML trae título, artista y duración que cuadran. Killmon Lyrics sigue yendo por `videoId` y, entre las filas de ese id, se queda con la que cumple título, artista y duración.

Al cambiar de pista se cancela la búsqueda en curso, se borra la letra de la pantalla y se ignora cualquier resultado cuyo id de pista no sea el de la pista actual. Si ningún proveedor da una coincidencia fiable, la tarjeta de letras dice «Letra no disponible». Un fallo de red sí puede enseñar la letra guardada de ese mismo `videoId`. Una búsqueda que respondió con otras canciones no usa esa copia.

## Versión

`gradle/libs.versions.toml`: `version-name` 0.7.3, `version-code` 68. La leen el teléfono y el escritorio.

## Guion de Pruebas QA para Grokbot

1. Instala el APK 0.7.3 (versionCode 68). Reproduce una canción. La tarjeta gris oscura sigue flotando con el mismo margen lateral de 0.7.2, el mismo hueco sobre los iconos y los mismos tres botones (dispositivos, añadir, play) separados. Es gris plana y opaca: el borde superior es una línea gris limpia, sin sombra, sin elevación y sin degradado oscuro pegado encima. El contenido de Inicio se ve nítido hasta ese borde. Referencia: `/workspace/simpmusic/miniplayer-ref2.jpg`.
2. Desplázate por Inicio. Las estanterías se ven detrás y alrededor de la tarjeta (en los huecos laterales) y en el hueco entre la tarjeta y la barra, igual que en Spotify. No hay fondo sólido ni banda ni scrim detrás de la tarjeta ni en ese hueco. La tarjeta sigue siendo gris opaca y tapa solo su rectángulo. Detrás de la barra hay un degradado suave translúcido, no un bloque negro: al pasar, las carátulas se siguen viendo levemente detrás de los iconos y las etiquetas.
3. Cierra el reproductor (desliza la tarjeta hacia abajo). Sin tarjeta, encima de Inicio / Buscar / Tu biblioteca / Crear vuelve el fundido de transparente hacia negro. No termina en un bloque negro sólido: la barra sigue con el degradado suave translúcido y el contenido se ve levemente al pasar detrás. Los iconos, el tamaño y el seleccionado en blanco y negrita no cambian respecto a 0.7.2.
4. Título blanco en negrita, artista gris, marquesina si no caben, play/pausa, añadir que pasa a disco verde con check, toque que abre Ahora suena, deslizamiento lateral de pista y línea blanca de progreso dentro de la tarjeta. Nada de eso cambia.
5. En Android Auto (o DHU), abre Killmon y entra en Inicio / Explorar. Los módulos, entre ellos "Listas de reproducción de la comunidad popular" y "Temas relajantes", salen en cuadrícula de mosaicos grandes, con carátula reconocible, no como filas pequeñas de texto sin imagen. La carátula no es un cuadrito de 60 px borroso.
6. Abre uno de esos módulos. Las listas o los temas de dentro también son mosaicos grandes con carátula, no una lista de iconos diminutos. Abre una lista: las canciones muestran carátula grande. Pulsar una canción la reproduce.
7. En Ajustes del teléfono, la versión es 0.7.3. Con sesión y red no debe salir "Nueva versión … disponible" mientras en GitHub no haya un tag mayor que 0.7.3, o el mismo 0.7.3 con `versionCode` mayor que 68, y un APK `KillmonMusic-*-arm64-release.apk`.
8. Reproduce «Palabras Sobran» de Blessd (3:36), o llega a ella saltando desde otra canción. La letra sincronizada es la de esa canción. No aparece «Terciado tengo el Scorpion», «Se operó en Colombia» ni la letra de otro tema. Referencia del fallo: `/workspace/simpmusic/lyrics-bug.jpg`.
9. Cambia de pista varias veces seguidas. La letra de la pista anterior desaparece en el acto. No se queda en pantalla ni llega después un resultado de otra pista.
10. Con una canción cuya búsqueda no coincide en título, artista y duración (la diferencia de duración pasa de unos 4 segundos), la app prueba el siguiente proveedor. Si ninguno es fiable, la tarjeta de letras muestra «Letra no disponible» y no la primera letra que haya devuelto el buscador.
