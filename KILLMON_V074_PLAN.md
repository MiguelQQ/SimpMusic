# Killmon Music 0.7.4 (69)

## Plan aplicado

1. Corregir el orden de construcción de SharedViewModel y revisar los observadores que pueden ejecutarse con Main.immediate/Unconfined.
2. Oscurecer el dock inferior al 92–95 % negro, incluidos los márgenes y el hueco del mini player, conservando su tarjeta opaca y el gradiente sutil de navegación.
3. Reforzar identidad y cancelación de letras, borrar estado anterior antes de publicar otra pista y rechazar caché antigua sin metadatos verificables.
4. Actualizar la versión central, compilar Kotlin Android y ejecutar las suites unitarias disponibles en Linux. Sin commit, push ni empaquetado release.

## Causa del crash

La traza en /tmp/retraced.txt sitúa resetLyricsVoteState dentro de una coroutine iniciada por el constructor. El bloque init aparecía antes de _lyricsVoteState y _translatedVoteState. La primera emisión puede ejecutarse sin suspensión y acceder al backing field JVM todavía nulo.

El bloque init ahora es el último elemento de SharedViewModel, después de TODOS los inicializadores de propiedades. Esto también protege los manejadores de formato, información de canción, descargas y smart downloads. No se depende de un delay, yield ni de escoger otro dispatcher. El Job de letras se crea LAZY, se asigna y después se inicia, para que los observadores inmediatos vean el manejador correcto.

## Letras

- Cancelar el Job anterior y borrar letras, traducciones y votos antes de publicar la nueva pista; también limpiar cuando desaparece la pista.
- Comparar el videoId tanto con el estado del ViewModel como con el del reproductor. Los observadores de duración descartan combinaciones de pista anterior con duración nueva.
- Verificar cancelación antes de aplicar letras, captions y traducciones. La reparación de traducción desincronizada queda vinculada al Job que la solicitó.
- Los proveedores de catálogo conservan la validación conjunta de título, artista y duración (tolerancia de cuatro segundos). Se eliminó la coincidencia por subcadena libre del título y por una sola palabra compartida del artista.
- Al terminar sin resultado válido, usar el estado existente «Letra no disponible».
- Las filas antiguas de la tabla lyrics solo contienen videoId/texto, sin título, artista ni duración. No se reutilizan como prueba de identidad. El fallback sin conexión acepta únicamente letras verificadas durante esta instancia del ViewModel. Tras reiniciar sin red, estas filas antiguas no se muestran; se requiere red para revalidar. No se borra la base de datos ni se introduce una migración.
- Se conservan identificadores internos de proveedores y paquetes; los recursos visibles ya usan Killmon Music y «Letra no disponible».

## Archivos

- gradle/libs.versions.toml: 0.7.4 / 69, consumidos por los módulos de aplicación.
- composeApp/src/commonMain/kotlin/com/maxrave/simpmusic/viewModel/SharedViewModel.kt: construcción, limpieza, Jobs, cancelación y caché.
- composeApp/src/commonMain/kotlin/com/maxrave/simpmusic/App.kt: opacidad y fondo bajo márgenes del mini player.
- core/service/lyricsService/src/commonMain/kotlin/org/simpmusic/lyrics/LyricsMatch.kt: comparación de identidad.
- core/service/lyricsService/src/commonTest/kotlin/org/simpmusic/lyrics/LyricsMatchTest.kt: regresiones de título y artista.
- KILLMON_V074_PLAN.md: decisiones, validación y QA.

## Validación

Compilación solicitada completada con éxito usando JAVA_HOME=~/opt/jdk-21.0.13+9 y ANDROID_HOME=~/android-sdk:

```sh
./gradlew :androidApp:compileReleaseKotlin -PisFullBuild=false --no-daemon
```

La última verificación de las fuentes finales también terminó con BUILD SUCCESSFUL (53 s). No se ensambló APK/AAB release, no se hizo commit ni push.

Suites ejecutadas: :composeApp:jvmTest, :domain:jvmTest, :lyricsService:jvmTest, :aiService:jvmTest, :kotlinYtmusicScraper:jvmTest, :loginSync:jvmTest, :listenTogether:jvmTest, :media3:testDebugUnitTest, :media3-ui:testDebugUnitTest, :crashlytics-empty:testDebugUnitTest y :crashlytics:testDebugUnitTest. Resultado XML: 121 casos, 119 aprobados, 2 omitidos por @Ignore, 0 fallos y 0 errores. LyricsMatchTest: 5/5 aprobados. :common:jvmTest terminó NO-SOURCE; sus tests Android host no están habilitados en la configuración existente. Los tests iOS requieren macOS; no se ejecutaron pruebas instrumentadas ni QA en dispositivo.

Advertencias existentes de Gradle/AGP/Kotlin y Room, sin errores de compilación. git diff --check pasó en el repositorio y en core.

Logs completos: /tmp/killmon-074-compile.log, /tmp/killmon-074-validation.log y /tmp/killmon-074-final.log.

## Guion de Pruebas QA

1. Actualizar desde 0.7.3 sin borrar datos. En Samsung SM-S948B / Android 17 SDK 37, abrir en frío con cola guardada, cerrar el proceso y repetir diez veces. No debe aparecer el NPE de StateFlow ni un crash de inicio.
2. Repetir con cola vacía, sin conexión y con reproducción restaurada en pausa. Comprobar que la navegación funciona y el mini player aparece solo si hay pista.
3. Desplazar carátulas claras y texto por detrás del dock. Barra y márgenes del mini player deben verse casi negros, con contenido apenas perceptible; controles y título permanecen legibles. Repetir con mini player cerrado y con tema claro/oscuro.
4. Reproducir «Palabras Sobran» de Blessd, abrir letras y comprobar que corresponde a la grabación. Probar títulos homónimos de otros artistas, remix/parte 2 y versiones de duración diferente: mostrar una coincidencia validada o «Letra no disponible».
5. Con red lenta, alternar A → B → C y A → B → A antes de recibir respuestas. La letra y los votos anteriores desaparecen inmediatamente; ninguna respuesta tardía debe reemplazar la pista actual. Repetir en letras a pantalla completa.
6. Repetir el cambio rápido con cada proveedor, captions y traducción automática activada. Cambiar proveedor con una petición pendiente y detener la reproducción durante la descarga de letras.
7. Con una letra guardada incorrecta de 0.7.3, iniciar sin red: no debe reaparecer. Recuperar red y cargar una coincidencia válida; cortar red y volver a esa pista durante la misma sesión para comprobar el fallback verificado.
8. Buscar una pista sin coincidencia: comprobar «Letra no disponible» y ausencia de branding visible SimpMusic. Verificar 0.7.4 (69) en Ajustes/información de aplicación.

Las comprobaciones visuales y del dispositivo requieren ejecutar la aplicación; la compilación y las pruebas unitarias no las sustituyen.
