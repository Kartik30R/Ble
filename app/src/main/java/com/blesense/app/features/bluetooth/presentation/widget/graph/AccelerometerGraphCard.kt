package com.blesense.app.features.bluetooth.presentation.widget.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.blesense.app.features.bluetooth.domain.model.AccPoint
import kotlin.math.roundToInt

@Composable
fun AccelerometerGraphCard(
    title: String,
    cur: Float?,
    hist: List<AccPoint>,
    lineCol: Color,
    cardBg: Color,
    txtCol: Color,
    txt2Col: Color,
    curLabel: String,
    naLabel: String,
    dark: Boolean
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {

        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            GraphHeader(title, cur, curLabel, naLabel, txtCol, txt2Col)

            Spacer(Modifier.height(16.dp))

            GraphCanvas(
                hist = hist,
                lineCol = lineCol,
                txt2Col = txt2Col
            )
        }
    }
}

@Composable
private fun GraphHeader(
    title: String,
    cur: Float?,
    curLabel: String,
    naLabel: String,
    txtCol: Color,
    txt2Col: Color
) {

    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = txtCol
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "$curLabel: ${cur?.toString() ?: naLabel}",
            style = MaterialTheme.typography.bodyMedium,
            color = txt2Col
        )
    }
}

@Composable
private fun GraphCanvas(
    hist: List<AccPoint>,
    lineCol: Color,
    txt2Col: Color
) {

    if (hist.isEmpty()) return

    var selectedId by remember { mutableStateOf<Long?>(null) }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .pointerInput(hist) {
                detectTapGestures { offset ->

                    val lm = 50f
                    val rm = 20f
                    val w = size.width - lm - rm
                    val stepX =
                        w / (hist.size.coerceAtLeast(2) - 1)

                    if (offset.x in lm..(lm + w)) {
                        val idx =
                            (((offset.x - lm) / stepX)
                                .roundToInt())
                                .coerceIn(0, hist.size - 1)

                        selectedId = hist[idx].id
                    }
                }
            }
    ) {

        val lm = 50f
        val rm = 20f
        val tm = 20f
        val bm = 30f

        val w = size.width - lm - rm
        val h = size.height - tm - bm

        val sx = lm
        val sy = tm
        val ex = sx + w
        val ey = sy + h

        val yMin = -20f
        val yMax = 20f
        val yRange = yMax - yMin

        val stepX =
            w / (hist.size.coerceAtLeast(2) - 1).toFloat()

        // -------- PRECOMPUTE PATH HERE (SAFE) --------
        val path = generatePath(
            hist,
            size.width,
            size.height,
            lm,
            tm
        )

        // -------- GRID --------
        for (v in -20..20 step 10) {

            val y =
                sy + h * (1 - (v - yMin) / yRange)

            drawLine(
                color = txt2Col.copy(alpha = 0.25f),
                start = Offset(sx, y),
                end = Offset(ex, y),
                strokeWidth = 1f
            )

            drawContext.canvas.nativeCanvas.drawText(
                v.toString(),
                sx - 15f,
                y + 8f,
                android.graphics.Paint().apply {
                    color =
                        android.graphics.Color.argb(
                            200,
                            (txt2Col.red * 255).toInt(),
                            (txt2Col.green * 255).toInt(),
                            (txt2Col.blue * 255).toInt()
                        )
                    textSize = 28f
                    textAlign =
                        android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }
            )
        }

        // -------- DRAW PATH --------
        drawPath(
            path = path,
            brush = Brush.horizontalGradient(
                listOf(lineCol, lineCol.copy(alpha = 0.6f))
            ),
            style = Stroke(
                width = 8f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // -------- SELECTION --------
        selectedId?.let { id ->

            val idx =
                hist.indexOfFirst { it.id == id }

            if (idx != -1) {

                val tx = sx + idx * stepX
                val ty =
                    sy + h *
                            (1 - (hist[idx].value - yMin) / yRange)

                drawLine(
                    color = lineCol.copy(alpha = 0.4f),
                    start = Offset(tx, sy),
                    end = Offset(tx, ey),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(8f, 8f)
                    )
                )

                drawCircle(lineCol, 10f, Offset(tx, ty))
            }
        }
    }
}
private fun generatePath(
    hist: List<AccPoint>,
    width: Float,
    height: Float,
    leftMargin: Float,
    topMargin: Float,
    yMin: Float = -20f,
    yMax: Float = 20f
): Path {

    val path = Path()

    val rightMargin = 20f
    val bottomMargin = 30f

    val w = width - leftMargin - rightMargin
    val h = height - topMargin - bottomMargin

    val sx = leftMargin
    val sy = topMargin

    val yRange = yMax - yMin
    val stepX =
        w / (hist.size.coerceAtLeast(2) - 1).toFloat()

    val firstY =
        sy + h * (1 - (hist[0].value - yMin) / yRange)

    path.moveTo(sx, firstY)

    for (i in 1 until hist.size) {

        val prevX = sx + (i - 1) * stepX
        val prevY =
            sy + h * (1 - (hist[i - 1].value - yMin) / yRange)

        val x = sx + i * stepX
        val y =
            sy + h * (1 - (hist[i].value - yMin) / yRange)

        val midX = (prevX + x) / 2
        val midY = (prevY + y) / 2

        path.quadraticBezierTo(prevX, prevY, midX, midY)
    }

    return path
}