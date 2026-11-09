package tj.soni.listoffice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tj.soni.listoffice.domain.Lang
import tj.soni.listoffice.domain.PresentationOutline
import tj.soni.listoffice.domain.ReportGenerator
import tj.soni.listoffice.domain.ReportType

class PresentationOutlineTest {
    @Test fun outline_fromTemplate() {
        val doc = ReportGenerator.generate(ReportType.REFERAT, Lang.TG, "Экология", "Ном", "Донишгоҳ")
        val slides = PresentationOutline.fromText(doc)
        assertEquals("Экология", slides[0].title)
        assertEquals("Ном", slides[0].body)
        assertEquals(tj.soni.listoffice.domain.SlideLayout.TITLE, slides[0].layout)
        assertTrue(slides.size >= 4)
        assertTrue(slides.any { it.title == "Муқаддима" })
        assertTrue(slides.any { it.title == "Хулоса" })
        assertTrue(slides.none { it.title.equals("Нақша", ignoreCase = true) })
    }

    @Test fun outline_fromPlainText_makesSlides() {
        val text = "Ин матни аввалин аст ва дорои якчанд ҷумла мебошад. Ҷумлаи дуюм низ ҳаст.\n\n" +
            "Банди дуюм дар ин ҷо оғоз мешавад ва матни дарозтар дорад. Ҷумлаи дигар."
        val slides = PresentationOutline.fromText(text)
        assertTrue(slides.size >= 2)
    }

    @Test fun bullets_limited() {
        val b = PresentationOutline.bullets("Ҷумлаи якум аст. Ҷумлаи дуюм аст. Ҷумлаи сеюм аст. Ҷумлаи чорум аст. Ҷумлаи панҷум аст.", 3)
        assertEquals(3, b.lines().size)
    }
}
