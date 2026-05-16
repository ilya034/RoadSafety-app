package team.kid.roadsafety.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val login: String,
    val password: String
)

@Serializable
data class RegisterRequestDto(
    val login: String,
    val password: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val birthDate: String? = null
)

@Serializable
data class AuthResponseDto(
    val userId: String,
    val accessToken: String,
    val accessTokenExpiresAt: String,
    val refreshToken: String,
    val refreshTokenExpiresAt: String
)

@Serializable
data class ErrorResponseDto(
    val code: String,
    val message: String
)
