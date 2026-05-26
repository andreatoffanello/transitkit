package com.transitkit.app.ui.orari

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Prefill payload for `transitkit://search?q=&scope=` deep links. The Orari
 * screen consumes this when present and applies the query + tab scope.
 */
data class PendingSearchPrefill(
    val query: String,
    val scope: OrariTab? = null,
)

object PendingSearchPrefillStore {
    private val _pending = MutableStateFlow<PendingSearchPrefill?>(null)
    val pending: StateFlow<PendingSearchPrefill?> = _pending

    fun set(prefill: PendingSearchPrefill) {
        _pending.value = prefill
    }

    fun consume(): PendingSearchPrefill? {
        val v = _pending.value
        _pending.value = null
        return v
    }
}
