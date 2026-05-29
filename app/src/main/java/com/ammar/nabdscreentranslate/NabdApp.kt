package com.ammar.nabdscreentranslate

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class NabdApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val channel = NotificationChannel(
            CAPTURE_CHANNEL_ID,
            getString(R.string.capture_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Screen capture service notification"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CAPTURE_CHANNEL_ID = "screen_capture_channel"
    }
}
