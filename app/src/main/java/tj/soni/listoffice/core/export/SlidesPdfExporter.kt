package tj.soni.listoffice.core.export

import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import tj.soni.listoffice.domain.HandoutMode
import tj.soni.listoffice.domain.Slide
import tj.soni.listoffice.domain.SlidePalette
import java.io.File
import java.io.FileOutputStream

/** Слайдҳоро ба PDF мебарорад: 16:9 ё якчанд слайд дар як саҳифаи A4 (1/2/4/6, ё 3 + ёддошт). */
object SlidesPdfExporter {

    fun write(
        slides: List<Slide>,
        pal: SlidePalette,
        mode: HandoutMode,
        showNumbers: Boolean,
        images: SlideRenderer.ImageSource,
        title: String,
        out: File
    ): Int {
        val doc = PdfDocument()
        val pages = if (mode.screen) screen(doc, slides, pal, showNumbers, images)
        else handout(doc, slides, pal, mode, showNumbers, images, title)
        FileOutputStream(out).use { doc.writeTo(it) }
        doc.close()
        return pages
    }

    private fun screen(
        doc: PdfDocument, slides: List<Slide>, pal: SlidePalette, showNumbers: Boolean, images: SlideRenderer.ImageSource
    ): Int {
        slides.forEachIndexed { i, s ->
            val page = doc.startPage(PdfDocument.PageInfo.Builder(960, 540, i + 1).create())
            SlideRenderer.draw(page.canvas, s, i + 1, showNumbers, pal, images)
            doc.finishPage(page)
        }
        return slides.size
    }

    private fun handout(
        doc: PdfDocument, slides: List<Slide>, pal: SlidePalette, mode: HandoutMode,
        showNumbers: Boolean, images: SlideRenderer.ImageSource, title: String
    ): Int {
        val pw = if (mode.landscape) 842 else 595
        val ph = if (mode.landscape) 595 else 842
        val m = 28f
        val headerH = 18f
        val footerH = 18f
        val gap = 14f
        val caption = 12f
        val perPage = mode.cols * mode.rows
        val pages = (slides.size + perPage - 1) / perPage

        val small = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 9f }
        val center = TextPaint(small).apply { textAlign = Paint.Align.CENTER }
        val border = Paint().apply { style = Paint.Style.STROKE; color = 0xFF8C9A93.toInt(); strokeWidth = 0.8f }
        val ruled = Paint().apply { color = 0xFFC9D2CD.toInt(); strokeWidth = 0.7f }
        val notePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 10f }

        val gridTop = m + headerH
        val availH = ph - m - footerH - gridTop
        val rowH = (availH - (mode.rows - 1) * gap) / mode.rows
        val colW = if (mode.notes) 300f else (pw - 2 * m - (mode.cols - 1) * gap) / mode.cols
        val cellW = minOf(colW, (rowH - caption) * 16f / 9f)
        val cellH = cellW * 9f / 16f
        val gridW = if (mode.notes) pw - 2 * m else mode.cols * cellW + (mode.cols - 1) * gap
        val startX = if (mode.notes) m else m + ((pw - 2 * m) - gridW) / 2f
        val usedH = mode.rows * (cellH + caption) + (mode.rows - 1) * gap
        val startY = gridTop + (availH - usedH) / 2f

        for (p in 0 until pages) {
            val page = doc.startPage(PdfDocument.PageInfo.Builder(pw, ph, p + 1).create())
            val c = page.canvas
            c.drawColor(Color.WHITE)
            c.drawText(title.take(80), m, m - 6f, small)
            c.drawText("${p + 1} / $pages", pw / 2f, ph - m + 6f, center)
            for (k in 0 until perPage) {
                val idx = p * perPage + k
                if (idx >= slides.size) break
                val col = k % mode.cols
                val row = k / mode.cols
                val x = startX + col * (cellW + gap)
                val y = startY + row * (cellH + caption + gap)
                c.save()
                c.translate(x, y)
                c.scale(cellW / SlideRenderer.W, cellW / SlideRenderer.W)
                SlideRenderer.draw(c, slides[idx], idx + 1, showNumbers, pal, images)
                c.restore()
                c.drawRect(x, y, x + cellW, y + cellH, border)
                c.drawText("${idx + 1}", x, y + cellH + 10f, small)

                if (mode.notes) {
                    val nx = x + cellW + 16f
                    val nw = (pw - m - nx).toInt()
                    var ny = y
                    val note = slides[idx].notes.trim()
                    if (note.isNotEmpty()) {
                        val sl = StaticLayout.Builder.obtain(note, 0, note.length, notePaint, nw)
                            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(0f, 1.15f).setIncludePad(false).build()
                        c.save(); c.translate(nx, ny); c.clipRect(0f, 0f, nw.toFloat(), cellH); sl.draw(c); c.restore()
                        ny += minOf(sl.height.toFloat(), cellH) + 8f
                    } else {
                        ny += 14f
                    }
                    var ly = ny + 8f
                    while (ly < y + cellH) {
                        c.drawLine(nx, ly, nx + nw, ly, ruled)
                        ly += 20f
                    }
                }
            }
            doc.finishPage(page)
        }
        return pages
    }
}
