package team.kid.roadsafety.domain.aggregates.user

import team.kid.roadsafety.data.dto.AuthResponseDto
import team.kid.roadsafety.data.dto.UserResponseDto
import team.kid.roadsafety.domain.aggregates.session.AuthTokens

interface AuthRepository {
    suspend fun login(login: String, password: String): Result<AuthResponseDto>
    
    suspend fun register(
        login: String,
        password: String
    ): Result<AuthResponseDto>

    suspend fun getCurrentUser(): Result<UserResponseDto>

    suspend fun logout(): Result<Unit>
    
    suspend fun getTokens(): AuthTokens?
    
    suspend fun saveTokens(tokens: AuthTokens)

    suspend fun getCachedUser(): UserResponseDto?
}
