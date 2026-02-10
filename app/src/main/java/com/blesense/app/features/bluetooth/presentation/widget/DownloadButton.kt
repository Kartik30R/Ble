package com.blesense.app.features.bluetooth.presentation.widget

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blesense.app.coreui.constants.AppStrings
import com.blesense.app.features.bluetooth.domain.model.HistoricalDataEntry
import com.blesense.app.features.bluetooth.domain.model.SensorData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import presentation.viewmodel.BluetoothScanViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DownloadButton(
    viewModel: BluetoothScanViewModel,
    deviceAddress: String,
    deviceName: String,
    deviceId: String
) {
    val context = LocalContext.current
    var isExporting by remember { mutableStateOf(false) }

    // Launcher for selecting a location to save the CSV
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            isExporting = true
            // Call your existing export function
            exportDataToCSV(
                context = context,
                uri = uri,
                viewModel = viewModel,
                deviceAddress = deviceAddress,
                deviceName = deviceName,
                deviceId = deviceId
            ) {
                isExporting = false
                Toast.makeText(context, "Data Exported Successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Button(
        onClick = {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "BLE_Sense_${deviceId}_$timestamp.csv"
            createDocumentLauncher.launch(filename)
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp) // Standard Material 3 button height
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        enabled = !isExporting,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.outline
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        if (isExporting) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = AppStrings.EXPORTING_DATA,
                style = MaterialTheme.typography.labelLarge
            )
        } else {
            Text(
                text = AppStrings.DOWNLOAD_DATA,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.25.sp
            )
        }
    }
}

fun exportDataToCSV(
    context: Context,
    uri: Uri,
    viewModel: BluetoothScanViewModel,
    deviceAddress: String,
    deviceName: String,
    deviceId: String,
    onComplete: () -> Unit
) {
    MainScope().launch {
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    // 1. Get historical data using the correct UseCase-backed method
                    val historicalData = viewModel.getFullDeviceHistory(deviceAddress).toMutableList()

                    // 2. Add current live data if no history exists
                    if (historicalData.isEmpty()) {
                        val currentDevice = viewModel.devices.value.find { it.address == deviceAddress }
                        currentDevice?.sensorData?.let { liveSensorData ->
                            historicalData.add(
                                HistoricalDataEntry(
                                    timestamp = System.currentTimeMillis(),
                                    sensorData = liveSensorData
                                )
                            )
                        }
                    }

                    if (historicalData.isEmpty()) return@use

                    // 3. Build CSV Header based on SensorData type
                    val headerBuilder = StringBuilder()
                    headerBuilder.append("Timestamp,Device Name,Device Address,Node ID,")

                    val firstEntry = historicalData.first().sensorData
                    when (firstEntry) {
                        is SensorData.SHT40Data -> headerBuilder.append("Temperature (°C),Humidity (%)")
                        is SensorData.SoilSensorData -> headerBuilder.append("Nitrogen (mg/kg),Phosphorus (mg/kg),Potassium (mg/kg),Moisture (%),Temperature (°C),EC (mS/cm),pH,Salinity (mg/L)")
                        is SensorData.AmmoniaSensorData -> headerBuilder.append("Ammonia (ppm),Raw Data")
                        is SensorData.LIS2DHData -> headerBuilder.append("X-Axis (m/s²),Y-Axis (m/s²),Z-Axis (m/s²)")
                        is SensorData.LuxSensorData -> headerBuilder.append("Light Intensity (LUX)")
                        is SensorData.SDTData -> headerBuilder.append("Speed (m/s),Distance (m)")
                        is SensorData.TempLoggerData -> headerBuilder.append("Log Temp (°C),Log Humidity (%)")
                        is SensorData.DataLoggerData -> headerBuilder.append("Packet ID,Total Packets,Accel Points,Hex Data")
                        null -> TODO()
                    }
                    headerBuilder.append("\n")
                    outputStream.write(headerBuilder.toString().toByteArray())

                    // 4. Write Data Rows
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

                    historicalData.forEachIndexed { index, entry ->
                        val dataBuilder = StringBuilder()
                        dataBuilder.append("${dateFormat.format(Date(entry.timestamp))},$deviceName,$deviceAddress,$deviceId,")

                        when (val data = entry.sensorData) {
                            is SensorData.SHT40Data -> dataBuilder.append("${data.temperature},${data.humidity}")
                            is SensorData.SoilSensorData -> dataBuilder.append("${data.nitrogen},${data.phosphorus},${data.potassium},${data.moisture},${data.temperature},${data.ec},${data.pH},${data.salinity}")
                            is SensorData.AmmoniaSensorData -> dataBuilder.append("${data.ammonia},${data.rawData}")
                            is SensorData.LIS2DHData -> dataBuilder.append("${data.x},${data.y},${data.z}")
                            is SensorData.LuxSensorData -> dataBuilder.append("${data.lux}")
                            is SensorData.SDTData -> dataBuilder.append("${data.speed},${data.distance}")
                            is SensorData.TempLoggerData -> dataBuilder.append("${data.temperature},${data.humidity}")
                            is SensorData.DataLoggerData -> dataBuilder.append("${data.lastPacketId},${data.currentPacketId},${data.payloadAccel.size},\"${data.rawData}\"")
                            null -> TODO()
                        }
                        dataBuilder.append("\n")
                        outputStream.write(dataBuilder.toString().toByteArray())

                        // Periodic flush to prevent memory bloat
                        if (index % 100 == 0) outputStream.flush()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            }
        }
    }
}