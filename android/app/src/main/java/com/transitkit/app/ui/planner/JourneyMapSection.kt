package com.transitkit.app.ui.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.model.Journey

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
        // Expand pill — top-right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(LucideIcons.Maximize2),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = colors.textPrimary,
            )
        }
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
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
            ) {
                Icon(
                    painterResource(LucideIcons.X),
                    contentDescription = stringResource(R.string.cd_close_map),
                    modifier = Modifier.size(18.dp),
                    tint = colors.textPrimary,
                )
            }
        }
    }
}
