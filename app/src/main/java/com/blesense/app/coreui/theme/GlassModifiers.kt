package com.blesense.app.coreui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// 1. Root Background Pattern (Dark gradient system)
fun Modifier.neumorphicBackground(): Modifier = this.then(
    Modifier.background(
        brush = Brush.verticalGradient(
            colors = listOf(
                DarkGradientStart,
                DarkGradientEnd
            )
        )
    )
)

/**
 * 2. Elevated Neomorphic Shadow (Convex/Raised)
 * Uses drawBehind and framework-level Paint to achieve soft, diffused dual shadows.
 */
fun Modifier.softNeomorphicShadow(
    shape: Shape = RoundedCornerShape(24.dp),
    shadowRadius: Dp = 14.dp,
    lightShadowColor: Color = Color.White.copy(alpha = 0.08f),
    darkShadowColor: Color = Color.Black.copy(alpha = 0.9f),
    offsetX: Dp = 8.dp,
    offsetY: Dp = 8.dp
): Modifier = this.drawBehind {
    val shadowRadiusPx = shadowRadius.toPx()
    val offsetXPx = offsetX.toPx()
    val offsetYPx = offsetY.toPx()

    drawIntoCanvas { canvas ->
        // --- 1. Top-Left Light Glow ---
        val lightPaint = Paint().apply {
            color = lightShadowColor
            asFrameworkPaint().apply {
                if (shadowRadiusPx > 0f) {
                    setShadowLayer(shadowRadiusPx, -offsetXPx, -offsetYPx, lightShadowColor.toArgb())
                }
            }
        }
        canvas.drawRoundRect(
            0f, 0f, size.width, size.height,
            24.dp.toPx(), 24.dp.toPx(),
            lightPaint
        )

        // --- 2. Bottom-Right Dark Shadow ---
        val darkPaint = Paint().apply {
            color = Color.Transparent
            asFrameworkPaint().apply {
                if (shadowRadiusPx > 0f) {
                    setShadowLayer(shadowRadiusPx, offsetXPx, offsetYPx, darkShadowColor.toArgb())
                }
            }
        }
        canvas.drawRoundRect(
            0f, 0f, size.width, size.height,
            24.dp.toPx(), 24.dp.toPx(),
            darkPaint
        )
    }
}

/**
 * 3. Inset Neomorphic Shadow (Concave/De-elevated)
 * Uses a clipped canvas to draw shadows INSIDE the shape.
 */
fun Modifier.neomorphicInsetShadow(
    shape: Shape = RoundedCornerShape(12.dp),
    shadowRadius: Dp = 8.dp,
    darkShadowColor: Color = Color.Black.copy(alpha = 0.8f),
    lightShadowColor: Color = Color.White.copy(alpha = 0.1f),
    offsetX: Dp = 4.dp,
    offsetY: Dp = 4.dp
): Modifier = this.drawBehind {
    val shadowRadiusPx = shadowRadius.toPx()
    val offsetXPx = offsetX.toPx()
    val offsetYPx = offsetY.toPx()
    val cornerRadiusPx = 12.dp.toPx() // Hardcoded for 12dp inset elements

    drawIntoCanvas { canvas ->
        // Clip to the shape so shadows only appear inside
        val path = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = 0f,
                    top = 0f,
                    right = size.width,
                    bottom = size.height,
                    radiusX = cornerRadiusPx,
                    radiusY = cornerRadiusPx
                )
            )
        }
        canvas.save()
        canvas.clipPath(path)

        // --- 1. Top-Left Dark Inset Shadow ---
        val darkPaint = Paint().apply {
            color = Color.Transparent
            asFrameworkPaint().apply {
                setShadowLayer(shadowRadiusPx, offsetXPx, offsetYPx, darkShadowColor.toArgb())
            }
        }
        canvas.drawRoundRect(
            -offsetXPx, -offsetYPx, size.width + offsetXPx, size.height + offsetYPx,
            cornerRadiusPx, cornerRadiusPx,
            darkPaint
        )

        // --- 2. Bottom-Right Light Inset Highlight ---
        val lightPaint = Paint().apply {
            color = Color.Transparent
            asFrameworkPaint().apply {
                setShadowLayer(shadowRadiusPx, -offsetXPx, -offsetYPx, lightShadowColor.toArgb())
            }
        }
        canvas.drawRoundRect(
            offsetXPx, offsetYPx, size.width - offsetXPx, size.height - offsetYPx,
            cornerRadiusPx, cornerRadiusPx,
            lightPaint
        )

        canvas.restore()
    }
}

/**
 * 4. Neomorphic Gradient Border (Catching the Light)
 */
fun Modifier.neomorphicGradientBorder(
    cornerShape: Shape = RoundedCornerShape(24.dp),
    width: Dp = 1.dp
): Modifier = this.then(
    Modifier.border(
        width = width,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.12f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.2f)
            )
        ),
        shape = cornerShape
    )
)

/**
 * 5. Elevated Glass Card Modifier
 */
fun Modifier.glassCard(
    cornerShape: Shape = RoundedCornerShape(24.dp),
): Modifier {
    return this.then(
        Modifier
            .softNeomorphicShadow(shape = cornerShape)
            .clip(cornerShape)
            .let { 
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    it.blur(30.dp)
                } else {
                    it
                }
            }
            .background(GlassSurfaceColor)
            .neomorphicGradientBorder(cornerShape = cornerShape)
    )
}

/**
 * 6. Inset Glass Card Modifier (Pressed/Sunken)
 */
fun Modifier.glassInset(
    cornerShape: Shape = RoundedCornerShape(12.dp)
): Modifier {
    return this.then(
        Modifier
            .neomorphicInsetShadow(shape = cornerShape)
            .clip(cornerShape)
            .background(GlassSurfaceColor.copy(alpha = 0.15f)) // Slightly darker for inset
    )
}

/**
 * 7. Neon Glow for Active Buttons/Toggles
 */
fun Modifier.neonGlow(active: Boolean = true, cornerShape: Shape = RoundedCornerShape(50)): Modifier {
    return if (active) {
        this.then(
            Modifier.shadow(
                elevation = 20.dp,
                shape = cornerShape,
                ambientColor = MintGreenGlow.copy(alpha = 0.5f),
                spotColor = MintGreenGlow.copy(alpha = 0.8f)
            )
        )
    } else {
        this
    }
}
