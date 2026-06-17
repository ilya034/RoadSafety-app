package team.kid.roadsafety.infrastructure

import android.util.Log
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import team.kid.roadsafety.BuildConfig
import team.kid.roadsafety.data.dto.RefreshTokensRequestDto
import team.kid.roadsafety.data.dto.RefreshTokensResponseDto
import team.kid.roadsafety.domain.aggregates.session.AuthTokens
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val json: Json
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        synchronized(this) {
            val tokens = tokenManager.getTokens()
            val refreshToken = tokens?.refreshToken ?: return null

            val requestHeader = response.request.header("Authorization")
            if (requestHeader != null && tokens.accessToken.isNotBlank()) {
                val currentBearerToken = "Bearer ${tokens.accessToken}"
                if (requestHeader != currentBearerToken) {
                    return response.request.newBuilder()
                        .header("Authorization", currentBearerToken)
                        .build()
                }
            }

            Log.d("TokenAuthenticator", "Access token expired. Attempting to refresh tokens...")

            val client = OkHttpClient()
            val requestDto = RefreshTokensRequestDto(refreshToken)
            val requestBody = json.encodeToString(RefreshTokensRequestDto.serializer(), requestDto)
                .toRequestBody("application/json".toMediaType())

            val refreshRequest = Request.Builder()
                .url("${BuildConfig.BASE_URL}auth/refresh")
                .post(requestBody)
                .build()

            try {
                client.newCall(refreshRequest).execute().use { refreshResponse ->
                    if (refreshResponse.isSuccessful) {
                        val responseBody = refreshResponse.body?.string()
                        if (responseBody != null) {
                            val refreshDto = json.decodeFromString(
                                RefreshTokensResponseDto.serializer(),
                                responseBody
                            )
                            val newTokens = AuthTokens(
                                accessToken = refreshDto.accessToken,
                                refreshToken = refreshDto.refreshToken
                            )
                            tokenManager.saveTokens(newTokens)
                            Log.d("TokenAuthenticator", "Tokens successfully refreshed.")

                            return response.request.newBuilder()
                                .header("Authorization", "Bearer ${newTokens.accessToken}")
                                .build()
                        }
                    } else {
                        Log.e("TokenAuthenticator", "Refresh token request failed with status: ${refreshResponse.code}")
                        tokenManager.clearTokens()
                    }
                }
            } catch (e: Exception) {
                Log.e("TokenAuthenticator", "Exception during token refresh", e)
            }

            return null
        }
    }
}
