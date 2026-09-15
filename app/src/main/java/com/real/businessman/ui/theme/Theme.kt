package com.real.businessman.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = LightAccent,
    onPrimary = PureBlack,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = PureWhite,

    secondary = LightAccentVariant,
    onSecondary = PureBlack,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = PureWhite,

    background = PureBlack,
    onBackground = PureWhite,

    surface = DarkSurface,
    onSurface = PureWhite,

    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = LightAccentVariant,

    // Переопределение фонов нижнего меню (NavigationBar) и диалоговых окон
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurfaceVariant,
    surfaceContainerHighest = DarkSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = DarkAccent,
    onPrimary = PureWhite,
    primaryContainer = LightSurfaceVariant,
    onPrimaryContainer = DarkAccent,

    secondary = DarkAccentVariant,
    onSecondary = PureWhite,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = DarkAccent,

    background = PureWhite,
    onBackground = DarkAccent,

    surface = LightSurface,
    onSurface = DarkAccent,

    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = DarkAccentVariant,

    // Переопределение фонов нижнего меню (NavigationBar) и диалоговых окон
    surfaceContainer = PureWhite,
    surfaceContainerHigh = LightSurface,
    surfaceContainerHighest = LightSurfaceVariant
)

@Composable
fun BusinessmanTheme(
    darkTheme: Boolean = false, // Светлая тема по умолчанию
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}