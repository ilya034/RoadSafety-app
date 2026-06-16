package team.kid.roadsafety.infrastructure

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import team.kid.roadsafety.data.dto.UserResponseDto
import team.kid.roadsafety.domain.aggregates.session.AuthTokens
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext context: Context,
    private val json: Json
) {
    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun saveTokens(tokens: AuthTokens) {
        prefs.edit()
            .putString("access_token", tokens.accessToken)
            .putString("refresh_token", tokens.refreshToken)
            .apply()
    }

    fun getTokens(): AuthTokens? {
        val accessToken = prefs.getString("access_token", null)
        val refreshToken = prefs.getString("refresh_token", null)
        return if (accessToken != null && refreshToken != null) {
            AuthTokens(accessToken, refreshToken)
        } else {
            null
        }
    }

    fun saveUser(user: UserResponseDto) {
        try {
            val userJson = json.encodeToString(UserResponseDto.serializer(), user)
            prefs.edit().putString("cached_user", userJson).apply()
        } catch (e: Exception) {
            // Ignore serialization error
        }
    }

    fun getUser(): UserResponseDto? {
        val userJson = prefs.getString("cached_user", null) ?: return null
        return try {
            json.decodeFromString(UserResponseDto.serializer(), userJson)
        } catch (e: Exception) {
            null
        }
    }

    fun clearTokens() {
        prefs.edit().clear().apply()
    }
}

