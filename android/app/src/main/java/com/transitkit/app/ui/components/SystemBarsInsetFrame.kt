package com.transitkit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.transitkit.app.config.TransitTheme

/**
 * Punto UNICO di gestione degli inset di sistema dell'Activity.
 *
 * L'app è edge-to-edge (obbligatorio con targetSdk 36 su Android 16) ma deve
 * apparire come prima: contenuto mai sotto status / navigation bar, tab bar
 * sopra la gesture bar. Qui:
 *  - la fascia di status bar (e i lati, es. cutout/3 tasti in landscape) prende
 *    il `background` della schermata, la fascia della navigation bar il
 *    `tabBarBg` opaco della tab bar che le sta sopra — gli stessi colori che
 *    prima davano statusBarColor / navigationBarColor;
 *  - il contenuto riceve `safeDrawing` (system bars + cutout + IME) come
 *    padding, che replica anche `adjustResize` con la tastiera aperta.
 *
 * `windowInsetsPadding` CONSUMA gli inset per i discendenti: i
 * `windowInsetsPadding(WindowInsets.statusBars)` / `navigationBars` sparsi
 * nelle schermate valgono 0 sotto questo frame → nessun inset doppio. Le
 * finestre separate (Dialog, ModalBottomSheet) gestiscono i propri inset.
 */
@Composable
fun SystemBarsInsetFrame(content: @Composable () -> Unit) {
    val colors = TransitTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                .background(colors.tabBarBg.copy(alpha = 1f)),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            content()
        }
    }
}
