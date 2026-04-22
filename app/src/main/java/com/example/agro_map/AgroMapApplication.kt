package com.example.agro_map

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.agro_map.data.repository.AuthRepository
import com.example.agro_map.data.repository.ReportRepository
import com.example.agro_map.data.repository.StorageRepository
import com.example.agro_map.data.repository.UserRepository

class AgroMapApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "proximity_channel",
                "Nearby Reports",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for nearby field reports"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}

class AppContainer {
    val authRepository = AuthRepository()
    val userRepository = UserRepository()
    val reportRepository = ReportRepository()
    val storageRepository = StorageRepository()
}
