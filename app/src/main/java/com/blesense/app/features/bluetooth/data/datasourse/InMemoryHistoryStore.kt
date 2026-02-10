package com.blesense.app.features.bluetooth.data.datasourse

import com.blesense.app.features.bluetooth.domain.model.HistoricalDataEntry

class InMemoryHistoryStore {
    private val map = mutableMapOf<String, MutableList<HistoricalDataEntry>>()

    fun add(address: String, entry: HistoricalDataEntry) {
        val list = map.getOrPut(address) { mutableListOf() }
        list.add(entry)
        if (list.size > 1000) list.removeAt(0)
    }

    fun get(address: String): List<HistoricalDataEntry> =
        map[address]?.toList() ?: emptyList()

    fun clearAll() {
        map.clear()
    }
}
