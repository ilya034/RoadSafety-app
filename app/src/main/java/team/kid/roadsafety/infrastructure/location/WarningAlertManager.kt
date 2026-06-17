package team.kid.roadsafety.infrastructure.location

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import team.kid.roadsafety.R
import team.kid.roadsafety.domain.aggregates.map.MapAreaColor
import team.kid.roadsafety.domain.aggregates.tracking.WarningAlert
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

@Singleton
class WarningAlertManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val clock: Clock = Clock.systemUTC()
    private val stateMachine = WarningAlertStateMachine(cooldown = 2.minutes)

    fun handleRisk(risk: MapAreaColor, offline: Boolean) {
        val alert = stateMachine.handleRisk(risk, clock.instant(), offline) ?: return

        WarningAlertEvents.show(alert)
        showNotification(alert)
    }

    private fun showNotification(alert: WarningAlert) {
        createChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification = NotificationCompat.Builder(context, ChannelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(alert.title)
            .setContentText(alert.message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NotificationId, notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            ChannelId,
            "Safety warnings",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Important road safety warnings"
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    private companion object {
        const val ChannelId = "safety_warnings"
        const val NotificationId = 22345
    }
}
