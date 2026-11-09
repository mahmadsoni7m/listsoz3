package tj.soni.listoffice

import org.junit.Assert.assertTrue
import org.junit.Test
import tj.soni.listoffice.core.export.PptxExporter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

class PptxExporterTest {
    private fun read(bytes: ByteArray): Map<String, String> {
        val parts = LinkedHashMap<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { z ->
            var e = z.nextEntry
            while (e != null) {
                parts[e.name] = z.readBytes().toString(Charsets.ISO_8859_1)
                e = z.nextEntry
            }
        }
        return parts
    }

    @Test fun pptx_hasRequiredParts() {
        val bos = ByteArrayOutputStream()
        PptxExporter.write(
            bos,
            listOf(
                PptxExporter.SlideData("Mavzu", "Nomi man", "TITLE", null, 0, 0),
                PptxExporter.SlideData("Intro", "Band 1\nBand 2 & <test>", "TEXT", null, 0, 0)
            ),
            "0C1712", "FFFFFF", "E6F0EA", "2F80FF", true
        )
        val parts = read(bos.toByteArray())
        listOf(
            "[Content_Types].xml", "_rels/.rels", "ppt/presentation.xml", "ppt/_rels/presentation.xml.rels",
            "ppt/slideMasters/slideMaster1.xml", "ppt/slideLayouts/slideLayout1.xml", "ppt/theme/theme1.xml",
            "ppt/slides/slide1.xml", "ppt/slides/slide2.xml"
        ).forEach { assertTrue("missing $it", parts.containsKey(it)) }
        val s2 = parts["ppt/slides/slide2.xml"] ?: ""
        assertTrue(s2.contains("Band 2 &amp; &lt;test&gt;"))
        assertTrue(s2.contains("0C1712"))
    }

    @Test fun pptx_embedsImage() {
        val bos = ByteArrayOutputStream()
        val fake = byteArrayOf(1, 2, 3, 4)
        PptxExporter.write(
            bos,
            listOf(
                PptxExporter.SlideData("A", "x", "TITLE", null, 0, 0),
                PptxExporter.SlideData("B", "y", "IMG_RIGHT", fake, 1200, 800),
                PptxExporter.SlideData("C", "z", "TEXT", fake, 1200, 800)
            ),
            "FFFFFF", "000000", "222222", "2F80FF", false
        )
        val parts = read(bos.toByteArray())
        assertTrue(parts.containsKey("ppt/media/image2.jpg"))
        assertTrue("TEXT layout must not embed image", !parts.containsKey("ppt/media/image3.jpg"))
        assertTrue((parts["ppt/slides/_rels/slide2.xml.rels"] ?: "").contains("../media/image2.jpg"))
        assertTrue((parts["ppt/slides/slide2.xml"] ?: "").contains("<p:pic>"))
    }
}
