package com.blesense.app.coreui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.blesense.app.coreui.theme.glassInset

/**
 * 2. Glass Inset Box (De-elevated/Sunken)
 * Used for internal elements that need to appear "recessed" into a card.
 */
@Composable
fun GlassInsetBox(
    modifier: Modifier = Modifier,
    cornerShape: Shape = RoundedCornerShape(12.dp),
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.glassInset(cornerShape),
        contentAlignment = contentAlignment
    ) {
        content()
    }
}
