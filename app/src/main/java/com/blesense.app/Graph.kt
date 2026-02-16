package com.blesense.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Scaffold
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.blesense.app.coreui.theme.DarkBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import presentation.viewmodel.BluetoothScanViewModel
import com.blesense.app.features.bluetooth.domain.model.SensorData
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.compose.runtime.collectAsState

/* -------------------------------------------------------------
   3-D helpers (unchanged)
   ------------------------------------------------------------- */
data class Point3D(val x: Float, val y: Float, val z: Float) {
    fun rotateX(a: Double) = Point3D(
        x,
        (y * cos(a) - z * sin(a)).toFloat(),
        (y * sin(a) + z * cos(a)).toFloat()
    )
    fun rotateY(a: Double) = Point3D(
        (x * cos(a) + z * sin(a)).toFloat(),
        y,
        (-x * sin(a) + z * cos(a)).toFloat()
    )
    fun rotateZ(a: Double) = Point3D(
        (x * cos(a) - y * sin(a)).toFloat(),
        (x * sin(a) + y * cos(a)).toFloat(),
        z
    )
}

/* -------------------------------------------------------------
   Main screen – English only, no translation
   ------------------------------------------------------------- */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(
    navController: NavController,
    deviceAddress: String?,
    viewModel: BluetoothScanViewModel
)
 {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val sensorData by remember(deviceAddress) {
        viewModel.devices.map { list ->
            list.find { it.address == deviceAddress }?.sensorData
        }
    }.collectAsState(initial = null)

    /* ---------------- RAW SENSOR VALUES ---------------- */

    val hum  = (sensorData as? SensorData.SHT40Data)?.humidity?.toFloatOrNull()
    val temp = (sensorData as? SensorData.SHT40Data)?.temperature?.toFloatOrNull()

    val speed = (sensorData as? SensorData.SDTData)?.speed?.toFloatOrNull()
    val dist  = (sensorData as? SensorData.SDTData)?.distance?.toFloatOrNull()

    val accX = (sensorData as? SensorData.LIS2DHData)?.x?.toFloatOrNull()
    val accY = (sensorData as? SensorData.LIS2DHData)?.y?.toFloatOrNull()
    val accZ = (sensorData as? SensorData.LIS2DHData)?.z?.toFloatOrNull()

    val soilM  = (sensorData as? SensorData.SoilSensorData)?.moisture?.toFloatOrNull()
    val soilT  = (sensorData as? SensorData.SoilSensorData)?.temperature?.toFloatOrNull()
    val soilN  = (sensorData as? SensorData.SoilSensorData)?.nitrogen?.toFloatOrNull()
    val soilP  = (sensorData as? SensorData.SoilSensorData)?.phosphorus?.toFloatOrNull()
    val soilK  = (sensorData as? SensorData.SoilSensorData)?.potassium?.toFloatOrNull()
    val soilEC = (sensorData as? SensorData.SoilSensorData)?.ec?.toFloatOrNull()
    val soilPH = (sensorData as? SensorData.SoilSensorData)?.pH?.toFloatOrNull()

    /* ---------------- HISTORY ---------------- */

    val tempH = remember { mutableStateListOf<Float>() }
    val humH = remember { mutableStateListOf<Float>() }
    val speedH = remember { mutableStateListOf<Float>() }
    val distH = remember {
        mutableStateListOf<Float>()
    }
     val luxHistory = remember { mutableStateListOf<Float>() }

     val accXH = remember { mutableStateListOf<Float>() }
    val accYH = remember { mutableStateListOf<Float>() }
    val accZH = remember { mutableStateListOf<Float>() }

    val soilMH = remember { mutableStateListOf<Float>() }
    val soilTH = remember { mutableStateListOf<Float>() }
    val soilNH = remember { mutableStateListOf<Float>() }
    val soilPHist = remember { mutableStateListOf<Float>() }
    val soilKHist = remember { mutableStateListOf<Float>() }
    val soilECHist = remember { mutableStateListOf<Float>() }
    val soilPHHist = remember { mutableStateListOf<Float>() }

    val timestamps = remember { mutableStateListOf<String>() }
    val fmt = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    /* ---------------- UPDATE HISTORY ---------------- */

     val devices by viewModel.devices.collectAsState()

     val matchedDevice = devices.find { it.address == deviceAddress }



    LaunchedEffect(
        temp, hum, speed, dist,
        accX, accY, accZ,
        soilM, soilT, soilN, soilP, soilK, soilEC, soilPH
    ) {
        temp?.let { updateHistory(tempH, it) }
        hum?.let { updateHistory(humH, it) }
        speed?.let { updateHistory(speedH, it) }
        dist?.let { updateHistory(distH, it) }

        accX?.let { updateHistory(accXH, it) }
        accY?.let { updateHistory(accYH, it) }
        accZ?.let { updateHistory(accZH, it) }

        val addTs = soilM != null || soilT != null || soilN != null ||
                soilP != null || soilK != null || soilEC != null || soilPH != null

        if (addTs) {
            if (timestamps.size >= 20) timestamps.removeAt(0)
            timestamps.add(fmt.format(Date()))
        }

        soilM?.let { updateHistory(soilMH, it) }
        soilT?.let { updateHistory(soilTH, it) }
        soilN?.let { updateHistory(soilNH, it) }
        soilP?.let { updateHistory(soilPHist, it) }
        soilK?.let { updateHistory(soilKHist, it) }
        soilEC?.let { updateHistory(soilECHist, it) }
        soilPH?.let { updateHistory(soilPHHist, it) }
    }

    /* ---------------- UI ---------------- */

    var soilClicked by remember { mutableStateOf(false) }
    var tabIdx by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Graphs",
                        style = typography.headlineLarge,
                        color = colorScheme.onSurface
                    )

                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = colorScheme.onSurface
                        )
                    }
                },
                backgroundColor = colorScheme.surface,
                elevation = 0.dp
            )
        },
        backgroundColor = colorScheme.background
    ) { pad ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp)
        ) {
            Text("Address: $deviceAddress")
            Text("Devices: ${viewModel.devices.collectAsState().value.size}")
            Text("Matched: ${matchedDevice != null}")
            Text("SensorData: ${matchedDevice?.sensorData}")
            /* ---- SHT40 ---- */
            if (sensorData is SensorData.SHT40Data) {
                SensorGraphCard(
                    title = "Temperature (°C)",
                    cur = temp,
                    hist = tempH,
                    lineCol = Color.Red,

                    curLabel = "Current",
                    naLabel = "N/A",

                )

                Spacer(Modifier.height(16.dp))

                SensorGraphCard(
                    title = "Humidity (%)",
                    cur = hum,
                    hist = humH,
                    lineCol = Color.Blue,
                    curLabel = "Current",
                    naLabel = "N/A",

                )
            }
            if (sensorData is SensorData.LuxSensorData) {

                val lux = (sensorData as SensorData.LuxSensorData).lux.toFloatOrNull()

                LaunchedEffect(lux) {
                    lux?.let { updateHistory(luxHistory, it) }
                }

                Spacer(Modifier.height(16.dp))

                SensorGraphCard(
                    title = "Light Intensity (Lux)",
                    cur = lux,
                    hist = luxHistory,
                    lineCol = Color.Yellow,
                    curLabel = "Current",
                    naLabel = "N/A"
                )
            }


            /* ---- LIS2DH ---- */
            if (sensorData is SensorData.LIS2DHData) {
                Spacer(Modifier.height(16.dp))
                Accelerometer3DVisualization(
                    xAxis = accX,
                    yAxis = accY,
                    zAxis = accZ,
                )
            }

            /* ---- SOIL ---- */
            if (sensorData is SensorData.SoilSensorData) {
                Spacer(Modifier.height(16.dp))
                SoilSensorDataTable(
                    soilMoistureHistory = soilMH,
                    soilTemperatureHistory = soilTH,
                    soilNitrogenHistory = soilNH,
                    soilPhosphorusHistory = soilPHist,
                    soilPotassiumHistory = soilKHist,
                    soilEcHistory = soilECHist,
                    soilPhHistory = soilPHHist,
                    timestamps = timestamps,
                    isReceivingData = soilMH.isNotEmpty(),
                    soilMoistureLabel = "Soil Moisture (%)",
                    soilTemperatureLabel = "Soil Temperature (°C)",
                    soilNitrogenLabel = "Soil Nitrogen (ppm)",
                    soilPhosphorusLabel = "Soil Phosphorus (ppm)",
                    soilPotassiumLabel = "Soil Potassium (ppm)",
                    soilEcLabel = "Soil EC (µS/cm)",
                    soilPhLabel = "Soil pH",
                    waitingForSensorData = "Waiting for sensor data...",

                )
            }
        }
    }
}


