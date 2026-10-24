# Killmon Music v0.7.3

`versionName` 0.7.3 y `versionCode` 68.

## Método

La sombra del mini reproductor no es una elevación de la tarjeta (`Card` ya va a 0 dp). En 0.7.2 la barra pinta una franja de 72 dp, de transparente a negro, como hermana **encima** de la tarjeta. El tramo final de esa franja es negro casi opaco y queda pegado al borde superior, así que se lee como una sombra. Spotify deja el borde superior limpio: el contenido llega hasta la tarjeta y el negro opaco empieza en el borde de la tarjeta, no antes.

Por eso el degradado solo se dibuja cuando no hay tarjeta (las estanterías siguen fundiéndose en la barra). Con la tarjeta visible, los 2 dp de aire quedan por encima del negro y el negro opaco cubre los márgenes laterales, el hueco de 12 dp y los iconos. Iconos, áreas de 48 dp y huecos no se tocan.

En Android Auto el inicio muestra cada módulo de Home (`Listas de reproducción de la comunidad popular`, `Temas relajantes`) como fila pequeña porque el árbol de `MediaLibraryService` no declara estilo y la carátula del módulo va vacía o a 60 px. El host solo pinta mosaico si los extras de la raíz y de cada carpeta piden `CONTENT_STYLE_* = 2` (`GRID_ITEM`) y `CONTENT_STYLE_SUPPORTED`. No hace falta cambiar la forma del árbol: la app del coche ya abre cada módulo y dibuja a sus hijos; si los hijos pasaran a ser la lista de Home, esa cuadrícula se vaciaría. `CONTENT_STYLE_GROUP_TITLE_HINT` agrupa hermanos bajo una cabecera dentro de una misma lista. Aquí cada módulo ya es su propia carpeta, y ponerle el hint duplicaría el título encima del mosaico, así que no se usa.

## Mini reproductor

Igual que en 0.7.2: tarjeta `#3A3A3A`, esquinas de 14 dp, margen lateral de 12 dp, 2 dp por encima de la tarjeta y 12 dp hasta los iconos. Portada de 52 dp, título y artista con `basicMarquee`, tres controles a 48 dp con 6 dp entre ellos, línea de progreso de 2 dp. Sin degradado ni sombra en el borde superior. Desde el borde superior de la tarjeta hacia abajo, el fondo es negro opaco.

Sin tarjeta, la franja de 72 dp (transparente a negro) sigue encima de la barra.

## Android Auto

Raíz (`onGetLibraryRoot`) y cada carpeta (`MediaMetadata.extras`, que Media3 copia al `MediaDescription` que lee el host):

- `android.media.browse.CONTENT_STYLE_BROWSABLE_HINT` = 2
- `android.media.browse.CONTENT_STYLE_PLAYABLE_HINT` = 2
- en la raíz, `android.media.browse.CONTENT_STYLE_SUPPORTED` = true y pista de arte a 512 px

Cada `MediaItem` lleva `artworkUri` grande: `=w60` / `=w120` (y el resto de `=wN-hN`) pasan a `=w544-h544`, `sddefault` a `maxresdefault`, `default.jpg` de `i.ytimg.com` a `hqdefault`. El módulo de Home usa la carátula más grande del primer contenido, no un icono de 60 px ni una uri vacía. Los dibujos locales (`android.resource`) no se reescriben.

## Versión

`gradle/libs.versions.toml`: `version-name` 0.7.3, `version-code` 68. La leen el teléfono y el escritorio.

## Guion de Pruebas QA para Grokbot

1. Instala el APK 0.7.3 (versionCode 68). Reproduce una canción. La tarjeta gris oscura sigue flotando con el mismo margen lateral, el mismo hueco sobre los iconos y los mismos tres botones (dispositivos, añadir, play) separados. El borde superior de la tarjeta es una línea gris limpia: no hay franja negra ni degradado oscuro pegado encima. El contenido de Inicio se ve nítido hasta ese borde.
2. Los lados de la tarjeta y el hueco hasta los iconos son negro opaco. Desplázate por Inicio: las estanterías no se leen a través de los iconos ni en los huecos laterales de la tarjeta, y no aparece una sombra negra recorriendo el borde superior.
3. Cierra el reproductor (desliza la tarjeta hacia abajo). Sin tarjeta, encima de Inicio / Buscar / Tu biblioteca / Crear vuelve el fundido de transparente a negro. Los iconos, el tamaño y el seleccionado en blanco y negrita no cambian respecto a 0.7.2.
4. Título blanco en negrita, artista gris, marquesina si no caben, play/pausa, añadir que pasa a disco verde con check, toque que abre Ahora suena, deslizamiento lateral de pista y línea blanca de progreso dentro de la tarjeta. Nada de eso cambia.
5. En Android Auto (o DHU), abre Killmon y entra en Inicio / Explorar. Los módulos, entre ellos "Listas de reproducción de la comunidad popular" y "Temas relajantes", salen en cuadrícula de mosaicos grandes, con carátula reconocible, no como filas pequeñas de texto sin imagen. La carátula no es un cuadrito de 60 px borroso.
6. Abre uno de esos módulos. Las listas o los temas de dentro también son mosaicos grandes con carátula, no una lista de iconos diminutos. Abre una lista: las canciones muestran carátula grande. Pulsar una canción la reproduce.
7. En Ajustes del teléfono, la versión es 0.7.3. Con sesión y red no debe salir "Nueva versión … disponible" mientras en GitHub no haya un tag mayor que 0.7.3, o el mismo 0.7.3 con `versionCode` mayor que 68, y un APK `KillmonMusic-*-arm64-release.apk`.
