package com.blesense.app.coreui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.blesense.app.ThemeManager

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    outline = LightOutline,
    error = LightError
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    outline = DarkOutline,
    error = DarkError
)

@Composable
fun BleSenseTheme(
    content: @Composable () -> Unit
) {
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()

    MaterialTheme(
        colorScheme = if (isDarkMode) DarkColorScheme else LightColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
