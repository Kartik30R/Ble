package com.blesense.app.features.bluetooth.domain.repository

import com.blesense.app.features.bluetooth.domain.model.HistoricalDataEntry

interface HistoryStore {

    /**
     * Add a historical entry for a device.
     * Must preserve insertion order.
     * Must enforce max size (1000) in implementation.
     */
    fun add(deviceAddress: String, entry: HistoricalDataEntry)

    /**
     * Get full history for a device.
     * Returns empty list if none exists.
     */
    fun get(deviceAddress: String): List<HistoricalDataEntry>

    /**
     * Clear history for a device.
     */
    fun clear(deviceAddress: String)
}
