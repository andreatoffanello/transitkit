package com.transitkit.app.ui.servizi

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.config.resolved

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactInfoScreen(
    onBack: () -> Unit,
    viewModel: ServiziViewModel = hiltViewModel(),
) {
    val config = viewModel.operatorConfig
    val contact = config.contact
    val colors = TransitTheme.colors
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.services_section_contact).lowercase()
                            .replaceFirstChar { it.uppercase() },
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(LucideIcons.ChevronLeft),
                            contentDescription = stringResource(R.string.cd_indietro),
                            tint = colors.accent,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
            )
        },
        containerColor = colors.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.bgSecondary),
                    border = BorderStroke(1.dp, colors.glassBorder),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    Column {
                        val rows = buildList {
                            contact?.phone?.takeIf { it.isNotBlank() }?.let {
                                add(
                                    ContactRowSpec(
                                        icon = LucideIcons.Phone,
                                        label = stringResource(R.string.info_link_telefono),
                                        value = it,
                                        onClick = {
                                            context.startActivity(
                                                Intent(
                                                    Intent.ACTION_DIAL,
                                                    Uri.parse("tel:${it.phoneDigits()}"),
                                                )
                                            )
                                        },
                                    )
                                )
                            }
                            contact?.email?.takeIf { it.isNotBlank() }?.let {
                                add(
                                    ContactRowSpec(
                                        icon = LucideIcons.Mail,
                                        label = stringResource(R.string.info_link_email),
                                        value = it,
                                        onClick = {
                                            context.startActivity(
                                                Intent(
                                                    Intent.ACTION_SENDTO,
                                                    Uri.parse("mailto:$it"),
                                                )
                                            )
                                        },
                                    )
                                )
                            }
                            contact?.tdd?.takeIf { it.isNotBlank() }?.let {
                                add(
                                    ContactRowSpec(
                                        icon = LucideIcons.Headphones,
                                        label = stringResource(R.string.services_label_tdd),
                                        value = it,
                                        onClick = {
                                            context.startActivity(
                                                Intent(
                                                    Intent.ACTION_DIAL,
                                                    Uri.parse("tel:${it.phoneDigits()}"),
                                                )
                                            )
                                        },
                                    )
                                )
                            }
                            contact?.address?.takeIf { it.isNotBlank() }?.let {
                                add(
                                    ContactRowSpec(
                                        icon = LucideIcons.MapPin,
                                        label = stringResource(R.string.services_label_address),
                                        value = it,
                                        onClick = {
                                            val encoded = Uri.encode(it)
                                            context.startActivity(
                                                Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("geo:0,0?q=$encoded"),
                                                )
                                            )
                                        },
                                    )
                                )
                            }
                            contact?.hours?.let { hoursMap ->
                                add(
                                    ContactRowSpec(
                                        icon = LucideIcons.Clock,
                                        label = stringResource(R.string.services_label_office_hours),
                                        value = hoursMap.resolved(),
                                        onClick = null,
                                    )
                                )
                            }
                        }
                        rows.forEachIndexed { index, row ->
                            if (index > 0) {
                                HorizontalDivider(
                                    color = colors.separator,
                                    modifier = Modifier.padding(start = 52.dp),
                                )
                            }
                            ContactRow(spec = row)
                        }
                    }
                }
            }
        }
    }
}

private data class ContactRowSpec(
    val icon: Int,
    val label: String,
    val value: String,
    val onClick: (() -> Unit)?,
)

@Composable
private fun ContactRow(spec: ContactRowSpec) {
    val colors = TransitTheme.colors
    val base = Modifier.fillMaxWidth()
    val clickable = if (spec.onClick != null) base.clickable(onClick = spec.onClick) else base
    Row(
        modifier = clickable.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(spec.icon),
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                spec.label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textTertiary,
            )
            Text(
                spec.value,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
            )
        }
        if (spec.onClick != null) {
            Icon(
                painter = painterResource(LucideIcons.ArrowRight),
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
