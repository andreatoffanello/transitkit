package com.transitkit.app.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.searchHistoryDataStore: DataStore<Preferences> by preferencesDataStore(name = "search_history")

private val KEY_RECENT_STOP_IDS = stringPreferencesKey("recent_stop_ids")
private val KEY_RECENT_ROUTE_IDS = stringPreferencesKey("recent_route_ids")

@Singleton
class SearchHistoryStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val recentStopIds: Flow<List<String>> = context.searchHistoryDataStore.data
        .map { prefs ->
            val raw = prefs[KEY_RECENT_STOP_IDS] ?: ""
            if (raw.isBlank()) emptyList() else raw.split(",").filter { it.isNotBlank() }
        }

    val recentRouteIds: Flow<List<String>> = context.searchHistoryDataStore.data
        .map { prefs ->
            val raw = prefs[KEY_RECENT_ROUTE_IDS] ?: ""
            if (raw.isBlank()) emptyList() else raw.split(",").filter { it.isNotBlank() }
        }

    suspend fun recordStop(stopId: String) {
        context.searchHistoryDataStore.edit { prefs ->
            val current = (prefs[KEY_RECENT_STOP_IDS] ?: "").split(",").filter { it.isNotBlank() }
            prefs[KEY_RECENT_STOP_IDS] = dedupeAndTrim(current, stopId, 8).joinToString(",")
        }
    }

    suspend fun recordRoute(routeId: String) {
        context.searchHistoryDataStore.edit { prefs ->
            val current = (prefs[KEY_RECENT_ROUTE_IDS] ?: "").split(",").filter { it.isNotBlank() }
            prefs[KEY_RECENT_ROUTE_IDS] = dedupeAndTrim(current, routeId, 5).joinToString(",")
        }
    }

    private fun dedupeAndTrim(current: List<String>, id: String, max: Int): List<String> {
        val mutable = current.toMutableList()
        mutable.remove(id)
        mutable.add(0, id)
        return mutable.take(max)
    }
}
