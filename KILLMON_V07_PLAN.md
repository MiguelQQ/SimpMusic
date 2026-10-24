# Killmon Music v0.7

## Menú del avatar
El avatar de arriba a la izquierda en Inicio deja de leer la cuenta de YouTube. Muestra el nombre y el correo de la sesión Killmon, el saludo del día junto a ese nombre, y un enlace a "Cuenta Killmon". Siguen Notificación y Ajustes. Se quita el botón verde "Iniciar sesión en YouTube". Entrar en YouTube queda en Ajustes, en la fila de cuenta de YouTube. La inicial del avatar sale del nombre Killmon.

## Cuenta obligatoria
Al arrancar se lee la sesión guardada y, si hay tokens, se comprueba con el servidor antes de abrir nada. Mientras tanto solo se ve una pantalla negra con el logo, sin Inicio y sin la barra de abajo. Sin sesión válida, la primera pantalla es iniciar sesión o crear cuenta, a pantalla completa y sin botón para omitir. Atrás en esa pantalla cierra la app. Crear cuenta o entrar abre Inicio y borra el historial. Cerrar sesión, eliminar la cuenta o perder la sesión (refresh rechazado) vuelven a esa pantalla y vacían la pila. Un fallo de red con tokens guardados no cierra la sesión.

## Pantalla de acceso
Logo grande (`app_icon`) y el texto "Killmon Music", fondo negro y acentos verde/cian, en el arranque, en iniciar sesión y en crear cuenta.

## Versión
`versionName` 0.7.0 y `versionCode` 65.

## Guion de Pruebas QA

1. Instala el APK 0.7.0 (versionCode 65). Si el teléfono ya tenía Killmon Music, borra los datos de la app. Ábrela. No debe asomar Inicio ni la barra de abajo. Sale una pantalla negra con el logo grande de Killmon Music, el nombre "Killmon Music" y el formulario de iniciar sesión, en verde y cian. No hay botón para omitir ni "Iniciar sesión en YouTube".
2. Pulsa el botón atrás del teléfono. La app se cierra y no queda Inicio detrás.
3. Ábrela otra vez. Pulsa "Iniciar sesión" con el correo vacío: pide el correo. Escribe un correo inválido y una contraseña de menos de 8 caracteres: los avisos salen en español.
4. Pulsa "Crear cuenta". Se ve el mismo logo grande y "Killmon Music". Registra un correo nuevo, una contraseña de al menos 8 caracteres y un nombre. La app entra en Inicio. Atrás sale de la app, no vuelve al formulario.
5. En Inicio, pulsa el avatar de arriba a la izquierda. El menú muestra tu nombre de Killmon, tu correo y el saludo (por ejemplo "Buenos días"). No dice "Invitado" ni el nombre de una cuenta de YouTube. No está el botón verde "Iniciar sesión en YouTube". Están "Cuenta Killmon", Notificación y Ajustes. La inicial del círculo es la de tu nombre de Killmon.
6. Pulsa "Cuenta Killmon". La pantalla muestra ese nombre y ese correo.
7. Abre Ajustes. Arriba, "Cuenta Killmon" repite el nombre y el correo. En Contenido, "Cuenta de YouTube" sigue ahí para quien quiera entrar en YouTube. El menú del avatar no ofrece ese acceso.
8. Vuelve a Cuenta Killmon y pulsa "Cerrar sesión". Confirmas. Aparece otra vez la pantalla de acceso, con el logo. Pulsa atrás: la app se cierra. No se puede volver a Inicio con atrás.
9. Entra con la misma cuenta. En Cuenta Killmon pulsa "Eliminar cuenta". Con una contraseña mala debe decir que no es correcta y seguir en la cuenta. Con la contraseña real, confirmas. Vuelves a la pantalla de acceso y atrás cierra la app. Ese correo ya no puede entrar.
10. Crea la cuenta otra vez, llega a Inicio y cierra la app desde el recents del teléfono. Ábrela de nuevo. Tras la pantalla negra de comprobación entras directo a Inicio, sin ver el formulario y sin un parpadeo de Inicio que luego salte al login. El avatar sigue con tu nombre y tu correo de Killmon.
11. En Ajustes del teléfono, Aplicaciones, Killmon Music: la versión es 0.7.0.
