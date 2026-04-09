package com.blesense.app.coreui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import android.app.Activity
import androidx.core.view.WindowInsetsControllerCompat

// Force dark mode exclusively utilizing the gradient edges as fallback colors
private val GlassColorScheme = darkColorScheme(
    primary = MintGreenAccent,
    onPrimary = Color.Black,
    primaryContainer = MintGreenGlow.copy(alpha = 0.3f),
    onPrimaryContainer = MintGreenAccent,
    
    secondary = MintGreenAccent,
    onSecondary = Color.Black,
    secondaryContainer = MintGreenGlow.copy(alpha = 0.2f),
    onSecondaryContainer = MintGreenAccent,
    
    tertiary = MintGreenGlow,
    onTertiary = Color.Black,
    
    background = DarkGradientStart,
    onBackground = TextPrimary,
    
    surface = DarkGradientEnd,
    onSurface = TextPrimary,
    surfaceVariant = GlassSurfaceColor,
    onSurfaceVariant = TextSecondary,
    
    outline = GlassBorderColor,
    outlineVariant = GlassBorderColor.copy(alpha = 0.5f),
    
    error = Color(0xFFCF6679),
    onError = Color.Black
)



@Composable
fun BleSenseTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Since we use a dark charcoal background (DarkGradientStart), 
            // we need light icons (white) for the status bar to be visible.
            WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = GlassColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
