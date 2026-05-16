package com.transitkit.app.ui.servizi

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.transitkit.app.R
import com.transitkit.app.config.AccessibilityInfo
import com.transitkit.app.config.ContactConfig
import com.transitkit.app.config.FareInfo
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.OperatorConfig
import com.transitkit.app.config.ServiceInfo
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.config.resolved

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiziScreen(
    onNavigateToService: (String) -> Unit = {},
    onNavigateToFares: (FareInfo, String?) -> Unit = { _, _ -> },
    onNavigateToAccessibility: () -> Unit = {},
    onNavigateToContact: () -> Unit = {},
    onNavigateToOperator: (OperatorConfig) -> Unit = {},
    onBack: (() -> Unit)? = null,
    viewModel: ServiziViewModel = hiltViewModel(),
) {
    val config = viewModel.operatorConfig
    val colors = TransitTheme.colors
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.services_title),
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        androidx.compose.material3.IconButton(onClick = onBack) {
                            androidx.compose.material3.Icon(
                                painter = androidx.compose.ui.res.painterResource(LucideIcons.ChevronLeft),
                                contentDescription = stringResource(R.string.cd_indietro),
                                tint = colors.textPrimary,
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = colors.background,
                    scrolledContainerColor = colors.bgSecondary,
                ),
            )
        },
        containerColor = colors.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // About-the-operator card promoted to the top (no section label).
            item {
                AboutOperatorCard(
                    config = config,
                    onClick = { onNavigateToOperator(config) },
                )
            }

            // Services list
            config.services?.takeIf { it.isNotEmpty() }?.let { services ->
                item { Spacer(Modifier.height(24.dp)) }
                item { SectionLabel(stringResource(R.string.services_section_title)) }
                item { Spacer(Modifier.height(8.dp)) }
                item {
                    ServicesCard(services = services, onServiceTap = onNavigateToService)
                }
            }

            // Fares
            config.fares?.takeIf { it.types.isNotEmpty() }?.let { fares ->
                item { Spacer(Modifier.height(24.dp)) }
                item { SectionLabel(stringResource(R.string.services_section_fares)) }
                item { Spacer(Modifier.height(8.dp)) }
                item {
                    FaresSummaryCard(
                        fares = fares,
                        onClick = {
                            onNavigateToFares(fares, config.url.takeIf { it.isNotBlank() })
                        },
                    )
                }
            }

            // Accessibility
            config.accessibility?.let { access ->
                item { Spacer(Modifier.height(24.dp)) }
                item { SectionLabel(stringResource(R.string.services_section_accessibility)) }
                item { Spacer(Modifier.height(8.dp)) }
                item {
                    AccessibilitySummaryCard(
                        access = access,
                        onClick = onNavigateToAccessibility,
                    )
                }
            }

            // Contact
            config.contact?.let { contact ->
                item { Spacer(Modifier.height(24.dp)) }
                item { SectionLabel(stringResource(R.string.services_section_contact)) }
                item { Spacer(Modifier.height(8.dp)) }
                item {
                    ContactSummaryCard(contact = contact, onClick = onNavigateToContact)
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

// ---------------------------------------------------------------------------
// Section label
// ---------------------------------------------------------------------------

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = TransitTheme.colors.textTertiary,
        modifier = Modifier.padding(start = 4.dp),
    )
}

// ---------------------------------------------------------------------------
// Services card
// ---------------------------------------------------------------------------

@Composable
private fun ServicesCard(
    services: List<ServiceInfo>,
    onServiceTap: (String) -> Unit,
) {
    val colors = TransitTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.bgSecondary),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column {
            services.forEachIndexed { index, service ->
                if (index > 0) {
                    HorizontalDivider(
                        color = colors.separator,
                        modifier = Modifier.padding(start = 78.dp),
                    )
                }
                ServiceRow(service = service, onClick = { onServiceTap(service.id) })
            }
        }
    }
}

@Composable
private fun ServiceRow(service: ServiceInfo, onClick: () -> Unit) {
    val colors = TransitTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(colors.accent.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconDrawableFor(service.icon)),
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                service.title.resolved(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
            )
            Text(
                service.subtitle.resolved(),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            painter = painterResource(LucideIcons.ChevronRight),
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(18.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Fares summary
// ---------------------------------------------------------------------------

private fun FareInfo.isFareFree(): Boolean {
    if (types.isEmpty()) return true
    return types.all { it.price.isFareFree() }
}

private fun String.isFareFree(): Boolean {
    val normalized = trim().lowercase()
    return normalized == "free" ||
        normalized == "gratis" ||
        normalized == "gratuito" ||
        normalized == "$0" ||
        normalized == "€0" ||
        normalized == "0" ||
        normalized == "$0.00"
}

@Composable
private fun FaresSummaryCard(fares: FareInfo, onClick: () -> Unit) {
    val colors = TransitTheme.colors
    val summary = if (fares.isFareFree()) {
        stringResource(R.string.services_fare_free)
    } else {
        val lowest = fares.types.minByOrNull { priceSortKey(it.price) }?.price
        if (lowest != null) stringResource(R.string.services_fare_from, lowest)
        else stringResource(R.string.services_section_fares)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.bgSecondary),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(colors.accent.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(LucideIcons.Ticket),
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.info_section_tariffe),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                painter = painterResource(LucideIcons.ChevronRight),
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Rough price sort: extracts the first number in the string; empty/unknown sort last. */
private fun priceSortKey(price: String): Double {
    val number = Regex("""-?\d+(?:[.,]\d+)?""").find(price)?.value?.replace(',', '.')
    return number?.toDoubleOrNull() ?: Double.MAX_VALUE
}

// ---------------------------------------------------------------------------
// Accessibility summary
// ---------------------------------------------------------------------------

@Composable
private fun AccessibilitySummaryCard(
    access: AccessibilityInfo,
    onClick: () -> Unit,
) {
    val colors = TransitTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.bgSecondary),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(colors.accent.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(LucideIcons.Accessibility),
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    access.title.resolved(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
                Text(
                    access.description.resolved(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                painter = painterResource(LucideIcons.ChevronRight),
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier
                    .size(18.dp)
                    .padding(top = 4.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Contact summary
// ---------------------------------------------------------------------------

@Composable
private fun ContactSummaryCard(contact: ContactConfig, onClick: () -> Unit) {
    val colors = TransitTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.bgSecondary),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column {
            val rows = buildList {
                contact.phone?.takeIf { it.isNotBlank() }?.let { add(LucideIcons.Phone to it) }
                contact.email?.takeIf { it.isNotBlank() }?.let { add(LucideIcons.Mail to it) }
                contact.address?.takeIf { it.isNotBlank() }?.let { add(LucideIcons.MapPin to it) }
            }
            rows.forEachIndexed { index, (icon, value) ->
                if (index > 0) {
                    HorizontalDivider(
                        color = colors.separator,
                        modifier = Modifier.padding(start = 48.dp),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (index == rows.lastIndex) {
                        Icon(
                            painter = painterResource(LucideIcons.ChevronRight),
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// About operator
// ---------------------------------------------------------------------------

@Composable
private fun AboutOperatorCard(config: OperatorConfig, onClick: () -> Unit) {
    val colors = TransitTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.bgSecondary),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(colors.accent.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(LucideIcons.Info),
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                stringResource(R.string.services_about_operator_fmt, config.name),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(LucideIcons.ChevronRight),
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
