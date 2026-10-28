# Killmon Music 0.7.6 (versionCode 71)

## Causa raíz

La ubicación de contenido ya tenía fallback CO en 0.7.5, pero las instalaciones existentes conservaban su valor guardado. Charts usaba otra preferencia (`chart_key`) con fallback ZZ (mundial), independiente de `location`; por eso «Artistas más escuchados» podía mostrar líderes globales de India aunque la ubicación de contenido fuera Colombia. No se puede deducir de la captura qué preferencias tenía ese dispositivo.

El cliente además arrancaba con la región/idioma del sistema y recibía la configuración mediante observadores asíncronos. Las sugerencias de Google no enviaban gl/hl; visitorData y las cachés de descubrimiento sobrevivían a los cambios de región.

## Plan

1. Subir versión a 0.7.6 / 71.
2. Migrar atómicamente preferencias una sola vez: ubicación vacía/US/IN → CO; idioma vacío → español; sincronizar charts con ubicación y borrar cachés regionales/visitorData. Conservar otras regiones explícitas.
3. Usar una única selección de país para Home y charts; cambiar país de charts también cambia la ubicación de contenido. El selector de charts ofrece países; se excluye ZZ (mundial), que no es un país válido para gl.
4. Resolver ubicación/idioma guardados antes de construir cada petición del cliente. Fallback CO y hl es-419; conservar idioma elegido por el usuario. Añadir gl/hl a sugerencias y bootstrap de visitorData.
5. Invalidar cachés persistidas y estado Home/continuaciones al cambiar región.
6. Añadir tests de migración, persistencia, invalidación y construcción de peticiones donde sea viable.
7. Ejecutar compileReleaseKotlin (sin empaquetar APK/release), tests JVM pedidos y tests de data. Registrar resultados.

## Alcance Git

Sin commit ni push. Cambios locales tanto en el repositorio principal como en el submódulo core (MiguelQQ/core, killmon-0.7.3-auto).

## Validación

Compilación y tests JVM completados correctamente. El contenido final de YouTube puede depender de la cuenta y popularidad; QA debe comprobar los campos enviados, además del contenido visible.

## Archivos modificados

Repositorio principal:
- `gradle/libs.versions.toml`: 0.7.6 / 71.
- `composeApp/src/commonMain/kotlin/com/maxrave/simpmusic/viewModel/HomeViewModel.kt`: selección de charts sincronizada, invalidación del estado regional y cancelación de continuaciones pendientes.
- `composeApp/src/commonMain/kotlin/com/maxrave/simpmusic/ui/screen/home/HomeScreen.kt`: selector de charts por países, sin ZZ; mostrar código seleccionado cuando no hay etiqueta de charts.
- `composeApp/src/commonMain/kotlin/com/maxrave/simpmusic/ui/screen/home/SettingScreen.kt`: fallback CO.
- `KILLMON_V076_PLAN.md`: causa, plan, alcance y validación.

Submódulo core (sí cambió; cambios sin commit):
- `data/src/commonMain/kotlin/com/maxrave/data/dataStore/DataStoreManagerImpl.kt`: lectura migrada, país único, limpieza atómica de caché/visitorData al cambiar país o idioma.
- `data/src/commonMain/kotlin/com/maxrave/data/dataStore/RegionalPreferences.kt`: migración idempotente marcada por `region_migrated_v076`.
- `data/src/commonMain/kotlin/com/maxrave/data/repository/CommonRepositoryImpl.kt`: proveedor de región/idioma/visitorData consultado antes de construir cada petición; evita los observadores que podían aplicar valores antiguos.
- `domain/src/commonMain/kotlin/com/maxrave/domain/repository/HomeRepository.kt`: fallback de país CO.
- `service/kotlinYtmusicScraper/src/commonMain/kotlin/com/maxrave/kotlinytmusicscraper/YouTube.kt`: exposición del proveedor de configuración regional.
- `service/kotlinYtmusicScraper/src/commonMain/kotlin/com/maxrave/kotlinytmusicscraper/Ytmusic.kt`: valores iniciales CO/es-419, aplicación de configuración antes del contexto de petición, sugerencias HTTPS y bootstrap/watch con gl/hl; fábrica de motor inyectable para tests manteniendo la configuración del motor de producción.
- `service/kotlinYtmusicScraper/build.gradle.kts`: Ktor MockEngine para tests.
- `data/src/commonTest/kotlin/com/maxrave/data/dataStore/RegionalPreferencesTest.kt`: seis tests de migración/persistencia/invalidez de caché.
- `service/kotlinYtmusicScraper/src/jvmTest/kotlin/com/maxrave/kotlinytmusicscraper/RegionalRequestsTest.kt`: tres tests de peticiones capturadas con MockEngine.

