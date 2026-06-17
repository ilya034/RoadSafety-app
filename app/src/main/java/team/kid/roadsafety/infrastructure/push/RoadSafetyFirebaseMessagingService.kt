package team.kid.roadsafety.infrastructure.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import team.kid.roadsafety.R
import javax.inject.Inject

@AndroidEntryPoint
class RoadSafetyFirebaseMessagingService : FirebaseMessagingService() {
    @Inject lateinit var pushTokenSynchronizer: PushTokenSynchronizer

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        serviceScope.launch {
            pushTokenSynchronizer.syncTokenIfAuthenticated(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "Road safety alert"
        val body = message.notification?.body ?: message.data["body"] ?: return
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        createChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification = NotificationCompat.Builder(this, ChannelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(messageNotificationId(), notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            ChannelId,
            "Road safety alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Important alerts from family tracking"
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    private fun messageNotificationId(): Int {
        return (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
    }

    private companion object {
        const val ChannelId = "road_safety_alerts"
    }
}
