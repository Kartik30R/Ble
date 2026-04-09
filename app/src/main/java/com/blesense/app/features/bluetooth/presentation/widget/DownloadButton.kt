package com.blesense.app.features.bluetooth.presentation.widget

import android.content.Context
import android.content.Intent
 import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
 import androidx.core.content.FileProvider
 import com.blesense.app.coreui.theme.GlassSurfaceColor
import com.blesense.app.coreui.theme.MintGreenAccent
import com.blesense.app.coreui.theme.TextPrimary
import com.blesense.app.features.bluetooth.domain.model.HistoricalDataEntry
import com.blesense.app.features.bluetooth.domain.model.SensorData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import presentation.viewmodel.BluetoothScanViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DownloadButton(
    viewModel: BluetoothScanViewModel,
    deviceAddress: String,
    deviceName: String,
    deviceId: String,
    isVertical: Boolean = true
) {

    val context = LocalContext.current
    var isExporting by remember { mutableStateOf(false) }

    val createDocumentLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/pdf")
        ) { uri: Uri? ->

            if (uri == null) return@rememberLauncherForActivityResult

            isExporting = true

            MainScope().launch {

                withContext(Dispatchers.IO) {

                    try {

                        context.contentResolver
                            .openOutputStream(uri)
                            ?.use { out ->

                                val tempFile =
                                    File(
                                        context.cacheDir,
                                        "temp_export.pdf"
                                    )

                                generatePDFFile(
                                    tempFile,
                                    viewModel,
                                    deviceAddress,
                                    deviceName,
                                    deviceId
                                )

                                tempFile.inputStream().copyTo(out)
                            }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                isExporting = false
            }
        }


    if (isVertical) {
        Column {
            PreviewActionButton(
                context = context,
                viewModel = viewModel,
                deviceAddress = deviceAddress,
                deviceName = deviceName,
                deviceId = deviceId,
                isExporting = isExporting,
                onExportStateChange = { isExporting = it },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            DownloadActionButton(
                deviceName = deviceName,
                isExporting = isExporting,
                onDownloadClick = { createDocumentLauncher.launch(it) },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            )
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PreviewActionButton(
                context = context,
                viewModel = viewModel,
                deviceAddress = deviceAddress,
                deviceName = deviceName,
                deviceId = deviceId,
                isExporting = isExporting,
                onExportStateChange = { isExporting = it },
                modifier = Modifier.weight(1f).height(56.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            DownloadActionButton(
                deviceName = deviceName,
                isExporting = isExporting,
                onDownloadClick = { createDocumentLauncher.launch(it) },
                modifier = Modifier.weight(1f).height(56.dp)
            )
        }
    }
}

@Composable
private fun PreviewActionButton(
    context: Context,
    viewModel: BluetoothScanViewModel,
    deviceAddress: String,
    deviceName: String,
    deviceId: String,
    isExporting: Boolean,
    onExportStateChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = {
            onExportStateChange(true)
            exportAndPreviewPDF(
                context,
                viewModel,
                deviceAddress,
                deviceName,
                deviceId
            ) {
                onExportStateChange(false)
            }
        },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = GlassSurfaceColor,
            contentColor = TextPrimary
        ),
        shape = RoundedCornerShape(28.dp),
        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 4.dp)
    ) {
        Text("Preview", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DownloadActionButton(
    deviceName: String,
    isExporting: Boolean,
    onDownloadClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val safeName = deviceName.replace("[^A-Za-z0-9_]".toRegex(), "_")
            val fileName = "${safeName}_$timestamp.pdf"
            onDownloadClick(fileName)
        },
        modifier = modifier,
        enabled = !isExporting,
        colors = ButtonDefaults.buttonColors(
            containerColor = MintGreenAccent,
            contentColor = Color.Black
        ),
        shape = RoundedCornerShape(28.dp),
        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 8.dp)
    ) {
        if (isExporting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.Black,
                strokeWidth = 2.dp
            )
        } else {
            Text("Download", fontWeight = FontWeight.ExtraBold)
        }
    }
}


fun exportAndPreviewPDF(
    context: Context,
    viewModel: BluetoothScanViewModel,
    deviceAddress: String,
    deviceName: String,
    deviceId: String,
    onComplete: () -> Unit
) {

    MainScope().launch {

        withContext(Dispatchers.IO) {

            try {

                val safeName =
                    deviceName.replace("[^A-Za-z0-9_]".toRegex(), "_")

                val file =
                    File(
                        context.cacheDir,
                        "${safeName}_report.pdf"
                    )

//                if (file.exists()) file.delete()


                generatePDFFile(
                    file,
                    viewModel,
                    deviceAddress,
                    deviceName,
                    deviceId
                )


                val uri =
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        file
                    )


                val intent =
                    Intent(Intent.ACTION_VIEW).apply {

                        setDataAndType(uri, "application/pdf")

                        flags =
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                    Intent.FLAG_ACTIVITY_NEW_TASK
                    }


                context.startActivity(
                    Intent.createChooser(intent, "Open PDF")
                )

            }
            catch (e: Exception) {

                e.printStackTrace()
            }

            withContext(Dispatchers.Main) {

                onComplete()
            }
        }
    }
}

