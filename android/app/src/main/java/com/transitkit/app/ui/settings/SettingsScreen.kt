package com.transitkit.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.transitkit.app.config.LucideIcons
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import com.transitkit.app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.transitkit.app.BuildConfig
import com.transitkit.app.config.DeveloperMode
import com.transitkit.app.config.OperatorConfig
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.ui.components.rememberHaptics

// ---------------------------------------------------------------------------
// SettingsRow — flat custom row, iOS-parity: 40dp icon box, 14dp vertical
// ---------------------------------------------------------------------------

@Composable
fun SettingsRow(
    modifier: Modifier = Modifier,
    icon: Int,
    iconTint: Color? = null,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = TransitTheme.colors
    val accent = iconTint ?: colors.accentReadable
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics {
                if (subtitle != null) contentDescription = "$title: $subtitle"
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(accent.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        } else if (onClick != null) {
            Spacer(Modifier.width(8.dp))
            Icon(
                painter = painterResource(LucideIcons.ChevronRight),
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Section label — sentence case 15sp SemiBold textPrimary, parità Home/iOS
// ---------------------------------------------------------------------------

@Composable
private fun SectionLabel(text: String) {
    val colors = TransitTheme.colors
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = colors.textPrimary,
        modifier = Modifier.padding(start = 18.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
    )
}

// ---------------------------------------------------------------------------
// Operator card
// ---------------------------------------------------------------------------

@Composable
private fun OperatorCard(config: OperatorConfig) {
    val colors = TransitTheme.colors
    val subtitle = config.region?.takeIf { it.isNotBlank() }
        ?: config.country.takeIf { it.isNotBlank() }

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.bgSecondary)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Header Settings mostra il brand dell'APP (icona bus AppalRider da
        // ic_launcher_foreground + nome AppalRider). MAI il logo operatore:
        // attribuirebbe l'app all'operatore (impersonazione). Il logo operatore
        // resta solo nella OperatorReferenceCard di Home, dove esplicitamente
        // diciamo "chi è l'operatore di trasporto".
        Image(
            painter = painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier
                .size(48.dp),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            // Brand dell'APP (AppalRider, da R.string.app_name), non dell'operatore.
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Favorites section content — flat row, iOS-parity
// ---------------------------------------------------------------------------

@Composable
private fun FavoriteStopRow(stop: ResolvedStop, onClick: () -> Unit, onRemove: () -> Unit) {
    val colors = TransitTheme.colors
    val haptics = rememberHaptics()
    val cdFermata = stringResource(R.string.cd_fermata_salvata, stop.name)
    val cdRimuovi = stringResource(R.string.cd_rimuovi_fermata, stop.name)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptics.navigate()
                onClick()
            }
            .semantics {
                contentDescription = cdFermata
                testTag = "favorite_stop_row"
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(LucideIcons.BusFront),
            contentDescription = null,
            tint = colors.accentReadable,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stop.name,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
            )
            // Le linee servite, non `stop.id`: l'id è uno slug interno
            // (`appalcart_howard_st_hardin_st`) e non dice niente a nessuno.
            // Parità con iOS `FavoritesListView.favoriteRow`.
            if (stop.routeNames.isNotEmpty()) {
                Text(
                    text = stop.routeNames.take(5).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    haptics.destructive()
                    onRemove()
                }
                .semantics { contentDescription = cdRimuovi },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(LucideIcons.Trash2),
                contentDescription = null,
                tint = colors.realtimeRed,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Card container for grouped items
// ---------------------------------------------------------------------------

@Composable
internal fun CardContainer(content: @Composable () -> Unit) {
    val colors = TransitTheme.colors
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.bgSecondary),
    ) {
        content()
    }
}

// ---------------------------------------------------------------------------
// SettingsScreen
// ---------------------------------------------------------------------------

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onNavigateToOrari: () -> Unit = {},
    onNavigateToStop: (stopId: String, stopName: String) -> Unit = { _, _ -> },
    onNavigateToLanguage: () -> Unit = {},
    onNavigateToDeveloper: () -> Unit = {},
) {
    val favoriteStops by viewModel.favoriteStops.collectAsStateWithLifecycle()
    val notificationsBusy by viewModel.notificationsBusy.collectAsStateWithLifecycle()
    val config = viewModel.operatorConfig
    val colors = TransitTheme.colors
    val haptics = rememberHaptics()
    val favoriteList = favoriteStops

    // ── POST_NOTIFICATIONS runtime permission (Android 13+) ─────────────────
    // The Switch's `checked` reads the *authoritative* OS permission status,
    // not a separate in-VM flag. The previous version bound the switch to a
    // MutableStateFlow that flipped only after a Firebase `ensureToken` round
    // trip — when Firebase wasn't configured for the operator, the flag stayed
    // false forever and the knob refused to slide despite the user tapping it.
    // Reading permission state directly keeps the toggle truthful regardless of
    // FCM availability.
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val notifPermissionGranted: () -> Boolean = {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
    }
    // Re-evaluated on every recomposition AND every ON_RESUME (user returns
    // from Settings app after granting/revoking the permission).
    var permissionTick by remember { androidx.compose.runtime.mutableStateOf(0) }
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) permissionTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val notificationsEnabled = remember(permissionTick) { notifPermissionGranted() }

    // ── Sblocco modalità sviluppatore (7 tap sulla riga versione) ───────────
    // Il conteggio vive qui e non in DeveloperMode: è stato di una gesture in
    // corso, non una preferenza. La finestra rolling evita che tap sparsi nel
    // tempo sommino fino a sbloccare per sbaglio.
    // Ri-letta a ogni ON_RESUME della destinazione (stesso tick del permesso):
    // tornando da Developer dopo averla spenta, la sezione deve sparire.
    var developerEnabled by remember(permissionTick) { mutableStateOf(DeveloperMode.isEnabled(context)) }
    var tapCount by remember { mutableStateOf(0) }
    var lastTapAt by remember { mutableStateOf(0L) }
    val onVersionTap: () -> Unit = {
        if (developerEnabled) {
            // Ri-tappare quando è già attiva non deve spegnerla di nascosto:
            // si disattiva solo dalla riga esplicita in Developer.
            android.widget.Toast.makeText(
                context, context.getString(R.string.dev_already_enabled),
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        } else {
            val now = android.os.SystemClock.elapsedRealtime()
            tapCount = if (now - lastTapAt > DeveloperMode.TAP_WINDOW_MS) 1 else tapCount + 1
            lastTapAt = now
            val remaining = DeveloperMode.UNLOCK_TAPS - tapCount
            when {
                remaining <= 0 -> {
                    haptics.commit()
                    DeveloperMode.setEnabled(context, true)
                    developerEnabled = true
                    tapCount = 0
                    viewModel.onDeveloperModeChanged()
                    android.widget.Toast.makeText(
                        context, context.getString(R.string.dev_unlock_done),
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                }
                remaining <= 3 -> {
                    haptics.tick()
                    android.widget.Toast.makeText(
                        context, context.resources.getQuantityString(R.plurals.dev_unlock_countdown, remaining, remaining),
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionTick++  // re-read OS state so the switch updates regardless
        if (granted) viewModel.onNotificationsPermissionGranted()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        // ── Header (back + title) ───────────────────────────────────────────
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            ) {
                androidx.compose.material3.IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(LucideIcons.ChevronLeft),
                        contentDescription = stringResource(R.string.cd_indietro),
                        tint = colors.textPrimary,
                    )
                }
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
            }
        }

        // ── Operator card ────────────────────────────────────────────────────
        item {
            OperatorCard(config = config)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── Preferiti ────────────────────────────────────────────────────────
        if (config.features.enableFavorites) {
            item {
                SectionLabel(text = stringResource(R.string.settings_section_preferiti))
            }

            item {
                CardContainer {
                    if (favoriteList.isEmpty()) {
                        SettingsRow(
                            icon = LucideIcons.Star,
                            iconTint = colors.textTertiary,
                            title = stringResource(R.string.settings_section_preferiti),
                            subtitle = stringResource(R.string.settings_nessuno_salvato),
                        )
                    } else {
                        favoriteList.forEachIndexed { index, stop ->
                            FavoriteStopRow(
                                stop = stop,
                                onClick = { onNavigateToStop(stop.id, stop.name) },
                                onRemove = { viewModel.removeFavorite(stop.id) },
                            )
                            if (index < favoriteList.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 46.dp),
                                    color = colors.separator,
                                    thickness = 0.5.dp,
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // ── Notifiche ────────────────────────────────────────────────────────
        if (config.features.enableNotifications) {
            item {
                SectionLabel(text = stringResource(R.string.settings_section_notifiche))
            }

            item {
                CardContainer {
                    SettingsRow(
                        icon = LucideIcons.Bell,
                        title = stringResource(R.string.settings_section_notifiche),
                        trailing = {
                            Switch(
                                checked = notificationsEnabled,
                                enabled = !notificationsBusy,
                                onCheckedChange = { wantOn ->
                                    if (wantOn) haptics.toggleOn() else haptics.toggleOff()
                                    if (wantOn) {
                                        if (notifPermissionGranted()) {
                                            viewModel.onNotificationsPermissionGranted()
                                            permissionTick++
                                        } else {
                                            permissionLauncher.launch(
                                                android.Manifest.permission.POST_NOTIFICATIONS
                                            )
                                        }
                                    } else {
                                        // Disabling: we can't revoke POST_NOTIFICATIONS programmatically
                                        // on Android 13+. Unsubscribe from topics and open the system
                                        // notification settings so the user can flip the OS switch
                                        // — on returning, ON_RESUME bumps `permissionTick` and the UI
                                        // catches up.
                                        viewModel.onNotificationsDisabled()
                                        runCatching {
                                            context.startActivity(
                                                Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                    putExtra(
                                                        android.provider.Settings.EXTRA_APP_PACKAGE,
                                                        context.packageName,
                                                    )
                                                }
                                            )
                                        }
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = colors.accent,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = colors.textTertiary.copy(alpha = 0.3f),
                                    uncheckedBorderColor = Color.Transparent,
                                ),
                            )
                        },
                    )
                }
                Text(
                    text = stringResource(R.string.settings_notifiche_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 6.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // ── Lingua ───────────────────────────────────────────────────────────
        item {
            SectionLabel(text = stringResource(R.string.settings_section_lingua))
        }

        item {
            CardContainer {
                SettingsRow(
                    icon = LucideIcons.Globe,
                    title = stringResource(R.string.settings_section_lingua),
                    subtitle = com.transitkit.app.config.AppLocaleManager.current(context).label(context),
                    onClick = {
                        haptics.navigate()
                        onNavigateToLanguage()
                    },
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Privacy ──────────────────────────────────────────────────────────
        item {
            SectionLabel(text = stringResource(R.string.settings_location_section))
        }

        item {
            PrivacyLocationCard()
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Info ─────────────────────────────────────────────────────────────
        item {
            SectionLabel(text = stringResource(R.string.settings_section_info))
        }

        item {
            CardContainer {
                config.url.takeIf { it.isNotBlank() }?.let { rawUrl ->
                    SettingsRow(
                        icon = LucideIcons.Globe,
                        title = stringResource(R.string.about_sito_web),
                        subtitle = config.name,
                        onClick = {
                            haptics.commit()
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(rawUrl))
                            )
                        },
                        trailing = {
                            Icon(
                                painter = painterResource(LucideIcons.ExternalLink),
                                contentDescription = null,
                                tint = colors.textTertiary,
                                modifier = Modifier.size(14.dp),
                            )
                        },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 70.dp),
                        color = colors.separator,
                        thickness = 0.5.dp,
                    )
                }
                config.privacyUrl?.takeIf { it.isNotBlank() }?.let { rawUrl ->
                    SettingsRow(
                        icon = LucideIcons.Shield,
                        title = stringResource(R.string.about_privacy_policy),
                        onClick = {
                            haptics.commit()
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(rawUrl))
                            )
                        },
                        trailing = {
                            Icon(
                                painter = painterResource(LucideIcons.ExternalLink),
                                contentDescription = null,
                                tint = colors.textTertiary,
                                modifier = Modifier.size(14.dp),
                            )
                        },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 70.dp),
                        color = colors.separator,
                        thickness = 0.5.dp,
                    )
                }
                SettingsRow(
                    // Bersaglio dello sblocco sviluppatore: passa dal modifier e
                    // non da `onClick` perché quest'ultimo aggiungerebbe il
                    // chevron, promettendo una destinazione che non c'è.
                    modifier = Modifier
                        .clickable(onClick = onVersionTap)
                        .semantics { testTag = "settings_version_row" },
                    icon = LucideIcons.Info,
                    iconTint = colors.textTertiary,
                    title = stringResource(R.string.settings_versione),
                    trailing = {
                        Text(
                            text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textTertiary,
                        )
                    },
                )
            }
            Text(
                // %1$s = app name (AppalRider), %2$s = operator name (AppalCART).
                text = stringResource(R.string.settings_disclaimer_body, stringResource(R.string.app_name), config.name),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 8.dp),
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        // ── Developer (solo dopo lo sblocco) ─────────────────────────────────
        if (developerEnabled) {
            item {
                SectionLabel(text = stringResource(R.string.dev_title))
                CardContainer {
                    SettingsRow(
                        modifier = Modifier.semantics { testTag = "settings_developer_row" },
                        icon = LucideIcons.Settings,
                        iconTint = colors.textTertiary,
                        title = stringResource(R.string.dev_title),
                        subtitle = "${config.id} · v${BuildConfig.VERSION_NAME}",
                        onClick = {
                            haptics.navigate()
                            onNavigateToDeveloper()
                        },
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Privacy — location access card
// ---------------------------------------------------------------------------

@Composable
private fun PrivacyLocationCard() {
    val colors = TransitTheme.colors
    val haptics = rememberHaptics()
    val context = androidx.compose.ui.platform.LocalContext.current
    val granted = androidx.core.content.ContextCompat.checkSelfPermission(
        context, android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    CardContainer {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(colors.accentReadable.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(LucideIcons.MapPin),
                    contentDescription = null,
                    tint = colors.accentReadable,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.settings_location_title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            if (granted) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(colors.realtimeGreen, androidx.compose.foundation.shape.CircleShape),
                )
            } else {
                Text(
                    text = stringResource(R.string.settings_location_open),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.secondaryFg,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.secondaryFill)
                        .clickable {
                            haptics.commit()
                            context.startActivity(
                                android.content.Intent(
                                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    android.net.Uri.fromParts("package", context.packageName, null)
                                )
                            )
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}
