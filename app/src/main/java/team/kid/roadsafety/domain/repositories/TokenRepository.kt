package team.kid.roadsafety.domain.repositories

import team.kid.roadsafety.domain.valueObjects.AuthTokens

interface TokenRepository {
    suspend fun save(tokens: AuthTokens)
    suspend fun get(): AuthTokens?
    suspend fun clear()
}