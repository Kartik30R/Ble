package com.blesense.app.core.network

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            return chain.proceed(request)
        }

        return try {
            // Synchronously wait for token in the interceptor
            val token = runBlocking {
                user.getIdToken(true).await().token
            }

            val authenticatedRequest = request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()

            chain.proceed(authenticatedRequest)
        } catch (e: Exception) {
            // Fallback to unauthenticated request if token fetch fails
            chain.proceed(request)
        }
    }
}
