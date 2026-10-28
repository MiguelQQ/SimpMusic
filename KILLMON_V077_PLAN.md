# Killmon Music 0.7.7 — versionCode 72

## Objetivo y alcance

Corregir el ANR de arranque de 0.7.6 conservando la migración única a CO, español/es-419, la sincronización Home/charts y la invalidación regional de cachés/visitorData.

Rama principal y submódulo core: `killmon-0.7.3-auto`. Se revisaron `git diff 10e9360..HEAD` y `git -C core diff b3e98b7..c52c591` (b3e98b7 es el padre de c52c591). Cambios locales en ambos repositorios; sin commit, push, empaquetado release ni APK.

## Causa raíz y evidencia

0.7.6 añadió un flow frío `regionalData` en `DataStoreManagerImpl`: cada suscripción comenzaba con `settingsDataStore.edit { migrateRegionalPreferences(it) }`, incluso cuando la marca `region_migrated_v076` ya estaba guardada. Afectaba `location`, `chartKey`, `language`, `getString`, `visitorData` y las cachés regionales. El proveedor de configuración de cada petición también hacía tres lecturas de este tipo.

DataStore serializa actualizaciones y ejecuta el transform de `updateData`/`edit` en el contexto de quien lo solicita. Un lector asíncrono desde Main podía encolar una actualización cuyo transform necesitaba volver a Main. Si el arranque ocupaba Main con una lectura `runBlocking { …first() }`, esa segunda lectura encolaba otro edit detrás del primero: Main esperaba a DataStore y DataStore esperaba a Main. El proyecto tiene esos lectores síncronos tanto en el arranque de SharedViewModel como en la inicialización del reproductor/Koin. Home también tenía dos lecturas `runBlocking` de país/idioma cuyos resultados no se utilizaban.

La marca evitaba repetir el cuerpo de migración, pero **no evitaba encolar actualizaciones**. No hizo falta un bucle infinito de migración ni un Mutex reentrante para reproducir el bloqueo.

Se añadieron dos tests JVM sobre DataStore real y se ejecutaron contra la implementación original de 0.7.6 antes de corregirla:

- `repeatedRegionalReadsNeverEnqueueWrites`: falló; 70 lecturas → 70 llamadas a updateData (esperadas: cero).
- `suspendedMainCollectorCannotDeadlockAnotherStartupReader`: falló por timeout al dejar sin ejecutar el dispatcher de Main de un primer lector y realizar una segunda lectura. Reproduce la dependencia circular sin colgar el proceso de tests.

Evidencia previa preservada en `/tmp/killmon-v077-regression-before.log` y `/tmp/killmon-v077-regression-before.xml`. La captura del dispositivo acredita el ANR, pero no incluye una traza del teléfono: la atribución se basa en la revisión del código y la reproducción JVM, no en una traza del Samsung.

## Implementación

1. Subir `gradle/libs.versions.toml` a 0.7.7 / 72.
2. Convertir la migración regional en `DataMigration<Preferences>` nativa, registrada en las fábricas comunes (Android/iOS) y JVM. DataStore la ejecuta en su scope de IO antes de publicar el primer snapshot. No acceder al mismo DataStore dentro de la migración.
3. Conservar la clave `region_migrated_v076`: instalaciones que ya migraron no vuelven a migrar ni pierden sus selecciones posteriores. Conservar las reglas de CO/español y la limpieza inicial de cachés de 0.7.6.
4. Hacer que todos los lectores consulten exclusivamente `settingsDataStore.data`; retirar la migración de los setters, puesto que DataStore ya garantiza la inicialización previa a lecturas y actualizaciones. Los cambios explícitos de región/idioma mantienen la limpieza atómica en IO.
5. Retirar de Home las lecturas bloqueantes y los campos que no se usaban; observar país/idioma juntos con `distinctUntilChanged`, invalidando continuaciones y contenido cuando cambien. Actualizar el selector de charts al recibir el país y dejar su recarga al observador para evitar la llamada duplicada de `exploreChart`.
6. Mantener el proveedor suspendido de configuración de YouTube y la construcción de peticiones CO/es-419 de 0.7.6. No introducir `runBlocking`, Mutex ni escrituras en sus lecturas.
7. Verificar con DataStore real la ausencia de escrituras al leer, el bloqueo de Main, migración al primer acceso con 32 lectores concurrentes, instalación limpia y persistencia tras reinicio. Mantener los tests de migración/invalidez y los tests HTTP regionales existentes.
8. Ejecutar la compilación Kotlin y los cinco conjuntos de tests pedidos; documentar resultados.

