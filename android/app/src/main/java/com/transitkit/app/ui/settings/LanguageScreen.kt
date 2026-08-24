package com.transitkit.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.transitkit.app.R
import com.transitkit.app.config.AppLanguage
import com.transitkit.app.config.AppLocaleManager
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme

/**
 * Selettore della lingua dell'interfaccia, pushato da Impostazioni.
 *
 * Applicare la scelta ricrea l'activity — da Android 13 lo fa il sistema,
 * sotto lo facciamo noi: la lingua delle risorse si decide in
 * `attachBaseContext`, prima che la gerarchia esista. Il back stack viene
 * ripristinato, quindi l'utente resta su questa schermata e la vede
 * tradursi sotto le dita.
 */
@Composable
fun LanguageScreen(onBack: () -> Unit = {}) {
    val colors = TransitTheme.colors
    val context = LocalContext.current
    val activity = remember(context) { context as? android.app.Activity }
    var selected by remember { mutableStateOf(AppLocaleManager.current(context)) }
    val languages = AppLanguage.entries

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(LucideIcons.ChevronLeft),
                        contentDescription = stringResource(R.string.cd_indietro),
                        tint = colors.textPrimary,
                    )
                }
                Text(
                    text = stringResource(R.string.settings_section_lingua),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        item {
            CardContainer {
                languages.forEachIndexed { index, language ->
                    val isSelected = language == selected
                    val label = language.label(context)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isSelected) {
                                selected = language
                                activity?.let { AppLocaleManager.select(it, language) }
                            }
                            .semantics { contentDescription = label }
                            .padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Box(modifier = Modifier.size(17.dp), contentAlignment = Alignment.Center) {
                            if (isSelected) {
                                Icon(
                                    painter = painterResource(LucideIcons.Check),
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                        }
                    }
                    if (index < languages.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            color = colors.separator,
                            thickness = 0.5.dp,
                        )
                    }
                }
            }
        }
    }
}
