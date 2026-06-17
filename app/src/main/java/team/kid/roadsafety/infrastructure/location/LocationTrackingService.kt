package team.kid.roadsafety.infrastructure.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import team.kid.roadsafety.R
import team.kid.roadsafety.domain.aggregates.map.MapAreaColor
import team.kid.roadsafety.domain.aggregates.tracking.TrackingRepository
import javax.inject.Inject

@AndroidEntryPoint
class LocationTrackingService : Service() {

    @Inject
    lateinit var trackingRepository: TrackingRepository

    @Inject
    lateinit var localRiskEvaluator: LocalRiskEvaluator

    @Inject
    lateinit var warningAlertManager: WarningAlertManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    serviceScope.launch {
                        try {
                            val result = trackingRepository.submitChildLocation(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                accuracyMeters = if (location.hasAccuracy()) location.accuracy.toDouble() else null
                            )
                            if (result.isFailure) {
                                Log.e("LocationTrackingService", "Failed to submit location API side", result.exceptionOrNull())
                                val localRisk = localRiskEvaluator.evaluate(location.latitude, location.longitude)
                                warningAlertManager.handleRisk(localRisk, offline = true)
                            } else {
                                val serverRisk = MapAreaColor.fromString(result.getOrThrow().currentRisk.name)
                                warningAlertManager.handleRisk(serverRisk, offline = false)
                            }
                        } catch (e: Exception) {
                            Log.e("LocationTrackingService", "Failed to submit location", e)
                            val localRisk = localRiskEvaluator.evaluate(location.latitude, location.longitude)
                            warningAlertManager.handleRisk(localRisk, offline = true)
                        }
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!hasForegroundLocationPermission(this)) {
            Log.w("LocationTrackingService", "Stopping service: foreground location permission is not granted")
            stopSelf()
            return START_NOT_STICKY
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
        requestLocationUpdates()
        return START_STICKY
    }

    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 15000L)
            .setMinUpdateIntervalMillis(15000L)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (unlikely: SecurityException) {
            Log.e("LocationTrackingService", "Lost location permission", unlikely)
            fusedLocationClient.removeLocationUpdates(locationCallback)
            stopSelf()
        }
    }

    private fun createNotification(): Notification {
        val channelId = "location_tracking_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("RoadSafety")
            .setContentText("Отслеживание местоположения активно")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        private const val NOTIFICATION_ID = 12345
    }
}
