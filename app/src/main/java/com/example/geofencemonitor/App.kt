package com.example.geofencemonitor

import android.app.Application
import android.app.NotificationManager

import android.app.NotificationChannel

import android.os.Build




class App : Application() {

    val CHANNEL_ID = "backgroundServiceChannel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Background Service Channel",
                NotificationManager.IMPORTANCE_MIN
            )
            val manager = getSystemService(
                NotificationManager::class.java
            )
            manager.createNotificationChannel(serviceChannel)
        }
    }
}