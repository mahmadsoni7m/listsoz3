package tj.soni.listoffice.core.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import tj.soni.listoffice.domain.Slide
import tj.soni.listoffice.domain.SlideLayout
import tj.soni.listoffice.domain.SlidePalette

/**
 * Як слайдро дар майдони мантиқии 960x540 мекашад. Ҳамон як код барои пешнамоиш, намоиш,
 * PDF ва чопи A4 истифода мешавад, то «чӣ мебинӣ, ҳамон мегирӣ».
 */
object SlideRenderer {
    const val W = 960f
    const val H = 540f

    fun interface ImageSource {
        fun bitmap(name: String): Bitmap?
    }

    private fun staticLayout(text: String, paint: TextPaint, width: Int, align: Layout.Alignment): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
            .setAlignment(align)
            .setLineSpacing(0f, 1.1f)
            .setIncludePad(false)
            .build()

    private fun textPaint(color: Int, size: Float, bold: Boolean): TextPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

    private fun fitTitle(
        text: String, color: Int, x: Float, y: Float, w: Float, h: Float,
        sizes: List<Float>, align: Layout.Alignment, anchorBottom: Boolean, c: Canvas
    ) {
        var chosen: StaticLayout? = null
        for (s in sizes) {
            val l = staticLayout(text, textPaint(color, s, true), w.toInt(), align)
            chosen = l
            if (l.height <= h) break
        }
        val l = chosen ?: return
        c.save()
        c.clipRect(x, y, x + w, y + h)
        c.translate(x, if (anchorBottom) y + h - l.height else y)
        l.draw(c)
        c.restore()
    }

