# Killmon Music 0.7.5 (70)

## Plan aplicado

1. Inspeccionar `/tmp/spotify_ref.jpg` y revisar la composición del dock de 0.7.4.
2. Unificar el fondo del mini player, sus márgenes, el hueco y la navegación en un solo degradado vertical continuo.
3. Ajustar la tarjeta flotante a gris #2a2a2a casi opaco, conservando los iconos y la geometría actuales.
4. Actualizar la versión central a 0.7.5 / 70, compilar Kotlin Android y ejecutar los dos módulos de tests solicitados. Sin commit, push ni empaquetado release.

## Decisiones visuales

El fondo al 92 % desde el borde superior de 0.7.4 se sustituye por una única llamada a `smoothScrimBrush` en la columna completa del dock. Empieza con negro de alfa cero y llega a alfa 0.93 al 72 % de su altura; desde ahí conserva esa opacidad bajo etiquetas y área de navegación del sistema. El helper existente genera 25 stops con interpolación smoothstep, con pendiente cero en los extremos. No se añaden fondos separados ni divisores en el mini player o en la fila de navegación.

La tarjeta Android usa #2a2a2a al 97 % de opacidad, esquinas de 14 dp, márgenes laterales de 12 dp y altura de 72 dp. Su fondo se mantiene independiente del degradado para que se perciba como una tarjeta flotante. Se conservan controles, iconos, etiquetas y navegación actuales, sin introducir branding visible SimpMusic. La barra plegable de escritorio conserva su propio tratamiento.

Sin pista se mantiene el espacio de transición existente de 72 dp, ahora dentro del mismo degradado continuo. La igualdad visual con la referencia debe verificarse en dispositivo, con contenido desplazándose por debajo.

## Archivos cambiados

- `composeApp/src/commonMain/kotlin/com/maxrave/simpmusic/App.kt`: un único scrim para todo el dock, transparente arriba y negro al 93 % abajo.
- `composeApp/src/commonMain/kotlin/com/maxrave/simpmusic/ui/screen/MiniPlayer.kt`: tarjeta gris #2a2a2a al 97 %.
- `gradle/libs.versions.toml`: versión 0.7.5 y versionCode 70.
- `KILLMON_V075_PLAN.md`: plan, decisiones, validación y QA.

## Validación

Comandos completados con `JAVA_HOME=~/opt/jdk-21.0.13+9` y `ANDROID_HOME=~/android-sdk`:

```sh
./gradlew :androidApp:compileReleaseKotlin -PisFullBuild=false --no-daemon
./gradlew :composeApp:jvmTest :lyricsService:jvmTest -PisFullBuild=false --no-daemon
```

Resultados:

- `:androidApp:compileReleaseKotlin`: BUILD SUCCESSFUL en 56 s.
- `:composeApp:jvmTest` y `:lyricsService:jvmTest`: BUILD SUCCESSFUL en 47 s. Informes XML: 44 y 5 casos respectivamente; 49 aprobados, 0 fallos, 0 errores y 0 omitidos.
- `git diff --check`: correcto.
- Avisos de Gradle/Kotlin sobre APIs obsoletas y configuración de targets, sin errores. Las pruebas iOS no se ejecutan en Linux; no forman parte de las suites solicitadas.
- No se ensambló APK/AAB release ni se hizo commit o push.

Logs: `/tmp/killmon-075-compile.log` y `/tmp/killmon-075-tests.log`.

## Guion de Pruebas QA

1. Abrir Killmon Music y verificar versión 0.7.5 (70) en información de aplicación. Revisar Inicio, Buscar, Tu biblioteca y Crear: conservar los iconos actuales y no mostrar branding SimpMusic.
2. Reproducir una pista y desplazar carátulas claras, oscuras y texto por detrás del dock. Comparar con `/tmp/spotify_ref.jpg`: el fondo debe ser transparente a la altura superior de la tarjeta y oscurecerse suavemente hacia los iconos, sin borde horizontal, banda ni cambio de fondo entre reproductor y navegación.
3. Verificar que la tarjeta flota con márgenes laterales, esquinas redondeadas y gris oscuro casi opaco. El título, artista, carátula y progreso deben ser legibles; el fondo no debe teñirse con la carátula.
4. Comprobar que el contenido apenas se intuye bajo iconos y etiquetas, con negro aproximadamente al 85–95 % en esa zona. Repetir con tema claro y oscuro, navegación por gestos y navegación de tres botones.
5. Probar play/pausa, guardar, controles de dispositivo y apertura del reproductor completo; volver al dock. Cambiar de pestaña y comprobar el estado seleccionado y la hoja Crear.
6. Cerrar/detener la pista y restaurar la reproducción. Revisar la transición con y sin mini player, sin fondos rectangulares ni bandas en los márgenes. Repetir tras cerrar y abrir la app.
7. Revisar pantalla estrecha, texto ampliado y orientación horizontal/tablet: controles accesibles y sin superposición de etiquetas. Verificar que el reproductor de tablet y la barra plegable de escritorio conservan su funcionamiento.

QA visual y pruebas en dispositivo pendientes; la compilación y los tests unitarios no sustituyen esa comprobación.
