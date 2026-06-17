package team.kid.roadsafety.data.repository

import team.kid.roadsafety.data.dto.AuthResponseDto
import team.kid.roadsafety.data.dto.LoginRequestDto
import team.kid.roadsafety.data.dto.RegisterRequestDto
import team.kid.roadsafety.data.dto.UserResponseDto
import team.kid.roadsafety.data.remote.RoadSafetyApi
import team.kid.roadsafety.domain.aggregates.session.AuthTokens
import team.kid.roadsafety.domain.aggregates.user.AuthRepository
import team.kid.roadsafety.infrastructure.TokenManager
import team.kid.roadsafety.infrastructure.parseErrorMessage
import team.kid.roadsafety.infrastructure.push.PushTokenSynchronizer
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: RoadSafetyApi,
    private val tokenManager: TokenManager,
    private val pushTokenSynchronizer: PushTokenSynchronizer
) : AuthRepository {

    override suspend fun login(login: String, password: String): Result<AuthResponseDto> {
        return try {
            val response = api.login(LoginRequestDto(login, password))
            if (response.isSuccessful) {
                val body = response.body()!!
                val tokens = AuthTokens(body.accessToken, body.refreshToken)
                tokenManager.saveTokens(tokens)
                pushTokenSynchronizer.syncCurrentTokenIfAuthenticated()
                Result.success(body)
            } else {
                Result.failure(Exception(response.parseErrorMessage("Login failed")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(
        login: String,
        password: String
    ): Result<AuthResponseDto> {
        return try {
            val request = RegisterRequestDto(
                login = login,
                password = password
            )
            val response = api.register(request)
            if (response.isSuccessful) {
                val body = response.body()!!
                val tokens = AuthTokens(body.accessToken, body.refreshToken)
                tokenManager.saveTokens(tokens)
                pushTokenSynchronizer.syncCurrentTokenIfAuthenticated()
                Result.success(body)
            } else {
                Result.failure(Exception(response.parseErrorMessage("Registration failed")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): Result<UserResponseDto> {
        return try {
            val response = api.getCurrentUser()
            if (response.isSuccessful) {
                val user = response.body()!!
                tokenManager.saveUser(user)
                Result.success(user)
            } else {
                Result.failure(Exception("Failed to get current user: ${response.code()}"))
            }
        } catch (e: Exception) {
            val cachedUser = tokenManager.getUser()
            if (cachedUser != null) {
                Result.success(cachedUser)
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun logout(): Result<Unit> {
        pushTokenSynchronizer.deleteCurrentTokenBestEffort()
        tokenManager.clearTokens()
        return Result.success(Unit)
    }

    override suspend fun getTokens(): AuthTokens? {
        return tokenManager.getTokens()
    }

    override suspend fun saveTokens(tokens: AuthTokens) {
        tokenManager.saveTokens(tokens)
    }

    override suspend fun getCachedUser(): UserResponseDto? {
        return tokenManager.getUser()
    }
}
