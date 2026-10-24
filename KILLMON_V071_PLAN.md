# Killmon Music v0.7.1

Misma versión: `versionName` 0.7.1 y `versionCode` 66.

## Botones de cuenta
"Iniciar sesión", "Crear cuenta", "Cerrar sesión" y "Eliminar" quedan en verde sólido con texto negro. Siguen verdes con el formulario vacío y mientras cargan (el spinner también es negro). "Crear cuenta" y "Ya tengo cuenta" en el formulario son texto cian. "Eliminar cuenta" en la pantalla de la cuenta es texto rojo.

## Selecciones rápidas
En Inicio, las tarjetas de dos columnas son gris oscuro (`#2A2A2A`), con esquinas redondeadas. El fondo y la miniatura usan `clip` con la misma forma. El verde del gesto "reproducir a continuación" no se dibuja en reposo, así que no asoma por las esquinas derechas. Al deslizar a la derecha el verde sigue apareciendo, recortado a la tarjeta.

## Actualizador
Tras el login, una sola vez por arranque, se consulta `https://api.github.com/repos/MiguelQQ/SimpMusic/releases` (incluye prereleases, hasta 100). Se elige la más nueva por semver del tag o del nombre. El sufijo `-killmon` es una etiqueta: `v0.7-killmon` vale 0.7.0 y `v0.7.1-killmon` vale 0.7.1. Si el cuerpo trae `versionCode: N`, desempatan dos tags iguales y, con el mismo semver, un código mayor que el instalado cuenta como versión nueva. Hace falta un asset `KillmonMusic-*-arm64-release.apk`. Los borradores no cuentan.

Si hay una más nueva, el diálogo dice "Nueva versión X disponible". "Actualizar" es verde con texto negro y descarga el APK con una barra de progreso. Al terminar abre el instalador de Android con FileProvider. Si no está permitido instalar apps desconocidas, abre Ajustes de Killmon y, al volver, sigue la descarga o el instalador. "Más tarde" cierra el aviso hasta el próximo arranque. Sin red o con rate-limit no hay diálogo ni cierre. El comprobador de GitHub y F-Droid de upstream sigue apagado. En escritorio no hay instalador.

## Guion de Pruebas QA

1. Instala el APK 0.7.1 (versionCode 66). En iniciar sesión, "Iniciar sesión" es verde con texto negro aunque el correo y la contraseña estén vacíos. Al entrar, el botón sigue verde y el spinner es negro. "Crear cuenta" debajo es texto cian, no un botón verde.
2. En crear cuenta, "Crear cuenta" es verde con texto negro, también vacío y mientras carga. "Ya tengo cuenta" es texto cian.
3. Entra y abre Cuenta Killmon. "Cerrar sesión" es verde con texto negro. En el diálogo de confirmación, "Cerrar sesión" sigue verde con texto negro y "Cancelar" es blanco. "Eliminar cuenta" es texto rojo. En su diálogo, "Eliminar" es verde con texto negro.
4. En Inicio, mira "Selecciones rápidas" en dos columnas. Cada tarjeta es gris oscuro, lisa, con esquinas redondeadas. En las esquinas derechas no hay verde, ni un borde ni un trozo de acento. Desliza una tarjeta a la derecha: detrás sale el verde de reproducir a continuación, recortado a la esquina, y la tarjeta vuelve a quedar gris.
5. Con sesión iniciada y red, espera unos segundos en Inicio. No debe salir "Nueva versión … disponible". Hoy en GitHub la más nueva con APK arm64 es `v0.7-killmon`, que es anterior a 0.7.1. Repite en modo avión: la app abre igual y tampoco hay diálogo.
6. Para la siguiente versión, Miguel publica un prerelease con tag mayor que 0.7.1, por ejemplo `v0.7.2-killmon` o `v0.8-killmon`, y adjunta `KillmonMusic-*-arm64-release.apk`. Opcional en el cuerpo: `versionCode: 67`. Instala este 0.7.1, entra y abre la app: sale "Nueva versión <tag> disponible", "Actualizar" (verde, texto negro) y "Más tarde". "Más tarde" lo cierra y no vuelve hasta el próximo arranque. "Actualizar" muestra "Descargando…" con la barra y abre el instalador de Android. Un tag `v0.7.1-killmon` sin `versionCode` mayor que 66 no debe avisar; con `versionCode: 67` sí.
7. En Ajustes del teléfono, quita a Killmon el permiso de instalar apps desconocidas. Pulsa "Actualizar". La app abre la pantalla de orígenes desconocidos de Killmon. Actívalo y vuelve: la descarga o el instalador continúan solos. Si cancelas el permiso y vuelves sin activarlo, el diálogo sigue ahí y no se cierra la app.
