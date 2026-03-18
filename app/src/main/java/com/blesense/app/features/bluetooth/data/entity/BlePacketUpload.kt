package com.blesense.app.features.bluetooth.data.entity

import com.google.gson.annotations.SerializedName

data class BlePacketUpload(

    @SerializedName("deviceId")
    val deviceId: String,

    @SerializedName("deviceAddress")
    val deviceAddress: String,

    @SerializedName("rssi")
    val rssi: Int,

    @SerializedName("rawAdvertisement")
    val rawAdvertisement: String,

    @SerializedName("parsedType")
    val parsedType: String,

    @SerializedName("parsedData")
    val parsedData: Map<String, Any>?,

    @SerializedName("timestamp")
    val timestamp: Long
)