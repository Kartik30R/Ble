package com.blesense.app.core.network

import com.blesense.app.app.Routes
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.Interceptor
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.tasks.Tasks

object RetrofitProvider {

    private const val BASE_URL = "http://${Routes.ip}/"

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            try {
                // Blocks until token is retrieved, safe because OkHttp runs this on a background thread
                val task = user.getIdToken(false)
                val result = Tasks.await(task)
                val token = result.token
                if (token != null) {
                    val request = original.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                    return@Interceptor chain.proceed(request)
                }
            } catch (e: Exception) {
                // Fallback to original request if token fetch fails
            }
        }
        chain.proceed(original)
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
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