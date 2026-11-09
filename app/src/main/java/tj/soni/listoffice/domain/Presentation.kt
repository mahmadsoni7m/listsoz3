package tj.soni.listoffice.domain

/** Тарҳбандии слайд. */
enum class SlideLayout(val label: String) {
    TITLE("Сарлавҳа"),
    TEXT("Матн"),
    IMG_RIGHT("Сурат рост"),
    IMG_LEFT("Сурат чап"),
    FULL("Сурат пурра")
}

/** Формати баровардан/чоп: «экран» (16:9) ё якчанд слайд дар як саҳифаи A4. */
enum class HandoutMode(
    val label: String,
    val cols: Int,
    val rows: Int,
    val landscape: Boolean,
    val notes: Boolean,
    val screen: Boolean = false
) {
    SCREEN("16:9 — барои экран ва мубодила", 1, 1, true, false, true),
    A4_1("A4: 1 слайд дар саҳифа", 1, 1, true, false),
    A4_2("A4: 2 слайд дар саҳифа", 1, 2, false, false),
    A4_4("A4: 4 слайд дар саҳифа", 2, 2, true, false),
    A4_6("A4: 6 слайд дар саҳифа", 2, 3, false, false),
    A4_NOTES("A4: 3 слайд + ёддошт", 1, 3, false, true)
}

/**
 * Як слайд: сарлавҳа + матн (ҳар сатр = як банд). Слайди TITLE: body = зерсарлавҳа.
 * [image] — номи файли сурат дар хотираи барнома ("" = бе сурат). [notes] — ёддошти баромадкунанда.
 */
data class Slide(
    val title: String,
    val body: String,
    val layout: SlideLayout = SlideLayout.TEXT,
    val image: String = "",
    val notes: String = ""
)

/** Қолаби рангҳо; рангҳо ҳамчун ARGB Int. */
data class SlidePalette(val name: String, val bg: Int, val title: Int, val text: Int, val accent: Int) {
    fun hex(color: Int): String = String.format("%06X", color and 0xFFFFFF)
}

object SlidePalettes {
    val all: List<SlidePalette> = listOf(
        SlidePalette("Сабзи шаб", 0xFF0C1712.toInt(), 0xFFFFFFFF.toInt(), 0xFFE6F0EA.toInt(), 0xFF2F80FF.toInt()),
        SlidePalette("Равшан", 0xFFFFFFFF.toInt(), 0xFF0B2A4A.toInt(), 0xFF1F2937.toInt(), 0xFF2F80FF.toInt()),
        SlidePalette("Уқёнус", 0xFF0A1F4D.toInt(), 0xFFFFFFFF.toInt(), 0xFFDCE6FF.toInt(), 0xFF5CC8FF.toInt()),
        SlidePalette("Гарм", 0xFFFFF7E8.toInt(), 0xFF5A2E0E.toInt(), 0xFF3B2A1A.toInt(), 0xFFE07A1F.toInt()),
        SlidePalette("Бунафш", 0xFF1B1033.toInt(), 0xFFFFFFFF.toInt(), 0xFFE9E1FF.toInt(), 0xFFB388FF.toInt()),
        SlidePalette("Бахор", 0xFFF4FFE9.toInt(), 0xFF12361F.toInt(), 0xFF24402D.toInt(), 0xFF2E9E4F.toInt())
    )
}

/** Сохтани нақшаи слайдҳо аз матни ҳуҷҷат (офлайн, бе AI). */
object PresentationOutline {
    private val skipHeadings = setOf(
        "нақша", "план", "outline", "рӯйхати адабиёт", "список литературы", "references"
    )
    private val topicKeys = listOf("Мавзӯъ:", "Тема:", "Topic:")
    private val authorKeys = listOf("Иҷрокунанда:", "Исполнитель:", "Author:")
    private val numbered = Regex("^\\d+\\.\\s+(.{3,80})$")
    private const val MAX_SLIDES = 30

    private fun isUpperHeading(l: String): Boolean =
        l.length in 1..60 && l.any { it.isLetter() } && l.none { it.isLowerCase() }

    private fun shorten(s: String, n: Int): String =
        if (s.length <= n) s else s.take(n - 1).trimEnd() + "…"

    fun bullets(text: String, maxBullets: Int = 4): String =
        text.split(Regex("(?<=[.!?])\\s+|\\n+"))
            .map { it.trim() }
            .filter { it.length >= 8 }
            .take(maxBullets)
            .joinToString("\n") { shorten(it, 120) }

    private fun cap(h: String): String = h.lowercase().replaceFirstChar { it.uppercase() }

    fun fromText(text: String, maxBullets: Int = 4): List<Slide> {
        val lines = text.replace('\u000C', '\n').replace("\r", "").lines().map { it.trim() }
        var topic = ""
        var author = ""
        for (l in lines) {
            topicKeys.firstOrNull { l.startsWith(it, ignoreCase = true) }?.let { k ->
                if (topic.isEmpty()) topic = l.substring(k.length).trim().trim('«', '»', '"')
            }
            authorKeys.firstOrNull { l.startsWith(it, ignoreCase = true) }?.let { k ->
                if (author.isEmpty()) author = l.substring(k.length).trim()
            }
        }
        if (topic.isEmpty()) topic = lines.firstOrNull { it.isNotEmpty() } ?: "Презентатсия"
        val result = mutableListOf(Slide(shorten(topic, 90), author, SlideLayout.TITLE))

        var heading: String? = null
        var firstUpperSeen = false
        var dropSection = false
        val buf = StringBuilder()

        fun flush() {
            val h = heading
            if (h != null && !dropSection && buf.isNotBlank() && h.lowercase() !in skipHeadings && result.size < MAX_SLIDES) {
                val b = bullets(buf.toString(), maxBullets)
                if (b.isNotEmpty()) result.add(Slide(shorten(cap(h), 80), b))
            }
            buf.clear()
        }

        for (l in lines) {
            if (l.isEmpty()) continue
            if (isUpperHeading(l)) {
                flush()
                heading = l
                dropSection = !firstUpperSeen
                firstUpperSeen = true
                continue
            }
            val m = numbered.find(l)
            if (m != null && !l.endsWith(".") && !l.endsWith("!") && !l.endsWith("?")) {
                flush()
                heading = m.groupValues[1]
                dropSection = false
                continue
            }
            buf.append(l).append('\n')
        }
        flush()

        if (result.size == 1) {
            val paras = text.replace('\u000C', '\n').split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.length >= 20 }
            for (p in paras.take(MAX_SLIDES - 1)) {
                val title = p.split(Regex("\\s+")).take(6).joinToString(" ")
                result.add(Slide(shorten(title, 60), bullets(p, maxBullets)))
            }
        }
        return result
    }
}
