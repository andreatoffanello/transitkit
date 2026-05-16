package com.transitkit.app.ui.orari

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.plugin.gestures.gestures
import com.transitkit.app.R
import com.transitkit.app.config.LocalTransitColors
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.ui.mappa.StopSymbolLayer
import com.transitkit.app.ui.mappa.applyTransitKitHeroStyleConfig
import kotlinx.coroutines.delay

/**
 * Hero map per StopDetailScreen — iOS parity (item #5) + Movete parity
 * (CompactMap del "Colosseo": vista 3D scenica, road labels visibili,
 * pin identico alla MapTab).
 *
 * **Single source of truth marker:** usa lo stesso [StopSymbolLayer] della
 * mappa principale ([com.transitkit.app.ui.mappa.MappaScreen]). Stop appare
 * come "selected" (1.28× scale = pin grande, glyph Signpost o "M" metro).
 *
 * **Style:** [applyTransitKitHeroStyleConfig] — abilita 3D buildings e road
 * labels (diverso dal main map che li disattiva). Camera entry a pitch ~50°
 * con fly-in cinematico (zoom 14.5 → 17) per dare il feel "wow" di Movete.
 *
 * **Gestures:** disabilitate. La hero map è una preview tappabile, non si
 * può pannare/zoomare. Tap → onExpand a fullscreen overlay.
 */
@OptIn(MapboxExperimental::class)
@Composable
internal fun StopDetailMapHero(
    stop: ResolvedStop,
    accent: Color,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTransitColors.current
    val isDark = isSystemInDarkTheme()
    val point = remember(stop.id) { Point.fromLngLat(stop.lon, stop.lat) }

    // Fly-in cinematico (Movete parity): start a 14.5/35°, dopo 500ms fly a
    // 17/50° — 1200ms cinematic.
    val viewport = rememberMapViewportState {
        setCameraOptions(
            CameraOptions.Builder()
                .center(point)
                .zoom(14.5)
                .pitch(35.0)
                .bearing(0.0)
                .build()
        )
    }
    LaunchedEffect(stop.id) {
        delay(500)
        viewport.flyTo(
            CameraOptions.Builder()
                .center(point)
                .zoom(17.0)
                .pitch(50.0)
                .bearing(0.0)
                .build()
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
            .clickable(onClick = onExpand)
            .semantics { testTag = "stop_detail_hero_map" },
    ) {
        MapboxMap(
            mapViewportState = viewport,
            style = { MapStyle(style = com.mapbox.maps.Style.STANDARD) },
            compass = {},
            scaleBar = {},
            modifier = Modifier.fillMaxSize(),
        ) {
            // Disabilita tutte le gesture: la hero è solo preview, tap esterno
            // (l'intera Box è clickable) apre l'expanded overlay.
            MapEffect(Unit) { mapView ->
                mapView.gestures.pitchEnabled = false
                mapView.gestures.scrollEnabled = false
                mapView.gestures.rotateEnabled = false
                mapView.gestures.pinchToZoomEnabled = false
                mapView.gestures.doubleTapToZoomInEnabled = false
                mapView.gestures.quickZoomEnabled = false
            }
            MapEffect(isDark) { mapView ->
                val applied = mapView.mapboxMap.style
                if (applied != null) {
                    applyTransitKitHeroStyleConfig(applied, isDark)
                } else {
                    mapView.mapboxMap.subscribeStyleLoaded {
                        mapView.mapboxMap.style?.let { applyTransitKitHeroStyleConfig(it, isDark) }
                    }
                }
            }
            // ⬇️ Single source of truth: STESSO componente StopSymbolLayer della
            // MapTab. selectedStop = stop → render 1.28× con bitmap pin + glyph
            // (Signpost o "M" metro a seconda di transitType).
            StopSymbolLayer(
                stops = listOf(stop),
                selectedStop = stop,
                selectedRoute = null,
                accentColor = accent,
            )
        }
        // Expand FAB top-end — apre overlay fullscreen.
        Surface(
            onClick = onExpand,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(36.dp)
                .semantics {
                    contentDescription = ""
                    testTag = "btn_stop_hero_expand"
                },
            shape = RoundedCornerShape(50),
            color = colors.glassFill,
            tonalElevation = 2.dp,
            shadowElevation = 4.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(LucideIcons.Maximize2),
                    contentDescription = stringResource(R.string.map_expand),
                    tint = colors.textPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
