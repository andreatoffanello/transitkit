package com.transitkit.app.ui.orari

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.model.ResolvedStop

// ---------------------------------------------------------------------------
// Stop row with vertical timeline
// ---------------------------------------------------------------------------

@Composable
internal fun StopTimelineRow(
    stop: ResolvedStop,
    index: Int,
    total: Int,
    accentColor: Color,
    currentRouteName: String,
    routeColorByName: Map<String, String> = emptyMap(),
    onClick: () -> Unit,
) {
    val colors = TransitTheme.colors
    val isFirst = index == 0
    val isLast = index == total - 1

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Movete parity: minimum row height + vertical breathing so the
            // timeline reads as a comfortable scroll, not a packed list. Without
            // these, rows collapse to the badges' intrinsic height and the
            // sequence feels claustrophobic.
            // height(IntrinsicSize.Min) gives the Row a BOUNDED height equal to its
            // tallest child (the text column) so the timeline connector's
            // fillMaxHeight() below actually resolves. Inside a LazyColumn the
            // incoming max-height is Infinity and fillMaxHeight is a no-op against
            // it → the connector collapsed into disconnected stubs (the "spezzata"
            // timeline). heightIn(min) keeps the comfortable floor.
            .height(IntrinsicSize.Min)
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "line_stop_${stop.id}" }
            // NO vertical padding on the Row: it would inset the timeline rail
            // inside each row, leaving a 12dp dead gap between adjacent rows'
            // connector halves → the rail rendered DASHED. The 6dp breathing is
            // moved onto the text Column instead, so the rail (timeline Box) fills
            // the FULL row height edge-to-edge and consecutive segments join into
            // one continuous line.
            .padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Timeline column: full-height line + circle overlay
        Box(
            modifier = Modifier
                .width(56.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            // Top half line — hidden for first stop
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight(0.5f)
                        .background(accentColor)
                        .align(Alignment.TopCenter),
                )
            }
            // Bottom half line — hidden for last stop
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight(0.5f)
                        .background(accentColor)
                        .align(Alignment.BottomCenter),
                )
            }
            // Node circle
            Box(
                modifier = Modifier
                    .size(if (isFirst || isLast) 14.dp else 10.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFirst || isLast) accentColor else accentColor.copy(alpha = 0.6f)
                    ),
            )
        }

        // Stop name + coincidence badges. The 6dp vertical breathing lives HERE
        // (not on the Row) so the timeline rail stays continuous — see Row above.
        Column(modifier = Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(
                text = stop.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isFirst || isLast) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isFirst || isLast) colors.textPrimary else colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val otherLines = stop.routeNames.filter { it.isNotBlank() && it != currentRouteName }
            if (otherLines.isNotEmpty()) {
                val maxVisible = 4
                val visible = otherLines.take(maxVisible)
                val overflow = otherLines.size - visible.size
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    visible.forEach { lineName ->
                        val gtfsHex = routeColorByName[lineName]?.takeIf { it.isNotBlank() }
                        val isColored = gtfsHex != null
                        val badgeColor = gtfsHex?.let {
                            com.transitkit.app.ui.components.parseHexColor(it, fallback = accentColor.copy(alpha = 0.15f))
                        } ?: accentColor.copy(alpha = 0.15f)
                        val fgColor = if (isColored) routeBadgeContrast(badgeColor) else accentColor
                        Box(
                            modifier = Modifier
                                .height(16.dp)
                                .background(badgeColor, RoundedCornerShape(3.dp))
                                .padding(horizontal = 5.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = lineName,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = fgColor,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    if (overflow > 0) {
                        Box(
                            modifier = Modifier
                                .height(16.dp)
                                .background(colors.bgSecondary, RoundedCornerShape(3.dp))
                                .padding(horizontal = 5.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "+$overflow",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = colors.textTertiary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        Icon(
            painter = painterResource(LucideIcons.ChevronRight),
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(16.dp),
        )
    }
}
