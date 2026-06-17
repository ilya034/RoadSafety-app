package team.kid.roadsafety.infrastructure.push

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import team.kid.roadsafety.data.dto.RegisterDeviceTokenRequestDto
import team.kid.roadsafety.data.remote.RoadSafetyApi
import team.kid.roadsafety.infrastructure.TokenManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushTokenSynchronizer @Inject constructor(
    private val api: RoadSafetyApi,
    private val tokenManager: TokenManager
) {
    suspend fun syncCurrentTokenIfAuthenticated() {
        val token = try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            Log.w("PushTokenSynchronizer", "Failed to get FCM token", e)
            tokenManager.getFcmToken()
        } ?: return

        syncTokenIfAuthenticated(token)
    }

    suspend fun syncTokenIfAuthenticated(token: String) {
        tokenManager.saveFcmToken(token)
        if (!tokenManager.hasAuthTokens()) {
            return
        }

        try {
            val response = api.registerDeviceToken(RegisterDeviceTokenRequestDto(token = token))
            if (!response.isSuccessful) {
                Log.w("PushTokenSynchronizer", "Device token registration failed: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.w("PushTokenSynchronizer", "Device token registration failed", e)
        }
    }

    suspend fun deleteCurrentTokenBestEffort() {
        val token = tokenManager.getFcmToken() ?: return
        try {
            val response = api.deleteDeviceToken(token)
            if (!response.isSuccessful) {
                Log.w("PushTokenSynchronizer", "Device token deletion failed: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.w("PushTokenSynchronizer", "Device token deletion failed", e)
        }
    }
}