## Guion de Pruebas QA

1. Instalación limpia: comprobar ubicación CO e idioma español; abrir Home, charts, artistas más escuchados, nuevos lanzamientos y búsqueda.
2. Inspeccionar peticiones: `context.client.gl=CO`, `context.client.hl=es-419`; charts debe incluir `formData.selectedValues=["CO"]`. Sugerencias Google y bootstrap `sw.js_data` deben enviar gl/hl en query.
3. Actualizar desde 0.7.5 con ubicación vacía, US e IN y charts ZZ/IN: confirmar migración a CO, marca persistida y eliminación de visitorData/cachés antiguos. Reiniciar y comprobar persistencia.
4. Después de migrar, elegir US o IN y reiniciar: la migración no debe ejecutarse de nuevo ni sustituir la selección. Probar también actualización con MX guardado: debe conservar MX.
5. Cambiar CO → MX → CO desde Ajustes y desde charts: ambos selectores deben coincidir y las peticiones siguientes deben usar el país nuevo. El idioma español debe seguir enviando es-419.
6. Cambiar de país con Home paginando: no deben reaparecer páginas, imágenes ni artistas del estado anterior. Volver a abrir Home y comprobar recarga de cachés.
7. Repetir con y sin cuenta Google. Comparar charts con Colombia seleccionada en YouTube Music; que aparezca algún artista extranjero por popularidad no implica por sí solo un fallo regional.

QA en dispositivo pendiente: esta tarea valida compilación Kotlin y tests JVM, sin generar APK ni ejecutar instalación en teléfono.

## Resultados finales

- Compilación pedida: `JAVA_HOME=~/opt/jdk-21.0.13+9 ANDROID_HOME=~/android-sdk ./gradlew :androidApp:compileReleaseKotlin -PisFullBuild=false --no-daemon` → **BUILD SUCCESSFUL**, 1m 9s. Log: `/tmp/killmon-v076-build.log`.
- Tests: `JAVA_HOME=~/opt/jdk-21.0.13+9 ANDROID_HOME=~/android-sdk ./gradlew :composeApp:jvmTest :lyricsService:jvmTest :kotlinYtmusicScraper:jvmTest :domain:jvmTest :data:jvmTest -PisFullBuild=false --no-daemon` → **BUILD SUCCESSFUL**, 59s. Log: `/tmp/killmon-v076-tests.log`.
- `:composeApp:jvmTest`: 44 tests, 0 fallos, 0 errores, 0 omitidos.
- `:lyricsService:jvmTest`: 5 tests, 0 fallos, 0 errores, 0 omitidos.
- `:kotlinYtmusicScraper:jvmTest`: 16 tests, 0 fallos, 0 errores, 0 omitidos.
- `:domain:jvmTest`: 20 tests, 0 fallos, 0 errores, 0 omitidos.
- `:data:jvmTest`: 6 tests, 0 fallos, 0 errores, 0 omitidos.
- Total: **91 tests**; nueve nuevos (seis de preferencias y tres HTTP).
- `git diff --check` en principal y core: correcto.
- Gradle/Kotlin muestran avisos de deprecaciones, opciones redundantes y safe calls existentes; no errores.
- Sin commit, push, empaquetado release ni APK generado. No se ejecutó QA en dispositivo.
