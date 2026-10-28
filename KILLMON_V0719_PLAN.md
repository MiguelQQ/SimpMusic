# Killmon Music 0.7.19 (84) — barra Desktop sólida (sin liquid glass)

## Pedido de Miguel (28/10/2026, ~9:04 COT)

> "La barra de reproduccion de musica la necesito mas bonita como la de la apk , se ve como transparente tipo liquid glass y asi no me gusta"

## Alcance

Sobre **0.7.18**, rama `killmon-0.7.3-auto`. Subir a **0.7.19 / versionCode 84**. User-Agent JAM: `Killmon Music/0.7.19`.

Solo Desktop (Windows/JVM) mini player / cápsula flotante. Android APK se queda igual (salvo bump de versión). No tocar JAM, Connect API, updater URL, applicationId.

## Causa

En `composeApp/.../ui/screen/MiniPlayer.kt` la cápsula Desktop **siempre** usa liquid glass:

```kotlin
val useGlassSurface = isLiquidGlassEnabled == TRUE || getPlatform() == Platform.Desktop
// ...
.liquidGlass(backdrop, layer, luminance, capsuleShape, blurScale = 1.2f)
```

En `App.kt` también: `liquidGlassEnabled = ... || getPlatform() == Platform.Desktop`.

En Android (rama `Platform.Android`) la barra es un Card sólido `MiniPlayerCard = Color(0xFF2A2A2A).copy(alpha = 0.97f)`.

## Arreglo

1. **Desktop deja de forzar liquid glass.** `useGlassSurface` solo sigue el setting (o Directamente `false` en Desktop). Quitar el `|| Platform.Desktop`.
2. **Superficie sólida opaca** en la cápsula Desktop: fondo `MiniPlayerCard` (o `Color(0xFF2A2A2A)` / surface oscuro del tema Killmon morado), **sin** `.liquidGlass`, sin smear/blur. Mantener forma cápsula (`RoundedCornerShape(50)`), layout, controles, tema morado `killmonPurple`.
3. **App.kt**: no forzar `liquidGlassEnabled` en Desktop; el backdrop/haze de la cápsula no debe depender de glass.
4. Texto/iconos legibles sobre fondo sólido oscuro (como Android).
5. Compilar `:composeApp:compileKotlinJvm`, `:desktopApp:compileKotlinJvm`, Android release APK si cabe en memoria.
6. Commits claros + push rama. **Sin merge a main.**
7. Al final del trabajo de código: sección **"Guion de Pruebas QA para Grokbot"** con qué mirar en Windows (barra opaca, no transparente; mismos controles).

## Fuera de alcance

- No renombrar repo GitHub.
- No cambiar API S2.
- Build Windows zip + APK + prerelease `v0.7.19-killmon` lo hace el orquestador tras tu código (o tú si te alcanza).

## Criterio listo

Desktop mini player se ve **sólido/opaco** como la barra de la APK (fondo gris oscuro/negro, no cristal), Windows caption y Connect de 0.7.18 intactos, versión 0.7.19/84.
