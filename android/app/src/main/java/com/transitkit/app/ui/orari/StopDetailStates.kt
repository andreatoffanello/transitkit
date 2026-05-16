package com.transitkit.app.ui.orari

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme

// iOS parity (StopDetailView.swift): clock icon 28dp tertiary + testo 14sp
// regolare secondary. NIENTE "Clear filter" button — il clear avviene
// tappando la chip "All" sempre visibile sopra (badge=filter pattern).

@Composable
internal fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(LucideIcons.Clock),
            contentDescription = null,
            tint = TransitTheme.colors.textTertiary,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = stringResource(R.string.nessuna_partenza_oggi),
            fontSize = 14.sp,
            color = TransitTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun EmptyStateForFilter(routeName: String, onClearFilter: () -> Unit) {
    // iOS parity: clear-filter NON va qui — è la chip "All" sopra. La signature
    // mantiene `onClearFilter` per compat retro (call site continua a passarlo)
    // ma il param non viene usato. Future cleanup: rimuovere quando tutte le
    // call site sono aggiornate.
    val colors = TransitTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(LucideIcons.Clock),
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = stringResource(R.string.nessuna_partenza_per_linea, routeName),
            fontSize = 14.sp,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun ErrorState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(TransitTheme.colors.realtimeRed.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(LucideIcons.WifiOff),
                contentDescription = null,
                tint = TransitTheme.colors.realtimeRed,
                modifier = Modifier.size(36.dp),
            )
        }
        Text(
            text = stringResource(R.string.partenze_non_disponibili),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TransitTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.controlla_connessione),
            style = MaterialTheme.typography.bodySmall,
            color = TransitTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = TransitTheme.colors.accent,
                contentColor = Color.White,
            ),
            shape = RoundedCornerShape(24.dp),
        ) {
            Icon(painterResource(LucideIcons.RefreshCw), contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_riprova), fontWeight = FontWeight.SemiBold)
        }
    }
}
