package com.transitkit.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.transitkit.app.BuildConfig
import com.transitkit.app.config.OperatorConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Verifica se la versione installata richiede un aggiornamento, leggendo la
 * sezione `appUpdate.android` di [OperatorConfig].
 *
 * Due meccanismi — specchio del comportamento iOS:
 *
 *  - **Force gate** (bloccante): `versionCode < minVersionCode && force == true`.
 *    La UI mostra [ForceUpdateScreen], overlay non scavalcabile.
 *
 *  - **Soft banner** (non bloccante): `versionCode < latestVersionCode` e
 *    l'utente non ha ancora tappato "Più tardi" per questa versione.
 *    Guidato interamente dal config (nessuna dipendenza da Play In-App Updates)
 *    → funziona su build sideloaded e simulatori.
 *    Il dismissal per-versione è persistito in SharedPreferences.
 *
 * Uso: singleton [AppUpdateChecker] inizializzato in `MainActivity.onCreate`
 * tramite [check]. La UI colleziona [state] e [softState].
 */
object AppUpdateChecker {

    private const val TAG = "AppUpdateChecker"
    private const val PREFS_NAME = "transitkit_prefs"
    private const val KEY_DISMISSED_SOFT_VC = "dismissed_soft_update_vc"

    // ── Stato forza (bloccante) ────────────────────────────────────────────

    sealed interface Requirement {
        data object None : Requirement
        data class Forced(val message: String, val storeUrl: String) : Requirement
    }

    private val _state = MutableStateFlow<Requirement>(Requirement.None)
    val state: StateFlow<Requirement> = _state.asStateFlow()

    // ── Stato soft (banner non bloccante) ─────────────────────────────────

    data class SoftUpdate(val message: String, val storeUrl: String, val versionCode: Int)

    private val _softState = MutableStateFlow<SoftUpdate?>(null)
    val softState: StateFlow<SoftUpdate?> = _softState.asStateFlow()

    // ── API pubblica ───────────────────────────────────────────────────────

    /**
     * Esegue il controllo versione usando il config già caricato (via Hilt).
     * Chiamato in `MainActivity.onCreate` e su `repeatOnLifecycle(STARTED)`.
     */
    fun check(context: Context, config: OperatorConfig, language: String) {
        val appUpdate = config.appUpdate
        val android = appUpdate?.android

        if (appUpdate == null || android == null) {
            _state.value = Requirement.None
            _softState.value = null
            return
        }

        val currentVc = BuildConfig.VERSION_CODE
        val storeUrl = android.storeUrl
            ?: "https://play.google.com/store/apps/details?id=${context.packageName}"

        // 1. Gate forzato: versionCode < min + force=true.
        if (android.force && currentVc < android.minVersionCode) {
            val message = appUpdate.localizedMessage(language)
                ?: if (language == "it") "Aggiorna l'app per continuare a usarla."
                else "Update the app to keep using it."
            _state.value = Requirement.Forced(message = message, storeUrl = storeUrl)
            _softState.value = null
            return
        }
        _state.value = Requirement.None

        // 2. Soft banner: versionCode < latestVersionCode e non dismissed.
        val latestVc = android.latestVersionCode
        if (latestVc > 0 && currentVc < latestVc) {
            val dismissed = dismissedSoftUpdateVersionCode(context)
            if (dismissed < latestVc) {
                val message = appUpdate.localizedWhatsNew(language)
                    ?: if (language == "it") "È disponibile una nuova versione dell'app."
                    else "A new version of the app is available."
                _softState.value = SoftUpdate(
                    message = message,
                    storeUrl = storeUrl,
                    versionCode = latestVc,
                )
                return
            }
        }
        _softState.value = null
    }

    /** "Più tardi": nasconde il banner per questo `versionCode`. */
    fun dismissSoftUpdate(context: Context) {
        val vc = _softState.value?.versionCode ?: return
        setDismissedSoftUpdateVersionCode(context, vc)
        _softState.value = null
    }

    /** Apre la scheda dell'app sul Play Store via Intent VIEW. */
    fun openStore(context: Context, storeUrl: String) {
        val playMarket = "market://details?id=${context.packageName}"
        val candidates = listOf(playMarket, storeUrl)
        for (url in candidates) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                return
            } catch (_: Exception) {
                continue
            }
        }
        Log.w(TAG, "Impossibile aprire lo store: $storeUrl")
    }

    // ── Dismissal persistito in SharedPreferences ─────────────────────────

    private fun dismissedSoftUpdateVersionCode(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_DISMISSED_SOFT_VC, 0)

    private fun setDismissedSoftUpdateVersionCode(context: Context, vc: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_DISMISSED_SOFT_VC, vc).apply()
    }
}
