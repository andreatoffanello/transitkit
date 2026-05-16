package com.transitkit.app.data.push

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.transitkit.app.config.OperatorConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Owns the push lifecycle on Android:
 *   - Subscribes/unsubscribes to FCM topics that mirror the operator + line favorites
 *   - Exposes the current FCM registration token for the "register as test device" flow
 *
 * Topics follow the convention pinned by transitkit-console:
 *   - `{operatorId}_all`            → broadcast to every user of the operator
 *   - `{operatorId}_line_{routeId}` → users who favorited that route
 *
 * Subscriptions live inside FCM itself — the server never sees who's listening
 * to which line.
 *
 * Firebase is automatically initialized by the google-services plugin if
 * `google-services.json` is in `app/`. We probe via `FirebaseMessaging` and
 * gracefully no-op when the plugin wasn't applied (operators not yet onboarded
 * to push).
 */
@Singleton
class PushNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: OperatorConfig,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val operatorId: String = topicSafe(config.id)

    private val allTopic: String get() = "${operatorId}_all"
    private fun lineTopic(routeId: String) = "${operatorId}_line_${topicSafe(routeId)}"

    /** Probe Firebase availability. Returns false if google-services.json
     *  wasn't applied (FirebaseApp not initialized). */
    val isFirebaseConfigured: Boolean
        get() = try {
            FirebaseMessaging.getInstance()
            true
        } catch (e: Throwable) {
            _lastError.value = "Firebase not configured: ${e.message}"
            false
        }

    /** Called by the FirebaseMessagingService when FCM hands us a new token. */
    fun onTokenRefreshed(token: String) {
        _fcmToken.value = token
    }

    /** Returns the current FCM token, fetching one if we don't have it cached. */
    suspend fun ensureToken(): String? {
        if (!isFirebaseConfigured) return null
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            _fcmToken.value = token
            token
        } catch (e: Exception) {
            _lastError.value = "Token fetch failed: ${e.message}"
            null
        }
    }

    // -------------------------------------------------------------------------
    // Topic subscriptions
    // -------------------------------------------------------------------------

    /** Subscribe to the operator-wide topic. Called when the user enables
     *  notifications. Resubscribes are idempotent. */
    suspend fun subscribeToOperatorAll() {
        if (!isFirebaseConfigured) return
        try {
            FirebaseMessaging.getInstance().subscribeToTopic(allTopic).await()
            Log.i(TAG, "Subscribed to $allTopic")
        } catch (e: Exception) {
            _lastError.value = "Subscribe $allTopic failed: ${e.message}"
        }
    }

    private suspend fun unsubscribeFromOperatorAll() {
        if (!isFirebaseConfigured) return
        try {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(allTopic).await()
        } catch (_: Exception) { /* best-effort */ }
    }

    suspend fun subscribeRoute(routeId: String) {
        if (!isFirebaseConfigured) return
        val topic = lineTopic(routeId)
        try {
            FirebaseMessaging.getInstance().subscribeToTopic(topic).await()
            Log.i(TAG, "Subscribed to $topic")
        } catch (e: Exception) {
            _lastError.value = "Subscribe $topic failed: ${e.message}"
        }
    }

    suspend fun unsubscribeRoute(routeId: String) {
        if (!isFirebaseConfigured) return
        val topic = lineTopic(routeId)
        try {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).await()
        } catch (_: Exception) { /* best-effort */ }
    }

    /** Called from the Settings toggle when the user enables push. */
    suspend fun enableAndSubscribeAll() {
        if (!isFirebaseConfigured) return
        ensureToken()
        subscribeToOperatorAll()
    }

    /** Called from the Settings toggle when the user disables push. */
    suspend fun disableAndUnsubscribeAll(knownFavoriteRouteIds: List<String>) {
        if (!isFirebaseConfigured) return
        unsubscribeFromOperatorAll()
        knownFavoriteRouteIds.forEach { unsubscribeRoute(it) }
        try {
            FirebaseMessaging.getInstance().deleteToken().await()
        } catch (_: Exception) { /* best-effort */ }
        _fcmToken.value = null
    }

    companion object {
        private const val TAG = "PushManager"

        /** Sanitize the operator/route id for FCM (allowed: a-z, A-Z, 0-9, -_.~%). */
        fun topicSafe(s: String): String = buildString {
            for (ch in s) {
                if (ch.isLetterOrDigit() || ch in "-_.~%") append(ch) else append('_')
            }
        }
    }
}
