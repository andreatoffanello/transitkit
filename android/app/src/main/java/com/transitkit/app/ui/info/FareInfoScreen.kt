package com.transitkit.app.ui.info

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
import com.transitkit.app.R
import com.transitkit.app.config.FareInfo
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme

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
                        Icon(painterResource(LucideIcons.ChevronLeft), contentDescription = stringResource(R.string.cd_indietro), tint = TransitTheme.colors.textPrimary)
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
