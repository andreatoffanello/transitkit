package com.transitkit.app.ui.planner

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.model.Journey
import com.transitkit.app.data.model.durationMinutes
import com.transitkit.app.data.model.totalWalkSeconds
import com.transitkit.app.data.model.transfers

@Composable
internal fun JourneyHeader(journey: Journey) {
    val colors = TransitTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatEpochTime(journey.departureTime),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                painterResource(LucideIcons.ArrowRight),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = colors.textSecondary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = formatEpochTime(journey.arrivalTime),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = headerSubtitle(journey),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
    }
}

@Composable
@ReadOnlyComposable
internal fun headerSubtitle(journey: Journey): String {
    val mins = journey.durationMinutes
    val transfersStr = when (journey.transfers) {
        0 -> stringResource(R.string.planner_direct)
        1 -> stringResource(R.string.planner_one_transfer)
        else -> stringResource(R.string.planner_n_transfers, journey.transfers)
    }
    val walkMin = journey.totalWalkSeconds / 60
    val walkStr = if (walkMin >= 2) stringResource(R.string.planner_subtitle_walking, walkMin) else ""
    return "$mins min · $transfersStr$walkStr"
}
