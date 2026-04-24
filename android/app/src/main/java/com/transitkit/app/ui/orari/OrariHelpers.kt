package com.transitkit.app.ui.orari

import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons

internal fun transitTypeIcon(types: List<Int>): Int {
    return when (types.firstOrNull() ?: 3) {
        0 -> LucideIcons.Train
        1 -> LucideIcons.Train
        2 -> LucideIcons.Train
        4 -> LucideIcons.Ship
        else -> LucideIcons.BusFront
    }
}

internal fun transitTypeLabel(type: Int, context: android.content.Context): String = when (type) {
    0 -> context.getString(R.string.transit_type_tram)
    1 -> context.getString(R.string.transit_type_metropolitana)
    2 -> context.getString(R.string.transit_type_treno)
    4 -> context.getString(R.string.transit_type_traghetto)
    else -> context.getString(R.string.transit_type_bus)
}
