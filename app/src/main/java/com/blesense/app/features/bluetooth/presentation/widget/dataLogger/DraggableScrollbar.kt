package com.blesense.app.features.bluetooth.presentation.widget.dataLogger
import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
 import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape

import kotlinx.coroutines.launch
import kotlin.math.max

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun DraggableScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(50.dp)
    ) {
        val maxHeightPx = constraints.maxHeight.toFloat()

        val totalItems = state.layoutInfo.totalItemsCount
        val visibleItems = state.layoutInfo.visibleItemsInfo

        if (totalItems > visibleItems.size && visibleItems.isNotEmpty()) {

            val thumbHeightPx =
                max(120f, maxHeightPx * (visibleItems.size.toFloat() / totalItems))

            val trackHeightPx = maxHeightPx - thumbHeightPx

            val scrollOffset =
                state.firstVisibleItemIndex.toFloat() /
                        (totalItems - visibleItems.size).coerceAtLeast(1)

            val thumbOffsetY =
                with(LocalDensity.current) {
                    (scrollOffset * trackHeightPx).toDp()
                }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(totalItems, maxHeightPx) {
                        detectVerticalDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = { isDragging = false },
                            onVerticalDrag = { change, _ ->
                                val dragY = change.position.y
                                val newOffset =
                                    (dragY / maxHeightPx).coerceIn(0f, 1f)

                                val itemToScroll =
                                    (newOffset * totalItems).toInt()

                                coroutineScope.launch {
                                    state.scrollToItem(
                                        itemToScroll.coerceIn(
                                            0,
                                            totalItems - 1
                                        )
                                    )
                                }
                            }
                        )
                    }
            ) {

                Box(
                    modifier = Modifier
                        .offset(y = thumbOffsetY)
                        .align(Alignment.TopEnd)
                        .width(10.dp)
                        .height(
                            with(LocalDensity.current) {
                                thumbHeightPx.toDp()
                            }
                        )
                        .padding(end = 6.dp)
                        .background(
                            color = if (isDragging)
                                Color.White
                            else
                                Color.White.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                )
            }
        }
    }
}
