package com.transitkit.app.data.api

import android.util.Log
import com.transitkit.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Reverse geocoding via Mapbox Geocoding API v6. Used by the planner location
 * picker map to resolve the place name (street / POI / address) under the
 * fixed center pin, mirroring Movete's RemoteGeocodingProvider.reverseGeocode.
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
}
