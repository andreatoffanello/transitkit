package com.transitkit.app.ui.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.model.Journey
import com.transitkit.app.ui.mappa.MapOverlayButton

@Composable
internal fun JourneyMapPreview(
    journey: Journey,
    userLocation: Pair<Double, Double>?,
    onTap: () -> Unit,
) {
    val colors = TransitTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
    ) {
        JourneyMapView(
            journey = journey,
            userLocation = userLocation,
            accentColor = colors.accent,
            modifier = Modifier.fillMaxSize(),
        )
        // Overlay sibling captures the tap before the MapView's native gesture handler.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onTap),
        )
        // Expand — top-right. Decorativo (l'intera preview è tappabile),
        // ma stesso bottone canonico del chrome mappa per coerenza visiva.
        MapOverlayButton(
            iconRes = LucideIcons.Maximize2,
            contentDescription = null,
            onClick = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
        )
    }
}

@Composable
internal fun JourneyMapFullscreen(
    journey: Journey,
    userLocation: Pair<Double, Double>?,
    onClose: () -> Unit,
) {
    val colors = TransitTheme.colors
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background),
        ) {
            JourneyMapView(
                journey = journey,
                userLocation = userLocation,
                accentColor = colors.accent,
                modifier = Modifier.fillMaxSize(),
            )
            // Close canonico — insets reali della status bar, niente top
            // hardcoded (48dp fissi finivano sotto la barra su device alti).
            MapOverlayButton(
                iconRes = LucideIcons.X,
                contentDescription = stringResource(R.string.cd_close_map),
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 8.dp, end = 16.dp),
            )
        }
    }
}
