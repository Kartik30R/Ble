//package com.blesense.app
//
//import android.graphics.Paint
//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.background
//import androidx.compose.foundation.gestures.detectTapGestures
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.shape.RoundedCornerShape
////noinspection UsingMaterialAndMaterial3Libraries
//import androidx.compose.material.Card
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.geometry.Size
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.Path
//import androidx.compose.ui.graphics.PathEffect
//import androidx.compose.ui.graphics.StrokeCap
//import androidx.compose.ui.graphics.StrokeJoin
//import androidx.compose.ui.graphics.drawscope.Stroke
//import androidx.compose.ui.graphics.nativeCanvas
//import androidx.compose.ui.input.pointer.pointerInput
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.navigation.NavController
//import com.blesense.app.coreui.theme.DarkBackground
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.flow.map
//import presentation.viewmodel.BluetoothScanViewModel
//import com.blesense.app.features.bluetooth.domain.model.SensorData
//import java.text.SimpleDateFormat
//import java.util.*
//import kotlin.math.cos
//import kotlin.math.sin
//import kotlin.math.sqrt
//import androidx.compose.runtime.collectAsState
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.sp
//import kotlin.math.roundToInt
//
///* -------------------------------------------------------------
//   3-D helpers (unchanged)
//   ------------------------------------------------------------- */
//data class Point3D(val x: Float, val y: Float, val z: Float) {
//    fun rotateX(a: Double) = Point3D(
//        x,
//        (y * cos(a) - z * sin(a)).toFloat(),
//        (y * sin(a) + z * cos(a)).toFloat()
//    )
//    fun rotateY(a: Double) = Point3D(
//        (x * cos(a) + z * sin(a)).toFloat(),
//        y,
//        (-x * sin(a) + z * cos(a)).toFloat()
//    )
//    fun rotateZ(a: Double) = Point3D(
//        (x * cos(a) - y * sin(a)).toFloat(),
//        (x * sin(a) + y * cos(a)).toFloat(),
//        z
//    )
//}
//data class AccPoint(
//    val id: Long,
//    val value: Float
//)
///* -------------------------------------------------------------
//   Main screen – English only, no translation
//   ------------------------------------------------------------- */
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ChartScreen(
//    navController: NavController,
//    deviceAddress: String?,
//    viewModel: BluetoothScanViewModel
//) {
//
//    val devices by viewModel.devices.collectAsState()
//
//    val sensorData = remember(devices, deviceAddress) {
//        devices.find { it.address == deviceAddress }?.sensorData
//    }
//
//    /* ------------ RAW VALUES ------------ */
//
//    val temp = (sensorData as? SensorData.SHT40Data)?.temperature?.toFloatOrNull()
//    val hum = (sensorData as? SensorData.SHT40Data)?.humidity?.toFloatOrNull()
//
//    val accX = (sensorData as? SensorData.LIS2DHData)?.x?.toFloatOrNull()
//    val accY = (sensorData as? SensorData.LIS2DHData)?.y?.toFloatOrNull()
//    val accZ = (sensorData as? SensorData.LIS2DHData)?.z?.toFloatOrNull()
//
//    /* ------------ HISTORY ------------ */
//
//    val tempH = remember { mutableStateListOf<Float>() }
//    val humH = remember { mutableStateListOf<Float>() }
//
//    val accXH = remember { mutableStateListOf<AccPoint>() }
//    val accYH = remember { mutableStateListOf<AccPoint>() }
//    val accZH = remember { mutableStateListOf<AccPoint>() }
//
//    LaunchedEffect(Unit) { viewModel.startScan() }
//
//    LaunchedEffect(temp, hum) {
//        temp?.let { updateHistory(tempH, it) }
//        hum?.let { updateHistory(humH, it) }
//    }
//
//    LaunchedEffect(accX, accY, accZ) {
//
//        accX?.let {
//            val clamped = it.coerceIn(-20f, 20f)
//            if (accXH.size >= 50) accXH.removeAt(0)
//            accXH.add(AccPoint(System.nanoTime(), clamped))
//        }
//
//        accY?.let {
//            val clamped = it.coerceIn(-20f, 20f)
//            if (accYH.size >= 50) accYH.removeAt(0)
//            accYH.add(AccPoint(System.nanoTime(), clamped))
//        }
//
//        accZ?.let {
//            val clamped = it.coerceIn(-20f, 20f)
//            if (accZH.size >= 50) accZH.removeAt(0)
//            accZH.add(AccPoint(System.nanoTime(), clamped))
//        }
//    }
//
//    /* ------------ THEME COLORS (OLD STYLE) ------------ */
//
//    val bgGrad = Brush.verticalGradient(
//        listOf(Color(0xFF0A74DA), Color(0xFFADD8E6))
//    )
//
//    val cardBg = Color.White
//    val txt = Color.Black
//    val txt2 = Color(0xFF2A2626)
//
//    /* ------------ UI ------------ */
//    Scaffold(
//        modifier = Modifier.fillMaxSize(),
//        contentWindowInsets = WindowInsets.safeDrawing,
//        topBar = {
//            TopAppBar(
//
//                title = {
//                    Text(
//                        "Graphs",
//                        fontSize = 25.sp,
//                        fontWeight = FontWeight.Bold
//                    )
//                },
//                navigationIcon = {
//                    IconButton(onClick = { navController.popBackStack() }) {
//                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
//                    }
//                }
//            )
//        }
//    )  { pad ->
//
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(bgGrad)
//                .padding(pad)
//        ) {
//
//            LazyColumn(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(16.dp),
//                verticalArrangement = Arrangement.spacedBy(16.dp)
//            ) {
//
//                if (sensorData is SensorData.SHT40Data) {
//                    item {
//                        SensorGraphCard(
//                            "Temperature (°C)",
//                            temp,
//                            tempH,
//                            Color(0xFFE53935),
//                            cardBg,
//                            txt,
//                            txt2,
//                            "Current",
//                            "N/A",
//                            false
//                        )
//                    }
//
//                    item {
//                        SensorGraphCard(
//                            "Humidity (%)",
//                            hum,
//                            humH,
//                            Color(0xFF1976D2),
//                            cardBg,
//                            txt,
//                            txt2,
//                            "Current",
//                            "N/A",
//                            false
//                        )
//                    }
//                }
//
//                if (sensorData is SensorData.LIS2DHData) {
//
//                    item {
//                        AccelerometerGraphCard(
//                            "X Axis (g)",
//                            accX,
//                            accXH,
//                            Color(0xFF0336FC),
//                            cardBg,
//                            txt,
//                            txt2,
//                            "Current",
//                            "N/A",
//                            false
//                        )
//                    }
//
//                    item {
//                        AccelerometerGraphCard(
//                            "Y Axis (g)",
//                            accY,
//                            accYH,
//                            Color.Red,
//                            cardBg,
//                            txt,
//                            txt2,
//                            "Current",
//                            "N/A",
//                            false
//                        )
//                    }
//
//                    item {
//                        AccelerometerGraphCard(
//                            "Z Axis (g)",
//                            accZ,
//                            accZH,
//                            Color(0xFF99FF00),
//                            cardBg,
//                            txt,
//                            txt2,
//                            "Current",
//                            "N/A",
//                            false
//                        )
//                    }
//
//                    item {
//                        Accelerometer3DVisualization(
//                            accX,
//                            accY,
//                            accZ,
//
//                        )
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun AccelerometerGraphCard(
//    title: String,
//    cur: Float?,
//    hist: List<AccPoint>,
//    lineCol: Color,
//    cardBg: Color,
//    txtCol: Color,
//    txt2Col: Color,
//    curLabel: String,
//    naLabel: String,
//    dark: Boolean
//) {
//
//    var selectedId by remember { mutableStateOf<Long?>(null) }
//
//    LaunchedEffect(hist) {
//        selectedId?.let { id ->
//            if (hist.none { it.id == id }) {
//                selectedId = null
//            }
//        }
//    }
//
//    Card(
//        modifier = Modifier.fillMaxWidth().height(220.dp),
//        elevation = 8.dp,
//        shape = RoundedCornerShape(20.dp),
//        backgroundColor = cardBg
//    ) {
//
//        Column(
//            modifier = Modifier.padding(16.dp).fillMaxWidth(),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//
//            Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = txtCol)
//            Spacer(Modifier.height(6.dp))
//            Text("$curLabel: ${cur?.toString() ?: naLabel}", fontSize = 15.sp, color = txt2Col)
//            Spacer(Modifier.height(16.dp))
//
//            if (hist.isNotEmpty()) {
//
//                Canvas(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(150.dp)
//                        .pointerInput(hist) {
//                            detectTapGestures { offset ->
//
//                                val lm = 50f
//                                val rm = 20f
//                                val w = size.width - lm - rm
//                                val stepX =
//                                    w / (hist.size.coerceAtLeast(2) - 1)
//
//                                if (offset.x in lm..(lm + w)) {
//                                    val idx =
//                                        (((offset.x - lm) / stepX)
//                                            .roundToInt())
//                                            .coerceIn(0, hist.size - 1)
//
//                                    selectedId = hist[idx].id
//                                }
//                            }
//                        }
//                ) {
//
//                    val lm = 50f
//                    val rm = 20f
//                    val tm = 20f
//                    val bm = 30f
//
//                    val w = size.width - lm - rm
//                    val h = size.height - tm - bm
//
//                    val sx = lm
//                    val sy = tm
//                    val ex = sx + w
//                    val ey = sy + h
//
//                    val yMin = -20f
//                    val yMax = 20f
//                    val yRng = yMax - yMin
//
//                    val stepX =
//                        w / (hist.size.coerceAtLeast(2) - 1).toFloat()
//
//                    // -------- Y GRID + SCALE --------
//                    for (v in -20..20 step 10) {
//
//                        val y =
//                            sy + h * (1 - (v - yMin) / yRng)
//
//                        drawLine(
//                            color = txt2Col.copy(alpha = 0.25f),
//                            start = Offset(sx, y),
//                            end = Offset(ex, y),
//                            strokeWidth = 1f
//                        )
//
//                        drawContext.canvas.nativeCanvas.drawText(
//                            v.toString(),
//                            sx - 15f,
//                            y + 8f,
//                            Paint().apply {
//                                color =
//                                    android.graphics.Color.argb(
//                                        200,
//                                        (txt2Col.red * 255).toInt(),
//                                        (txt2Col.green * 255).toInt(),
//                                        (txt2Col.blue * 255).toInt()
//                                    )
//                                textSize = 28f
//                                textAlign = Paint.Align.RIGHT
//                                isAntiAlias = true
//                            }
//                        )
//                    }
//
//                    // -------- LINE PATH --------
//                    val path = Path()
//
//                    val firstY =
//                        sy + h * (1 - (hist[0].value - yMin) / yRng)
//
//                    path.moveTo(sx, firstY)
//
//                    for (i in 1 until hist.size) {
//
//                        val prevX = sx + (i - 1) * stepX
//                        val prevY =
//                            sy + h * (1 - (hist[i - 1].value - yMin) / yRng)
//
//                        val x = sx + i * stepX
//                        val y =
//                            sy + h * (1 - (hist[i].value - yMin) / yRng)
//
//                        val midX = (prevX + x) / 2
//                        val midY = (prevY + y) / 2
//
//                        path.quadraticBezierTo(prevX, prevY, midX, midY)
//                    }
//
//                    drawPath(
//                        path = path,
//                        brush = Brush.horizontalGradient(
//                            listOf(lineCol, lineCol.copy(alpha = 0.6f))
//                        ),
//                        style = Stroke(8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
//                    )
//
//                    // -------- SELECTION --------
//                    selectedId?.let { id ->
//
//                        val idx = hist.indexOfFirst { it.id == id }
//
//                        if (idx != -1) {
//
//                            val tx = sx + idx * stepX
//                            val ty =
//                                sy + h *
//                                        (1 - (hist[idx].value - yMin) / yRng)
//
//                            drawLine(
//                                color = lineCol.copy(alpha = 0.4f),
//                                start = Offset(tx, sy),
//                                end = Offset(tx, ey),
//                                strokeWidth = 1f,
//                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
//                            )
//
//                            drawCircle(lineCol, 10f, Offset(tx, ty))
//
//                            // Show value above tapped point
//                            drawContext.canvas.nativeCanvas.drawText(
//                                "%.2f".format(hist[idx].value),
//                                tx,
//                                ty - 20f,
//                                Paint().apply {
//                                    color =
//                                        android.graphics.Color.argb(
//                                            255,
//                                            (lineCol.red * 255).toInt(),
//                                            (lineCol.green * 255).toInt(),
//                                            (lineCol.blue * 255).toInt()
//                                        )
//                                    textSize = 32f
//                                    textAlign = Paint.Align.CENTER
//                                    isAntiAlias = true
//                                }
//                            )
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
//
///* -------------------------------------------------------------
//   Graph card (touch-enabled)
//   ------------------------------------------------------------- */
//@Composable
//fun SensorGraphCard(
//    title: String,
//    cur: Float?,
//    hist: List<Float>,
//    lineCol: Color,
//    cardBg: Color,
//    txtCol: Color,
//    txt2Col: Color,
//    curLabel: String,
//    naLabel: String,
//    dark: Boolean
//) {
//
//    var selectedIndex by remember { mutableStateOf<Int?>(null) }
//
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(200.dp),
//        elevation = 8.dp,
//        shape = RoundedCornerShape(20.dp),
//        backgroundColor = cardBg
//    ) {
//
//        Column(
//            modifier = Modifier
//                .padding(16.dp)
//                .fillMaxWidth(),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//
//            Text(
//                title,
//                fontSize = 18.sp,
//                fontWeight = FontWeight.SemiBold,
//                color = txtCol
//            )
//
//            Spacer(Modifier.height(6.dp))
//
//            Text(
//                "$curLabel: ${cur?.toString() ?: naLabel}",
//                fontSize = 15.sp,
//                color = txt2Col
//            )
//
//            Spacer(Modifier.height(16.dp))
//
//            if (hist.isNotEmpty()) {
//
//                Canvas(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(140.dp)
//                        .pointerInput(hist) {
//                            detectTapGestures { offset ->
//                                val lm = 50f
//                                val rm = 20f
//                                val w = size.width - lm - rm
//                                val stepX =
//                                    w / (hist.size.coerceAtLeast(2) - 1)
//
//                                val idx =
//                                    (((offset.x - lm) / stepX)
//                                        .roundToInt())
//                                        .coerceIn(0, hist.size - 1)
//
//                                selectedIndex = idx
//                            }
//                        }
//                ) {
//
//                    val pts = hist
//
//                    val lm = 50f
//                    val rm = 20f
//                    val tm = 20f
//                    val bm = 30f
//
//                    val w = size.width - lm - rm
//                    val h = size.height - tm - bm
//
//                    val sx = lm
//                    val sy = tm
//                    val ex = sx + w
//                    val ey = sy + h
//
//                    val yMin = -20f
//                    val yMax = 20f
//                    val yRng = yMax - yMin
//
//                    val stepX =
//                        w / (pts.size.coerceAtLeast(2) - 1).toFloat()
//
//                    // Y Grid + Labels
//                    for (v in -20..20 step 10) {
//
//                        val y =
//                            sy + h * (1 - (v - yMin) / yRng)
//
//                        drawLine(
//                            color = txt2Col.copy(alpha = 0.2f),
//                            start = Offset(sx, y),
//                            end = Offset(ex, y),
//                            strokeWidth = 1f
//                        )
//
//                        drawContext.canvas.nativeCanvas.drawText(
//                            v.toString(),
//                            sx - 15f,
//                            y + 8f,
//                            android.graphics.Paint().apply {
//                                color =
//                                    android.graphics.Color.argb(
//                                        200,
//                                        (txt2Col.red * 255).toInt(),
//                                        (txt2Col.green * 255).toInt(),
//                                        (txt2Col.blue * 255).toInt()
//                                    )
//                                textSize = 28f
//                                textAlign =
//                                    android.graphics.Paint.Align.RIGHT
//                                isAntiAlias = true
//                            }
//                        )
//                    }
//
//                    // Smooth path
//                    val path = Path()
//                    val firstY =
//                        sy + h * (1 - (pts[0] - yMin) / yRng)
//                    path.moveTo(sx, firstY)
//
//                    for (i in 1 until pts.size) {
//
//                        val prevX = sx + (i - 1) * stepX
//                        val prevY =
//                            sy + h *
//                                    (1 - (pts[i - 1] - yMin) / yRng)
//
//                        val x = sx + i * stepX
//                        val y =
//                            sy + h *
//                                    (1 - (pts[i] - yMin) / yRng)
//
//                        val midX = (prevX + x) / 2
//                        val midY = (prevY + y) / 2
//
//                        path.quadraticBezierTo(
//                            prevX,
//                            prevY,
//                            midX,
//                            midY
//                        )
//                    }
//
//                    drawPath(
//                        path = path,
//                        brush = Brush.horizontalGradient(
//                            listOf(
//                                lineCol,
//                                lineCol.copy(alpha = 0.6f)
//                            )
//                        ),
//                        style = Stroke(
//                            width = 8f,
//                            cap = StrokeCap.Round,
//                            join = StrokeJoin.Round
//                        )
//                    )
//
//                    // Selected point
//                    selectedIndex?.let { idx ->
//                        if (idx < pts.size) {
//
//                            val tx = sx + idx * stepX
//                            val ty =
//                                sy + h *
//                                        (1 - (pts[idx] - yMin) / yRng)
//
//                            drawLine(
//                                color = lineCol.copy(alpha = 0.4f),
//                                start = Offset(tx, sy),
//                                end = Offset(tx, ey),
//                                strokeWidth = 1f,
//                                pathEffect =
//                                    PathEffect.dashPathEffect(
//                                        floatArrayOf(8f, 8f)
//                                    )
//                            )
//
//                            drawCircle(lineCol, 10f, Offset(tx, ty))
//
//                            drawContext.canvas.nativeCanvas.drawText(
//                                "%.2f".format(pts[idx]),
//                                tx,
//                                ty - 20f,
//                                android.graphics.Paint().apply {
//                                    color =
//                                        android.graphics.Color.argb(
//                                            255,
//                                            (lineCol.red * 255).toInt(),
//                                            (lineCol.green * 255).toInt(),
//                                            (lineCol.blue * 255).toInt()
//                                        )
//                                    textSize = 32f
//                                    textAlign =
//                                        android.graphics.Paint.Align.CENTER
//                                    isAntiAlias = true
//                                }
//                            )
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
///* -------------------------------------------------------------
//   Helper – keep history at max 50 points
//   ------------------------------------------------------------- */
//private fun updateHistory(list: MutableList<Float>, v: Float) {
//    if (list.size >= 50) list.removeAt(0)
//    list.add(v)
//}
//
///* -------------------------------------------------------------
//   Soil table (tab 1)
//   ------------------------------------------------------------- */
//@Composable
//fun SoilSensorDataTable(
//    soilMoistureHistory: List<Float>,
//    soilTemperatureHistory: List<Float>,
//    soilNitrogenHistory: List<Float>,
//    soilPhosphorusHistory: List<Float>,
//    soilPotassiumHistory: List<Float>,
//    soilEcHistory: List<Float>,
//    soilPhHistory: List<Float>,
//    timestamps: List<String>,
//    isReceivingData: Boolean,
//    soilMoistureLabel: String,
//    soilTemperatureLabel: String,
//    soilNitrogenLabel: String,
//    soilPhosphorusLabel: String,
//    soilPotassiumLabel: String,
//    soilEcLabel: String,
//    soilPhLabel: String,
//    waitingForSensorData: String
//) {
//
//    val colors = MaterialTheme.colorScheme
//    val typography = MaterialTheme.typography
//
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(16.dp),
//        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = colors.surface
//        )
//    ) {
//        Column(Modifier.padding(16.dp)) {
//
//            if (isReceivingData) {
//
//                LazyColumn {
//                    items(timestamps.size) { i ->
//
//                        Column {
//
//                            Text(
//                                text = "Timestamp: ${timestamps.getOrNull(i) ?: "-"}",
//                                style = typography.titleMedium,
//                                color = colors.onSurface,
//                                fontWeight = FontWeight.Bold
//                            )
//
//                            Text(
//                                text = "$soilMoistureLabel: ${soilMoistureHistory.getOrNull(i) ?: "-"}",
//                                style = typography.bodyMedium,
//                                color = colors.onSurface.copy(alpha = 0.7f)
//                            )
//
//                            Text(
//                                text = "$soilTemperatureLabel: ${soilTemperatureHistory.getOrNull(i) ?: "-"}",
//                                style = typography.bodyMedium,
//                                color = colors.onSurface.copy(alpha = 0.7f)
//                            )
//
//                            Text(
//                                text = "$soilNitrogenLabel: ${soilNitrogenHistory.getOrNull(i) ?: "-"}",
//                                style = typography.bodyMedium,
//                                color = colors.onSurface.copy(alpha = 0.7f)
//                            )
//
//                            Text(
//                                text = "$soilPhosphorusLabel: ${soilPhosphorusHistory.getOrNull(i) ?: "-"}",
//                                style = typography.bodyMedium,
//                                color = colors.onSurface.copy(alpha = 0.7f)
//                            )
//
//                            Text(
//                                text = "$soilPotassiumLabel: ${soilPotassiumHistory.getOrNull(i) ?: "-"}",
//                                style = typography.bodyMedium,
//                                color = colors.onSurface.copy(alpha = 0.7f)
//                            )
//
//                            Text(
//                                text = "$soilEcLabel: ${soilEcHistory.getOrNull(i) ?: "-"}",
//                                style = typography.bodyMedium,
//                                color = colors.onSurface.copy(alpha = 0.7f)
//                            )
//
//                            Text(
//                                text = "$soilPhLabel: ${soilPhHistory.getOrNull(i) ?: "-"}",
//                                style = typography.bodyMedium,
//                                color = colors.onSurface.copy(alpha = 0.7f)
//                            )
//
//                            HorizontalDivider(
//                                thickness = 1.dp,
//                                color = colors.outline.copy(alpha = 0.2f)
//                            )
//                        }
//                    }
//                }
//
//            } else {
//
//                Text(
//                    text = waitingForSensorData,
//                    modifier = Modifier.padding(vertical = 32.dp),
//                    style = typography.bodyMedium,
//                    color = colors.onSurface
//                )
//            }
//        }
//    }
//}
//
///* -------------------------------------------------------------
//   3-D accelerometer cube
//   ------------------------------------------------------------- */
//@Composable
//fun Accelerometer3DVisualization(
//    xAxis: Float?,
//    yAxis: Float?,
//    zAxis: Float?
//) {
//
//    val colors = MaterialTheme.colorScheme
//    val typography = MaterialTheme.typography
//
//    val x = xAxis ?: 0f
//    val y = yAxis ?: 0f
//    val z = zAxis ?: 0f
//
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(380.dp),
//        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = colors.surface
//        )
//    ) {
//        Column(
//            modifier = Modifier
//                .padding(16.dp)
//                .fillMaxWidth(),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//
//            Text(
//                text = "3D Orientation Visualizer",
//                color = colors.onSurface,
//                style = typography.titleMedium,
//                fontWeight = FontWeight.SemiBold,
//                textAlign = TextAlign.Center
//            )
//
//            Canvas(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(280.dp)
//                    .padding(8.dp)
//            ) {
//
//                val cx = size.width / 2f
//                val cy = size.height / 2f
//                val scale = minOf(size.width, size.height) * 0.25f
//                val axisLen = 300f
//                val diag = axisLen / sqrt(2f)
//
//                val edge = colors.onSurface
//
//                // ---- cube vertices ----
//                val verts = arrayOf(
//                    floatArrayOf(-1f, -1f, -1f), floatArrayOf(1f, -1f, -1f),
//                    floatArrayOf(1f, 1f, -1f),   floatArrayOf(-1f, 1f, -1f),
//                    floatArrayOf(-1f, -1f, 1f),  floatArrayOf(1f, -1f, 1f),
//                    floatArrayOf(1f, 1f, 1f),    floatArrayOf(-1f, 1f, 1f)
//                )
//
//                val edges = arrayOf(
//                    intArrayOf(0,1), intArrayOf(1,2), intArrayOf(2,3), intArrayOf(3,0),
//                    intArrayOf(4,5), intArrayOf(5,6), intArrayOf(6,7), intArrayOf(7,4),
//                    intArrayOf(0,4), intArrayOf(1,5), intArrayOf(2,6), intArrayOf(3,7)
//                )
//
//                fun rot(x: Float, y: Float, z: Float): FloatArray {
//                    val rx = Math.toRadians(20.0).toFloat()
//                    val ry = Math.toRadians(25.0).toFloat()
//                    val rz = Math.toRadians(5.0).toFloat()
//
//                    var yy = y * cos(rx) - z * sin(rx)
//                    var zz = y * sin(rx) + z * cos(rx)
//                    var xx = x
//
//                    val zz2 = zz * cos(ry) - xx * sin(ry)
//                    val xx2 = zz * sin(ry) + xx * cos(ry)
//
//                    val xx3 = xx2 * cos(rz) - yy * sin(rz)
//                    val yy3 = xx2 * sin(rz) + yy * cos(rz)
//
//                    return floatArrayOf(xx3, yy3, zz2)
//                }
//
//                fun proj(x: Float, y: Float, z: Float): Offset {
//                    val d = 5f
//                    val p = d / (d - z)
//                    return Offset(cx + x * scale * p, cy - y * scale * p)
//                }
//
//                val projected = verts.map {
//                    val r = rot(it[0], it[1], it[2])
//                    proj(r[0], r[1], r[2])
//                }
//
//                edges.forEach { (a, b) ->
//                    drawLine(
//                        color = edge,
//                        start = projected[a],
//                        end = projected[b],
//                        strokeWidth = 3f
//                    )
//                }
//
//                projected.forEach {
//                    drawCircle(edge, 4f, it)
//                }
//
//                // ---- axes ----
//                val xEnd = Offset(cx + axisLen, cy)
//                val yEnd = Offset(cx, cy - axisLen)
//                val zEnd = Offset(cx - diag, cy + diag)
//
//                drawLine(Color.Red, Offset(cx, cy), xEnd, 3f)
//                drawLine(Color.Green, Offset(cx, cy), yEnd, 3f)
//                drawLine(Color.Cyan, Offset(cx, cy), zEnd, 3f)
//
//                drawContext.canvas.nativeCanvas.apply {
//                    val p = Paint().apply {
//                        textSize = 36f
//                        isAntiAlias = true
//                    }
//
//                    p.color = android.graphics.Color.RED
//                    drawText("X", xEnd.x + 10f, xEnd.y, p)
//
//                    p.color = android.graphics.Color.GREEN
//                    drawText("Y", yEnd.x + 10f, yEnd.y, p)
//
//                    p.color = android.graphics.Color.CYAN
//                    drawText("Z", zEnd.x + 20f, zEnd.y + 30f, p)
//                }
//
//                // ---- sensor dot ----
//                val range = 10f
//                val sx = (x / range).coerceIn(-1f, 1f)
//                val sy = (y / range).coerceIn(-1f, 1f)
//                val sz = (z / range).coerceIn(-1f, 1f)
//
//                val r = rot(sx, sy, sz)
//                val dot = proj(r[0], r[1], r[2])
//
//                drawCircle(
//                    color = colors.primary,
//                    radius = 10f,
//                    center = dot
//                )
//            }
//
//            Text(
//                text = "X: %.2f, Y: %.2f, Z: %.2f".format(x, y, z),
//                color = colors.onSurface,
//                style = typography.bodyMedium,
//                modifier = Modifier.padding(top = 8.dp),
//                textAlign = TextAlign.Center
//            )
//        }
//    }
//}
