package com.transitkit.app.ui.orari

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.model.ScheduleRoute
import com.transitkit.app.ui.components.LiveIndicator

// ---------------------------------------------------------------------------
// Header with color gradient + stop count pill
// ---------------------------------------------------------------------------

@Composable
internal fun LineDetailHeader(
    route: ScheduleRoute,
    stopCount: Int,
    liveCount: Int,
    onBack: () -> Unit,
) {
    val accentColor = TransitTheme.colors.accent
    val lineColor = remember(route.color, accentColor) {
        runCatching { Color(android.graphics.Color.parseColor("#${route.color}")) }
            .getOrDefault(accentColor)
    }
    val lineTextColor = remember(route.textColor) {
        if (route.textColor.isNotBlank())
            runCatching { Color(android.graphics.Color.parseColor("#${route.textColor}")) }
                .getOrDefault(Color.White)
        else Color.White
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        lineColor,
                        lineColor.copy(alpha = 0.72f),
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 4.dp, vertical = 8.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.semantics { contentDescription = "btn_back" },
        ) {
            Icon(
                painter = painterResource(LucideIcons.ChevronLeft),
                contentDescription = stringResource(R.string.cd_indietro),
                tint = Color.White,
            )
        }

        Column(
            modifier = Modifier.padding(start = 16.dp, top = 52.dp, end = 16.dp, bottom = 16.dp),
        ) {
            // Route badge
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = route.name.take(5),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = lineTextColor,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = route.longName.ifBlank { route.name },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val transitTypeLabel = transitTypeDisplayName(route.transitType)
            val displayedName = route.longName.ifBlank { route.name }
            if (!displayedName.contains(transitTypeLabel, ignoreCase = true)) {
                Text(
                    text = transitTypeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.72f),
                )
            }
            if (stopCount > 0) {
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.stop_count_fermate, stopCount),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                    if (liveCount > 0) {
                        Box(
                            modifier = Modifier
                                .background(TransitTheme.colors.realtimeGreen.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                LiveIndicator(size = 6.dp, animated = true)
                                Text(
                                    text = stringResource(R.string.live_count, liveCount),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TransitTheme.colors.realtimeGreen,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
