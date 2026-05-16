package team.kid.roadsafety.infrastructure

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import team.kid.roadsafety.domain.aggregates.session.AuthTokens
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext context: Context
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

    fun clearTokens() {
        prefs.edit().clear().apply()
    }
}
