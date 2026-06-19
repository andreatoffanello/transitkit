package com.transitkit.app.ui.update

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowCircleDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transitkit.app.R
import com.transitkit.app.config.TransitTheme

/**
 * Banner soft (non bloccante) per aggiornamento opzionale dell'app.
 *
 * Stile coerente con [HomeAlertChip] (card arrotondata, surface + tint accent
 * operatore, border sottile). Mostrato in Home sopra le sezioni principali
 * quando `AppUpdateChecker.softState` è valorizzato.
 */
@Composable
fun SoftUpdateBanner(
    message: String,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = TransitTheme.colors.accent
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val bgAlpha = if (isDark) 0.18f else 0.10f
    val borderAlpha = if (isDark) 0.30f else 0.20f
    val cardShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .semantics { testTag = "soft_update_banner" }
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surface)
            .background(accent.copy(alpha = bgAlpha))
            .border(0.5.dp, accent.copy(alpha = borderAlpha), cardShape),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowCircleDown,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = accent,
                )
                Text(
                    text = stringResource(R.string.update_available_title),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = message,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp,
            )

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onUpdate,
                    modifier = Modifier.semantics { testTag = "btn_soft_update" },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.update_now_label),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(Modifier.width(8.dp))

                TextButton(
                    onClick = onLater,
                    modifier = Modifier.semantics { testTag = "btn_soft_update_later" },
                ) {
                    Text(
                        text = stringResource(R.string.update_later_label),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ── Estensione privata per leggere luminance dal Color M3 ─────────────────

private fun Color.luminance(): Float {
    val r = if (red <= 0.03928f) red / 12.92f else ((red + 0.055f) / 1.055f).pow(2.4)
    val g = if (green <= 0.03928f) green / 12.92f else ((green + 0.055f) / 1.055f).pow(2.4)
    val b = if (blue <= 0.03928f) blue / 12.92f else ((blue + 0.055f) / 1.055f).pow(2.4)
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}

private fun Float.pow(exp: Double): Float = Math.pow(this.toDouble(), exp).toFloat()
