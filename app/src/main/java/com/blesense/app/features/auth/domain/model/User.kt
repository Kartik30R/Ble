package com.blesense.app.features.auth.domain.model
data class User(
    val id: String,
    val email: String?,
    val isAnonymous: Boolean,
    val name: String? = null,
    val profilePictureUrl: String? = null,
    val signInTime: Long = System.currentTimeMillis()
)