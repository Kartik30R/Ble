package com.blesense.app.features.bluetooth.presentation.widget.graph
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import android.graphics.Paint
@Composable
fun Accelerometer3DVisualization(
    xAxis: Float?,
    yAxis: Float?,
    zAxis: Float?
) {

    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val shapes = MaterialTheme.shapes

    val x = xAxis ?: 0f
    val y = yAxis ?: 0f
    val z = zAxis ?: 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp),
        shape = shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "3D Orientation Visualizer",
                style = typography.titleMedium,
                color = colors.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .padding(8.dp)
            ) {

                val cx = size.width / 2f
                val cy = size.height / 2f
                val scale = minOf(size.width, size.height) * 0.25f
                val axisLen = 300f
                val diag = axisLen / sqrt(2f)

                val edge = colors.onSurface

                val verts = arrayOf(
                    floatArrayOf(-1f, -1f, -1f), floatArrayOf(1f, -1f, -1f),
                    floatArrayOf(1f, 1f, -1f),   floatArrayOf(-1f, 1f, -1f),
                    floatArrayOf(-1f, -1f, 1f),  floatArrayOf(1f, -1f, 1f),
                    floatArrayOf(1f, 1f, 1f),    floatArrayOf(-1f, 1f, 1f)
                )

                val edges = arrayOf(
                    intArrayOf(0,1), intArrayOf(1,2), intArrayOf(2,3), intArrayOf(3,0),
                    intArrayOf(4,5), intArrayOf(5,6), intArrayOf(6,7), intArrayOf(7,4),
                    intArrayOf(0,4), intArrayOf(1,5), intArrayOf(2,6), intArrayOf(3,7)
                )

                fun rot(x: Float, y: Float, z: Float): FloatArray {
                    val rx = Math.toRadians(20.0).toFloat()
                    val ry = Math.toRadians(25.0).toFloat()
                    val rz = Math.toRadians(5.0).toFloat()

                    var yy = y * cos(rx) - z * sin(rx)
                    var zz = y * sin(rx) + z * cos(rx)
                    var xx = x

                    val zz2 = zz * cos(ry) - xx * sin(ry)
                    val xx2 = zz * sin(ry) + xx * cos(ry)

                    val xx3 = xx2 * cos(rz) - yy * sin(rz)
                    val yy3 = xx2 * sin(rz) + yy * cos(rz)

                    return floatArrayOf(xx3, yy3, zz2)
                }

                fun proj(x: Float, y: Float, z: Float): Offset {
                    val d = 5f
                    val p = d / (d - z)
                    return Offset(cx + x * scale * p, cy - y * scale * p)
                }

                val projected = verts.map {
                    val r = rot(it[0], it[1], it[2])
                    proj(r[0], r[1], r[2])
                }

                edges.forEach { (a, b) ->
                    drawLine(
                        color = edge,
                        start = projected[a],
                        end = projected[b],
                        strokeWidth = 3f
                    )
                }

                projected.forEach {
                    drawCircle(edge, 4f, it)
                }

                // Axes
                val xEnd = Offset(cx + axisLen, cy)
                val yEnd = Offset(cx, cy - axisLen)
                val zEnd = Offset(cx - diag, cy + diag)

                drawLine(Color.Red, Offset(cx, cy), xEnd, 3f)
                drawLine(Color.Green, Offset(cx, cy), yEnd, 3f)
                drawLine(Color.Cyan, Offset(cx, cy), zEnd, 3f)

                drawContext.canvas.nativeCanvas.apply {
                    val p = Paint().apply {
                        textSize = 36f
                        isAntiAlias = true
                    }

                    p.color = android.graphics.Color.RED
                    drawText("X", xEnd.x + 10f, xEnd.y, p)

                    p.color = android.graphics.Color.GREEN
                    drawText("Y", yEnd.x + 10f, yEnd.y, p)

                    p.color = android.graphics.Color.CYAN
                    drawText("Z", zEnd.x + 20f, zEnd.y + 30f, p)
                }

                // Sensor dot
                val range = 10f
                val sx = (x / range).coerceIn(-1f, 1f)
                val sy = (y / range).coerceIn(-1f, 1f)
                val sz = (z / range).coerceIn(-1f, 1f)

                val r = rot(sx, sy, sz)
                val dot = proj(r[0], r[1], r[2])

                drawCircle(
                    color = colors.primary,
                    radius = 10f,
                    center = dot
                )
            }

            Text(
                text = "X: %.2f, Y: %.2f, Z: %.2f".format(x, y, z),
                style = typography.bodyMedium,
                color = colors.onSurface,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}