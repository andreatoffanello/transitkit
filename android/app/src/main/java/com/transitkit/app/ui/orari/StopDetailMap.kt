package com.transitkit.app.ui.orari

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
internal fun StopMarkerDetail(accentColor: Color, transitType: Int = 3) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(accentColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            // Design system: a stop on a map is a signpost, not a vehicle.
            painter = painterResource(com.transitkit.app.ui.components.stopIcon(listOf(transitType))),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
    }
}
