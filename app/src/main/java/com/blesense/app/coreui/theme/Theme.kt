package com.blesense.app.coreui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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
//    val view = LocalView.current

    val colorScheme =
        if (isDarkMode) DarkColorScheme else LightColorScheme

//    SideEffect {
//        val window = (view.context as Activity).window
//        window.statusBarColor = Color.Transparent.toArgb()
//
//        WindowCompat.getInsetsController(window, view)
//            .isAppearanceLightStatusBars = !isDarkMode
//    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

