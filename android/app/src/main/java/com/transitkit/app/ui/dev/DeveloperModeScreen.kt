package com.transitkit.app.ui.dev

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme

@Composable
fun DeveloperModeScreen(
    onBack: () -> Unit = {},
    viewModel: DeveloperModeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = TransitTheme.colors
    val context = LocalContext.current

    var label by remember(state.defaultLabel) { mutableStateOf(state.defaultLabel) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        // Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(LucideIcons.ChevronLeft),
                        contentDescription = null,
                        tint = colors.textPrimary,
                    )
                }
                Text(
                    text = stringResource(R.string.dev_mode_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Device identity ──
        item {
            SectionHeader(stringResource(R.string.dev_mode_device_identity))
        }
        item {
            Card {
                if (state.fcmToken == null) {
                    InfoRow(
                        icon = LucideIcons.AlertTriangle,
                        text = stringResource(R.string.dev_mode_no_token),
                        iconColor = colors.realtimeRed,
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.dev_mode_fcm_token),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                            )
                            SelectionContainer {
                                Text(
                                    text = tokenSuffix(state.fcmToken!!),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.textPrimary,
                                )
                            }
                        }
                        Button(
                            onClick = { copyToClipboard(context, state.fcmToken!!) },
                            colors = ButtonDefaults.outlinedButtonColors(),
                        ) {
                            Text(stringResource(R.string.dev_mode_copy_token))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Register ──
        item {
            SectionHeader(stringResource(R.string.dev_mode_register_section))
        }
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text(stringResource(R.string.dev_mode_device_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.registerDevice(label.trim()) },
                        enabled = state.fcmToken != null && label.isNotBlank() && !state.registering,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.registering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            if (state.registering) stringResource(R.string.dev_mode_registering)
                            else stringResource(R.string.dev_mode_register_button)
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.dev_mode_register_footnote),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 6.dp),
            )

            state.resultMessage?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.resultIsError) colors.realtimeRed else colors.accent,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Status ──
        item {
            SectionHeader(stringResource(R.string.dev_mode_status))
        }
        item {
            Card {
                Column {
                    LabeledRow(stringResource(R.string.dev_mode_operator_id), state.operatorId)
                    HorizontalDivider(color = colors.separator, thickness = 0.5.dp)
                    LabeledRow(
                        stringResource(R.string.dev_mode_firebase),
                        if (state.firebaseConfigured) stringResource(R.string.dev_mode_configured)
                        else stringResource(R.string.dev_mode_missing_plist)
                    )
                    HorizontalDivider(color = colors.separator, thickness = 0.5.dp)
                    LabeledRow(
                        stringResource(R.string.dev_mode_console_api),
                        state.consoleBaseUrl ?: "—",
                        mono = true,
                    )
                    state.lastError?.let { err ->
                        HorizontalDivider(color = colors.separator, thickness = 0.5.dp)
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                stringResource(R.string.dev_mode_last_error),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                                modifier = Modifier.weight(0.4f),
                            )
                            Text(
                                err,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.realtimeRed,
                                modifier = Modifier.weight(0.6f),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = TransitTheme.colors.textSecondary,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 32.dp, vertical = 6.dp),
    )
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TransitTheme.colors.bgSecondary),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) { content() }
    }
}

@Composable
private fun LabeledRow(label: String, value: String, mono: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TransitTheme.colors.textSecondary,
            modifier = Modifier.weight(0.4f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            color = TransitTheme.colors.textPrimary,
            modifier = Modifier.weight(0.6f),
        )
    }
}

@Composable
private fun InfoRow(icon: Int, text: String, iconColor: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TransitTheme.colors.textPrimary,
        )
    }
}

private fun tokenSuffix(token: String): String =
    if (token.length > 16) "…${token.takeLast(12)}" else token

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("FCM token", text))
}
