package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.model.ThemePreference

private val LightColorScheme = lightColorScheme(
    primary = AlokEmerald,
    onPrimary = Color.White,
    primaryContainer = AlokEmeraldContainer,
    onPrimaryContainer = AlokEmeraldDark,
    secondary = AlokMint,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7F9E3),
    onSecondaryContainer = Color(0xFF00391B),
    tertiary = AlokEmeraldDark,
    background = AlokBackgroundLight,
    onBackground = Color(0xFF111B21),
    surface = AlokSurfaceLight,
    onSurface = Color(0xFF111B21),
    surfaceVariant = AlokSurfaceVariantLight,
    onSurfaceVariant = Color(0xFF54656F),
    outline = AlokDividerLight,
    error = AlokDangerRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = AlokMint,
    onPrimary = Color(0xFF00391B),
    primaryContainer = AlokEmeraldDark,
    onPrimaryContainer = Color(0xFFD0F4E8),
    secondary = AlokEmerald,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF13382D),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = AlokMint,
    background = AlokBackgroundDark,
    onBackground = Color(0xFFE9EDEF),
    surface = AlokSurfaceDark,
    onSurface = Color(0xFFE9EDEF),
    surfaceVariant = AlokSurfaceVariantDark,
    onSurfaceVariant = Color(0xFF8696A0),
    outline = AlokDividerDark,
    error = Color(0xFFF28B82),
    onError = Color(0xFF601410)
)

@Composable
fun AlokChatTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    fontScaleMultiplier: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themePreference) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = buildAlokTypography(fontScaleMultiplier),
        content = content
    )
}
