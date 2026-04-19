package com.transitkit.app.ui.servizi

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
fun AccessibilityInfoScreen(
    onBack: () -> Unit,
    viewModel: ServiziViewModel = hiltViewModel(),
) {
    val config = viewModel.operatorConfig
    val access = config.accessibility
    val colors = TransitTheme.colors
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        access?.title?.resolved() ?: stringResource(R.string.services_section_accessibility),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
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
        if (access == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.services_section_accessibility),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textSecondary,
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Hero
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(colors.accent.copy(alpha = 0.14f), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(LucideIcons.Accessibility),
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                    Text(
                        access.title.resolved(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                }
            }

            // Description
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.bgSecondary),
                    border = BorderStroke(1.dp, colors.glassBorder),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    Text(
                        access.description.resolved(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            // Bullets
            if (access.bullets.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.bgSecondary),
                        border = BorderStroke(1.dp, colors.glassBorder),
                        elevation = CardDefaults.cardElevation(0.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            access.bullets.forEach { bullet ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 7.dp)
                                            .size(6.dp)
                                            .background(colors.accent, CircleShape),
                                    )
                                    Text(
                                        bullet.resolved(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.textPrimary,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Learn more
            access.moreUrl?.takeIf { it.isNotBlank() }?.let { url ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.bgSecondary),
                        border = BorderStroke(1.dp, colors.glassBorder),
                        elevation = CardDefaults.cardElevation(0.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                painter = painterResource(LucideIcons.ExternalLink),
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                stringResource(R.string.services_learn_more),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                painter = painterResource(LucideIcons.ArrowRight),
                                contentDescription = null,
                                tint = colors.textTertiary,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
