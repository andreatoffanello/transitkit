package com.transitkit.app.ui.planner

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Prefill payload for `transitkit://planner?from=&to=&when=HH:MM` deep links.
 *
 * `from` / `to` are free-form labels (the consumer resolves them via geocoding
 * or treats them as already-resolved location names). `whenStr` is `"HH:MM"` in
 * the operator-local timezone — the consumer parses it with the operator TZ to
 * avoid the bug where 14:30 in NYC accidentally became 20:30 because the device
 * is in CET.
 */
data class PendingPlannerPrefill(
    val from: String? = null,
    val to: String? = null,
    val whenStr: String? = null,
)

object PendingPlannerPrefillStore {
    private val _pending = MutableStateFlow<PendingPlannerPrefill?>(null)
    val pending: StateFlow<PendingPlannerPrefill?> = _pending

    fun set(prefill: PendingPlannerPrefill) {
        _pending.value = prefill
    }

    fun consume(): PendingPlannerPrefill? {
        val v = _pending.value
        _pending.value = null
        return v
    }
}
