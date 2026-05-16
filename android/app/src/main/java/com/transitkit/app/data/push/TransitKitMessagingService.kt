package com.transitkit.app.data.push

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Forwarder between Firebase and the rest of the app:
 *   - New FCM tokens → `PushNotificationManager.onTokenRefreshed`
 *   - Notification taps with `data.deep_link` → open transitkit:// URLs so
 *     `MainActivity`'s deep-link intent filter handles routing
 */
@AndroidEntryPoint
class TransitKitMessagingService : FirebaseMessagingService() {

    @Inject lateinit var pushManager: PushNotificationManager

    override fun onNewToken(token: String) {
        Log.i(TAG, "New FCM token (${token.take(8)}…)")
        pushManager.onTokenRefreshed(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // System tray notifications (with a `notification` payload) are
        // rendered automatically by Firebase. We only need to handle the
        // tap → deep-link bridge when the user taps the notification — but
        // that flow goes through the activity launch intent which already
        // includes our extras. Nothing to do here for v1 beyond logging.
        Log.i(TAG, "FCM message received: from=${message.from}, data=${message.data}")
    }

    companion object { private const val TAG = "TKMessaging" }
}
