package com.transitkit.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.transitkit.app.R
import androidx.compose.ui.graphics.compositeOver
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.config.surfaceOverMap
import com.transitkit.app.data.model.PlannerLocation
import com.transitkit.app.ui.planner.PlannerViewModel
import com.transitkit.app.ui.planner.WhenChipRow

// ---------------------------------------------------------------------------
// PlannerHomeBox — two-row Da/A card, parità Movete PlannerHomeBox.
// ---------------------------------------------------------------------------

@Composable
internal fun PlannerHomeBox(
    plannerViewModel: PlannerViewModel,
    onNavigateToLocationPicker: (role: String) -> Unit,
    onNavigateToAssign: (key: String) -> Unit,
    onNavigateToPlanner: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TransitTheme.colors
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(14.dp)

    val homeOrigin by plannerViewModel.homeOrigin.collectAsStateWithLifecycle()
    val homeDestination by plannerViewModel.homeDestination.collectAsStateWithLifecycle()
    val whenSel by plannerViewModel.whenSelection.collectAsStateWithLifecycle()
    val currentLocation by plannerViewModel.currentLocation.collectAsStateWithLifecycle()
    val myLocationLabel = stringResource(R.string.planner_my_location)

    // iOS parity: default origin = "My location" instead of nearest stop.
    // Auto-fill only once GPS is available; if the user has set their own
    // origin (kind != CurrentLocation), we leave it alone.
    LaunchedEffect(currentLocation) {
        val existing = plannerViewModel.homeOrigin.value
        val loc = currentLocation ?: return@LaunchedEffect
        if (existing == null || existing.kind == PlannerLocation.Kind.CurrentLocation) {
            plannerViewModel.setHomeOrigin(
                PlannerLocation(
                    kind = PlannerLocation.Kind.CurrentLocation,
                    name = myLocationLabel,
                    lat = loc.first,
                    lon = loc.second,
                )
            )
        }
    }

    val bothFilled = homeOrigin != null && homeDestination != null

    // Niente auto-navigate: l'utente lancia esplicitamente col pulsante Cerca,
    // così può impostare prima il "quando" e non ri-triggera a ogni modifica.
    fun launchPlanner() {
        val o = plannerViewModel.homeOrigin.value ?: return
        val d = homeDestination ?: return
        plannerViewModel.setOrigin(o)
        plannerViewModel.setDestination(d)
        plannerViewModel.clearHomeState()
        onNavigateToPlanner()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Stessa identica superficie card di tutta la home
                // (surfaceOverMap opaca): niente tinta accent qui — l'utente
                // vuole UN solo materiale, coerente con card/blocchi/controlli.
                .background(colors.surfaceOverMap, shape)
                .semantics { testTag = "planner_home_box" },
        ) {
            // Row Da (origin) — icon column has dot + dashed line below
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateToLocationPicker("origin")
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OrigDestIconColumn(
                    role = OrigDestRole.Origin,
                    accent = colors.accent,
                    modifier = Modifier.size(width = 28.dp, height = 56.dp),
                )
                Text(
                    text = homeOrigin?.name ?: stringResource(R.string.planner_from),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    fontWeight = if (homeOrigin != null) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (homeOrigin != null) colors.textPrimary else colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            // No divider between From and To — keeps the dashed line continuous.

            // Row A (destination) — icon column has dashed line above + ring
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateToLocationPicker("destination")
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OrigDestIconColumn(
                    role = OrigDestRole.Destination,
                    accent = colors.accent,
                    modifier = Modifier.size(width = 28.dp, height = 56.dp),
                )
                Text(
                    text = homeDestination?.name ?: stringResource(R.string.planner_to),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    fontWeight = if (homeDestination != null) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (homeDestination != null) colors.textPrimary else colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (bothFilled) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            plannerViewModel.swapHomeStops()
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            painter = painterResource(LucideIcons.ArrowUpDown),
                            contentDescription = "Swap",
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
        // ── When chip row + pulsante Cerca ──────────────────────────────────
        // In modalità "Parti alle/Arriva entro" compaiono i chip ora+data: per
        // non tagliarli, il bottone Cerca scende a tutta larghezza sulla riga
        // sotto. In "Adesso" resta inline a destra (un solo chip "Quando").
        if (whenSel.mode != 0) {
            WhenChipRow(
                selection = whenSel,
                onSelectionChange = plannerViewModel::setWhenSelection,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            )
            SearchButton(
                enabled = bothFilled,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    launchPlanner()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WhenChipRow(
                    selection = whenSel,
                    onSelectionChange = plannerViewModel::setWhenSelection,
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                )
                SearchButton(
                    enabled = bothFilled,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        launchPlanner()
                    },
                )
            }
        }
    }
}

// ── Search button (capsule, enabled solo con Da+A) ───────────────────────────

@Composable
private fun SearchButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TransitTheme.colors
    // surfaceOverMap: il bottone sta sopra lo shader — bgSecondary glass
    // in dark sarebbe trasparente (stessa classe della card su mappa).
    val bg = if (enabled) colors.accent else colors.surfaceOverMap
    val fg = if (enabled) Color.White else colors.textTertiary
    Row(
        modifier = modifier
            .background(bg, RoundedCornerShape(percent = 50))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp)
            .semantics { testTag = "planner_home_search" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        Icon(
            painter = painterResource(LucideIcons.Search),
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = stringResource(R.string.planner_search_button),
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            maxLines = 1,
            softWrap = false,
        )
    }
}

// ── Origin/Destination icon column (Lucide icon + neutral connector) ─────────

private enum class OrigDestRole { Origin, Destination }

@Composable
private fun OrigDestIconColumn(
    role: OrigDestRole,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    // Connector neutro per non competere col verde delle icone — il connector
    // è scaffolding spaziale, non decorazione. Match iOS PlannerScreen.
    val connectorColor = TransitTheme.colors.textTertiary
    androidx.compose.foundation.layout.Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val iconHalf = 10.dp.toPx() // icon is 20dp → half
            val gap = 2.dp.toPx()
            val lineStroke = 1.dp.toPx()

            when (role) {
                OrigDestRole.Origin -> drawLine(
                    color = connectorColor,
                    start = Offset(cx, cy + iconHalf + gap),
                    end = Offset(cx, size.height),
                    strokeWidth = lineStroke,
                    cap = StrokeCap.Round,
                )
                OrigDestRole.Destination -> drawLine(
                    color = connectorColor,
                    start = Offset(cx, 0f),
                    end = Offset(cx, cy - iconHalf - gap),
                    strokeWidth = lineStroke,
                    cap = StrokeCap.Round,
                )
            }
        }

        Icon(
            painter = painterResource(
                when (role) {
                    OrigDestRole.Origin -> LucideIcons.LocateFixed
                    OrigDestRole.Destination -> LucideIcons.MapPin
                }
            ),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(20.dp),
        )
    }
}
