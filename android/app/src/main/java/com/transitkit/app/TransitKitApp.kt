package com.transitkit.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.mapbox.common.TelemetryUtils
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TransitKitApp : Application() {

    // Sotto Android 13 la lingua scelta va applicata anche al Context
    // dell'application, non solo a quello dell'activity: le stringhe che
    // nascono fuori dalla UI (errori del repository, canali di notifica) le
    // risolvono da qui e altrimenti uscirebbero nella lingua di sistema
    // mentre il resto dell'app è in quella scelta.
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(com.transitkit.app.config.AppLocaleManager.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()

        // Mapbox raccoglie eventi di telemetria di default (Device IDs, App
        // interactions, crash metrics verso Mapbox). Disabilitato per allineare
        // il binary al Data safety form ("Precise location only") e alla privacy
        // policy (le uniche statistiche sono le nostre, anonime e spegnibili da
        // Impostazioni, vedi UsageStats). Va eseguito PRIMA di
        // istanziare qualunque MapView.
        TelemetryUtils.setEventsCollectionState(false) { /* no-op */ }

        registerDefaultNotificationChannel()
        initUsageStats()
    }

    /** Statistiche d'uso anonime: l'id e il fuso dell'operatore si leggono da
     *  config.json (stessa fonte di OperatorConfig) prima di qualunque DI. */
    private fun initUsageStats() {
        runCatching {
            val json = org.json.JSONObject(assets.open("config.json").bufferedReader().use { it.readText() })
            com.transitkit.app.data.stats.UsageStats.init(
                this, json.getString("id"), json.optString("timezone", "UTC"),
            )
        }
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
