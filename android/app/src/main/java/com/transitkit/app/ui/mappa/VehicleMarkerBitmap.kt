package com.transitkit.app.ui.mappa

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface

/**
 * Factory bitmap per marker veicoli registrati come `iconImage` del SymbolLayer.
 *
 * Due varianti (parità con `VehicleAnnotationView` Composable e DoVe
 * `MapMarkerBitmap.vehiclePill` / `vehicleDot`):
 *
 *  - [dot]  → cerchio colorato + ring bianco.
 *             `iconAnchor=CENTER` posiziona il centro del dot sulla coordinata GPS.
 *             Usato a zoom City/Neighborhood.
 *
 *  - [pill] → badge rettangolare (route name) + tail triangolare + cerchio dot.
 *             Il bitmap è costruito con il dot center esattamente al CENTRO
 *             VERTICALE del bitmap, aggiungendo uno spazio bilanciato sotto.
 *             Così `iconAnchor=CENTER` posiziona la coordinata GPS sul dot center.
 *             Usato a zoom Street tier.
 *
 * Parità DoVe `MapMarkerBitmap.vehiclePill` / `vehicleDot`:
 * anchoring CENTER, dot al centro verticale del bitmap.
 *
 * Stateless, nessuna cache interna — il chiamante decide quando rigenerare.
 */
internal object VehicleMarkerBitmap {

    /**
     * Dot only — cerchio colorato con ring bianco.
     * Usato a zoom City/Neighborhood (< Street tier).
     */
    fun dot(
        ctx: Context,
        fillArgb: Int,
        dotDp: Float = 13f,
        ringWidthDp: Float = 3f,
        shadowRadiusDp: Float = 3f,
    ): Bitmap {
        val density = ctx.resources.displayMetrics.density
        val dotPx = (dotDp * density).toInt()
        val ringPx = ringWidthDp * density
        val padPx = (shadowRadiusDp * density).toInt()
        val ringR = dotPx / 2f + ringPx / 2f
        val totalPx = dotPx + padPx * 2

        val bmp = Bitmap.createBitmap(totalPx, totalPx, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val cx = totalPx / 2f
        val cy = totalPx / 2f
        val dotR = dotPx / 2f

        // Drop shadow sul ring
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x80000000.toInt()
            style = Paint.Style.FILL
        }
        c.drawCircle(cx, cy + 1f * density, ringR, shadowPaint)

        // Ring bianco
        c.drawCircle(cx, cy, ringR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AColor.WHITE
            style = Paint.Style.FILL
        })

