package com.transitkit.app.data.stats

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.transitkit.app.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Statistiche d'uso ANONIME con interruttore in Impostazioni (contratto v1 di
 * `collect.andreatoffanello.com`, repo `stats`). Parità con `UsageStats.swift`.
 *
 *  - Attive SOLO per gli operatori in [ALLOWLIST] (oggi: AppalRider). Per gli
 *    altri niente client, niente interruttore, niente invio.
 *  - Mai invio in build DEBUG: il JSON conforme va nel log (tag `UsageStats`).
 *  - Spento = coda e install id cancellati; riacceso = install id NUOVO.
 *
 * Eventi (allowlist del server): app_open {source, cold}, session_end
 * {duration}, screen {screen, from}, settings {language, color_scheme}.
 *
 * Tutto lo stato vive su un executor a thread singolo: nessuna corsa fra
 * record, flush e opt-out. `generation` si ricontrolla dopo la rete.
 */
object UsageStats {
    /** Allowlist a build-time. Aggiungere un operatore = decisione di prodotto
     *  + registrarlo in stats/collect/src/apps.ts + etichette store. */
    private val ALLOWLIST = setOf("appalcart")

    private const val PREFS = "usage_stats"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_SETTINGS_DAY = "settings_last_day"
    private const val ENDPOINT = "https://collect.andreatoffanello.com/v1/events"
    private const val TAG = "UsageStats"

    private lateinit var app: Application
    private var operatorId: String? = null
    private var timeZone: TimeZone = TimeZone.getDefault()
    private val io = Executors.newSingleThreadExecutor { r -> Thread(r, "usage-stats").apply { isDaemon = true } }
    private val http by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // Stato (toccato solo da `io`, tranne i volatili letti dalla UI).
    @Volatile private var enabled = true
    private var generation = 0
    private var rows = mutableListOf<Row>()
    private var pendingId: String? = null
    private var pendingRows = listOf<Row>()
    private var install: String? = null
    private var loaded = false

    // Sessione (thread principale).
    private var sessionStart = 0L
    private var hasOpened = false
    private var lastScreen = "none"
    private var initialScreen = "home"

    private data class Row(val e: String, val d: String, val h: Int, val p: Map<String, Any>, var n: Int)

