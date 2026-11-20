package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.domain.model.ThemePreference

private val HardikDarkColorScheme = darkColorScheme(
    primary = CrimsonAccent,
    onPrimary = Color.White,
    primaryContainer = CrimsonAccentSoft,
    onPrimaryContainer = Color(0xFFFFDADA),
    secondary = Color(0xFFE4E4E7),
    onSecondary = Color(0xFF09090B),
    secondaryContainer = DarkSurfaceElevated,
    onSecondaryContainer = DarkOnBackground,
    tertiary = CrimsonAccentGlow,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkBorder,
    outlineVariant = Color(0xFF23232B),
    error = CrimsonAccentGlow,
    onError = Color.White,
)

private val HardikLightColorScheme = lightColorScheme(
    primary = CrimsonAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFDE8E9),
    onPrimaryContainer = Color(0xFF6B090E),
    secondary = Color(0xFF18181B),
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceElevated,
    onSecondaryContainer = LightOnBackground,
    tertiary = CrimsonAccentGlow,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightBorder,
    outlineVariant = Color(0xFFE0E0E8),
    error = CrimsonAccent,
    onError = Color.White,
)

val HardikShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

data class HardikMotionConfig(
    val highRefreshMotion: Boolean = true,
)

val LocalMotionConfig = staticCompositionLocalOf { HardikMotionConfig() }

@Composable
fun HardikBudsTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    highRefreshMotion: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themePreference) {
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) HardikDarkColorScheme else HardikLightColorScheme

    CompositionLocalProvider(LocalMotionConfig provides HardikMotionConfig(highRefreshMotion)) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = HardikBudsTypography,
            shapes = HardikShapes,
            content = content,
        )
    }
}