/* -------------------------------------------------------------
   Graph card (touch-enabled)
   ------------------------------------------------------------- */
@Composable
fun SensorGraphCard(
    title: String,
    cur: Float?,
    hist: List<Float>,
    lineCol: Color,
    curLabel: String,
    naLabel: String
) {
    val colorScheme =MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val isDark = colorScheme.background == DarkBackground

    var tapPos by remember { mutableStateOf<Offset?>(null) }
    var tapVal by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(tapPos) {
        if (tapPos != null) {
            kotlinx.coroutines.delay(1000)
            tapPos = null
            tapVal = null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = title,
                style = typography.titleMedium,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "$curLabel: ${cur?.toString() ?: naLabel}",
                style = typography.bodyMedium,
                color = colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            if (hist.isNotEmpty()) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures { tapPos = it }
                            }
                    ) {

                        val pts = hist
                        val lm = 50f; val rm = 20f; val tm = 20f; val bm = 30f
                        val w = size.width - lm - rm
                        val h = size.height - tm - bm
                        val sx = lm
                        val sy = tm
                        val ex = sx + w
                        val ey = sy + h

                        val minVal = pts.minOrNull() ?: 0f
                        val maxVal = pts.maxOrNull() ?: 1f

                        val padding = (maxVal - minVal) * 0.1f
                        val yMin = minVal - padding
                        val yMax = maxVal + padding
                        val yRng = (yMax - yMin).takeIf { it != 0f } ?: 1f

                        val stepX = w / (pts.size.coerceAtLeast(2) - 1).toFloat()

                        // Background grid box
                        drawRect(
                            color = if (isDark)
                                colorScheme.onSurface.copy(alpha = 0.05f)
                            else
                                colorScheme.onSurface.copy(alpha = 0.03f),
                            topLeft = Offset(sx, sy),
                            size = Size(w, h)
                        )

                        val grid = colorScheme.onSurface.copy(alpha = 0.15f)

                        // Horizontal grid
                        for (v in yMin.toInt()..yMax.toInt() step 2) {
                            val y = sy + h * (1 - (v - yMin) / yRng)
                            drawLine(grid, Offset(sx, y), Offset(ex, y), 0.5f)
                        }

                        // Vertical grid
                        for (i in 0..pts.size step 2) {
                            if (i < pts.size) {
                                val x = sx + i * stepX
                                drawLine(grid, Offset(x, sy), Offset(x, ey), 0.5f)
                            }
                        }

                        // Border
                        drawRect(
                            colorScheme.outline,
                            Offset(sx, sy),
                            Size(w, h),
                            style = Stroke(1.5f)
                        )

                        // Graph line
                        val path = Path().apply {
                            val firstY = sy + h * (1 - (pts[0] - yMin) / yRng)
                            moveTo(sx, firstY)
                            for (i in 1 until pts.size) {
                                val x = sx + i * stepX
                                val y = sy + h * (1 - (pts[i] - yMin) / yRng)
                                lineTo(x, y)
                            }
                        }

                        drawPath(
                            path = path,
                            color = lineCol,
                            style = Stroke(
                                width = 2.5f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // Latest point
                        val lastIndex = pts.size - 1
                        val lx = sx + lastIndex * stepX
                        val ly = sy + h * (1 - (pts.last() - yMin) / yRng)
                        drawCircle(lineCol, 4f, Offset(lx, ly))

                        // Tap indicator
                        tapPos?.let { tp ->
                            val idx = ((tp.x - lm) / stepX)
                                .toInt()
                                .coerceIn(0, pts.size - 1)

                            val tx = sx + idx * stepX
                            val ty = sy + h * (1 - (pts[idx] - yMin) / yRng)

                            tapVal = pts[idx]

                            drawLine(
                                color = lineCol.copy(alpha = 0.5f),
                                start = Offset(tx, sy),
                                end = Offset(tx, ey),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                            )

                            drawCircle(lineCol, 6f, Offset(tx, ty))
                        }
                    }
                }

                tapVal?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Touched value: %.2f".format(it),
                        style = typography.labelLarge,
                        color = lineCol,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}


/* -------------------------------------------------------------
   Helper – keep history at max 50 points
   ------------------------------------------------------------- */
private fun updateHistory(list: MutableList<Float>, v: Float) {
    if (list.size >= 50) list.removeAt(0)
    list.add(v)
}

/* -------------------------------------------------------------
   Soil table (tab 1)
   ------------------------------------------------------------- */
@Composable
fun SoilSensorDataTable(
    soilMoistureHistory: List<Float>,
    soilTemperatureHistory: List<Float>,
    soilNitrogenHistory: List<Float>,
    soilPhosphorusHistory: List<Float>,
    soilPotassiumHistory: List<Float>,
    soilEcHistory: List<Float>,
    soilPhHistory: List<Float>,
    timestamps: List<String>,
    isReceivingData: Boolean,
    soilMoistureLabel: String,
    soilTemperatureLabel: String,
    soilNitrogenLabel: String,
    soilPhosphorusLabel: String,
    soilPotassiumLabel: String,
    soilEcLabel: String,
    soilPhLabel: String,
    waitingForSensorData: String
) {

    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        )
    ) {
        Column(Modifier.padding(16.dp)) {

            if (isReceivingData) {

                LazyColumn {
                    items(timestamps.size) { i ->

                        Column {

                            Text(
                                text = "Timestamp: ${timestamps.getOrNull(i) ?: "-"}",
                                style = typography.titleMedium,
                                color = colors.onSurface,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "$soilMoistureLabel: ${soilMoistureHistory.getOrNull(i) ?: "-"}",
                                style = typography.bodyMedium,
                                color = colors.onSurface.copy(alpha = 0.7f)
                            )

                            Text(
                                text = "$soilTemperatureLabel: ${soilTemperatureHistory.getOrNull(i) ?: "-"}",
                                style = typography.bodyMedium,
                                color = colors.onSurface.copy(alpha = 0.7f)
                            )

                            Text(
                                text = "$soilNitrogenLabel: ${soilNitrogenHistory.getOrNull(i) ?: "-"}",
                                style = typography.bodyMedium,
                                color = colors.onSurface.copy(alpha = 0.7f)
                            )

                            Text(
                                text = "$soilPhosphorusLabel: ${soilPhosphorusHistory.getOrNull(i) ?: "-"}",
                                style = typography.bodyMedium,
                                color = colors.onSurface.copy(alpha = 0.7f)
                            )

                            Text(
                                text = "$soilPotassiumLabel: ${soilPotassiumHistory.getOrNull(i) ?: "-"}",
                                style = typography.bodyMedium,
                                color = colors.onSurface.copy(alpha = 0.7f)
                            )

                            Text(
                                text = "$soilEcLabel: ${soilEcHistory.getOrNull(i) ?: "-"}",
                                style = typography.bodyMedium,
                                color = colors.onSurface.copy(alpha = 0.7f)
                            )

                            Text(
                                text = "$soilPhLabel: ${soilPhHistory.getOrNull(i) ?: "-"}",
                                style = typography.bodyMedium,
                                color = colors.onSurface.copy(alpha = 0.7f)
                            )

                            HorizontalDivider(
                                thickness = 1.dp,
                                color = colors.outline.copy(alpha = 0.2f)
                            )
                        }
                    }
                }

            } else {

                Text(
                    text = waitingForSensorData,
                    modifier = Modifier.padding(vertical = 32.dp),
                    style = typography.bodyMedium,
                    color = colors.onSurface
                )
            }
        }
    }
}

/* -------------------------------------------------------------
   3-D accelerometer cube
   ------------------------------------------------------------- */
@Composable
fun Accelerometer3DVisualization(
    xAxis: Float?,
    yAxis: Float?,
    zAxis: Float?
) {

    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val x = xAxis ?: 0f
    val y = yAxis ?: 0f
    val z = zAxis ?: 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "3D Orientation Visualizer",
                color = colors.onSurface,
                style = typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

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

                // ---- cube vertices ----
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

                // ---- axes ----
                val xEnd = Offset(cx + axisLen, cy)
                val yEnd = Offset(cx, cy - axisLen)
                val zEnd = Offset(cx - diag, cy + diag)

                drawLine(Color.Red, Offset(cx, cy), xEnd, 3f)
                drawLine(Color.Green, Offset(cx, cy), yEnd, 3f)
                drawLine(Color.Cyan, Offset(cx, cy), zEnd, 3f)

                drawContext.canvas.nativeCanvas.apply {
                    val p = android.graphics.Paint().apply {
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

                // ---- sensor dot ----
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
                color = colors.onSurface,
                style = typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}
