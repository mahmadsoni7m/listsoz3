package tj.soni.listoffice

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import tj.soni.listoffice.core.export.DocxExporter
import tj.soni.listoffice.core.export.PdfExporter
import tj.soni.listoffice.core.export.PptxExporter
import tj.soni.listoffice.core.export.SlideRenderer
import tj.soni.listoffice.core.export.SlidesPdfExporter
import tj.soni.listoffice.core.ui.openExternal
import tj.soni.listoffice.data.FileRepository
import tj.soni.listoffice.data.Prefs
import tj.soni.listoffice.data.SlideImageStore
import tj.soni.listoffice.domain.HandoutMode
import tj.soni.listoffice.domain.Lang
import tj.soni.listoffice.domain.PresentationOutline
import tj.soni.listoffice.domain.ReportGenerator
import tj.soni.listoffice.domain.ReportType
import tj.soni.listoffice.domain.Slide
import tj.soni.listoffice.domain.SlideLayout
import tj.soni.listoffice.domain.SlidePalettes
import tj.soni.listoffice.domain.TextTools
import tj.soni.listoffice.feature.handwriting.HandwritingRenderer
import tj.soni.listoffice.feature.handwriting.HwLayout
import tj.soni.listoffice.feature.handwriting.HwParams
import tj.soni.listoffice.feature.handwriting.Paper
import tj.soni.listoffice.feature.printer.PrintHelper
import java.io.File

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = FileRepository(app)
    private val prefs = Prefs(app)
    private val images = SlideImageStore(app)
    private val imageSource = SlideRenderer.ImageSource { name -> images.bitmap(name) }
    val renderer = HandwritingRenderer(app)

    private val _files = MutableStateFlow<List<File>>(emptyList())
    val files: StateFlow<List<File>> = _files.asStateFlow()

    // Навигатсия: 0 асосӣ, 1 ҳуҷҷат, 2 слайд, 3 дастнавис, 4 файлҳо
    var tab by mutableIntStateOf(0)
    var viewing by mutableStateOf<File?>(null)
    var message by mutableStateOf<String?>(null)
    var busy by mutableStateOf(false)
    var showSettings by mutableStateOf(false)

    // Document Studio
    var docName by mutableStateOf("Ҳуҷҷати нав")
    var docText by mutableStateOf("")
    var docFontSize by mutableIntStateOf(1)
    val docFontSizes = listOf(12, 14, 16)
    var templateRequest by mutableStateOf(false)
    private var saveJob: Job? = null

    // Presentation Studio
    val slides = mutableStateListOf<Slide>()
    var slideIdx by mutableIntStateOf(0)
    var presPalette by mutableIntStateOf(0)
    var presNumbers by mutableStateOf(true)
    var slideshow by mutableStateOf(false)

    // Handwriting Studio
    var hwText by mutableStateOf("")
    var hwStyle by mutableIntStateOf(0)
    var hwPaper by mutableIntStateOf(1)
    var hwMargin by mutableStateOf(false)
    var hwLeft by mutableFloatStateOf(20f)
    var hwRight by mutableFloatStateOf(10f)
    var hwTop by mutableFloatStateOf(15f)
    var hwBottom by mutableFloatStateOf(15f)
    var hwIndent by mutableStateOf(true)
    var hwCenter by mutableStateOf(true)
    var hwPageNum by mutableStateOf(true)
    var hwSizeScale by mutableFloatStateOf(1f)
    var hwInk by mutableIntStateOf(0)
    var hwCustomSize by mutableFloatStateOf(46f)
    var hwCustomRot by mutableFloatStateOf(2f)
    var hwCustomJitter by mutableFloatStateOf(2f)
    var hwPreview by mutableStateOf<List<Bitmap>>(emptyList())
    var fontWarning by mutableStateOf<String?>(null)
    var fontStatus by mutableStateOf<Map<String, Boolean>>(emptyMap())

    init {
        loadSettings()
        loadSlides()
        refresh()
        viewModelScope.launch(Dispatchers.Default) {
            val st = listOf("natural.ttf", "beautiful.ttf", "clean.ttf", "soni.ttf").associateWith { renderer.coversTajik(it) }
            withContext(Dispatchers.Main) {
                fontStatus = st
                val bad = st.filterValues { !it }.keys
                fontWarning = if (bad.isEmpty()) null else
                    "Шрифтҳои дастнавис (${bad.joinToString()}) дар APK нестанд ё кириллиро надоранд, барои ҳамин хати оддии системавӣ истифода мешавад. " +
                        "Дар GitHub Actions шрифтҳо худкор зеркашӣ мешаванд; build-ро аз нав иҷро кунед ва APK-и навро насб намоед."
            }
        }
        viewModelScope.launch {
            snapshotFlow {
                listOf<Any>(
                    hwStyle, hwPaper, hwMargin, hwLeft, hwRight, hwTop, hwBottom, hwIndent, hwCenter,
                    hwPageNum, hwSizeScale, hwInk, hwCustomSize, hwCustomRot, hwCustomJitter,
                    docFontSize, presPalette, presNumbers, docName
                )
            }.collect { saveSettings() }
        }
        viewModelScope.launch {
            snapshotFlow { slides.toList() }.collect { saveSlides() }
        }
    }

    // ---------- Танзимот ----------

    private fun loadSettings() {
        hwStyle = prefs.getInt("hwStyle", 0).coerceIn(0, 4)
        hwPaper = prefs.getInt("hwPaper", 1).coerceIn(0, Paper.values().lastIndex)
        hwMargin = prefs.getBool("hwMargin", false)
        hwLeft = prefs.getFloat("hwLeft", 20f)
        hwRight = prefs.getFloat("hwRight", 10f)
        hwTop = prefs.getFloat("hwTop", 15f)
        hwBottom = prefs.getFloat("hwBottom", 15f)
        hwIndent = prefs.getBool("hwIndent", true)
        hwCenter = prefs.getBool("hwCenter", true)
        hwPageNum = prefs.getBool("hwPageNum", true)
        hwSizeScale = prefs.getFloat("hwSizeScale", 1f)
        hwInk = prefs.getInt("hwInk", 0).coerceIn(0, HandwritingRenderer.INK_COLORS.lastIndex)
        hwCustomSize = prefs.getFloat("hwCustomSize", 46f)
        hwCustomRot = prefs.getFloat("hwCustomRot", 2f)
        hwCustomJitter = prefs.getFloat("hwCustomJitter", 2f)
        docFontSize = prefs.getInt("docFontSize", 1).coerceIn(0, 2)
        presPalette = prefs.getInt("presPalette", 0).coerceIn(0, SlidePalettes.all.lastIndex)
        presNumbers = prefs.getBool("presNumbers", true)
        docName = prefs.getString("docName", "Ҳуҷҷати нав")
        val last = repo.fileFor(docName, "txt")
        if (last.exists()) {
            try {
                docText = repo.read(last)
            } catch (e: Exception) {
                docText = ""
            }
        }
    }

    private fun saveSettings() {
        prefs.edit {
            putInt("hwStyle", hwStyle)
            putInt("hwPaper", hwPaper)
            putBoolean("hwMargin", hwMargin)
            putFloat("hwLeft", hwLeft)
            putFloat("hwRight", hwRight)
            putFloat("hwTop", hwTop)
            putFloat("hwBottom", hwBottom)
            putBoolean("hwIndent", hwIndent)
            putBoolean("hwCenter", hwCenter)
            putBoolean("hwPageNum", hwPageNum)
            putFloat("hwSizeScale", hwSizeScale)
            putInt("hwInk", hwInk)
            putFloat("hwCustomSize", hwCustomSize)
            putFloat("hwCustomRot", hwCustomRot)
            putFloat("hwCustomJitter", hwCustomJitter)
            putInt("docFontSize", docFontSize)
            putInt("presPalette", presPalette)
            putBoolean("presNumbers", presNumbers)
            putString("docName", docName)
        }
    }

    fun resetHandwritingSettings() {
        hwStyle = 0; hwPaper = 1; hwMargin = false
        hwLeft = 20f; hwRight = 10f; hwTop = 15f; hwBottom = 15f
        hwIndent = true; hwCenter = true; hwPageNum = true
        hwSizeScale = 1f; hwInk = 0
        hwCustomSize = 46f; hwCustomRot = 2f; hwCustomJitter = 2f
        message = "Танзимоти дастнавис аз нав гузошта шуд"
    }

    fun clearCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.cacheDir.listFiles()?.forEach { it.delete() } }
            message = "Кэши чоп тоза шуд"
        }
    }

    private fun saveSlides() {
        val arr = JSONArray()
        slides.forEach {
            arr.put(
                JSONObject().put("t", it.title).put("b", it.body).put("l", it.layout.name)
                    .put("i", it.image).put("n", it.notes)
            )
        }
        prefs.edit { putString("slides", arr.toString()) }
    }

    private fun loadSlides() {
        val s = prefs.getString("slides", "")
        if (s.isNotEmpty()) {
            try {
                val arr = JSONArray(s)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val layout = try {
                        SlideLayout.valueOf(o.optString("l", if (i == 0) "TITLE" else "TEXT"))
                    } catch (e: Exception) {
                        if (i == 0) SlideLayout.TITLE else SlideLayout.TEXT
                    }
                    slides.add(Slide(o.optString("t"), o.optString("b"), layout, o.optString("i"), o.optString("n")))
                }
            } catch (e: Exception) {
                slides.clear()
            }
        }
        if (slides.isEmpty()) slides.add(Slide("Мавзӯи презентатсия", "Номи шумо", SlideLayout.TITLE))
    }

    fun refresh() {
        _files.value = repo.list()
    }

    // ---------- Document Studio ----------

    fun onDocTextChange(t: String) {
        docText = t
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(1500)
            withContext(Dispatchers.IO) { repo.saveText(docName, docText) }
            refresh()
        }
    }

    fun saveDoc() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.saveText(docName, docText) }
            refresh()
            message = "Нигоҳ дошта шуд"
        }
    }

    fun newDoc() {
        docName = "Ҳуҷҷати нав"
        docText = ""
    }

    fun applyTemplate(type: ReportType, lang: Lang, topic: String, author: String, org: String) {
        docName = "${type.title(lang)} - ${topic.trim().take(40)}".trim()
        docText = ReportGenerator.generate(type, lang, topic, author, org)
        hwText = docText
        saveDoc()
    }

    fun openFile(context: Context, f: File) {
        when (f.extension.lowercase()) {
            "pdf" -> viewing = f
            "txt" -> {
                viewModelScope.launch {
                    val t = withContext(Dispatchers.IO) { repo.read(f) }
                    docName = f.nameWithoutExtension
                    docText = t
                    tab = 1
                }
            }
            else -> if (!openExternal(context, f)) {
                message = "Барномае барои кушодани .${f.extension} ёфт нашуд"
            }
        }
    }

    fun exportPdf(onDone: (File) -> Unit) {
        runIo("PDF сохта нашуд", {
            val f = repo.fileFor(docName, "pdf")
            PdfExporter.textToPdf(docText, f, docFontSizes[docFontSize].toFloat())
            f
        }) { f ->
            message = "PDF сохта шуд: ${f.name}"
            onDone(f)
        }
    }

    fun exportDocx() {
        runIo("DOCX сохта нашуд", {
            val f = repo.fileFor(docName, "docx")
            f.outputStream().use { DocxExporter.write(it, docText, docFontSizes[docFontSize]) }
            f
        }) { f -> message = "DOCX сохта шуд: ${f.name}" }
    }

    fun printDocument(context: Context) {
        if (docText.isBlank()) {
            message = "Ҳуҷҷат холӣ аст"
            return
        }
        runIo("Чоп омода нашуд", {
            val f = File(repo.cacheDir, "print.pdf")
            PdfExporter.textToPdf(docText, f, docFontSizes[docFontSize].toFloat())
            f
        }) { f -> printPdfFile(context, f, docName) }
    }

    fun summarize(): String = TextTools.summarize(docText, 3)

    fun cleanText() {
        docText = TextTools.cleanSpaces(docText)
    }

    fun toHandwriting() {
        hwText = docText
        tab = 3
    }

    // ---------- Presentation Studio ----------

    fun selectSlide(i: Int) {
        slideIdx = i.coerceIn(0, (slides.size - 1).coerceAtLeast(0))
    }

    fun addSlide() {
        val at = (slideIdx + 1).coerceAtMost(slides.size)
        slides.add(at, Slide("Слайди нав", ""))
        slideIdx = at
    }

    fun duplicateSlide() {
        val s = slides.getOrNull(slideIdx) ?: return
        slides.add(slideIdx + 1, s)
        slideIdx += 1
    }

    fun deleteSlide() {
        if (slides.size <= 1) {
            val old = slides[0].image
            slides[0] = Slide("", "", SlideLayout.TITLE)
            cleanupImage(old)
            return
        }
        val old = slides[slideIdx].image
        slides.removeAt(slideIdx)
        slideIdx = slideIdx.coerceAtMost(slides.size - 1)
        cleanupImage(old)
    }

    fun moveSlide(dir: Int) {
        val j = slideIdx + dir
        if (j !in slides.indices || slideIdx !in slides.indices) return
        val a = slides[slideIdx]
        slides[slideIdx] = slides[j]
        slides[j] = a
        slideIdx = j
    }

    fun updateSlide(title: String? = null, body: String? = null, notes: String? = null) {
        val s = slides.getOrNull(slideIdx) ?: return
        slides[slideIdx] = s.copy(title = title ?: s.title, body = body ?: s.body, notes = notes ?: s.notes)
    }

    fun setSlideLayout(layout: SlideLayout) {
        val s = slides.getOrNull(slideIdx) ?: return
        slides[slideIdx] = s.copy(layout = layout)
    }

    fun pickSlideImage(uri: Uri) {
        val idx = slideIdx
        runIo("Сурат илова нашуд", { images.importFrom(uri) }) { name ->
            val s = slides.getOrNull(idx)
            if (s != null) {
                val old = s.image
                val layout = if (s.layout == SlideLayout.TEXT) SlideLayout.IMG_RIGHT else s.layout
                slides[idx] = s.copy(image = name, layout = layout)
                cleanupImage(old)
            } else {
                images.delete(name)
            }
        }
    }

    fun removeSlideImage() {
        val s = slides.getOrNull(slideIdx) ?: return
        val old = s.image
        val layout = when (s.layout) {
            SlideLayout.IMG_RIGHT, SlideLayout.IMG_LEFT, SlideLayout.FULL -> SlideLayout.TEXT
            else -> s.layout
        }
        slides[slideIdx] = s.copy(image = "", layout = layout)
        cleanupImage(old)
    }

    private fun cleanupImage(name: String) {
        if (name.isNotBlank() && slides.none { it.image == name }) images.delete(name)
    }

    fun slidesFromDoc() {
        if (docText.isBlank()) {
            message = "Аввал ҳуҷҷат ё шаблон созед"
            return
        }
        val list = PresentationOutline.fromText(docText)
        val oldImages = slides.map { it.image }.filter { it.isNotBlank() }
        slides.clear()
        slides.addAll(list)
        oldImages.forEach { cleanupImage(it) }
        slideIdx = 0
        tab = 2
        message = "${list.size} слайд сохта шуд"
    }

    fun renderSlide(slide: Slide, index: Int, widthPx: Int): Bitmap =
        SlideRenderer.render(slide, index + 1, presNumbers, SlidePalettes.all[presPalette], imageSource, widthPx)

    /** Вақти тахминии баромад (дақиқа), ~120 калима дар як дақиқа. */
    fun presMinutes(): Int {
        val words = slides.sumOf { TextTools.wordCount(it.title + " " + it.body + " " + it.notes) }
        return ((words + 119) / 120).coerceAtLeast(1)
    }

    private fun presName(snap: List<Slide>): String =
        snap.firstOrNull()?.title?.take(40)?.ifBlank { "Презентатсия" } ?: "Презентатсия"

    fun exportPptx() {
        val snap = slides.toList()
        val pal = SlidePalettes.all[presPalette]
        val numbers = presNumbers
        runIo("PPTX сохта нашуд", {
            val data = snap.map { s ->
                val bytes = if (s.image.isBlank()) null else images.bytes(s.image)
                val dims = if (bytes == null) Pair(0, 0) else images.size(s.image)
                PptxExporter.SlideData(s.title, s.body, s.layout.name, bytes, dims.first, dims.second)
            }
            val f = repo.fileFor(presName(snap), "pptx")
            f.outputStream().use {
                PptxExporter.write(it, data, pal.hex(pal.bg), pal.hex(pal.title), pal.hex(pal.text), pal.hex(pal.accent), numbers)
            }
            f
        }) { f -> message = "PPTX сохта шуд: ${f.name}" }
    }

    fun exportSlidesPdf(mode: HandoutMode) {
        val snap = slides.toList()
        val pal = SlidePalettes.all[presPalette]
        val numbers = presNumbers
        runIo("PDF сохта нашуд", {
            val f = repo.fileFor(presName(snap) + if (mode.screen) "" else " (A4)", "pdf")
            SlidesPdfExporter.write(snap, pal, mode, numbers, imageSource, presName(snap), f)
            f
        }) { f ->
            message = "PDF сохта шуд: ${f.name}"
            viewing = f
        }
    }

    fun printSlides(context: Context, mode: HandoutMode) {
        val snap = slides.toList()
        val pal = SlidePalettes.all[presPalette]
        val numbers = presNumbers
        runIo("Чоп омода нашуд", {
            val f = File(repo.cacheDir, "slides.pdf")
            SlidesPdfExporter.write(snap, pal, mode, numbers, imageSource, presName(snap), f)
            f
        }) { f -> printPdfFile(context, f, presName(snap), mode.landscape) }
    }

    // ---------- Handwriting Studio ----------

    fun hwLayout(): HwLayout = HwLayout(
        leftMm = hwLeft, rightMm = hwRight, topMm = hwTop, bottomMm = hwBottom,
        marginLine = hwMargin, indent = hwIndent, centerTitles = hwCenter, pageNumbers = hwPageNum,
        inkColor = HandwritingRenderer.INK_COLORS[hwInk]
    )

    fun hwParams(): HwParams = when (hwStyle) {
        0 -> HwParams("natural.ttf", 50f * hwSizeScale, 1.6f, 2.0f, 0.5f)
        1 -> HwParams("beautiful.ttf", 52f * hwSizeScale, 0.8f, 1.0f, 0.8f)
        2 -> HwParams("clean.ttf", 46f * hwSizeScale, 0.4f, 0.3f, 0.6f)
        4 -> HwParams("soni.ttf", 52f * hwSizeScale, 1.0f, 1.0f, 0.5f)
        else -> HwParams("natural.ttf", hwCustomSize, hwCustomRot, hwCustomJitter, 0.8f)
    }

    fun previewHandwriting() {
        if (hwText.isBlank()) {
            message = "Аввал матн ворид кунед"
            return
        }
        runIo("Пешнамоиш сохта нашуд", {
            val list = ArrayList<Bitmap>()
            renderer.render(hwText, hwParams(), Paper.values()[hwPaper], hwLayout(), 0.6f, 3) { list.add(it) }
            list as List<Bitmap>
        }) { list -> hwPreview = list }
    }

    fun exportHandwritingPdf() {
        if (hwText.isBlank()) {
            message = "Аввал матн ворид кунед"
            return
        }
        runIo("PDF-и дастнавис сохта нашуд", {
            val f = repo.fileFor("Дастнавис-${System.currentTimeMillis() / 1000}", "pdf")
            renderer.exportPdf(hwText, hwParams(), Paper.values()[hwPaper], hwLayout(), f)
            f
        }) { f ->
            message = "PDF сохта шуд: ${f.name}"
            viewing = f
        }
    }

    fun printHandwriting(context: Context) {
        if (hwText.isBlank()) {
            message = "Аввал матн ворид кунед"
            return
        }
        runIo("Чоп омода нашуд", {
            val f = File(repo.cacheDir, "handwriting.pdf")
            renderer.exportPdf(hwText, hwParams(), Paper.values()[hwPaper], hwLayout(), f)
            f
        }) { f -> printPdfFile(context, f, "Дастнавис") }
    }

    // ---------- File manager ----------

    fun rename(f: File, newName: String) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { repo.rename(f, newName) }
            message = if (ok) "Ном тағйир ёфт" else "Номи нав аллакай мавҷуд аст ё нодуруст аст"
            refresh()
        }
    }

    fun delete(f: File) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { repo.delete(f) }
            message = if (ok) "Нест карда шуд" else "Нест карда нашуд"
            refresh()
        }
    }

    fun importFrom(uri: Uri) {
        runIo("Ворид кардан муяссар нашуд", { repo.importFrom(uri) }) { f ->
            message = if (f != null) "Ворид шуд: ${f.name}" else "Файл хонда нашуд"
            refresh()
        }
    }

    fun exportTo(f: File, uri: Uri) {
        runIo("Содирот муяссар нашуд", { repo.exportTo(f, uri) }) { ok ->
            message = if (ok) "Содирот анҷом ёфт" else "Содирот муяссар нашуд"
        }
    }

    fun printFile(context: Context, f: File) {
        when (f.extension.lowercase()) {
            "pdf" -> printPdfFile(context, f, f.nameWithoutExtension)
            "txt" -> runIo("Чоп омода нашуд", {
                val out = File(repo.cacheDir, "file-print.pdf")
                PdfExporter.textToPdf(repo.read(f), out, docFontSizes[docFontSize].toFloat())
                out
            }) { out -> printPdfFile(context, out, f.nameWithoutExtension) }
            else -> message = "Ин формат мустақиман чоп намешавад. Ҳуҷҷатро ба PDF содир кунед."
        }
    }

    // ---------- helpers ----------

    private fun printPdfFile(context: Context, f: File, name: String, landscape: Boolean = false) {
        val ok = PrintHelper.printPdf(context, f, name, landscape)
        if (!ok) message = "Хидмати чоп дар ин дастгоҳ дастрас нест. Принтер ё плагини чопро дар танзимоти Android фаъол кунед."
    }

    private fun <T> runIo(errorText: String, work: () -> T, done: (T) -> Unit) {
        viewModelScope.launch {
            busy = true
            try {
                val r = withContext(Dispatchers.IO) { work() }
                refresh()
                done(r)
            } catch (e: Exception) {
                message = "$errorText: ${e.message ?: e.javaClass.simpleName}"
            } finally {
                busy = false
            }
        }
    }
}
