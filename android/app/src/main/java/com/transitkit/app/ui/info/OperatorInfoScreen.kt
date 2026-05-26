package com.transitkit.app.ui.info

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.OperatorConfig
import com.transitkit.app.config.TransitTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperatorInfoScreen(
    config: OperatorConfig,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.info_section_operatore), fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(painterResource(LucideIcons.ChevronLeft), contentDescription = stringResource(R.string.cd_indietro), tint = TransitTheme.colors.textPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TransitTheme.colors.background),
            )
        },
        containerColor = TransitTheme.colors.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Card 1: Identity
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = TransitTheme.colors.bgSecondary),
                    border = BorderStroke(1.dp, TransitTheme.colors.glassBorder),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(TransitTheme.colors.primary.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(painterResource(LucideIcons.BusFront), null, tint = TransitTheme.colors.primary, modifier = Modifier.size(32.dp))
                        }
                        Text(
                            config.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TransitTheme.colors.textPrimary,
                            textAlign = TextAlign.Center,
                        )
                        if (config.fullName.isNotBlank() && config.fullName != config.name) {
                            Text(
                                config.fullName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TransitTheme.colors.textSecondary,
                                textAlign = TextAlign.Center,
                            )
                        }
                        val region = listOfNotNull(
                            config.region?.takeIf { it.isNotBlank() },
                            config.country.takeIf { it.isNotBlank() },
                        ).joinToString(", ")
                        if (region.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .background(TransitTheme.colors.glassBorder, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(painterResource(LucideIcons.MapPin), null, tint = TransitTheme.colors.textTertiary, modifier = Modifier.size(12.dp))
                                    Text(region, style = MaterialTheme.typography.labelSmall, color = TransitTheme.colors.textTertiary)
                                }
                            }
                        }
                    }
                }
            }
            // Card 2: Links
            item {
                val labelSitoWeb = stringResource(R.string.info_link_sito_web)
                val labelTelefono = stringResource(R.string.info_link_telefono)
                val labelEmail = stringResource(R.string.info_link_email)
                val labelPrivacy = stringResource(R.string.info_link_privacy)
                val links = buildList {
                    config.url.takeIf { it.isNotBlank() }?.let { add(Triple(LucideIcons.Globe, labelSitoWeb, it)) }
                    config.contact?.phone?.takeIf { it.isNotBlank() }?.let { add(Triple(LucideIcons.Phone, labelTelefono, "tel:$it")) }
                    config.contact?.email?.takeIf { it.isNotBlank() }?.let { add(Triple(LucideIcons.Mail, labelEmail, "mailto:$it")) }
                    config.privacyUrl?.takeIf { it.isNotBlank() }?.let { add(Triple(LucideIcons.Shield, labelPrivacy, it)) }
                }
                if (links.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = TransitTheme.colors.bgSecondary),
                        border = BorderStroke(1.dp, TransitTheme.colors.glassBorder),
                        elevation = CardDefaults.cardElevation(0.dp),
                    ) {
                        Column {
                            links.forEachIndexed { i, (icon, label, url) ->
                                if (i > 0) HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = TransitTheme.colors.glassBorder)
                                val context = LocalContext.current
                                val displayValue = when {
                                    url.startsWith("tel:") -> url.removePrefix("tel:")
                                    url.startsWith("mailto:") -> url.removePrefix("mailto:")
                                    else -> url.removePrefix("https://").removePrefix("http://").trimEnd('/')
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                            context.startActivity(intent)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Icon(painterResource(icon), null, tint = TransitTheme.colors.accent, modifier = Modifier.size(20.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(label, style = MaterialTheme.typography.bodyMedium, color = TransitTheme.colors.textPrimary)
                                        Text(displayValue, style = MaterialTheme.typography.bodySmall, color = TransitTheme.colors.textSecondary)
                                    }
                                    Icon(painterResource(LucideIcons.ArrowRight), null, tint = TransitTheme.colors.textTertiary, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
            // Card 3: Data attribution
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = TransitTheme.colors.bgSecondary),
                    border = BorderStroke(1.dp, TransitTheme.colors.glassBorder),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(painterResource(LucideIcons.Table), null, tint = TransitTheme.colors.textTertiary, modifier = Modifier.size(16.dp))
                            Text(
                                stringResource(R.string.info_dati_trasporto),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TransitTheme.colors.textSecondary,
                            )
                        }
                        Text(
                            stringResource(R.string.info_dati_descrizione),
                            style = MaterialTheme.typography.bodySmall,
                            color = TransitTheme.colors.textSecondary,
                            lineHeight = 20.sp,
                        )
                    }
                }
            }
            // Card 4: TransitKit attribution
            item {
                TransitKitAttributionCard(
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun TransitKitAttributionCard(modifier: Modifier = Modifier) {
    val colors = TransitTheme.colors
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.bgSecondary),
        border = BorderStroke(1.dp, colors.glassBorder),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colors.accent.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(LucideIcons.BusFront),
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column {
                Text(
                    text = stringResource(R.string.info_powered_by),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textTertiary,
                )
                Text(
                    text = "TransitKit",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
            }
        }
    }
}
