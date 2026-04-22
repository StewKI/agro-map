package com.example.agro_map.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.agro_map.AgroMapApplication
import com.example.agro_map.MainActivity
import com.example.agro_map.R
import com.example.agro_map.data.model.Report
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val notifiedReportIds = mutableSetOf<String>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createNotification("Tracking your location"))

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30_000L)
            .setMinUpdateIntervalMillis(15_000L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    serviceScope.launch {
                        onNewLocation(location)
                    }
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper()
            )
        } catch (_: SecurityException) {}

        return START_STICKY
    }

    private suspend fun onNewLocation(location: Location) {
        val container = (application as AgroMapApplication).container
        val uid = container.authRepository.currentUser?.uid ?: return

        // Update user location in Firestore
        try {
            container.userRepository.updateLocation(uid, location.latitude, location.longitude)
        } catch (_: Exception) {}

        // Check for nearby reports
        try {
            val reports = container.reportRepository.getAllReports()
            reports.forEach { report ->
                if (report.id !in notifiedReportIds && report.authorId != uid) {
                    val distance = FloatArray(1)
                    Location.distanceBetween(
                        location.latitude, location.longitude,
                        report.latitude, report.longitude, distance
                    )
                    if (distance[0] <= 500f) { // 500 meters
                        notifiedReportIds.add(report.id)
                        showProximityNotification(report)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun showProximityNotification(report: Report) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, report.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "proximity_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Nearby: ${report.title}")
            .setContentText("${report.category} reported by ${report.authorUsername}")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(report.id.hashCode(), notification)
    }

    private fun createNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, "proximity_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("AgroMap")
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
    }
}
