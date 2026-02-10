package com.blesense.app.features.auth.data.datasource

import com.blesense.app.features.auth.data.entity.AuthUserDto
import com.blesense.app.features.auth.data.entity.toDto
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class FirebaseAuthDataSource(
    private val auth: FirebaseAuth
) {

    fun getCurrentUser(): AuthUserDto? =
        auth.currentUser?.toDto()

    suspend fun login(email: String, password: String): AuthUserDto =
        auth.signInWithEmailAndPassword(email, password)
            .await()
            .user
            ?.toDto()
            ?: throw Exception("Login failed")

    suspend fun register(email: String, password: String): AuthUserDto =
        auth.createUserWithEmailAndPassword(email, password)
            .await()
            .user
            ?.toDto()
            ?: throw Exception("Registration failed")

    suspend fun loginAsGuest(): AuthUserDto =
        auth.signInAnonymously()
            .await()
            .user
            ?.toDto()
            ?: throw Exception("Guest login failed")

    suspend fun loginWithGoogle(idToken: String): AuthUserDto {
        val credential =
            com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)

        return auth.signInWithCredential(credential)
            .await()
            .user
            ?.toDto()
            ?: throw Exception("Google sign-in failed")
    }

    suspend fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    suspend fun deleteCurrentUser(password: String?) {
        val user = auth.currentUser ?: throw Exception("No user logged in")

        password?.let {
            val email = user.email ?: throw Exception("Email not available")
            val credential =
                com.google.firebase.auth.EmailAuthProvider.getCredential(email, it)
            user.reauthenticate(credential).await()
        }

        user.delete().await()
    }

    fun signOut() {
        auth.signOut()
    }
}
