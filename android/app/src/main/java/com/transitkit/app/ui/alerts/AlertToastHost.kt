package com.transitkit.app.ui.alerts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.config.surfaceOverMap
import com.transitkit.app.data.model.AlertSeverity
import kotlinx.coroutines.delay

/**
 * Top-aligned overlay that reveals a pending new alert on a favorite stop.
 * Auto-dismisses after 4s. Tap to open the alert detail.
 *
 * Mount once at the app root so it's visible across all tabs.
 */
@Composable
fun AlertToastHost(
    onNavigateToAlert: (alertId: String) -> Unit,
    viewModel: AlertToastViewModel = hiltViewModel(),
) {
    val pending by viewModel.pendingAlert.collectAsStateWithLifecycle()
    val colors = TransitTheme.colors

    // Auto-dismiss after display duration.
    LaunchedEffect(pending?.id) {
        if (pending != null) {
            delay(4_000)
            viewModel.dismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedVisibility(
            visible = pending != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        ) {
            pending?.let { alert ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 10.dp, shape = RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        // surfaceOverMap, non bgSecondary: il toast galleggia
                        // sopra QUALUNQUE tab (mappa, shader home) — il token
                        // glass in dark sarebbe trasparente sul contenuto.
                        .background(colors.surfaceOverMap)
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(16.dp))
                        .clickable {
                            onNavigateToAlert(alert.id)
                            viewModel.dismiss()
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val sev = severityColor(alert.severity)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(sev.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(LucideIcons.AlertTriangle),
                            contentDescription = null,
                            tint = sev,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(R.string.alerts_toast_kicker).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textTertiary,
                            letterSpacing = 0.4.sp,
                        )
                        Text(
                            text = localizedHeader(alert),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            maxLines = 2,
                        )
                    }
                    IconButton(
                        onClick = { viewModel.dismiss() },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            painter = painterResource(LucideIcons.X),
                            contentDescription = stringResource(R.string.action_close),
                            tint = colors.textTertiary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}

