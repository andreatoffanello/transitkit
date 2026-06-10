package com.transitkit.app.ui.mappa

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.transitkit.app.config.TransitTheme

/**
 * Token unici del chrome flottante su mappa — stessi materiali del
 * [UnifiedMapControlsPill]. Ogni bottone/pill overlay su mappa DEVE
 * leggere da qui, niente valori magici nei call-site.
 */
internal object MapChromeTokens {
    /** Lato del bottone circolare singolo (M3 estende il touch target a 48dp). */
    val buttonSize: Dp = 44.dp
    val iconSize: Dp = 18.dp
    val tonalElevation: Dp = 3.dp
    val shadowElevation: Dp = 8.dp
}

/**
 * Bottone circolare canonico per azioni overlay su mappa (expand hero,
 * close fullscreen, recenter standalone). Sostituisce le reimplementazioni
 * sparse (Surface 36dp glassFill, IconButton 40dp clip+background, ecc.):
 * stesso ruolo → stesso trattamento.
 *
 * `onClick = null` → variante decorativa (es. affordance expand su una
 * preview interamente tappabile): nessun ripple, nessuna semantica click.
 */
@Composable
internal fun MapOverlayButton(
    iconRes: Int,
    contentDescription: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    buttonTestTag: String? = null,
    tint: Color = TransitTheme.colors.textPrimary,
) {
    val base = modifier
        .size(MapChromeTokens.buttonSize)
        .then(
            if (buttonTestTag != null) Modifier.semantics { testTag = buttonTestTag }
            else Modifier
        )
    val content: @Composable () -> Unit = {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(MapChromeTokens.iconSize),
            )
        }
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = base,
            shape = CircleShape,
            tonalElevation = MapChromeTokens.tonalElevation,
            shadowElevation = MapChromeTokens.shadowElevation,
            content = content,
        )
    } else {
        Surface(
            modifier = base,
            shape = CircleShape,
            tonalElevation = MapChromeTokens.tonalElevation,
            shadowElevation = MapChromeTokens.shadowElevation,
            content = content,
        )
    }
}
