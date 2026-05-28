package team.kid.roadsafety.data.repository

import team.kid.roadsafety.data.dto.AuthResponseDto
import team.kid.roadsafety.data.dto.LoginRequestDto
import team.kid.roadsafety.data.dto.RegisterRequestDto
import team.kid.roadsafety.data.remote.RoadSafetyApi
import team.kid.roadsafety.data.dto.UserResponseDto
import team.kid.roadsafety.domain.aggregates.session.AuthTokens
import team.kid.roadsafety.domain.aggregates.user.AuthRepository
import team.kid.roadsafety.infrastructure.TokenManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: RoadSafetyApi,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun login(login: String, password: String): Result<AuthResponseDto> {
        return try {
            val response = api.login(LoginRequestDto(login, password))
            if (response.isSuccessful) {
                val body = response.body()!!
                val tokens = AuthTokens(body.accessToken, body.refreshToken)
                tokenManager.saveTokens(tokens)
                Result.success(body)
            } else {
                Result.failure(Exception("Login failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(
        login: String,
        password: String,
        firstName: String?,
        lastName: String?,
        birthDate: LocalDate?
    ): Result<AuthResponseDto> {
        return try {
            val request = RegisterRequestDto(
                login = login,
                password = password,
                firstName = firstName,
                lastName = lastName,
                birthDate = birthDate?.format(DateTimeFormatter.ISO_LOCAL_DATE)
            )
            val response = api.register(request)
            if (response.isSuccessful) {
                val body = response.body()!!
                val tokens = AuthTokens(body.accessToken, body.refreshToken)
                tokenManager.saveTokens(tokens)
                Result.success(body)
            } else {
                Result.failure(Exception("Registration failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): Result<UserResponseDto> {
        return try {
            val response = api.getCurrentUser()
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get current user: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        tokenManager.clearTokens()
        return Result.success(Unit)
    }

    override suspend fun getTokens(): AuthTokens? {
        return tokenManager.getTokens()
    }

    override suspend fun saveTokens(tokens: AuthTokens) {
        tokenManager.saveTokens(tokens)
    }
}
