# Killmon Music 0.7.14 (79) — Aviso de actualización

## Alcance

Base: 0.7.13 ya commiteado en `killmon-0.7.3-auto`, app y submódulo `core`. Entrega sin commit ni push. `gradle/libs.versions.toml` pasa a 0.7.14 / 79. En core se actualiza el User-Agent de la sesión Killmon y sus pruebas a `Killmon Music/0.7.14`.

## Updater y ciclo de vida

El updater Killmon existente ya usaba el repositorio correcto, pero el flag local `checked` limitaba la consulta a una vez y su montaje dependía de la cuenta iniciada. Ahora está activo por defecto desde la raíz de la app, incluida la pantalla de acceso, sin depender del ajuste heredado de SimpMusic.

- `ProcessLifecycleOwner` observa `ON_RESUME`; si la composición llega con el proceso ya en RESUMED, solicita también la comprobación inicial.
- Estado y coroutine de consulta viven durante el proceso: las recomposiciones y recreaciones de Activity no duplican solicitudes. No se persiste el debounce: cada arranque en frío vuelve a consultar.
- Reloj monotónico `SystemClock.elapsedRealtime()` y debounce de 30 minutos desde el intento anterior, también ante errores. Se reintenta en el siguiente retorno a primer plano que cumpla ese plazo.
- Un aviso pendiente se conserva sin reemplazarlo durante la descarga. “Más tarde” cancela la descarga y descarta el par tag/versionCode durante ese proceso. Otra versión o un código nuevo pueden ofrecerse después del debounce. Un proceso nuevo vuelve a ofrecer la versión descartada si sigue siendo superior.
- Los caminos antiguos GitHub/F-Droid del SharedViewModel y scraper siguen inactivos; no hay una segunda consulta al repositorio original ni un diálogo heredado activo.

## Origen, selección y acción

`GET https://api.github.com/repos/MiguelQQ/SimpMusic/releases?per_page=100`, nunca `/latest`. Se incluyen los prereleases publicados y se excluyen drafts. Se elige la versión numérica mayor, independientemente del orden de la respuesta; `v0.7.14-killmon` equivale a 0.7.14. Se mantiene la precedencia semver para otros sufijos prerelease. Un `versionCode NN` (también con `:` o `=`) en las notas desempata versiones semver iguales; una versión semver inferior no provoca downgrade por tener código mayor. Versiones con números fuera de rango y respuestas inválidas se ignoran sin excepción visible.

El diálogo usa branding Killmon Music y el botón “Actualizar”. Prefiere el asset `KillmonMusic-<ver>-arm64-release.apk`, conserva progreso, permiso de instalación y FileProvider. Si falta ese asset o falla la descarga, abre la página del release de MiguelQQ/SimpMusic. Respuestas HTTP fallidas, timeout, falta de red y rate limit son silenciosos. La consulta cubre los cien releases devueltos por la primera página.

## Validación

Comandos ejecutados con JDK 21 y Android SDK locales:

```sh
JAVA_HOME=$HOME/opt/jdk-21.0.13+9 ANDROID_HOME=$HOME/android-sdk ./gradlew :androidApp:compileReleaseKotlin :composeApp:jvmTest -PisFullBuild=false --no-daemon
JAVA_HOME=$HOME/opt/jdk-21.0.13+9 ANDROID_HOME=$HOME/android-sdk ./gradlew :listenTogether:jvmTest -PisFullBuild=false --no-daemon
```

Ambos terminaron con **BUILD SUCCESSFUL**. ComposeApp: **61 tests**, incluidos 10 de selección/parseo de releases y 2 de política de sesión. ListenTogether: **17 tests**, incluidos los handshakes HTTP/WebSocket con el User-Agent 0.7.14. Cero fallos, errores u omitidos. `git diff --check` limpio en app y core.

No se han probado instalación ni transiciones de ciclo de vida en un teléfono físico; la comprobación del lifecycle real y la descarga/instalación quedan en el guion QA.

## Guion de Pruebas QA

1. Con un fixture/proxy de releases que publique `v0.7.15-killmon`, prerelease y versionCode 80, abrir 0.7.14/79 tras forzar detención. Debe consultar `/releases` y mostrar “Killmon Music”, versión y “Actualizar”. Repetir sin cuenta iniciada y con el ajuste heredado desactivado. Para probar el release real 0.7.14/79, usar un cliente anterior que incluya este updater o un build QA con versión instalada inferior.
2. Responder inicialmente sin versión nueva; enviar a segundo plano y volver antes de 30 minutos: no debe duplicar consulta. Publicar versión nueva en el fixture, esperar 30 minutos desde el intento anterior y volver: debe consultar y mostrar el aviso. Repetir con reproducción activa en segundo plano, sin matar el proceso.
3. Pulsar “Más tarde”, rotar/recrear Activity y hacer varios retornos: no debe reaparecer esa misma versión durante el proceso. Cambiar fixture a otra versión/código superior y volver después del debounce: debe ofrecerla. Forzar detención y abrir de inmediato: debe consultar y ofrecer de nuevo la versión descartada.
4. Probar lista desordenada con estable, prerelease y draft; verificar que gana la versión pública mayor. Probar 0.7.9 frente a 0.7.10, instalado igual/superior, sufijo -killmon y versión igual con versionCode mayor. No ofrecer versiones iguales o inferiores sin incremento aplicable.
5. Pulsar “Actualizar” con el asset arm64: comprobar progreso, permiso de fuentes desconocidas, retorno desde ajustes e instalador. Cancelar descarga, luego ofrecer otra versión y comprobar que no reutiliza el APK anterior. Repetir sin asset arm64 y con descarga fallida: debe abrir la página correcta del release.
6. Simular sin conexión, timeout, HTTP 403/429, rate limit y JSON inválido. La app debe seguir usable sin aviso de error; al recuperar red y volver tras el debounce debe reintentar. Un arranque en frío debe reintentar sin esperar el debounce anterior.
