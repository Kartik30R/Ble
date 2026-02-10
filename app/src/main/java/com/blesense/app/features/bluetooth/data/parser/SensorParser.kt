package com.blesense.app.features.bluetooth.data.parser

import android.bluetooth.le.ScanResult
import com.blesense.app.features.bluetooth.domain.model.SensorData

interface SensorParser {
    fun parse(result: ScanResult): SensorData?
}