    private fun drawBullets(
        c: Canvas, lines: List<String>, x: Float, y: Float, w: Float, h: Float, color: Int, accent: Int, maxSize: Float
    ) {
        val sizes = listOf(28f, 26f, 24f, 22f, 20f, 18f, 16f, 14f, 12f).filter { it <= maxSize }
        val indent = 27f
        var chosen = sizes.last()
        for (s in sizes) {
            val p = textPaint(color, s, false)
            var total = 0f
            for (l in lines) total += staticLayout(l, p, (w - indent).toInt(), Layout.Alignment.ALIGN_NORMAL).height + s * 0.45f
            if (total <= h) {
                chosen = s
                break
            }
        }
        val p = textPaint(color, chosen, false)
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = accent }
        var cy = y
        for (l in lines) {
            val sl = staticLayout(l, p, (w - indent).toInt(), Layout.Alignment.ALIGN_NORMAL)
            if (cy + sl.height > y + h + 2f) break
            c.drawCircle(x + 7f, cy + chosen * 0.6f, chosen * 0.17f, dot)
            c.save()
            c.translate(x + indent, cy)
            sl.draw(c)
            c.restore()
            cy += sl.height + chosen * 0.45f
        }
    }

    private fun drawImage(c: Canvas, b: Bitmap, dst: RectF, radius: Float) {
        val da = dst.width() / dst.height()
        val ia = b.width.toFloat() / b.height
        val src = if (ia > da) {
            val nw = (b.height * da).toInt()
            val l = (b.width - nw) / 2
            Rect(l, 0, l + nw, b.height)
        } else {
            val nh = (b.width / da).toInt()
            val t = (b.height - nh) / 2
            Rect(0, t, b.width, t + nh)
        }
        c.save()
        if (radius > 0f) {
            val p = Path()
            p.addRoundRect(dst, radius, radius, Path.Direction.CW)
            c.clipPath(p)
        } else {
            c.clipRect(dst)
        }
        c.drawBitmap(b, src, dst, Paint(Paint.FILTER_BITMAP_FLAG))
        c.restore()
    }

    fun draw(c: Canvas, slide: Slide, number: Int, showNumber: Boolean, pal: SlidePalette, images: ImageSource) {
        c.save()
        c.clipRect(0f, 0f, W, H)
        c.drawColor(pal.bg)

        val bmp = if (slide.image.isBlank()) null else images.bitmap(slide.image)
        val lines = slide.body.replace("\r", "").split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        val accentPaint = Paint().apply { color = pal.accent }
        val full = slide.layout == SlideLayout.FULL && bmp != null
        val titleLike = slide.layout == SlideLayout.TITLE || (slide.layout == SlideLayout.FULL && bmp == null)

        if (titleLike) {
            val onImage = bmp != null && slide.layout == SlideLayout.TITLE
            val tc = if (onImage) 0xFFFFFFFF.toInt() else pal.title
            val xc = if (onImage) 0xFFE6F0EA.toInt() else pal.text
            if (onImage && bmp != null) {
                drawImage(c, bmp, RectF(0f, 0f, W, H), 0f)
                c.drawRect(0f, 0f, W, H, Paint().apply { color = 0x8C000000.toInt() })
            }
            fitTitle(slide.title, tc, 72f, 144f, 816f, 136.8f, listOf(44f, 40f, 36f, 32f, 28f, 24f),
                Layout.Alignment.ALIGN_CENTER, true, c)
            c.drawRect(426f, 295.2f, 534f, 299.5f, accentPaint)
            var y = 309.6f
            val sp = textPaint(xc, 24f, false)
            for (l in lines) {
                val sl = staticLayout(l, sp, 816, Layout.Alignment.ALIGN_CENTER)
                if (y + sl.height > 309.6f + 144f) break
                c.save(); c.translate(72f, y); sl.draw(c); c.restore()
                y += sl.height + 6f
            }
        } else if (full && bmp != null) {
            drawImage(c, bmp, RectF(0f, 0f, W, H), 0f)
            c.drawRect(0f, 0f, W, H, Paint().apply { color = 0x99000000.toInt() })
            fitTitle(slide.title, 0xFFFFFFFF.toInt(), 50.4f, 230f, 859.2f, 110f, listOf(40f, 36f, 32f, 28f, 24f),
                Layout.Alignment.ALIGN_NORMAL, true, c)
            c.drawRect(50.4f, 348f, 158.4f, 352.3f, accentPaint)
            drawBullets(c, lines, 50.4f, 366f, 859.2f, 140f, 0xFFE6F0EA.toInt(), pal.accent, 24f)
        } else {
            val right = slide.layout == SlideLayout.IMG_RIGHT && bmp != null
            val left = slide.layout == SlideLayout.IMG_LEFT && bmp != null
            fitTitle(slide.title, pal.title, 50.4f, 28.8f, 859.2f, 79.2f, listOf(36f, 32f, 28f, 24f, 20f),
                Layout.Alignment.ALIGN_NORMAL, true, c)
            c.drawRect(50.4f, 115.2f, 158.4f, 119.5f, accentPaint)
            var bx = 50.4f
            var bw = 859.2f
            if (right && bmp != null) {
                bw = 453.6f
                drawImage(c, bmp, RectF(525.6f, 136.8f, 909.6f, 489.6f), 24f)
            } else if (left && bmp != null) {
                bx = 468f
                bw = 441.6f
                drawImage(c, bmp, RectF(50.4f, 136.8f, 434.4f, 489.6f), 24f)
            }
            drawBullets(c, lines, bx, 136.8f, bw, 352.8f, pal.text, pal.accent, 28f)
        }

        if (showNumber && number > 1) {
            val np = textPaint(if (full) 0xFFFFFFFF.toInt() else pal.text, 12f, false).apply {
                textAlign = Paint.Align.RIGHT
                alpha = 170
            }
            c.drawText(number.toString(), 930f, 526f, np)
        }
        c.restore()
    }

    fun render(slide: Slide, number: Int, showNumber: Boolean, pal: SlidePalette, images: ImageSource, widthPx: Int): Bitmap {
        val h = (widthPx * 9f / 16f).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(widthPx, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.scale(widthPx / W, widthPx / W)
        draw(c, slide, number, showNumber, pal, images)
        return bmp
    }
}
