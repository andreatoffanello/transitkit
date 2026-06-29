package com.transitkit.app.data.store

import android.content.Context
import androidx.core.content.edit
import com.transitkit.app.config.OperatorConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/** Indirizzo salvato (casa/lavoro) per scorciatoie nel planner. Parità iOS SavedPlace. */
data class SavedPlace(val name: String, val lat: Double, val lon: Double)

/** Chiavi canoniche per gli indirizzi salvati. Parità iOS SavedPlaceKey. */
object SavedPlaceKeys {
    const val HOME = "home"
    const val WORK = "work"
}

/**
 * Persistenza degli indirizzi casa/lavoro per le scorciatoie planner.
 * Usa SharedPreferences con chiave operator-scoped. Parità iOS SavedPlacesStore.
 */
@Singleton
class SavedPlacesStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val operatorConfig: OperatorConfig,
) {
    private val prefsName = "saved_places"
    private val prefsKey get() = "${operatorConfig.id}_saved_places"

    private val prefs by lazy {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }

    private val _savedPlaces = MutableStateFlow<Map<String, SavedPlace>>(loadPlaces())
    val savedPlaces: StateFlow<Map<String, SavedPlace>> = _savedPlaces

    fun savedPlace(placeKey: String): SavedPlace? = _savedPlaces.value[placeKey]

    fun setPlace(placeKey: String, name: String, lat: Double, lon: Double) {
        val updated = _savedPlaces.value.toMutableMap().apply {
            put(placeKey, SavedPlace(name, lat, lon))
        }.toMap()
        _savedPlaces.value = updated
        persistPlaces(updated)
    }

    fun removePlace(placeKey: String) {
        val updated = _savedPlaces.value.toMutableMap().apply { remove(placeKey) }.toMap()
        _savedPlaces.value = updated
        persistPlaces(updated)
    }

    private fun loadPlaces(): Map<String, SavedPlace> {
        val raw = prefs.getString(prefsKey, null) ?: return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            buildMap {
                obj.keys().forEach { k ->
                    val o = obj.getJSONObject(k)
                    put(k, SavedPlace(o.getString("name"), o.getDouble("lat"), o.getDouble("lon")))
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun persistPlaces(places: Map<String, SavedPlace>) {
        val json = JSONObject()
        places.forEach { (k, p) ->
            json.put(k, JSONObject().put("name", p.name).put("lat", p.lat).put("lon", p.lon))
        }
        prefs.edit { putString(prefsKey, json.toString()) }
    }
}
