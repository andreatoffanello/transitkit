package com.transitkit.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.runtime.LaunchedEffect
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
import com.transitkit.app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.transitkit.app.BuildConfig
import com.transitkit.app.config.OperatorConfig
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.model.ResolvedStop

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/** Generates initials for an operator name — up to 2 uppercase letters. */
private fun operatorInitials(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        words.size >= 2 -> "${words[0].first()}${words[1].first()}".uppercase()
        words.size == 1 -> words[0].take(2).uppercase()
        else -> "?"
    }
}

// ---------------------------------------------------------------------------
// SettingsRow — flat custom row, iOS-parity: 16dp horizontal / 12dp vertical
// ---------------------------------------------------------------------------

@Composable
fun SettingsRow(
     icon: Int,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = TransitTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics {
                if (subtitle != null) contentDescription = "$title: $subtitle"
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(colors.accent.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
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
// Section label
// ---------------------------------------------------------------------------

@Composable
private fun SectionLabel(text: String) {
    val colors = TransitTheme.colors
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
        fontWeight = FontWeight.SemiBold,
        color = colors.textTertiary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

// ---------------------------------------------------------------------------
// Operator card
// ---------------------------------------------------------------------------

@Composable
private fun OperatorCard(config: OperatorConfig) {
    val colors = TransitTheme.colors
    val initials = operatorInitials(config.name)
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
        // Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(listOf(colors.accent, colors.primary))
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = config.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
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
private fun FavoriteStopRow(stop: ResolvedStop, onRemove: () -> Unit) {
    val colors = TransitTheme.colors
    val cdFermata = stringResource(R.string.cd_fermata_salvata, stop.name)
    val cdRimuovi = stringResource(R.string.cd_rimuovi_fermata, stop.name)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = cdFermata }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(LucideIcons.BusFront),
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stop.name,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
            )
            Text(
                text = stop.id,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary,
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onRemove)
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
private fun CardContainer(content: @Composable () -> Unit) {
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
    onNavigateToAbout: () -> Unit = {},
    onNavigateToOrari: () -> Unit = {},
    onNavigateToDeveloperMode: () -> Unit = {},
) {
    val favoriteStops by viewModel.favoriteStops.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val notificationsBusy by viewModel.notificationsBusy.collectAsStateWithLifecycle()
    val config = viewModel.operatorConfig
    val colors = TransitTheme.colors
    val favoriteList = favoriteStops
    val currentLanguage = java.util.Locale.getDefault().displayLanguage.replaceFirstChar { it.uppercaseChar() }

    // ── Developer-mode 7-tap unlock ─────────────────────────────────────────
    var versionTaps by remember { mutableStateOf(0) }
    var developerModeUnlocked by remember { mutableStateOf(false) }
    LaunchedEffect(versionTaps) {
        if (versionTaps >= 7 && !developerModeUnlocked) developerModeUnlocked = true
    }

    // ── POST_NOTIFICATIONS runtime permission (Android 13+) ─────────────────
    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onNotificationsPermissionGranted()
    }
    val notifPermissionGranted: () -> Boolean = {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionLabel(text = stringResource(R.string.settings_section_preferiti))
                    if (favoriteList.isNotEmpty()) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = if (favoriteList.size == 1) stringResource(R.string.settings_fermate_salvate_one)
                                   else stringResource(R.string.settings_fermate_salvate_other, favoriteList.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(end = 16.dp),
                        )
                    }
                }
            }

            item {
                CardContainer {
                    if (favoriteList.isEmpty()) {
                        SettingsRow(
                            icon = LucideIcons.Star,
                            title = stringResource(R.string.settings_section_preferiti),
                            subtitle = stringResource(R.string.settings_nessuno_salvato),
                            onClick = onNavigateToOrari,
                        )
                    } else {
                        favoriteList.forEachIndexed { index, stop ->
                            FavoriteStopRow(
                                stop = stop,
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
                                    if (wantOn) {
                                        if (notifPermissionGranted()) {
                                            viewModel.onNotificationsPermissionGranted()
                                        } else {
                                            permissionLauncher.launch(
                                                android.Manifest.permission.POST_NOTIFICATIONS
                                            )
                                        }
                                    } else {
                                        viewModel.onNotificationsDisabled()
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
                    subtitle = currentLanguage,
                )
            }
            Text(
                text = stringResource(R.string.settings_lingua_desc, stringResource(R.string.app_name)),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 6.dp),
            )
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
                SettingsRow(
                    icon = LucideIcons.Info,
                    title = stringResource(R.string.settings_info_su, config.name),
                    onClick = onNavigateToAbout,
                )
                HorizontalDivider(modifier = Modifier.padding(start = 46.dp), color = TransitTheme.colors.separator, thickness = 0.5.dp)
                SettingsRow(
                    icon = LucideIcons.Shield,
                    title = stringResource(R.string.settings_versione),
                    subtitle = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    onClick = { versionTaps += 1 },
                )
                if (developerModeUnlocked) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 46.dp),
                        color = TransitTheme.colors.separator,
                        thickness = 0.5.dp,
                    )
                    SettingsRow(
                        icon = LucideIcons.Settings,
                        title = stringResource(R.string.dev_mode_row_title),
                        subtitle = stringResource(R.string.dev_mode_row_subtitle),
                        onClick = onNavigateToDeveloperMode,
                    )
                }
            }
            Text(
                text = stringResource(R.string.settings_disclaimer_body, config.name, config.fullName),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Privacy — location access card
// ---------------------------------------------------------------------------

@Composable
private fun PrivacyLocationCard() {
    val colors = TransitTheme.colors
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
                    .background(colors.accent.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(LucideIcons.MapPin),
                    contentDescription = null,
                    tint = colors.accent,
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
                    color = colors.accent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
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
