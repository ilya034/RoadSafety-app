package team.kid.roadsafety.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val login: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val login: String,
    val password: String
)

@Serializable
data class RefreshTokensRequest(
    val refreshToken: String
)

@Serializable
data class LogOutRequest(
    val refreshToken: String
)

@Serializable
data class AuthResponse(
    val userId: String,
    val accessToken: String,
    val accessTokenExpiresAt: String,
    val refreshToken: String,
    val refreshTokenExpiresAt: String
)

@Serializable
data class RefreshTokensResponse(
    val accessToken: String,
    val accessTokenExpiresAt: String,
    val refreshToken: String,
    val refreshTokenExpiresAt: String
)
