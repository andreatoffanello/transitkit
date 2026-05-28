package com.transitkit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.transitkit.app.config.TransitTheme

/**
 * Shared dialog chrome for the planner pickers, aligned to the app's visual
 * language instead of the stock Material3 dialog:
 *  - corner radius 14dp (same as cards), not the M3 28dp extra-large
 *  - surface = bgSecondary, tonalElevation 0 (no M3 lavender tonal overlay)
 *  - real shadow via shadowElevation
 *  - confirm = accent filled pill (white text), cancel = neutral text
 *
 * Both the time wheel and the date calendar render inside this, so the two
 * pickers finally match each other and the rest of the app.
 */
@Composable
fun BrandPickerDialog(
    title: String,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = TransitTheme.colors
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = colors.bgSecondary,
            tonalElevation = 0.dp,
            shadowElevation = 16.dp,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            // Padding orizzontale SOLO su titolo e bottoni: il contenuto
            // (calendario M3 / wheel) prende la larghezza piena del dialog,
            // così la griglia a 7 colonne non viene clippata sull'ultima.
            Column(modifier = Modifier.padding(vertical = 20.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 16.dp),
                )

                content()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = cancelLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                    )
                    Text(
                        text = confirmLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(colors.accent)
                            .clickable(onClick = onConfirm)
                            .padding(horizontal = 18.dp, vertical = 9.dp),
                    )
                }
            }
        }
    }
}