## Archivos cambiados

Repositorio principal:

- `gradle/libs.versions.toml`.
- `composeApp/src/commonMain/kotlin/com/maxrave/simpmusic/viewModel/HomeViewModel.kt`.
- `KILLMON_V077_PLAN.md`.

Submódulo **core sí cambió**:

- `data/src/commonMain/kotlin/com/maxrave/data/dataStore/DataStoreManagerImpl.kt`.
- `data/src/commonMain/kotlin/com/maxrave/data/dataStore/RegionalPreferences.kt`.
- `data/src/commonMain/kotlin/createDataStore.kt`.
- `data/src/jvmMain/kotlin/com/maxrave/data/dataStore/DataStoreManagerImpl.jvm.kt`.
- `data/src/commonTest/kotlin/com/maxrave/data/dataStore/RegionalPreferencesTest.kt`.
- `data/src/jvmTest/kotlin/com/maxrave/data/dataStore/RegionalDataStoreTest.kt` (nuevo).

## Validación

- Regresión previa: dos tests fallaron con la implementación de 0.7.6, como se describe arriba.
- `:data:jvmTest` después de corregir: **BUILD SUCCESSFUL**, 33 s; 11 tests, cero fallos. Log: `/tmp/killmon-v077-data-tests.log`.
- Compilación solicitada: `JAVA_HOME=~/opt/jdk-21.0.13+9 ANDROID_HOME=~/android-sdk ./gradlew :androidApp:compileReleaseKotlin -PisFullBuild=false --no-daemon` → **BUILD SUCCESSFUL**, 1 min 12 s. Log: `/tmp/killmon-v077-build.log`. Solo compilación Kotlin y sus prerrequisitos, sin empaquetar release.
- Suite solicitada: `JAVA_HOME=~/opt/jdk-21.0.13+9 ANDROID_HOME=~/android-sdk ./gradlew :composeApp:jvmTest :lyricsService:jvmTest :kotlinYtmusicScraper:jvmTest :domain:jvmTest :data:jvmTest -PisFullBuild=false --no-daemon` → **BUILD SUCCESSFUL**, 58 s. Log: `/tmp/killmon-v077-tests.log`.
- Informes JVM: `composeApp` 44, `lyricsService` 5, `kotlinYtmusicScraper` 16, `domain` 20 y `data` 11: **96 tests, cero fallos, cero errores y cero omitidos**. Cinco tests nuevos en data. Gradle reutilizó los resultados vigentes de los módulos sin cambios y de data, que ya se había ejecutado después de la corrección; composeApp se recompiló y ejecutó en la suite completa.
- `git diff --check` en principal y core: correcto. Avisos existentes de Gradle/AGP/Kotlin; ningún error de compilación.
- QA en Samsung SM-S948B / Android 17 pendiente: no hay dispositivo conectado para validar el arranque en ese equipo.

## Guion de Pruebas QA

1. En Samsung SM-S948B / Android 17, actualizar de 0.7.6 a 0.7.7 sin borrar datos. Forzar detención y abrir al menos diez veces; el splash debe dejar paso a Home, sin ANR, y la interfaz debe responder aunque no haya red.
2. Repetir arranque en frío sin red, con red lenta y con/sin cuenta Google. Registrar tiempo hasta interfaz y capturar logcat/traza ANR si reaparece el síntoma.
3. Instalación limpia: verificar CO, UI española y las primeras peticiones de Home, charts, novedades, sugerencias y bootstrap de visitorData con `gl=CO` / `hl=es-419` (contexto o query según endpoint).
4. Actualizar desde 0.7.5 con ubicación ausente/US/IN y charts ZZ/IN: debe migrar una vez a CO, sincronizar charts y borrar visitorData/cachés regionales. Reiniciar: no debe repetir la limpieza.
5. Actualizar desde 0.7.6 ya migrada con una selección posterior US, IN o MX e idioma explícito: conservar ambas selecciones y las cachés válidas. Probar además una región explícita MX sin marca de migración: conservar MX.
6. Cambiar CO → MX → CO desde Ajustes y charts, también mientras Home pagina: los selectores deben coincidir, las peticiones siguientes deben usar el país nuevo y las continuaciones del país anterior deben descartarse.
7. Cambiar idioma: invalidar descubrimiento una vez, sin recargas interminables. Con español, comprobar `hl=es-419`; con otro idioma explícito, conservar su selección.
8. Navegar, buscar, iniciar/pausar reproducción, volver desde segundo plano y reiniciar. Confirmar que guardar cachés/visitorData no genera un ciclo de recargas de Home.
