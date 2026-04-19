package com.transitkit.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import com.transitkit.app.config.LucideIcons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.transitkit.app.R
import com.transitkit.app.BuildConfig
import com.transitkit.app.config.OperatorConfig
import com.transitkit.app.config.TransitTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    config: OperatorConfig,
    onBack: () -> Unit = {},
) {
    val colors = TransitTheme.colors
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title), fontWeight = FontWeight.SemiBold, color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(LucideIcons.ChevronLeft), contentDescription = stringResource(R.string.cd_indietro), tint = colors.accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
            )
        },
        containerColor = colors.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = 48.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Identity card ────────────────────────────────────────────
            item {
                AboutCard {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(colors.accent.copy(alpha = 0.15f), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(painterResource(LucideIcons.BusFront), contentDescription = null, tint = colors.accent, modifier = Modifier.size(32.dp))
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(config.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            val subtitle = listOfNotNull(config.region?.takeIf { it.isNotBlank() }, config.country.takeIf { it.isNotBlank() }).joinToString(", ")
                            if (subtitle.isNotBlank()) {
                                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .background(colors.bgSecondary, RoundedCornerShape(50))
                                .border(1.dp, colors.glassBorder, RoundedCornerShape(50))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text("v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = colors.textTertiary)
                        }
                    }
                }
            }

            // ── Powered by TransitKit card ───────────────────────────────
            item {
                AboutCard {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(painterResource(LucideIcons.Settings), contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(stringResource(R.string.about_sviluppato_con), style = MaterialTheme.typography.labelSmall, color = colors.textTertiary)
                            Text(stringResource(R.string.about_transitkit), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        }
                    }
                }
            }

            // ── Links card ───────────────────────────────────────────────
            val hasUrl = config.url.isNotBlank()
            val hasPrivacy = config.privacyUrl?.isNotBlank() == true
            if (hasUrl || hasPrivacy) {
                item {
                    AboutCard {
                        if (hasUrl) {
                            AboutLinkRow(
                                icon = LucideIcons.ExternalLink,
                                title = stringResource(R.string.about_sito_web),
                                subtitle = config.name,
                                onClick = { uriHandler.openUri(config.url) },
                            )
                        }
                        if (hasUrl && hasPrivacy) {
                            HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = colors.separator, thickness = 0.5.dp)
                        }
                        if (hasPrivacy) {
                            AboutLinkRow(
                                icon = LucideIcons.Shield,
                                title = stringResource(R.string.about_privacy_policy),
                                subtitle = null,
                                onClick = { uriHandler.openUri(config.privacyUrl!!) },
                            )
                        }
                    }
                }
            }

            // ── Open source card ─────────────────────────────────────────
            item {
                AboutCard {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(painterResource(LucideIcons.List), contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                            Text(stringResource(R.string.about_licenze_open_source), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        }
                        Text(
                            stringResource(R.string.about_licenze_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            lineHeight = MaterialTheme.typography.bodySmall.fontSize * 1.5,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutCard(content: @Composable () -> Unit) {
    val colors = TransitTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.bgSecondary)
            .border(1.dp, colors.glassBorder, RoundedCornerShape(16.dp)),
    ) {
        content()
    }
}

@Composable
private fun AboutLinkRow( icon: Int, title: String, subtitle: String?, onClick: () -> Unit) {
    val colors = TransitTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = colors.textPrimary)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
        Icon(painterResource(LucideIcons.ExternalLink), contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(16.dp))
    }
}
