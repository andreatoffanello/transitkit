package com.transitkit.app.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "favorites")

private val KEY_FAVORITE_STOP_IDS = stringPreferencesKey("favorite_stop_ids_ordered")
private val KEY_FAVORITE_ROUTE_IDS = stringPreferencesKey("favorite_route_ids_ordered")

@Singleton
class FavoritesStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    // -------------------------------------------------------------------------
    // Stop favorites
    // -------------------------------------------------------------------------

    val favoriteStopIds: Flow<List<String>> = context.dataStore.data
        .map { prefs ->
            val raw = prefs[KEY_FAVORITE_STOP_IDS] ?: ""
            if (raw.isBlank()) emptyList() else raw.split(",").filter { it.isNotEmpty() }
        }

    suspend fun addFavorite(stopId: String) {
        context.dataStore.edit { prefs ->
            val current = (prefs[KEY_FAVORITE_STOP_IDS] ?: "")
                .split(",").filter { it.isNotEmpty() }
            if (!current.contains(stopId)) {
                prefs[KEY_FAVORITE_STOP_IDS] = (listOf(stopId) + current).joinToString(",")
            }
        }
    }

    suspend fun removeFavorite(stopId: String) {
        context.dataStore.edit { prefs ->
            val current = (prefs[KEY_FAVORITE_STOP_IDS] ?: "")
                .split(",").filter { it.isNotEmpty() }
            prefs[KEY_FAVORITE_STOP_IDS] = current.filter { it != stopId }.joinToString(",")
        }
    }

    suspend fun isFavorite(stopId: String): Boolean =
        favoriteStopIds.first().contains(stopId)

    // -------------------------------------------------------------------------
    // Route favorites — iOS parity. Used by LineDetail toolbar star button
    // per gestire alert per linea (subscribe ad alert solo per linee a cui
    // l'utente è interessato).
    // -------------------------------------------------------------------------

    val favoriteRouteIds: Flow<List<String>> = context.dataStore.data
        .map { prefs ->
            val raw = prefs[KEY_FAVORITE_ROUTE_IDS] ?: ""
            if (raw.isBlank()) emptyList() else raw.split(",").filter { it.isNotEmpty() }
        }

    suspend fun addFavoriteRoute(routeId: String) {
        context.dataStore.edit { prefs ->
            val current = (prefs[KEY_FAVORITE_ROUTE_IDS] ?: "")
                .split(",").filter { it.isNotEmpty() }
            if (!current.contains(routeId)) {
                prefs[KEY_FAVORITE_ROUTE_IDS] = (listOf(routeId) + current).joinToString(",")
            }
        }
    }

    suspend fun removeFavoriteRoute(routeId: String) {
        context.dataStore.edit { prefs ->
            val current = (prefs[KEY_FAVORITE_ROUTE_IDS] ?: "")
                .split(",").filter { it.isNotEmpty() }
            prefs[KEY_FAVORITE_ROUTE_IDS] = current.filter { it != routeId }.joinToString(",")
        }
    }
}
