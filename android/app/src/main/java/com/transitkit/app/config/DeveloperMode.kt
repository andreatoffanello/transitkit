package com.transitkit.app.config

import android.content.Context

/**
 * Modalità sviluppatore: si sblocca con 7 tap sulla riga della versione in
 * Impostazioni e apre una schermata di diagnostica (build, feature flag, token
 * FCM, ricarica forzata degli orari).
 *
 * Vive nello stesso `transitkit_prefs` di onboarding e update checker: una
 * lettura sincrona, nessun DataStore, nessun quarto file di preferenze.
 * `PushNotificationManager` la legge da un contesto non-Compose, quindi
 * l'API è statica e prende il `Context`, come [AppLocaleManager].
 *
 * Parità iOS: `Services/DeveloperMode.swift`.
 */
object DeveloperMode {

    private const val PREFS = "transitkit_prefs"
    private const val KEY_ENABLED = "developer_mode"

    /** Numero di tap sulla riga versione che sbloccano la modalità. */
    const val UNLOCK_TAPS = 7

    /** Oltre questa distanza tra due tap il conteggio riparte da capo. */
    const val TAP_WINDOW_MS = 2_000L

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, value).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
