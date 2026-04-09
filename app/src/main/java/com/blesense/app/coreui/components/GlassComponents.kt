package com.blesense.app.coreui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.blesense.app.coreui.theme.*

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // 1. Background layer with blur (Frosted Glass Effect)
        Box(
            modifier = Modifier
                .matchParentSize()
                .glassCard() // This blurs only the background surface
        )
        
        // 2. Content layer (Sharp and Clear)
        Box(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

// 2. Neon Pill Button
@Composable
fun NeonPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = true
) {
    Box(
        modifier = modifier
            .neonGlow(active = isActive, cornerShape = CircleShape)
            .background(
                brush = if (isActive) Brush.linearGradient(
                    colors = listOf(MintGreenAccent, MintGreenGlow)
                ) else Brush.linearGradient(
                    colors = listOf(TextDisabled.copy(alpha=0.2f), TextDisabled.copy(alpha=0.1f))
                ),
                shape = CircleShape
            )
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isActive) Color.Black else TextSecondary,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

// 3. Glowing Switch
@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.neonGlow(active = checked),
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = MintGreenAccent,
            uncheckedThumbColor = TextDisabled,
            uncheckedTrackColor = GlassSurfaceColor,
            uncheckedBorderColor = GlassBorderColor
        )
    )
}

// 4. Glowing Slider
@Composable
fun GlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        modifier = modifier.neonGlow(active = true),
        colors = SliderDefaults.colors(
            thumbColor = MintGreenGlow,
            activeTrackColor = MintGreenAccent,
            inactiveTrackColor = GlassSurfaceColor
        )
    )
}
