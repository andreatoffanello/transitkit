package com.transitkit.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.OperatorConfig
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.ui.components.LiveIndicator

// ---------------------------------------------------------------------------
// OperatorReferenceSection — credits block: headline + OperatorReferenceCard.
// Mirrors the Movete "Chi muove la città" pattern.
// ---------------------------------------------------------------------------

@Composable
internal fun OperatorReferenceSection(
    config: OperatorConfig,
    liveVehicleCount: Int,
    routesCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TransitTheme.colors
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(start = 2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.home_operators_section_title),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.home_operators_attribution, config.name),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        OperatorReferenceCard(
            config = config,
            liveVehicleCount = liveVehicleCount,
            routesCount = routesCount,
            onClick = onClick,
        )
    }
}

@Composable
internal fun OperatorReferenceCard(
    config: OperatorConfig,
    liveVehicleCount: Int,
    routesCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TransitTheme.colors
    val context = LocalContext.current
    // iOS parity (item #15): logo reale operatore invece di initials gradient.
    // Asset `operator_logo` viene caricato runtime via resources getIdentifier
    // (white-label: cambia operatore = swap dell'asset).
    val operatorLogoRes = remember(context) {
        context.resources.getIdentifier("operator_logo", "drawable", context.packageName)
            .takeIf { it != 0 }
    }
    val initials = remember(config.name) {
        val words = config.name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        when {
            words.size >= 2 -> "${words[0].first()}${words[1].first()}".uppercase()
            words.size == 1 -> words[0].take(2).uppercase()
            else -> "?"
        }
    }
    // iOS parity: subtitle "X live now · Y routes" (no address/region).
    val routesLabel = if (routesCount > 0) {
        stringResource(R.string.home_operator_card_routes, routesCount)
    } else null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.bgSecondary)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (operatorLogoRes != null) {
            Image(
                painter = painterResource(operatorLogoRes),
                contentDescription = config.name,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Fit,
            )
        } else {
            // Fallback: initials su gradient — solo se l'asset manca.
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(listOf(colors.accent, colors.primary))
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = config.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (liveVehicleCount > 0) {
                    LiveIndicator(size = 6.dp)
                    Text(
                        text = stringResource(R.string.home_operator_card_live, liveVehicleCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.realtimeGreen,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (routesLabel != null) {
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textTertiary,
                        )
                        Text(
                            text = routesLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.home_operator_card_no_live),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                    )
                    if (routesLabel != null) {
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textTertiary,
                        )
                        Text(
                            text = routesLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        Icon(
            painter = painterResource(LucideIcons.ChevronRight),
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(18.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// HomeServiziLink — tile linking to the full "Servizi" screen
// ---------------------------------------------------------------------------

@Composable
internal fun HomeServiziLink(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TransitTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.bgSecondary)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(LucideIcons.Grid2x2Plus),
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.tab_servizi),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.home_servizi_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
            )
        }
        Icon(
            painter = painterResource(LucideIcons.ChevronRight),
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(16.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// HomeFooterDisclaimer — data attribution footnote
// ---------------------------------------------------------------------------

@Composable
internal fun HomeFooterDisclaimer(
    operatorName: String,
    modifier: Modifier = Modifier,
) {
    if (operatorName.isEmpty()) return
    Text(
        text = stringResource(R.string.home_footer_disclaimer, operatorName),
        style = MaterialTheme.typography.labelSmall,
        color = TransitTheme.colors.textTertiary,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}
