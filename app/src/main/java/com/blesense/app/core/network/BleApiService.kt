package com.blesense.app.core.network

import com.blesense.app.features.bluetooth.data.entity.BlePacketUpload
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface BleApiService {

    @POST("api/packets/batch")
    suspend fun uploadBatch(
        @Body packets: List<BlePacketUpload>
    ): Response<Unit>
}