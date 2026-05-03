package com.vecturai.tools.admin.model

import kotlinx.serialization.Serializable

@Serializable
data class ManagerResponse(
    val id: String,
    val email: String,
    val fullName: String,
    val createdAt: String,
)

@Serializable
data class SignupRequest(
    val email: String,
    val password: String,
    val fullName: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class AuthResponse(
    val token: String,
    val manager: ManagerResponse,
)

@Serializable
data class DbAdminLoginRequest(
    val username: String,
    val password: String,
)

@Serializable
data class DbAdminAuthResponse(
    val token: String,
)
