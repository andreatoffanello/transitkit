package com.transitkit.app.ui.mappa

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import com.transitkit.app.R
import com.transitkit.app.config.LocalTransitColors
import com.transitkit.app.config.LucideIcons

/**
 * FAB column — allineata al centro verticale dell'area mappa per evitare
 * collisioni con nav bar (top) e tab bar (bottom). Regola progetto:
 * controlli mappa sempre al centro, mai vicino ai bordi.
 */
@Composable
internal fun MappaFabColumn(
    is3D: Boolean,
    onResetView: () -> Unit,
    onRecenter: () -> Unit,
    onToggle3D: () -> Unit,
) {
    val transitColors = LocalTransitColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(end = 16.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val cdVistaPredefinita = stringResource(R.string.cd_vista_predefinita)
            val cdCentraMappa = stringResource(R.string.cd_centra_mappa)
            val cdVista3D = stringResource(R.string.cd_vista_3d)
            val cdVista2D = stringResource(R.string.cd_vista_2d)

            SmallFloatingActionButton(
                onClick = onResetView,
                containerColor = transitColors.bgSecondary,
                contentColor = transitColors.textPrimary,
                modifier = Modifier
                    .shadow(elevation = 6.dp, shape = CircleShape)
                    .semantics {
                        testTag = "btn_map_reset_view"
                        contentDescription = cdVistaPredefinita
                    },
            ) {
                Icon(
                    painter = painterResource(LucideIcons.Map),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }

            SmallFloatingActionButton(
                onClick = onRecenter,
                containerColor = transitColors.bgSecondary,
                contentColor = transitColors.textPrimary,
                modifier = Modifier
                    .shadow(elevation = 6.dp, shape = CircleShape)
                    .semantics {
                        testTag = "btn_map_recenter"
                        contentDescription = cdCentraMappa
                    },
            ) {
                Icon(
                    painter = painterResource(LucideIcons.Crosshair),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }

            SmallFloatingActionButton(
                onClick = onToggle3D,
                containerColor = if (is3D) transitColors.accent else transitColors.bgSecondary,
                contentColor = if (is3D) Color.White else transitColors.textPrimary,
                modifier = Modifier
                    .shadow(elevation = 6.dp, shape = CircleShape)
                    .semantics {
                        testTag = "btn_map_toggle_3d"
                        contentDescription = if (is3D) cdVista2D else cdVista3D
                    },
            ) {
                Icon(
                    painter = painterResource(LucideIcons.Box),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
