package com.transitkit.app.ui.mappa

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// MapZoomTier e soglie in MapZoomLevels.kt — single source of truth.

/**
 * Vehicle annotation stile "share location" WhatsApp:
 * - pallino colore GTFS con stroke bianca + halo pulsante (sempre)
 * - badge rettangolare sopra col route name (solo Street tier)
 *
 * iOS parity (item #6): a tier Street il dot deve cadere PER ESATTO al centro
 * del composable, così che l'anchor `CENTER` Mapbox coincida con la coordinata
 * GPS. Inoltre badge → tail → ring devono essere flush (zero gap), altrimenti
 * sembrano tre elementi staccati.
 *
 * Fix:
 *  1. Niente spacer tra badge e tail (erano 1dp fluttuanti).
 *  2. Quando il badge è visibile, la circle del dot/ring viene disegnata in
 *     CIMA al Canvas (center.y = ringRadius) invece che al centro — così il
 *     bordo superiore del ring coincide col tip del tail. L'halo si espande
 *     verso il basso (effetto pulse "che cala").
 *  3. Balance spacer sotto il Canvas calcolato così che il centro verticale
 *     della Column coincida con la y del dot. Math: con badge ~22dp + tail
 *     5dp + Canvas haloSize, balance = haloSize/2 − ringR + (haloSize/2)
 *     equivalentemente `2 * ringR` in più rispetto al solo halo bottom.
 *
 * L'alpha dell'halo è hoisted — una sola InfiniteTransition in MappaScreen,
 * passata a tutti i veicoli.
 */
@Composable
fun VehicleAnnotationView(
    lineColor: Color,
    routeName: String,
    tier: MapZoomTier,
    haloAlpha: Float,
) {
    val dotSize: Dp = when (tier) {
        MapZoomTier.City -> 8.dp
        MapZoomTier.Neighborhood -> 11.dp
        MapZoomTier.Street -> 13.dp
    }
    val haloSize: Dp = when (tier) {
        MapZoomTier.City -> 18.dp
        MapZoomTier.Neighborhood -> 24.dp
        MapZoomTier.Street -> 30.dp
    }
    val showBadge = tier == MapZoomTier.Street && routeName.isNotEmpty()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (showBadge) {
            Box(
                modifier = Modifier
                    .shadow(5.dp, RoundedCornerShape(11.dp))
                    .background(lineColor, RoundedCornerShape(11.dp))
                    .padding(horizontal = 9.dp, vertical = 4.dp),
            ) {
                Text(
                    text = routeName,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
            // Tail triangle — collega badge al pallino. Niente Spacer prima:
            // il tail tip deve toccare la sommità del ring del dot (no gap).
            Canvas(modifier = Modifier.size(width = 8.dp, height = 5.dp)) {
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width / 2f, size.height)
                    close()
                }
                drawPath(path, color = lineColor)
            }
        }

        // Halo + ring + dot in un singolo Canvas.
        // Conditional center.y:
        //   - senza badge → centro del canvas (dot al centro composable).
        //   - con badge   → ringR dall'alto, così il bordo top del ring/dot
        //                   coincide col tail tip (flush). L'halo si espande
        //                   verso il basso.
        Canvas(modifier = Modifier.size(haloSize)) {
            val ringR = (dotSize.toPx() + 3.dp.toPx()) / 2f
            val dotR = dotSize.toPx() / 2f
            val centerX = size.width / 2f
            val centerY = if (showBadge) ringR else size.height / 2f
            val center = Offset(centerX, centerY)

            // Halo (pulsing alpha) — sempre raggio = haloSize/2 dal centro
            // logico del puck (non del canvas), così la pulse "respira" attorno
            // al dot indipendentemente dallo shift verticale.
            drawCircle(
                color = lineColor.copy(alpha = haloAlpha),
                radius = size.width / 2f,
                center = center,
            )
            // Drop shadow del ring
            drawCircle(
                color = Color.Black.copy(alpha = 0.22f),
                radius = ringR + 0.5.dp.toPx(),
                center = center.copy(y = center.y + 1.dp.toPx()),
            )
            // Ring bianco
            drawCircle(color = Color.White, radius = ringR, center = center)
            // Dot colorato
            drawCircle(color = lineColor, radius = dotR, center = center)
        }

        // Balance spacer (solo con badge): bilancia il fatto che il dot è in
        // alto nel Canvas haloSize. Math: centro Column = altezza_totale/2
        // deve = posizione dot. Con badge (~22dp) + tail (5dp) + Canvas
        // (haloSize), e dot a (badge+tail+ringR) dall'alto, lo spazio sotto
        // il Canvas deve essere (haloSize − 2*ringR). Con Street tier:
        // haloSize=30, dotSize=13 → ringR=8 → balance = 30 − 16 = 14dp.
        // Approssima il valore iOS spec "pill_height − 9 = 11pt" adattato
        // ai dp Android.
        if (showBadge) {
            val balanceDp = haloSize - (dotSize + 3.dp) // haloSize − 2*ringR
            Spacer(Modifier.height(balanceDp))
        }
    }
}

// MARK: - StopDotView / StopPinView rimossi —
// le fermate sono ora rese via Canvas bitmap (vedi StopMarkerBitmap.kt)
// e registrate nel SymbolLayer (vedi StopSymbolLayer.kt).
