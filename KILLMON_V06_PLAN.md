# Killmon Music v0.6

## Discord
El icono no sale porque `RpcImage.DiscordImage("killmon_icon")` se convierte en `mp:killmon_icon`. Ese prefijo solo vale para rutas que devuelve `external-assets`, y `application_id` solo se enviaba si había botones. La actividad pasa a llamarse "Killmon Music", siempre manda `application_id` `1563405206408929401`, y el icono (`APP_ICON_URL` del `app_icon.png` en GitHub) se resuelve como `RpcImage.ExternalImage` para la imagen pequeña y como respaldo de la carátula. El fallback http también pasa por `resolveExternalImage`. La miniatura de la canción sigue siendo la imagen grande.

## Cuentas
API `https://killmon.seriepokemon.online/api` con el cliente Ktor y Koin que ya usa el proyecto. Registro, inicio de sesión, refresh (una vez, con mutex, y reintento; si falla se cierra la sesión local), logout, perfil, borrar cuenta y preferencias. Los tokens van en `EncryptedSharedPreferences` en Android. No hay aviso al arrancar: la entrada está en Ajustes. Al iniciar sesión se hace GET y gana el documento con `updated_at` más nuevo; al cambiar idioma, tema, región o el resto de ajustes sincronizados se hace PUT del documento completo y un 409 adopta la copia del servidor. Pantallas en español, fondo negro y acentos verde/cian.

## Versión
`versionName` 0.6.0 y `versionCode` 64.

## Guion de Pruebas QA

1. Instala el APK 0.6.0 (versionCode 64) y ábrelo. La app entra directo a Inicio, sin pedir cuenta.
2. Reproduce una canción con carátula. En Discord, tu estado debe decir "Killmon Music", mostrar el título y el artista, la carátula de la canción y el icono de Killmon en pequeño. Si la canción no tiene carátula, el icono de Killmon ocupa la imagen grande.
3. En Ajustes, arriba, abre "Cuenta Killmon". Pulsa "Iniciar sesión". Deja el correo vacío y pulsa el botón: debe decir que escribas el correo. Escribe un correo inválido y una contraseña de menos de 8 caracteres: los mensajes salen en español.
4. Pulsa "Crear cuenta". Registra un correo nuevo, una contraseña de al menos 8 caracteres y un nombre. Debes entrar a la pantalla de cuenta con ese nombre y ese correo.
5. Vuelve a Ajustes y cambia el tema, el idioma o el país del contenido. Cierra la app por completo y ábrela en otro teléfono (o borra los datos de esta, sin borrar la cuenta en el servidor) e inicia sesión con la misma cuenta. Esos ajustes deben coincidir con lo que guardó el servidor.
6. En la pantalla de cuenta, pulsa "Cerrar sesión". Ajustes vuelve a mostrar "Iniciar sesión" y la app sigue usable sin cuenta.
7. Inicia sesión otra vez. En la cuenta, pulsa "Eliminar cuenta", escribe una contraseña incorrecta: debe decir que no es correcta. Escribe la contraseña real y confirma. La cuenta desaparece y un intento de entrar con ese correo debe fallar.
8. Repite el inicio de sesión con un correo que no exista o una contraseña mala: el mensaje es "Correo o contraseña incorrectos." y no cierra la app.
