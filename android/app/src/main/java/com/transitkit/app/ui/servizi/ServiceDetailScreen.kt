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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.config.resolved

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailScreen(
    serviceId: String,
    onBack: () -> Unit,
    onNavigateToMappa: () -> Unit,
    viewModel: ServiziViewModel = hiltViewModel(),
) {
    val config = viewModel.operatorConfig
    val service = config.services?.firstOrNull { it.id == serviceId }
    val colors = TransitTheme.colors
    val context = LocalContext.current
    val contactPhone = config.contact?.phone?.takeIf { it.isNotBlank() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        service?.title?.resolved() ?: stringResource(R.string.services_title),
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
        bottomBar = {
            service?.cta?.let { cta ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.background)
                        .navigationBarsPadding(),
                ) {
                    HorizontalDivider(color = colors.separator)
                    Button(
                        onClick = {
                            val handledInternally = cta.type == "internal" &&
                                handleInternalCtaUri(cta.value, onNavigateToMappa)
                            if (handledInternally) return@Button
                            val intent = when (cta.type) {
                                "phone" -> Intent(
                                    Intent.ACTION_DIAL,
                                    Uri.parse("tel:${cta.value.phoneDigits()}"),
                                )
                                else -> Intent(Intent.ACTION_VIEW, Uri.parse(cta.value))
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = androidx.compose.ui.graphics.Color.White,
                        ),
                    ) {
                        Icon(
                            painter = painterResource(
                                when (cta.type) {
                                    "phone"    -> LucideIcons.Phone
                                    "internal" -> LucideIcons.Map
                                    else       -> LucideIcons.ExternalLink
                                }
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(
                            cta.label.resolved(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        },
    ) { padding ->
        if (service == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.services_title),
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
            // Hero header (not a card)
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
                            painter = painterResource(iconDrawableFor(service.icon)),
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                    Text(
                        service.title.resolved(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                    Text(
                        service.subtitle.resolved(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textSecondary,
                    )
                }
            }

            // Description
            item {
                DetailCardContainer(horizontalPadding = 16.dp) {
                    Text(
                        service.description.resolved(),
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = colors.textPrimary,
                    )
                }
            }

            // Audience
            service.audience?.let { text ->
                item {
                    LabelledCard(
                        label = stringResource(R.string.services_label_audience),
                        icon = LucideIcons.Users,
                    ) {
                        Text(
                            text.resolved(),
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            color = colors.textPrimary,
                        )
                    }
                }
            }

            // Steps
            service.steps?.takeIf { it.isNotEmpty() }?.let { steps ->
                item {
                    LabelledCard(
                        label = stringResource(R.string.services_label_how),
                        icon = LucideIcons.ListOrdered,
                    ) {
                        Column {
                            steps.forEachIndexed { index, step ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        color = colors.separator,
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 0.dp),
                                    )
                                }
                                StepRow(number = index + 1, text = step.resolved())
                            }
                        }
                    }
                }
            }

            // Hours
            service.hours?.let { text ->
                item {
                    LabelledCard(
                        label = stringResource(R.string.services_label_hours),
                        icon = LucideIcons.Clock,
                    ) {
                        Text(
                            text.resolved(),
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            color = colors.textPrimary,
                        )
                    }
                }
            }

            // Fare
            service.fare?.let { text ->
                item {
                    LabelledCard(
                        label = stringResource(R.string.services_label_fare),
                        icon = LucideIcons.Ticket,
                    ) {
                        Text(
                            text.resolved(),
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            color = colors.textPrimary,
                        )
                    }
                }
            }

            // Service area
            service.serviceArea?.let { text ->
                item {
                    LabelledCard(
                        label = stringResource(R.string.services_label_area),
                        icon = LucideIcons.Map,
                    ) {
                        Text(
                            text.resolved(),
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            color = colors.textPrimary,
                        )
                    }
                }
            }

            // Notes
            service.notes?.takeIf { it.isNotEmpty() }?.let { notes ->
                item {
                    LabelledCard(
                        label = stringResource(R.string.services_label_notes),
                        icon = LucideIcons.Info,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            notes.forEach { note ->
                                NoteBulletRow(text = note.resolved())
                            }
                        }
                    }
                }
            }

            // Links
            service.links?.takeIf { it.isNotEmpty() }?.let { links ->
                item {
                    LabelledCard(
                        label = stringResource(R.string.services_label_links),
                        icon = LucideIcons.ExternalLink,
                    ) {
                        Column {
                            links.forEachIndexed { index, link ->
                                if (index > 0) {
                                    HorizontalDivider(color = colors.separator)
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
                                            context.startActivity(intent)
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Text(
                                        link.label.resolved(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.textPrimary,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Icon(
                                        painter = painterResource(LucideIcons.ArrowRight),
                                        contentDescription = null,
                                        tint = colors.textTertiary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Questions pill — hidden when a phone CTA is already pinned (avoid duplicate phone actions).
            val showQuestionsPill = contactPhone != null && service.cta?.type != "phone"
            if (showQuestionsPill) {
                item {
                    val questionsCd = stringResource(R.string.services_label_questions, contactPhone!!)
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .height(40.dp)
                            .background(
                                colors.accent.copy(alpha = 0.10f),
                                RoundedCornerShape(20.dp),
                            )
                            .clickable {
                                val intent = Intent(
                                    Intent.ACTION_DIAL,
                                    Uri.parse("tel:${contactPhone.phoneDigits()}"),
                                )
                                context.startActivity(intent)
                            }
                            .padding(horizontal = 16.dp)
                            .semantics { contentDescription = questionsCd },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            painter = painterResource(LucideIcons.Phone),
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            stringResource(R.string.services_label_questions, contactPhone),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = colors.accent,
                            textAlign = TextAlign.Start,
                        )
                    }
                }
            }

            // Spacer to keep last content clear of CTA bar
            item { Spacer(Modifier.height(if (service.cta != null) 24.dp else 40.dp)) }
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

@Composable
internal fun LabelledCard(
    label: String,
    icon: Int,
    content: @Composable () -> Unit,
) {
    val colors = TransitTheme.colors
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.textTertiary,
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.bgSecondary),
            border = BorderStroke(1.dp, colors.glassBorder),
            elevation = CardDefaults.cardElevation(0.dp),
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun DetailCardContainer(
    horizontalPadding: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit,
) {
    val colors = TransitTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.bgSecondary),
        border = BorderStroke(1.dp, colors.glassBorder),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Box(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun StepRow(number: Int, text: String) {
    val colors = TransitTheme.colors
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(colors.accent.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                number.toString(),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.accent,
            )
        }
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun NoteBulletRow(text: String) {
    val colors = TransitTheme.colors
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
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}

