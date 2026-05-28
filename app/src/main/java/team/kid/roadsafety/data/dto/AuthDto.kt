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
data class UserResponseDto(
    val id: String,
    val email: String? = null,
    val phoneNumber: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val patronymic: String? = null,
    val birthDate: String? = null,
    val familyId: String? = null
)

@Serializable
data class AuthResponseDto(
    val userId: String,
    val accessToken: String,
    val accessTokenExpiresAt: String,
    val refreshToken: String,
    val refreshTokenExpiresAt: String,
    val user: UserResponseDto? = null
)

@Serializable
data class ErrorResponseDto(
    val code: String,
    val message: String
)
