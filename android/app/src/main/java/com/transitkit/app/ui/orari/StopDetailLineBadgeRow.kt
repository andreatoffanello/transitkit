package com.transitkit.app.ui.orari

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.model.ResolvedDeparture

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LineBadgeRow(routes: List<ResolvedDeparture>, modifier: Modifier = Modifier) {
    val colors = TransitTheme.colors
    val uniqueRoutes = remember(routes) { routes.distinctBy { it.routeId } }
    if (uniqueRoutes.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val displayRoutes = if (uniqueRoutes.size <= 8 || expanded) uniqueRoutes else uniqueRoutes.take(8)
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        displayRoutes.forEach { route ->
            com.transitkit.app.ui.components.LineBadge(
                name = route.routeName.take(6),
                colorHex = route.routeColor,
                textColorHex = route.routeTextColor,
                // iOS parity: stop-detail coincidences use Medium (not Small).
                size = com.transitkit.app.ui.components.LineBadgeSize.Medium,
            )
        }
        if (uniqueRoutes.size > 8) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(colors.accent.copy(alpha = 0.15f))
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = if (expanded) stringResource(R.string.action_meno) else "+${uniqueRoutes.size - 8}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.accent,
                )
                Icon(
                    painter = painterResource(if (expanded) LucideIcons.ChevronUp else LucideIcons.ChevronDown),
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}
