package team.kid.roadsafety.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import team.kid.roadsafety.data.local.SessionManager
import team.kid.roadsafety.data.remote.dto.*

class RoadSafetyApi(
    private val sessionManager: SessionManager
) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
        install(Logging) {
            level = LogLevel.ALL
        }
        install(Auth) {
            bearer {
                loadTokens {
                    val accessToken = sessionManager.accessToken.first()
                    val refreshToken = sessionManager.refreshToken.first()
                    if (accessToken != null && refreshToken != null) {
                        BearerTokens(accessToken, refreshToken)
                    } else null
                }
                refreshTokens {
                    val refreshToken = sessionManager.refreshToken.first() ?: return@refreshTokens null
                    try {
                        val response = client.post("http://localhost:5103/api/auth/refresh") {
                            setBody(RefreshTokensRequest(refreshToken))
                            contentType(ContentType.Application.Json)
                            markAsRefreshTokenRequest()
                        }.body<RefreshTokensResponse>()
                        
                        val userId = sessionManager.userId.first() ?: ""
                        sessionManager.saveSession(userId, response.accessToken, response.refreshToken)
                        
                        BearerTokens(response.accessToken, response.refreshToken)
                    } catch (e: Exception) {
                        sessionManager.clearSession()
                        null
                    }
                }
            }
        }
        defaultRequest {
            url("http://localhost:5103/api/")
        }
    }

    suspend fun register(request: RegisterRequest): AuthResponse =
        client.post("auth/register") {
            setBody(request)
            contentType(ContentType.Application.Json)
        }.body()

    suspend fun login(request: LoginRequest): AuthResponse =
        client.post("auth/login") {
            setBody(request)
            contentType(ContentType.Application.Json)
        }.body()

    suspend fun createFamily(request: CreateFamilyRequest): CreateFamilyResponse =
        client.post("families") {
            setBody(request)
            contentType(ContentType.Application.Json)
        }.body()

    suspend fun joinFamily(request: JoinFamilyByInviteCodeRequest): JoinFamilyByInviteCodeResponse =
        client.post("families/join-by-invite") {
            setBody(request)
            contentType(ContentType.Application.Json)
        }.body()

    suspend fun getFamilyMembers(familyId: String): GetFamilyMembersResponse =
        client.get("families/$familyId/members").body()

    suspend fun createInviteCode(request: CreateInviteCodeRequest): CreateInviteCodeResponse =
        client.post("families/invite-code") {
            setBody(request)
            contentType(ContentType.Application.Json)
        }.body()

    suspend fun logout(refreshToken: String) {
        client.post("auth/logout") {
            setBody(LogOutRequest(refreshToken))
            contentType(ContentType.Application.Json)
        }
    }
}
