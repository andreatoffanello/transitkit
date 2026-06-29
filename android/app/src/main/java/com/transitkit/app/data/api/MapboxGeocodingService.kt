package com.transitkit.app.data.api

import android.util.Log
import com.transitkit.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

enum class GeocodeResultKind { Stop, Place }

/**
 * Risultato unificato di forward/reverse geocode. [stopGtfsId] è popolato solo
 * per kind == Stop. [locality] è la stringa contestuale (città/regione/paese)
 * per i risultati Place — es. "Boone, North Carolina". Parità con Movete GeocodeResult.
 */
data class GeocodeResult(
    val id: String,
    val kind: GeocodeResultKind,
    val name: String,
    val lat: Double,
    val lon: Double,
    val stopGtfsId: String? = null,
    val locality: String? = null,
)

/**
 * Geocoding via Mapbox Search API v6.
 * - [reverseGeocode]: coordinate → nome leggibile (già esistente).
 * - [forwardGeocode]: testo → lista di luoghi con bias geografico.
 */
object MapboxGeocodingService {

    private const val TAG = "MapboxGeocoding"

    /** Reverse geocode coordinates → human-readable place name, or null on failure. */
    suspend fun reverseGeocode(lat: Double, lon: Double, language: String = "en"): String? {
        return withContext(Dispatchers.IO) {
            val token = BuildConfig.MAPBOX_ACCESS_TOKEN
            if (token.isBlank()) return@withContext null
            val url = URL(
                "https://api.mapbox.com/search/geocode/v6/reverse" +
                    "?longitude=$lon" +
                    "&latitude=$lat" +
                    "&limit=1" +
                    "&language=$language" +
                    "&access_token=$token"
            )
            try {
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    if (conn.responseCode !in 200..299) {
                        Log.w(TAG, "HTTP ${conn.responseCode}")
                        return@withContext null
                    }
                    val raw = conn.inputStream.bufferedReader().use { it.readText() }
                    parseName(raw)
                } finally {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                Log.w(TAG, "reverseGeocode error: ${e.message}")
                null
            }
        }
    }

    /**
     * Forward geocode: testo libero → lista di [GeocodeResult] di tipo Place.
     * [biasNear] (lat, lon) migliora la rilevanza geografica: viene passato
     * come `proximity=lon,lat` a Mapbox (v6). [country] (es. "US", "IT") limita
     * i risultati alla nazione dell'operatore. [types] restringe i feature type
     * ai più utili per il picker. Errori → emptyList, no crash.
     */
    suspend fun forwardGeocode(
        query: String,
        biasNear: Pair<Double, Double>? = null,
        language: String = "en",
        country: String? = null,
        // Mapbox Geocoding v6 feature types ("poi" non esiste qui — sta nella Search Box API;
        // includerlo causa HTTP 422). Tipi validi e utili per il picker indirizzi/luoghi.
        types: String = "address,street,place,locality,neighborhood",
    ): List<GeocodeResult> {
        return withContext(Dispatchers.IO) {
            val token = BuildConfig.MAPBOX_ACCESS_TOKEN
            if (token.isBlank()) return@withContext emptyList()
            val trimmed = query.trim()
            if (trimmed.isEmpty()) return@withContext emptyList()

            val q = URLEncoder.encode(trimmed, "UTF-8")
            val proxParam = if (biasNear != null) "&proximity=${biasNear.second},${biasNear.first}" else ""
            val countryParam = if (!country.isNullOrBlank()) "&country=${country.lowercase()}" else ""
            val url = URL(
                "https://api.mapbox.com/search/geocode/v6/forward" +
                    "?q=$q" +
                    "&limit=5" +
                    "&language=$language" +
                    "&types=$types" +
                    proxParam +
                    countryParam +
                    "&access_token=$token"
            )
            try {
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    if (conn.responseCode !in 200..299) {
                        Log.w(TAG, "forwardGeocode HTTP ${conn.responseCode}")
                        return@withContext emptyList()
                    }
                    val raw = conn.inputStream.bufferedReader().use { it.readText() }
                    parseForwardResults(raw)
                } finally {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                Log.w(TAG, "forwardGeocode error: ${e.message}")
                emptyList()
            }
        }
    }

    private fun parseName(raw: String): String? {
        return try {
            val root = JSONObject(raw)
            val features = root.optJSONArray("features") ?: return null
            if (features.length() == 0) return null
            val props = features.getJSONObject(0).optJSONObject("properties") ?: return null
            // Prefer `name` (short label), fall back to full_address / place_formatted.
            props.optString("name").takeIf { it.isNotBlank() }
                ?: props.optString("name_preferred").takeIf { it.isNotBlank() }
                ?: props.optString("full_address").takeIf { it.isNotBlank() }
                ?: props.optString("place_formatted").takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.w(TAG, "parse error: ${e.message}")
            null
        }
    }

    private fun parseForwardResults(raw: String): List<GeocodeResult> {
        return try {
            val root = JSONObject(raw)
            val features: JSONArray = root.optJSONArray("features") ?: return emptyList()
            buildList {
                for (i in 0 until features.length()) {
                    val feature = features.optJSONObject(i) ?: continue
                    val props = feature.optJSONObject("properties") ?: continue
                    val coords = feature.optJSONObject("geometry")
                        ?.optJSONArray("coordinates") ?: continue
                    if (coords.length() < 2) continue
                    val lon = coords.optDouble(0, Double.NaN)
                    val lat = coords.optDouble(1, Double.NaN)
                    if (lon.isNaN() || lat.isNaN()) continue
                    val name = props.optString("name").takeIf { it.isNotBlank() }
                        ?: props.optString("name_preferred").takeIf { it.isNotBlank() }
                        ?: props.optString("full_address").takeIf { it.isNotBlank() }
                        ?: continue
                    val id = props.optString("mapbox_id").takeIf { it.isNotBlank() }
                        ?: "$lat,$lon"
                    // locality: prefer place_formatted (human-readable "City, State"),
                    // then build from context.place + context.region, then full_address.
                    val locality: String? = run {
                        val pf = props.optString("place_formatted").takeIf { it.isNotBlank() }
                        if (pf != null) return@run pf
                        val ctx = props.optJSONObject("context")
                        if (ctx != null) {
                            val place = ctx.optJSONObject("place")?.optString("name").takeIf { !it.isNullOrBlank() }
                            val region = ctx.optJSONObject("region")?.optString("name").takeIf { !it.isNullOrBlank() }
                            val country = ctx.optJSONObject("country")?.optString("name").takeIf { !it.isNullOrBlank() }
                            listOfNotNull(place, region ?: country).joinToString(", ").takeIf { it.isNotBlank() }
                        } else null
                    } ?: props.optString("full_address").takeIf { it.isNotBlank() }
                    add(GeocodeResult(id = id, kind = GeocodeResultKind.Place, name = name, lat = lat, lon = lon, locality = locality))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "parseForwardResults error: ${e.message}")
            emptyList()
        }
    }
}
