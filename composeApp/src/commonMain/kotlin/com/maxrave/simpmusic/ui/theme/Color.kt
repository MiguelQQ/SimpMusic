package com.maxrave.simpmusic.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.getPlatform

// ===== Brand =====

/**
 * Killmon Music accent: green → cyan, taken from the app icon.
 * The Material 3 ColorScheme is generated from [seed] at runtime — see [AppTheme].
 */
val killmonGreen = Color(0xFF39E75F)

/** Label and spinner on a [killmonGreen] primary button. Never a gray "disabled" tone. */
val killmonOnGreen = Color(0xFF000000)
val killmonCyan = Color(0xFF18E0E8)
val killmonAccentGradient = listOf(killmonGreen, killmonCyan)

/** Desktop window only. Android keeps [killmonGreen]. */
val killmonPurple = Color(0xFF7C4DFF)
val killmonPurpleLight = Color(0xFFB388FF)
val killmonPurpleGradient = listOf(killmonPurple, Color(0xFF9B59B6))

/**
 * Brand seed color. The whole Material 3 ColorScheme is generated from this
 * color at runtime — see [AppTheme]. Android uses green. Desktop uses purple.
 */
val seed = killmonGreen

@Composable
fun killmonBrand(): Color = if (getPlatform() == Platform.Desktop) killmonPurple else killmonGreen

@Composable
fun killmonBrandSecondary(): Color = if (getPlatform() == Platform.Desktop) killmonPurpleLight else killmonCyan

@Composable
fun killmonBrandGradient(): List<Color> = if (getPlatform() == Platform.Desktop) killmonPurpleGradient else killmonAccentGradient

/** Spotify-style page and cards (dark theme). */
val killmonBlack = Color(0xFF000000)
val killmonSurface = Color(0xFF121212)
val killmonCard = Color(0xFF2A2A2A)

// ===== Semantic colors (not derivable from the color scheme) =====

/** Liked/favorite state (heart buttons, favorite tiles). */
val favoriteColor = Color(0xFFFF4081)

/** Currently playing lyric line. */
val lyricActiveColor = Color(0xFFFFFF00)

val shimmerBackground = Color(0x7E383737)
val shimmerLine = Color(0xFF4D4848)

// Light-theme counterparts of the shimmer tokens.
val shimmerBackgroundLight = Color(0x7EDCD8D8)
val shimmerLineLight = Color(0xFFCFC8C8)

val overlay = Color(0x32242424)
val blackMoreOverlay = Color(0x8f242424)

// ===== Desktop shell =====
// Spotify-style layering for the desktop window: the window itself takes the extreme, panels
// step one shade back towards the middle so they read as raised. The light panel is not here —
// it comes from colorScheme.surfaceContainer, which already sits at the right distance.

val desktopWindowDark = Color(0xFF100818)
val desktopWindowLight = Color(0xFFF7F2FF)
val desktopPanelDark = Color(0xFF1C1230)

// ===== Desktop window controls =====
// The macOS traffic lights. Fixed by convention rather than by theme — users read these by
// colour, so they stay the same in light and dark.

val windowCloseButton = Color(0xFFFF605C)
val windowCloseButtonHover = Color(0xFFE54942)
val windowMinimiseButton = Color(0xFFFFBD44)
val windowMinimiseButtonHover = Color(0xFFE5A93D)
val windowMaximiseButton = Color(0xFF00CA4E)
val windowMaximiseButtonHover = Color(0xFF00B344)

// ===== Legacy — do not add new usages =====

/**
 * Old M3 primary (lavender). Kept only for the SettingScreen storage bar,
 * which stays untouched by owner's decision.
 */
@Deprecated("Legacy storage bar color only — use MaterialTheme.colorScheme.primary in new code")
val md_theme_dark_primary = Color(0xFFB2C5FF)
