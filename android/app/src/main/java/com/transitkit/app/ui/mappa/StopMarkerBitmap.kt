package com.transitkit.app.ui.mappa

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

/**
 * Factory bitmap per i marker fermate registrati come `iconImage` del
 * SymbolLayer (Mapbox 11.x). Stateless: niente cache interno — il chiamante
 * decide quando rigenerare (cambio colore selezione, cambio tema).
 *
 * Le forme replicano 1:1 i Composable precedenti (`StopDotView`,
 * `StopPinView`) per non-regressione visiva fermate.
 */
internal object StopMarkerBitmap {

    /**
     * Rounded square piccolo (tier City/Neighborhood). Shadow inclusa nel
     * bitmap: senza, il dot 11dp si confonde con la basemap Mapbox Standard.
     */
    fun dot(
        ctx: Context,
        fillArgb: Int,
        sizeDp: Float = 11f,
        cornerRadiusDp: Float = 2f,
        borderArgb: Int = AColor.WHITE,
        borderWidthDp: Float = 1.5f,
        shadowRadiusDp: Float = 3f,
    ): Bitmap {
        val density = ctx.resources.displayMetrics.density
        val coreSizePx = (sizeDp * density).toInt()
        val padPx = (shadowRadiusDp * density).toInt()
        val totalPx = coreSizePx + padPx * 2
        val borderPx = borderWidthDp * density
        val radiusPx = cornerRadiusDp * density
        val bmp = Bitmap.createBitmap(totalPx, totalPx, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val rect = RectF(
            padPx + borderPx / 2f,
            padPx + borderPx / 2f,
            padPx + coreSizePx - borderPx / 2f,
            padPx + coreSizePx - borderPx / 2f,
        )

        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fillArgb
            style = Paint.Style.FILL
            setShadowLayer(shadowRadiusDp * density, 0f, 1f * density, 0x80000000.toInt())
        }
        c.drawRoundRect(rect, radiusPx, radiusPx, fill)

        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderArgb
            style = Paint.Style.STROKE
            strokeWidth = borderPx
        }
        c.drawRoundRect(rect, radiusPx, radiusPx, border)
        return bmp
    }

    /**
     * Pill bianco con bordo soft per badge label fermata. Pensato per
     * `iconTextFit(BOTH)`: i punti `stretchX`/`stretchY` definiscono
     * l'area centrale stretchable, così il bitmap si adatta in larghezza
     * al testo senza deformare i bordi rounded.
     *
     * Return: Triple(bitmap, stretchAreaX, stretchAreaY) — il caller passa
     * gli `ImageStretches` allo `addImage` Mapbox.
     */
    fun labelPill(
        ctx: Context,
        widthDp: Float = 40f,
        heightDp: Float = 20f,
        cornerRadiusDp: Float = 4f,
        bgArgb: Int = AColor.WHITE,
        borderArgb: Int = 0xFFE5E7EB.toInt(),
        borderWidthDp: Float = 0.75f,
        shadowRadiusDp: Float = 2f,
    ): Bitmap {
        val density = ctx.resources.displayMetrics.density
        val padPx = (shadowRadiusDp * density).toInt()
        val coreWpx = (widthDp * density).toInt()
        val coreHpx = (heightDp * density).toInt()
        val totalW = coreWpx + padPx * 2
        val totalH = coreHpx + padPx * 2
        val borderPx = borderWidthDp * density
        val radiusPx = cornerRadiusDp * density

        val bmp = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val rect = RectF(
            padPx + borderPx / 2f,
            padPx + borderPx / 2f,
            padPx + coreWpx - borderPx / 2f,
            padPx + coreHpx - borderPx / 2f,
        )
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgArgb
            style = Paint.Style.FILL
            setShadowLayer(shadowRadiusDp * density, 0f, 1f * density, 0x33000000)
        }
        c.drawRoundRect(rect, radiusPx, radiusPx, fill)
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderArgb
            style = Paint.Style.STROKE
            strokeWidth = borderPx
        }
        c.drawRoundRect(rect, radiusPx, radiusPx, border)
        return bmp
    }

    /**
     * Pin Street tier: rounded rect colorato + bordo bianco + tail triangle
     * sotto. Il tail è incluso nel bitmap → `iconAnchor=BOTTOM` allinea la
     * punta alla coordinata GPS.
     *
     * @param glyph se non null, disegna il carattere (es. "M") al centro del rect.
     * @param iconRes se non null, disegna il VectorDrawable tinted al centro
     *   del rect. Mutuamente esclusivo con [glyph]. Per fermate non-metro
     *   passare `LucideIcons.Signpost` per parità con `StopPinView`.
     */
    fun pin(
        ctx: Context,
        fillArgb: Int,
        sizeDp: Float = 28f,
        cornerRadiusDp: Float = 7f,
        borderArgb: Int = AColor.WHITE,
        borderWidthDp: Float = 2f,
        tailWidthDp: Float = 8f,
        tailHeightDp: Float = 5f,
        glyph: String? = null,
        glyphArgb: Int = AColor.WHITE,
        glyphSizeDp: Float = 13f,
        @DrawableRes iconRes: Int? = null,
        iconSizeDp: Float = 13f,
        iconTintArgb: Int = AColor.WHITE,
        shadowRadiusDp: Float = 3f,
    ): Bitmap {
        val density = ctx.resources.displayMetrics.density
        val rectSizePx = (sizeDp * density).toInt()
        val tailHpx = (tailHeightDp * density).toInt()
        val tailWpx = tailWidthDp * density
        val borderPx = borderWidthDp * density
        val radiusPx = cornerRadiusDp * density
        val padPx = (shadowRadiusDp * density).toInt()

        val widthPx = rectSizePx + padPx * 2
        val heightPx = rectSizePx + tailHpx + padPx * 2
        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        val rect = RectF(
            padPx + borderPx / 2f,
            padPx + borderPx / 2f,
            padPx + rectSizePx - borderPx / 2f,
            padPx + rectSizePx - borderPx / 2f,
        )
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fillArgb
            style = Paint.Style.FILL
            setShadowLayer(shadowRadiusDp * density, 0f, 1.5f * density, 0x66000000)
        }
        c.drawRoundRect(rect, radiusPx, radiusPx, fill)

        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderArgb
            style = Paint.Style.STROKE
            strokeWidth = borderPx
        }
        c.drawRoundRect(rect, radiusPx, radiusPx, border)

        // Tail triangle bianco — punta al centro-bottom.
        val tailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderArgb
            style = Paint.Style.FILL
        }
        val cx = widthPx / 2f
        val tailTop = (padPx + rectSizePx).toFloat()
        val path = Path().apply {
            moveTo(cx - tailWpx / 2f, tailTop)
            lineTo(cx + tailWpx / 2f, tailTop)
            lineTo(cx, tailTop + tailHpx)
            close()
        }
        c.drawPath(path, tailPaint)

        when {
            !glyph.isNullOrEmpty() -> {
                val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = glyphArgb
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textSize = glyphSizeDp * density
                }
                val cyRect = padPx + rectSizePx / 2f
                val baseline = cyRect - (tp.descent() + tp.ascent()) / 2f
                c.drawText(glyph, cx, baseline, tp)
            }
            iconRes != null -> {
                val drawable = ContextCompat.getDrawable(ctx, iconRes)?.mutate()
                if (drawable != null) {
                    drawable.colorFilter = PorterDuffColorFilter(iconTintArgb, PorterDuff.Mode.SRC_IN)
                    val iconPx = (iconSizeDp * density).toInt()
                    val cyRect = padPx + rectSizePx / 2f
                    val left = (cx - iconPx / 2f).toInt()
                    val top = (cyRect - iconPx / 2f).toInt()
                    drawable.setBounds(left, top, left + iconPx, top + iconPx)
                    drawable.draw(c)
                }
            }
        }
        return bmp
    }
}
