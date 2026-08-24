package com.transitkit.app.config

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Lingue selezionabili dall'utente in Impostazioni.
 *
 * Aggiungerne una richiede solo un caso qui più il relativo `values-<tag>`:
 * picker, persistenza e `locales_config.xml` ne discendono.
 */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    SPANISH("es"),
    ITALIAN("it");

    /**
     * Endonimo: il nome della lingua *nella lingua stessa*, non tradotto —
     * chi cerca la propria lingua in una lista la riconosce come la scrive lui.
     * `SYSTEM` fa eccezione: è una parola dell'interfaccia, non una lingua.
     */
    fun label(context: Context): String = when (this) {
        SYSTEM -> context.getString(com.transitkit.app.R.string.settings_language_system)
        ENGLISH -> "English"
        SPANISH -> "Español"
        ITALIAN -> "Italiano"
    }

    companion object {
        fun matching(tag: String?): AppLanguage? {
            val language = tag?.substringBefore('-')?.lowercase() ?: return null
            return entries.firstOrNull { it.tag == language }
        }
    }
}

/**
 * Lingua dell'interfaccia scelta dall'utente.
 *
 * Da Android 13 la fonte di verità è `LocaleManager.applicationLocales`: è la
 * stessa che il sistema scrive quando l'utente sceglie da Impostazioni → App →
 * Lingua, quindi le due strade restano allineate e l'activity la ricrea da sé.
 * Sotto la 13 quell'API non esiste: la scelta va in `SharedPreferences` (lettura
 * sincrona, serve in `attachBaseContext` prima che parta qualunque coroutine) e
 * la si applica avvolgendo il context dell'activity.
 */
object AppLocaleManager {

    private const val PREFS = "transitkit_locale"
    private const val KEY_LANGUAGE = "app_language"

    fun current(context: Context): AppLanguage {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            if (locales.isEmpty) return AppLanguage.SYSTEM
            return AppLanguage.matching(locales[0]?.toLanguageTag()) ?: AppLanguage.SYSTEM
        }
        return AppLanguage.matching(prefs(context).getString(KEY_LANGUAGE, null)) ?: AppLanguage.SYSTEM
    }

    fun select(activity: Activity, language: AppLanguage) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.getSystemService(LocaleManager::class.java).applicationLocales =
                language.tag?.let { LocaleList.forLanguageTags(it) } ?: LocaleList.getEmptyLocaleList()
            return
        }
        prefs(activity).edit().apply {
            if (language.tag == null) remove(KEY_LANGUAGE) else putString(KEY_LANGUAGE, language.tag)
        }.apply()
        activity.recreate()
    }

    /** Da chiamare in `Activity.attachBaseContext`. No-op da Android 13 in su. */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val tag = prefs(base).getString(KEY_LANGUAGE, null) ?: return base
        val locale = Locale.forLanguageTag(tag)
        // Anche il default della JVM, non solo le risorse: date, orari e nomi
        // dei giorni sono formattati con `Locale.getDefault()`.
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
        return base.createConfigurationContext(config)
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
