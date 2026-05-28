package team.kid.roadsafety.infrastructure

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val tokens = tokenManager.getTokens()

        return if (tokens != null) {
            val authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer ${tokens.accessToken}")
                .build()
            chain.proceed(authenticatedRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}
