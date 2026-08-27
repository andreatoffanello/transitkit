package com.transitkit.app.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.transitkit.app.BuildConfig
import com.transitkit.app.R
import com.transitkit.app.config.DeveloperMode
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.ui.components.rememberHaptics

/**
 * Diagnostica interna, raggiungibile solo dopo i 7 tap sulla riga versione.
 *
 * Non è una schermata di prodotto: è deliberatamente densa e non localizzata
 * (vedi le chiavi `dev_*` con `translatable="false"`). Tutto ciò che mostra
 * viene dal config iniettato o da BuildConfig — niente valori hardcodati che
 * poi divergono dal vero per un altro operatore.
 *
 * Parità iOS: `Views/Settings/DeveloperView.swift`.
 */
@Composable
fun DeveloperScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onReplayOnboarding: () -> Unit = {},
) {
    val colors = TransitTheme.colors
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val config = viewModel.operatorConfig
    val fcmToken by viewModel.fcmToken.collectAsStateWithLifecycle()
    val reloading by viewModel.reloadingSchedule.collectAsStateWithLifecycle()
    val notSet = stringResource(R.string.dev_not_set)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .semantics { testTag = "developer_screen" },
    ) {
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
                        contentDescription = stringResource(R.string.cd_indietro),
                        tint = colors.textPrimary,
                    )
                }
                Text(
                    text = stringResource(R.string.dev_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // ── Build ────────────────────────────────────────────────────────────
        item {
            DevSectionLabel(stringResource(R.string.dev_section_build))
            CardContainer {
                DevValueRow(stringResource(R.string.dev_version), "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                DevDivider()
                DevValueRow(stringResource(R.string.dev_application_id), BuildConfig.APPLICATION_ID)
                DevDivider()
                DevValueRow(stringResource(R.string.dev_operator), "${config.id} · ${config.name}")
                DevDivider()
                DevValueRow(stringResource(R.string.dev_brand), config.brandName ?: config.name)
                DevDivider()
                DevValueRow(stringResource(R.string.dev_cdn), config.cdnUrl ?: notSet)
                DevDivider()
                DevValueRow(stringResource(R.string.dev_routing), config.routingEndpoint ?: notSet)
                DevDivider()
                DevValueRow(stringResource(R.string.dev_console), config.consoleApiUrl ?: notSet)
                DevDivider()
                DevValueRow(stringResource(R.string.dev_rt_vehicles), config.gtfsRt?.vehiclePositionsUrl ?: notSet)
                DevDivider()
                DevValueRow(stringResource(R.string.dev_rt_trips), config.gtfsRt?.tripUpdatesUrl ?: notSet)
                DevDivider()
                DevValueRow(stringResource(R.string.dev_rt_alerts), config.gtfsRt?.serviceAlertsUrl ?: notSet)
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── Feature flags ────────────────────────────────────────────────────
        item {
            DevSectionLabel(stringResource(R.string.dev_section_flags))
            CardContainer {
                val flags = config.features
                DevValueRow("enableMap", flags.enableMap.toString())
                DevDivider()
                DevValueRow("enableGeolocation", flags.enableGeolocation.toString())
                DevDivider()
                DevValueRow("enableFavorites", flags.enableFavorites.toString())
                DevDivider()
                DevValueRow("enableNotifications", flags.enableNotifications.toString())
                DevDivider()
                DevValueRow("useRemoteEngine", flags.useRemoteEngine.toString())
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── Push ─────────────────────────────────────────────────────────────
        item {
            DevSectionLabel(stringResource(R.string.dev_section_push))
            CardContainer {
                val token = fcmToken
                val onCopyToken: (() -> Unit)? = token?.let { value ->
                    {
                        haptics.commit()
                        copyToClipboard(context, "FCM token", value)
                        Toast.makeText(
                            context, context.getString(R.string.dev_fcm_token_copied),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
                SettingsRow(
                    modifier = Modifier.semantics { testTag = "dev_fcm_token_row" },
                    icon = LucideIcons.Bell,
                    iconTint = colors.textTertiary,
                    title = stringResource(R.string.dev_fcm_token),
                    subtitle = token?.let { it.take(18) + "…" + it.takeLast(6) }
                        ?: stringResource(R.string.dev_fcm_token_missing),
                    onClick = onCopyToken,
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── Azioni ───────────────────────────────────────────────────────────
        item {
            DevSectionLabel(stringResource(R.string.dev_section_actions))
            val onReload: () -> Unit = {
                haptics.commit()
                viewModel.forceReloadSchedule { ok ->
                    Toast.makeText(
                        context,
                        context.getString(if (ok) R.string.dev_reload_done else R.string.dev_reload_failed),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
            CardContainer {
                SettingsRow(
                    modifier = Modifier.semantics { testTag = "dev_clear_cache_row" },
                    icon = LucideIcons.RefreshCw,
                    iconTint = colors.textTertiary,
                    title = stringResource(R.string.dev_reload_schedule),
                    subtitle = if (reloading) stringResource(R.string.dev_reload_running)
                               else stringResource(R.string.dev_reload_schedule_sub),
                    onClick = onReload.takeIf { !reloading },
                )
                DevDivider()
                SettingsRow(
                    modifier = Modifier.semantics { testTag = "dev_onboarding_row" },
                    icon = LucideIcons.Compass,
                    iconTint = colors.textTertiary,
                    title = stringResource(R.string.dev_replay_onboarding),
                    onClick = {
                        haptics.navigate()
                        onReplayOnboarding()
                    },
                )
                DevDivider()
                SettingsRow(
                    modifier = Modifier.semantics { testTag = "dev_disable_row" },
                    icon = LucideIcons.X,
                    iconTint = colors.realtimeRed,
                    title = stringResource(R.string.dev_disable),
                    onClick = {
                        haptics.toggleOff()
                        DeveloperMode.setEnabled(context, false)
                        viewModel.onDeveloperModeChanged()
                        onBack()
                    },
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

/** Coppia chiave/valore monospaziata: i valori sono URL e id, non prosa —
 *  la larghezza fissa li rende confrontabili a colpo d'occhio. */
@Composable
private fun DevValueRow(key: String, value: String) {
    val colors = TransitTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = key,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            color = colors.textPrimary,
        )
    }
}

@Composable
private fun DevSectionLabel(text: String) {
    val colors = TransitTheme.colors
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = colors.textPrimary,
        modifier = Modifier.padding(start = 18.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun DevDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        color = TransitTheme.colors.separator,
        thickness = 0.5.dp,
    )
}

private fun copyToClipboard(context: Context, label: String, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
}
