package com.transitkit.app.ui.mappa

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.transitkit.app.config.LucideIcons

// Tier di zoom — parità con movete Android (ZoomTier)
//   zoom < 12        → City           (nessun veicolo, stop micro)
//   12 ≤ zoom < 14   → Neighborhood   (stop square, veicoli dot)
//   zoom ≥ 14        → Street         (stop pin, veicoli dot + badge)
enum class MapZoomTier { City, Neighborhood, Street }

internal fun zoomTierFor(zoom: Double): MapZoomTier = when {
    zoom < 12.0 -> MapZoomTier.City
    zoom < 14.0 -> MapZoomTier.Neighborhood
    else -> MapZoomTier.Street
}

/**
 * Vehicle annotation stile "share location" WhatsApp:
 * - pallino colore GTFS con stroke bianca + halo pulsante (sempre)
 * - badge rettangolare sopra col route name (solo Street tier)
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
            // Tail triangle — collega badge al pallino
            Canvas(modifier = Modifier.size(width = 8.dp, height = 5.dp)) {
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width / 2f, size.height)
                    close()
                }
                drawPath(path, color = lineColor)
            }
            Spacer(Modifier.height(1.dp))
        }

        // Halo + ring + dot: single Canvas per evitare glitch di Modifier.shadow
        // annidati dentro ViewAnnotation al primo layout pass Mapbox.
        Canvas(modifier = Modifier.size(haloSize)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val haloR = size.width / 2f
            val ringR = (dotSize.toPx() + 3.dp.toPx()) / 2f
            val dotR = dotSize.toPx() / 2f

            drawCircle(
                color = lineColor.copy(alpha = haloAlpha),
                radius = haloR,
                center = center,
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.22f),
                radius = ringR + 0.5.dp.toPx(),
                center = center.copy(y = center.y + 1.dp.toPx()),
            )
            drawCircle(color = Color.White, radius = ringR, center = center)
            drawCircle(color = lineColor, radius = dotR, center = center)
        }

        // Bilancia badge(20) + tail(5) + gap(1) = 26dp così il centro composable
        // (CENTER anchor) coincide col centro del pallino.
        if (showBadge) {
            Spacer(Modifier.height(26.dp))
        }
    }
}

/**
 * Stop dot a zoom distante — rounded square. Forma diversa dai veicoli (cerchio)
 * per distinzione immediata tra infrastruttura statica e mezzi live.
 */
@Composable
fun StopDotView(tier: MapZoomTier, color: Color) {
    val size = when (tier) {
        MapZoomTier.City -> 7.dp
        MapZoomTier.Neighborhood -> 11.dp
        MapZoomTier.Street -> 11.dp
    }
    Box(
        modifier = Modifier
            .size(size)
            .background(color, RoundedCornerShape(2.dp))
            .border(1.5.dp, Color.White, RoundedCornerShape(2.dp)),
    )
}

/**
 * Stop pin street tier — rounded-rect 28/36dp colorato + icona bianca +
 * bordo bianco 2dp + triangolino bianco sotto. Parità con iOS + movete.
 *
 * `transitType` è l'Int GTFS (0=tram, 1=metro, 2=rail, 3=bus, 4=ferry, 5=cable_tram, 6=gondola, 7=funicular, 11=trolleybus, 12=monorail).
 */
@Composable
fun StopPinView(
    color: Color,
    transitType: Int,
    isSelected: Boolean,
) {
    val boxSize by animateDpAsState(
        targetValue = if (isSelected) 36.dp else 28.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "stop_pin_size",
    )
    val cornerRadius: Dp = if (isSelected) 10.dp else 7.dp
    val iconSize: Dp = if (isSelected) 17.dp else 13.dp
    val letterSize = if (isSelected) 17.sp else 13.sp

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(boxSize)
                .shadow(elevation = 3.dp, shape = RoundedCornerShape(cornerRadius))
                .background(color, RoundedCornerShape(cornerRadius))
                .border(2.dp, Color.White, RoundedCornerShape(cornerRadius)),
            contentAlignment = Alignment.Center,
        ) {
            if (transitType == 1) {
                // Metro → "M"
                Text(
                    text = "M",
                    color = Color.White,
                    fontSize = letterSize,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Icon(
                    painter = painterResource(id = LucideIcons.Signpost),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
        Canvas(modifier = Modifier.size(8.dp, 5.dp)) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
                close()
            }
            drawPath(path, color = Color.White)
        }
    }
}
