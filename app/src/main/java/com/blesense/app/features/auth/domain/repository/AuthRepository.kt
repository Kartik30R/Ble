package com.blesense.app.features.auth.domain.repository

import com.blesense.app.features.auth.domain.model.User

interface AuthRepository {

    // --- Auth state ---
    fun getCurrentUser(): User?
    fun isUserAuthenticated(): Boolean

    // --- Login flows ---
    suspend fun login(email: String, password: String): User
    suspend fun loginWithGoogle(idToken: String): User
    suspend fun loginAsGuest(): User

    // --- Registration ---
    suspend fun register(email: String, password: String): User

    // --- Password recovery ---
    suspend fun sendPasswordResetEmail(email: String)

    // --- Account lifecycle ---
    suspend fun deleteAccount(password: String? = null)
    fun logout()
}
