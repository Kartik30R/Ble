package com.blesense.app.core.network

import com.blesense.app.app.Routes
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitProvider {

    private const val BASE_URL = "http://${Routes.ip}/"

    private val client = OkHttpClient.Builder()
        .retryOnConnectionFailure(true)
        .build()

    val api: BleApiService by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BleApiService::class.java)
    }
}