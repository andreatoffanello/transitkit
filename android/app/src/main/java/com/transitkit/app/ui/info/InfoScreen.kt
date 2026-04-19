package com.transitkit.app.ui.info

import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.transitkit.app.config.LucideIcons
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
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.transitkit.app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.transitkit.app.config.FareInfo
import com.transitkit.app.config.OperatorConfig
import com.transitkit.app.config.PointOfSale
import com.transitkit.app.config.TransitTheme

@Composable
fun InfoScreen(
    onNavigateToFares: (FareInfo, String?) -> Unit = { _, _ -> },
    onNavigateToOperator: (OperatorConfig) -> Unit = {},
    viewModel: InfoViewModel = hiltViewModel(),
) {
    val config = viewModel.operatorConfig
    val lastUpdated by viewModel.lastUpdated.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TransitTheme.colors.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                stringResource(R.string.tab_info),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TransitTheme.colors.textPrimary,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
        }

        // Fares section — only if types are present
        config.fares?.takeIf { it.types.isNotEmpty() }?.let { fares ->
            item {
                FaresCard(fares = fares, onClick = { onNavigateToFares(fares, config.url.takeIf { it.isNotBlank() }) })
            }
        }

        // Points of Sale section — only if list is non-empty
        config.pointsOfSale?.takeIf { it.isNotEmpty() }?.let { pois ->
            item {
                SectionHeader(icon = LucideIcons.MapPin, label = stringResource(R.string.info_section_punti_vendita))
            }
            item { PointsOfSaleCard(pois = pois) }
        }

        // Operator section header + card
        item {
            SectionHeader(icon = LucideIcons.BusFront, label = stringResource(R.string.info_section_operatore))
        }
        item { OperatorInfoCard(config = config, onClick = { onNavigateToOperator(config) }) }

        // Data section header + card
        item {
            SectionHeader(icon = LucideIcons.Table, label = stringResource(R.string.info_section_dati))
        }
        item { DataInfoCard(lastUpdated = lastUpdated) }
    }
}

@Composable
private fun SectionHeader( icon: Int, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = TransitTheme.colors.accent,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = TransitTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun FaresCard(fares: FareInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TransitTheme.colors.bgSecondary),
        border = BorderStroke(1.dp, TransitTheme.colors.glassBorder),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    painterResource(LucideIcons.Ticket),
                    contentDescription = null,
                    tint = TransitTheme.colors.accent,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    stringResource(R.string.info_section_tariffe),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TransitTheme.colors.textPrimary,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    painterResource(LucideIcons.ArrowRight),
                    contentDescription = null,
                    tint = TransitTheme.colors.textTertiary,
                    modifier = Modifier.size(16.dp),
                )
            }
            HorizontalDivider(color = TransitTheme.colors.glassBorder)
            val displayedFares = fares.types.take(3)
            displayedFares.forEachIndexed { i, fare ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            fare.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TransitTheme.colors.textPrimary,
                        )
                        fare.notes?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                color = TransitTheme.colors.textSecondary,
                            )
                        }
                    }
                    Text(
                        fare.price,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TransitTheme.colors.accent,
                    )
                }
                if (i < displayedFares.lastIndex) {
                    HorizontalDivider(color = TransitTheme.colors.glassBorder)
                }
            }
            if (fares.types.size > 3) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        stringResource(R.string.info_altre_tariffe, fares.types.size - 3),
                        style = MaterialTheme.typography.labelSmall,
                        color = TransitTheme.colors.accent,
                    )
                    Icon(
                        painterResource(LucideIcons.ChevronRight),
                        contentDescription = null,
                        tint = TransitTheme.colors.accent,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PointsOfSaleCard(pois: List<PointOfSale>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TransitTheme.colors.bgSecondary),
        border = BorderStroke(1.dp, TransitTheme.colors.glassBorder),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                Icon(
                    painterResource(LucideIcons.Store),
                    contentDescription = null,
                    tint = TransitTheme.colors.accent,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    stringResource(R.string.info_section_punti_vendita),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TransitTheme.colors.textPrimary,
                )
            }
            pois.forEachIndexed { i, poi ->
                if (i > 0) {
                    HorizontalDivider(
                        color = TransitTheme.colors.glassBorder,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        painterResource(LucideIcons.MapPin),
                        contentDescription = null,
                        tint = TransitTheme.colors.accent,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            poi.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = TransitTheme.colors.textPrimary,
                        )
                        poi.address?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                color = TransitTheme.colors.textSecondary,
                            )
                        }
                        poi.hours?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                color = TransitTheme.colors.textTertiary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OperatorInfoCard(config: OperatorConfig, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TransitTheme.colors.bgSecondary),
        border = BorderStroke(1.dp, TransitTheme.colors.glassBorder),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        TransitTheme.colors.accent.copy(alpha = 0.12f),
                        RoundedCornerShape(10.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(LucideIcons.BusFront),
                    contentDescription = null,
                    tint = TransitTheme.colors.accent,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    config.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TransitTheme.colors.textPrimary,
                )
                val subtitle = config.fullName.takeIf { it.isNotBlank() && it != config.name }
                    ?: config.region?.takeIf { it.isNotBlank() }
                    ?: config.country.takeIf { it.isNotBlank() }
                subtitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = TransitTheme.colors.textSecondary,
                    )
                }
            }
            Icon(
                painterResource(LucideIcons.ChevronRight),
                contentDescription = null,
                tint = TransitTheme.colors.textTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun formatIsoDate(iso: String?): String? {
    if (iso == null) return null
    return try {
        val input = java.time.OffsetDateTime.parse(iso)
        val fmt = java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.ITALIAN)
        input.format(fmt)
    } catch (_: Exception) { null }
}