        // Dot colorato
        c.drawCircle(cx, cy, dotR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fillArgb
            style = Paint.Style.FILL
        })

        return bmp
    }

    /**
     * Badge + tail + dot centrato nel bitmap.
     *
     * Layout (top→bottom nel bitmap):
     *   [spazio bilanciato = topPad]
     *   [badge arrotondato con routeName]
     *   [tail triangolo punta verso il basso]
     *   [dot center ← QUESTO è il centro verticale del bitmap]
     *   [spazio bilanciato = topPad]
     *
     * Il dot center è posizionato esattamente a metà altezza del bitmap →
     * `iconAnchor=CENTER` allinea la coordinata GPS al dot center.
     *
     * Parità DoVe `vehiclePill`: stesso layout, stesso anchoraggio CENTER.
     */
    fun pill(
        ctx: Context,
        fillArgb: Int,
        routeName: String,
        badgeHeightDp: Float = 22f,
        badgeHPadDp: Float = 9f,
        cornerRadiusDp: Float = 11f,
        tailWidthDp: Float = 8f,
        tailHeightDp: Float = 5f,
        dotDp: Float = 13f,
        ringWidthDp: Float = 3f,
        shadowRadiusDp: Float = 3f,
    ): Bitmap {
        val density = ctx.resources.displayMetrics.density
        val padPx = (shadowRadiusDp * density)
        val dotPx = dotDp * density
        val ringPx = ringWidthDp * density
        val dotR = dotPx / 2f
        val ringR = dotR + ringPx / 2f
        val tailHpx = tailHeightDp * density
        val tailWpx = tailWidthDp * density
        val cornerPx = cornerRadiusDp * density
        val hPadPx = badgeHPadDp * density
        val badgeH = badgeHeightDp * density

        // Text paint for measuring
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AColor.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 12f * density
        }
        val textWidth = if (routeName.isNotEmpty()) textPaint.measureText(routeName) else 0f
        val badgeW = maxOf(textWidth + hPadPx * 2, badgeH)  // min width = height (circular)

        // Total ring diameter
        val ringDiam = ringR * 2f

        // Content above dot center: shadow-pad + badge + tail + ringR
        val aboveDotCenter = padPx + badgeH + tailHpx + ringR
        // Content below dot center: ringR + shadow-pad
        val belowDotCenter = ringR + padPx

        // To CENTER the dot in the bitmap, total height = 2 * max(above, below)
        // But we want the badge always on top, so:
        //   totalHeight = aboveDotCenter + belowDotCenter
        //   dotCenterY = aboveDotCenter
        //   → dotCenterY == totalHeight / 2  only if above == below
        // We force symmetric height: totalHeight = 2 * aboveDotCenter
        // which adds extra padding below (matching DoVe's approach).
        val totalHeight = (aboveDotCenter * 2f).toInt()
        val dotCenterY = aboveDotCenter  // = totalHeight / 2

        // Width = max of badge and ring, plus shadow padding
        val contentW = maxOf(badgeW, ringDiam) + padPx * 2f
        val totalWidth = contentW.toInt()
        val cx = totalWidth / 2f

        val bmp = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        // Badge rect (top of content area, centered)
        val badgeLeft = cx - badgeW / 2f
        val badgeTop = padPx
        val badgeRight = cx + badgeW / 2f
        val badgeBottom = badgeTop + badgeH

        // Shadow paint for badge
        val shadowBadge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fillArgb
            style = Paint.Style.FILL
            setShadowLayer(shadowRadiusDp * density, 0f, 1.5f * density, 0x66000000)
        }
        val badgeRect = RectF(badgeLeft, badgeTop, badgeRight, badgeBottom)
        c.drawRoundRect(badgeRect, cornerPx, cornerPx, shadowBadge)

        // Badge fill (redraw without shadow for crisp edges)
        c.drawRoundRect(badgeRect, cornerPx, cornerPx, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fillArgb
            style = Paint.Style.FILL
        })

        // Badge text
        if (routeName.isNotEmpty()) {
            val textY = badgeTop + badgeH / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
            c.drawText(routeName, cx, textY, textPaint)
        }

        // Tail triangle: base at badgeBottom, apex at (badgeBottom + tailH)
        val tailTop = badgeBottom
        val tailPath = Path().apply {
            moveTo(cx - tailWpx / 2f, tailTop)
            lineTo(cx + tailWpx / 2f, tailTop)
            lineTo(cx, tailTop + tailHpx)
            close()
        }
        c.drawPath(tailPath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fillArgb
            style = Paint.Style.FILL
        })

        // Verify geometry: tail apex should be at dotCenterY - ringR
        // (tail tip touches top of ring). Assertion (debug only):
        // assert(tailTop + tailHpx == dotCenterY - ringR) // holds by construction

        // Drop shadow on ring
        c.drawCircle(cx, dotCenterY + 1f * density, ringR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x80000000.toInt()
            style = Paint.Style.FILL
        })

        // Ring bianco
        c.drawCircle(cx, dotCenterY, ringR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AColor.WHITE
            style = Paint.Style.FILL
        })

        // Dot colorato
        c.drawCircle(cx, dotCenterY, dotR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fillArgb
            style = Paint.Style.FILL
        })

        return bmp
    }

    // ── Image ID helpers (deterministic, shared across vehicles on same line) ─

    /** ID per il bitmap dot. Pattern: "tk_veh_dot_<argb>" */
    fun dotImageId(fillArgb: Int): String = "tk_veh_dot_$fillArgb"

    /** ID per il bitmap pill. Pattern: "tk_veh_pill_<argb>_<name>" */
    fun pillImageId(fillArgb: Int, routeName: String): String =
        "tk_veh_pill_${fillArgb}_${routeName.replace(" ", "_")}"
}
