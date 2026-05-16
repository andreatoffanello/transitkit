package com.transitkit.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TransitKitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        registerDefaultNotificationChannel()
    }

    /**
     * Register the `default` channel referenced by the FCM payload built
     * server-side (`android.notification.channelId = "default"`). Without
     * this, FCM falls back to `fcm_fallback_notification_channel` and logs
     * a warning. Importance HIGH so push banners surface above the status
     * bar (transit alerts are time-sensitive).
     */
    private fun registerDefaultNotificationChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(DEFAULT_CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            DEFAULT_CHANNEL_ID,
            getString(R.string.notification_channel_default_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.notification_channel_default_description)
            enableLights(true)
            enableVibration(true)
        }
        nm.createNotificationChannel(channel)
    }

    companion object {
        private const val DEFAULT_CHANNEL_ID = "default"
    }
}
