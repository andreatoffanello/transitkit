package com.transitkit.app.ui.mappa

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transitkit.app.R
import com.transitkit.app.config.LocalTransitColors
import com.transitkit.app.config.LucideIcons
import kotlin.math.abs

/**
 * Controllo mappa unificato in un singolo pill verticale.
 *
 * Allineato al centro verticale dell'area mappa (regola progetto: mai vicino
 * a nav bar / tab bar). Cornerradius 22dp, celle 44×44dp separate da divider
 * 28×0.5dp. Stesso componente in [MappaScreen] e nell'overlay mappa espansa
 * di StopDetail / LineDetail.
 *
 * Bottoni, in ordine top→bottom:
 *  1. **2D/3D** — text label, accent quando in 3D.
 *  2. **Recenter** — crosshair, centra sulla posizione utente.
 *  3. **Reset bearing** — compass, mostrato SOLO se `abs(currentBearing) > 1°`.
 *  4. **Expand / Close** — `Maximize2`/`Minimize2`, mostrato solo se
 *     [onExpandToggle] non è null. Icona dipende da [expanded].
 *
 * @param onResetBearing se null, il pulsante reset bearing è disabilitato del
 *   tutto (es. quando la mappa non supporta rotation gesture).
 * @param onExpandToggle se null, niente pulsante expand. In MappaTab non
 *   serve (la mappa è già fullscreen); serve in StopDetail/LineDetail dove
 *   l'hero map può essere espansa a full-screen overlay.
 */
@Composable
internal fun UnifiedMapControlsPill(
    is3D: Boolean,
    onToggle3D: () -> Unit,
    onRecenter: () -> Unit,
    modifier: Modifier = Modifier,
    currentBearing: Double = 0.0,
    onResetBearing: (() -> Unit)? = null,
    expanded: Boolean = false,
    onExpandToggle: (() -> Unit)? = null,
) {
    val colors = LocalTransitColors.current
    val pillShape = RoundedCornerShape(22.dp)
    val dividerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    val cdCentraMappa = stringResource(R.string.cd_centra_mappa)
    val cdVista3D = stringResource(R.string.cd_vista_3d)
    val cdVista2D = stringResource(R.string.cd_vista_2d)
    val cdResetBearing = stringResource(R.string.map_reset_bearing)
    val cdExpand = stringResource(R.string.map_expand)
    val cdClose = stringResource(R.string.map_close_expanded)

    val showResetBearing = onResetBearing != null && abs(currentBearing) > 1.0

    Surface(
        modifier = modifier,
        shape = pillShape,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(44.dp),
        ) {
            // 2D/3D toggle — text label
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable { onToggle3D() }
                    .semantics {
                        testTag = "btn_map_toggle_3d"
                        contentDescription = if (is3D) cdVista2D else cdVista3D
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (is3D) "2D" else "3D",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    ),
                    color = colors.textPrimary,
                )
            }

            PillDivider(dividerColor)

            // Recenter
            IconButton(
                onClick = onRecenter,
                modifier = Modifier
                    .size(44.dp)
                    .semantics {
                        testTag = "btn_map_recenter"
                        contentDescription = cdCentraMappa
                    },
            ) {
                Icon(
                    painter = painterResource(LucideIcons.Crosshair),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colors.textPrimary,
                )
            }

            // Reset bearing — conditional
            AnimatedVisibility(
                visible = showResetBearing,
                enter = fadeIn() + scaleIn(initialScale = 0.7f),
                exit = fadeOut() + scaleOut(targetScale = 0.7f),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PillDivider(dividerColor)
                    IconButton(
                        onClick = { onResetBearing?.invoke() },
                        modifier = Modifier
                            .size(44.dp)
                            .semantics {
                                testTag = "btn_map_reset_bearing"
                                contentDescription = cdResetBearing
                            },
                    ) {
                        Icon(
                            painter = painterResource(LucideIcons.Compass),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = colors.textPrimary,
                        )
                    }
                }
            }

            // Expand / Close — conditional
            if (onExpandToggle != null) {
                PillDivider(dividerColor)
                IconButton(
                    onClick = onExpandToggle,
                    modifier = Modifier
                        .size(44.dp)
                        .semantics {
                            testTag = if (expanded) "btn_map_close" else "btn_map_expand"
                            contentDescription = if (expanded) cdClose else cdExpand
                        },
                ) {
                    Icon(
                        painter = painterResource(
                            if (expanded) LucideIcons.Minimize2 else LucideIcons.Maximize2
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = colors.textPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun PillDivider(color: androidx.compose.ui.graphics.Color) {
    // 28dp largo × 0.5dp alto, come la spec iOS (MapControlsColumn.swift).
    // Box invece di HorizontalDivider per width fissa senza fillMaxWidth.
    Box(
        modifier = Modifier
            .width(28.dp)
            .height(0.5.dp)
            .background(color),
    )
}

/**
 * Wrapper di posizionamento per uso in MappaScreen: centra verticalmente nel
 * box parent e applica padding navigation bar + end 16dp.
 */
@Composable
internal fun MappaTabControlsPill(
    is3D: Boolean,
    onToggle3D: () -> Unit,
    onRecenter: () -> Unit,
    currentBearing: Double = 0.0,
    onResetBearing: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(end = 16.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        UnifiedMapControlsPill(
            is3D = is3D,
            onToggle3D = onToggle3D,
            onRecenter = onRecenter,
            currentBearing = currentBearing,
            onResetBearing = onResetBearing,
        )
    }
}