fun generatePDFFile(
    file: File,
    viewModel: BluetoothScanViewModel,
    deviceAddress: String,
    deviceName: String,
    deviceId: String
) {

    val history =
        viewModel.getDeviceHistory(deviceAddress).toMutableList()

    if (history.isEmpty()) {
        viewModel.devices.value
            .find { it.address == deviceAddress }
            ?.sensorData
            ?.let {
                history.add(
                    HistoricalDataEntry(
                        System.currentTimeMillis(),
                        it
                    )
                )
            }
    }


    val sensorType = determineDeviceType(deviceName)

    val pdf = PdfDocument()

    val pageWidth = 595   // portrait A4
    val pageHeight = 842

    val margin = 40f
    val rowHeight = 18f

    val titlePaint = Paint().apply {
        textSize = 18f
        isFakeBoldText = true
    }

    val headerPaint = Paint().apply {
        textSize = 10f
        isFakeBoldText = true
    }

    val textPaint = Paint().apply {
        textSize = 9f
    }

    val linePaint = Paint()

    val df =
        SimpleDateFormat(
            "yy-MM-dd HH:mm:ss",
            Locale.getDefault()
        )


    val columns = arrayOf(
        "Time",
        "N",
        "P",
        "K",
        "M%",
        "T°C",
        "EC",
        "pH",
        "Sal"
    )


    val colWidths = floatArrayOf(
        110f,
        40f,
        40f,
        40f,
        50f,
        50f,
        45f,
        40f,
        50f
    )

    var pageNumber = 1
    var y = margin

    var pageInfo =
        PdfDocument.PageInfo.Builder(
            pageWidth,
            pageHeight,
            pageNumber
        ).create()

    var page = pdf.startPage(pageInfo)
    var canvas = page.canvas

    fun drawHeader() {

        y = margin

        canvas.drawText(
            "$sensorType REPORT",
            margin,
            y,
            titlePaint
        )

        y += 25

        canvas.drawText(
            "Device: $deviceName",
            margin,
            y,
            textPaint
        )

        y += 15

        canvas.drawText(
            "Node: $deviceId",
            margin,
            y,
            textPaint
        )

        y += 25


        var x = margin

        for (i in columns.indices) {

            canvas.drawText(
                columns[i],
                x,
                y,
                headerPaint
            )

            canvas.drawLine(
                x,
                y + 4,
                x + colWidths[i],
                y + 4,
                linePaint
            )

            x += colWidths[i]
        }

        y += rowHeight
    }


    drawHeader()


    history.forEach { entry ->

        if (y > pageHeight - margin) {

            pdf.finishPage(page)

            pageNumber++

            pageInfo =
                PdfDocument.PageInfo.Builder(
                    pageWidth,
                    pageHeight,
                    pageNumber
                ).create()

            page = pdf.startPage(pageInfo)
            canvas = page.canvas

            drawHeader()
        }


        val soil =
            entry.sensorData as? SensorData.SoilSensorData
                ?: return@forEach


        val values = arrayOf(

            df.format(Date(entry.timestamp)),

            soil.nitrogen,
            soil.phosphorus,
            soil.potassium,
            soil.moisture,
            soil.temperature,
            soil.ec,
            soil.pH,
            soil.salinity
        )


        var x = margin

        for (i in values.indices) {

            val text =
                values[i].take(14)   // prevents overflow

            canvas.drawText(
                text,
                x,
                y,
                textPaint
            )

            x += colWidths[i]
        }

        y += rowHeight
    }


    pdf.finishPage(page)

    file.outputStream().use {
        pdf.writeTo(it)
    }

    pdf.close()
}

fun determineDeviceType(name: String?): String =
    when {

        name?.contains("SHT", true) == true ->
            "SHT40"

        name?.contains("Lux", true) == true ->
            "Lux Sensor"

        name?.contains("SOIL", true) == true ->
            "Soil Sensor"

        name?.contains("NH", true) == true ->
            "Ammonia Sensor"

        name?.contains("Activity", true) == true ->
            "LIS2DH"

        else ->
            "Sensor"
    }