package team.kid.roadsafety.domain.aggregates.user

import team.kid.roadsafety.domain.aggregates.session.AuthTokens
import java.time.LocalDate

interface AuthRepository {
    suspend fun login(login: String, password: String): Result<AuthTokens>
    
    suspend fun register(
        login: String,
        password: String,
        firstName: String?,
        lastName: String?,
        birthDate: LocalDate?
    ): Result<AuthTokens>

    suspend fun logout(): Result<Unit>
    
    suspend fun getTokens(): AuthTokens?
    
    suspend fun saveTokens(tokens: AuthTokens)
}