    fun init(application: Application, operatorId: String, timeZoneId: String) {
        app = application
        this.operatorId = operatorId
        timeZone = TimeZone.getTimeZone(timeZoneId)
        if (!isAvailable) {
            // Nessun client: se un residuo esiste (build precedente), via.
            file().delete(); return
        }
        enabled = prefs().getBoolean(KEY_ENABLED, true)
        io.execute { load() }
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = sessionStarted()
            override fun onStop(owner: LifecycleOwner) = sessionEnded()
        })
    }

    /** L'operatore corrente partecipa alle statistiche (mostra l'interruttore). */
    val isAvailable: Boolean get() = operatorId in ALLOWLIST

    /** Default ON: chiave assente = mai toccata dall'utente. */
    fun isEnabled(): Boolean = isAvailable && prefs().getBoolean(KEY_ENABLED, true)

    fun setEnabled(on: Boolean) {
        if (!isAvailable) return
        prefs().edit().putBoolean(KEY_ENABLED, on).apply()
        enabled = on
        io.execute {
            load()
            if (!on) {
                generation++
                rows = mutableListOf(); pendingId = null; pendingRows = emptyList(); install = null
                file().delete()
            }
        }
        if (on) sessionStarted() else sessionStart = 0L
    }

    // ── Ciclo di vita ───────────────────────────────────────────────────────

    private fun sessionStarted() {
        if (!isEnabled() || sessionStart != 0L) return
        sessionStart = System.currentTimeMillis()
        val cold = !hasOpened
        hasOpened = true
        record("app_open", mapOf("source" to "launcher", "cold" to cold))
        recordSettingsOnceADay()
        if (lastScreen == "none") screen(initialScreen)
        io.execute { if (pendingId != null || rows.size > 50) flush() }
    }

    private fun sessionEnded() {
        lastScreen = "none"
        if (!isEnabled() || sessionStart == 0L) return
        val seconds = (System.currentTimeMillis() - sessionStart) / 1000
        sessionStart = 0L
        val bucket = when {
            seconds < 30 -> "lt30s"
            seconds < 120 -> "30s_2m"
            seconds < 600 -> "2m_10m"
            else -> "gt10m"
        }
        record("session_end", mapOf("duration" to bucket))
        io.execute { flush() }
    }

    /** Nomi schermata snake_case stabili, identici su iOS. */
    fun screen(name: String) {
        initialScreen = name
        if (!isEnabled() || name == lastScreen) return
        val from = lastScreen
        lastScreen = name
        record("screen", mapOf("screen" to name, "from" to from))
    }

    /** Route di navigazione -> nome schermata; le altre non generano eventi. */
    fun screenForRoute(route: String?) {
        val name = when (route) {
            "home" -> "home"
            "orari" -> "stops"
            "linee_graph" -> "lines"
            "mappa" -> "map"
            "alerts" -> "alerts"
            "settings_from_home" -> "settings"
            else -> return
        }
        screen(name)
    }

    private fun recordSettingsOnceADay() {
        val today = dayAndHour(System.currentTimeMillis()).first
        val p = prefs()
        if (p.getString(KEY_SETTINGS_DAY, null) == today) return
        p.edit().putString(KEY_SETTINGS_DAY, today).apply()
        // Il motore non ha un selettore di tema: l'aspetto segue sempre il sistema.
        record("settings", mapOf("language" to currentLanguage(), "color_scheme" to "system"))
    }

    /** Lingua effettiva dell'interfaccia (en | es | it). */
    private fun currentLanguage(): String {
        val lang = app.resources.configuration.locales[0].language.lowercase(Locale.ROOT)
        return if (lang in setOf("en", "es", "it")) lang else "en"
    }

    // ── Coda ────────────────────────────────────────────────────────────────

    private fun dayAndHour(ms: Long): Pair<String, Int> {
        val cal = Calendar.getInstance(timeZone).apply { timeInMillis = ms }
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = this@UsageStats.timeZone }
        return fmt.format(cal.time) to cal.get(Calendar.HOUR_OF_DAY)
    }

    private fun record(name: String, props: Map<String, Any>) {
        if (operatorId !in ALLOWLIST) return
        val (d, h) = dayAndHour(System.currentTimeMillis())
        val gen = generation
        io.execute {
            load()
            if (!enabled || gen != generation) return@execute
            val existing = rows.firstOrNull { it.e == name && it.d == d && it.h == h && it.p == props }
            if (existing != null) existing.n++ else rows.add(Row(name, d, h, props, 1))
            if (rows.size > 500) rows = rows.takeLast(500).toMutableList()
            if (BuildConfig.DEBUG) Log.d(TAG, "evento $name $props")
            save()
        }
    }

    // ── Invio ───────────────────────────────────────────────────────────────

    /** Sull'executor. */
    private fun flush() {
        val op = operatorId ?: return
        load()
        val gen = generation
        repeat(5) {
            if (!enabled || gen != generation) return
            if (pendingId == null) {
                if (rows.isEmpty()) return
                pendingRows = rows.take(400)
                rows = rows.drop(pendingRows.size).toMutableList()
                pendingId = UUID.randomUUID().toString()
                save()
            }
            if (install == null) { install = UUID.randomUUID().toString(); save() }
            val body = JSONObject().apply {
                put("v", 1)
                put("batch", pendingId)
                put("install", install)
                put("platform", "android")
                put("app", op)
                put("version", BuildConfig.VERSION_NAME)
                put("os", Build.VERSION.SDK_INT.toString())
                put("lang", currentLanguage())
                put("events", JSONArray().apply { pendingRows.forEach { put(it.toJson()) } })
            }.toString()

            if (BuildConfig.DEBUG) {
                // Mai invio da DEBUG: si logga il JSON conforme e basta.
                Log.d(TAG, "[DEBUG, non inviato] $body")
                return
            }
            val status = try {
                val req = Request.Builder().url(ENDPOINT)
                    .post(body.toRequestBody("application/json".toMediaType())).build()
                http.newCall(req).execute().use { it.code }
            } catch (_: Exception) {
                return // rete: il batch resta con lo stesso id per il retry
            }
            if (!enabled || gen != generation) return
            when (status) {
                in 200..299, 400, 413 -> { pendingId = null; pendingRows = emptyList(); save() }
                else -> return // 429/5xx: ritenta più tardi
            }
        }
    }

    // ── Persistenza ─────────────────────────────────────────────────────────

    private fun prefs() = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun file() = File(app.filesDir, "usage-stats.json")

    private fun Row.toJson() = JSONObject().apply {
        put("e", e); put("d", d); put("h", h); put("n", n)
        put("p", JSONObject(p))
    }

    private fun load() {
        if (loaded) return
        loaded = true
        enabled = prefs().getBoolean(KEY_ENABLED, true)
        if (!enabled) { file().delete(); return }
        runCatching {
            val f = file()
            if (!f.exists()) return
            val j = JSONObject(f.readText())
            install = j.optString("install").takeIf { it.isNotEmpty() }
            pendingId = j.optString("pendingId").takeIf { it.isNotEmpty() }
            pendingRows = parseRows(j.optJSONArray("pendingRows"))
            rows = parseRows(j.optJSONArray("rows")).toMutableList()
        }
    }

    private fun parseRows(a: JSONArray?): List<Row> {
        if (a == null) return emptyList()
        return (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            val p = o.getJSONObject("p")
            Row(o.getString("e"), o.getString("d"), o.getInt("h"),
                p.keys().asSequence().associateWith { p.get(it) }, o.getInt("n"))
        }
    }

    private fun save() {
        if (!enabled) return
        runCatching {
            val j = JSONObject().apply {
                put("install", install ?: "")
                put("pendingId", pendingId ?: "")
                put("pendingRows", JSONArray().apply { pendingRows.forEach { put(it.toJson()) } })
                put("rows", JSONArray().apply { rows.forEach { put(it.toJson()) } })
            }
            file().writeText(j.toString())
        }
    }
}