@Composable
private fun DataInfoCard(lastUpdated: String? = null) {
    val formattedDate = formatIsoDate(lastUpdated)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TransitTheme.colors.bgSecondary),
        border = BorderStroke(1.dp, TransitTheme.colors.glassBorder),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column {
            if (formattedDate != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painterResource(LucideIcons.Clock),
                            contentDescription = null,
                            tint = TransitTheme.colors.textTertiary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            stringResource(R.string.info_ultimo_aggiornamento),
                            style = MaterialTheme.typography.bodySmall,
                            color = TransitTheme.colors.textSecondary,
                        )
                    }
                    Text(
                        formattedDate,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TransitTheme.colors.textPrimary,
                    )
                }
                HorizontalDivider(color = TransitTheme.colors.glassBorder)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painterResource(LucideIcons.Table),
                        contentDescription = null,
                        tint = TransitTheme.colors.textTertiary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        stringResource(R.string.info_sorgente_dati),
                        style = MaterialTheme.typography.bodySmall,
                        color = TransitTheme.colors.textSecondary,
                    )
                }
                Text(
                    "GTFS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TransitTheme.colors.textPrimary,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FareInfoScreen(
    fares: FareInfo,
    operatorUrl: String?,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.info_section_tariffe), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(LucideIcons.ChevronLeft), contentDescription = stringResource(R.string.cd_indietro), tint = TransitTheme.colors.accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TransitTheme.colors.background),
            )
        },
        containerColor = TransitTheme.colors.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Sticky header row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        stringResource(R.string.fare_col_tipo),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TransitTheme.colors.textTertiary,
                    )
                    Text(
                        stringResource(R.string.fare_col_prezzo),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TransitTheme.colors.textTertiary,
                    )
                }
                HorizontalDivider(color = TransitTheme.colors.glassBorder)
            }
            // Fare rows — single unified card with hairline dividers (mirrors iOS FareInfoView)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = TransitTheme.colors.bgSecondary),
                    border = BorderStroke(1.dp, TransitTheme.colors.glassBorder),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    Column {
                        fares.types.forEachIndexed { index, fare ->
                            if (index > 0) {
                                HorizontalDivider(
                                    color = TransitTheme.colors.glassBorder,
                                    modifier = Modifier.padding(start = 16.dp),
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        fare.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = TransitTheme.colors.textPrimary,
                                    )
                                    fare.notes?.let {
                                        Text(
                                            it,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TransitTheme.colors.textSecondary,
                                            modifier = Modifier.padding(top = 2.dp),
                                        )
                                    }
                                }
                                Text(
                                    fare.price,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TransitTheme.colors.accent,
                                )
                            }
                        }
                    }
                }
            }
            // Notes card
            fares.notes?.let { notes ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = TransitTheme.colors.bgSecondary),
                        border = BorderStroke(1.dp, TransitTheme.colors.glassBorder),
                        elevation = CardDefaults.cardElevation(0.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                painterResource(LucideIcons.Info),
                                null,
                                tint = TransitTheme.colors.accent,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp),
                            )
                            Text(
                                notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = TransitTheme.colors.textSecondary,
                            )
                        }
                    }
                }
            }
            // Purchase CTA
            val purchaseUrl = fares.purchaseUrl ?: operatorUrl
            purchaseUrl?.takeIf { it.isNotBlank() }?.let { url ->
                item {
                    val context = LocalContext.current
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = TransitTheme.colors.bgSecondary),
                        border = BorderStroke(1.dp, TransitTheme.colors.glassBorder),
                        elevation = CardDefaults.cardElevation(0.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse(url),
                                    )
                                    context.startActivity(intent)
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                painterResource(LucideIcons.Ticket),
                                null,
                                tint = TransitTheme.colors.accent,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                stringResource(R.string.info_acquista_online),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = TransitTheme.colors.textPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                painterResource(LucideIcons.ArrowRight),
                                null,
                                tint = TransitTheme.colors.textTertiary,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

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
                navigationIcon = { IconButton(onClick = onBack) { Icon(painterResource(LucideIcons.ChevronLeft), contentDescription = stringResource(R.string.cd_indietro), tint = TransitTheme.colors.accent) } },
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
