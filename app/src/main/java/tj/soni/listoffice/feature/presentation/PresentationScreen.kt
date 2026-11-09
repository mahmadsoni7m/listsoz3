package tj.soni.listoffice.feature.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import tj.soni.listoffice.MainViewModel
import tj.soni.listoffice.core.ui.ChipRow
import tj.soni.listoffice.core.ui.GradientHeader
import tj.soni.listoffice.core.ui.PremiumCard
import tj.soni.listoffice.core.ui.ToolButton
import tj.soni.listoffice.domain.HandoutMode
import tj.soni.listoffice.domain.SlideLayout
import tj.soni.listoffice.domain.SlidePalettes

@Composable
fun PresentationScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val pal = SlidePalettes.all[vm.presPalette]
    val idx = vm.slideIdx.coerceIn(0, (vm.slides.size - 1).coerceAtLeast(0))
    val cur = vm.slides.getOrNull(idx)
    var chooser by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.pickSlideImage(uri)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        GradientHeader("Слайдҳо", "${vm.slides.size} слайд • ~${vm.presMinutes()} дақ. • ${pal.name}")
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ToolButton(Icons.Default.Add, "Слайд") { vm.addSlide() }
                ToolButton(Icons.Default.PlayArrow, "Намоиш") { vm.slideIdx = idx; vm.slideshow = true }
                ToolButton(Icons.Default.Description, "Аз ҳуҷҷат") { vm.slidesFromDoc() }
                ToolButton(Icons.Default.Slideshow, "PPTX") { vm.exportPptx() }
                ToolButton(Icons.Default.PictureAsPdf, "PDF") { chooser = "pdf" }
                ToolButton(Icons.Default.Print, "Чоп A4") { chooser = "print" }
            }

            Text("Қолаб", color = MaterialTheme.colorScheme.onSurfaceVariant)
            ChipRow(SlidePalettes.all.map { it.name }, vm.presPalette) { vm.presPalette = it }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = vm.presNumbers, onCheckedChange = { vm.presNumbers = it })
                Spacer(Modifier.width(10.dp))
                Text("Рақами слайд")
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(vm.slides) { i, s ->
                    val bmp = remember(s, i, vm.presPalette, vm.presNumbers) { vm.renderSlide(s, i, 320) }
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .width(130.dp)
                            .border(
                                if (i == idx) 2.dp else 0.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(8.dp)
                            )
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { vm.selectSlide(i) }
                    )
                }
            }

            if (cur != null) {
                val big = remember(cur, idx, vm.presPalette, vm.presNumbers) { vm.renderSlide(cur, idx, 960) }
                Image(
                    bitmap = big.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                )

                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ToolButton(Icons.Default.ArrowBack, "Пеш") { vm.moveSlide(-1) }
                    ToolButton(Icons.Default.ArrowForward, "Баъд") { vm.moveSlide(1) }
                    ToolButton(Icons.Default.ContentCopy, "Нусха") { vm.duplicateSlide() }
                    ToolButton(Icons.Default.Delete, "Нест") { vm.deleteSlide() }
                }

                PremiumCard(Modifier.fillMaxWidth()) {
                    Text("Тарҳбандӣ", style = MaterialTheme.typography.titleMedium)
                    Column(Modifier.padding(top = 8.dp)) {
                        ChipRow(SlideLayout.values().map { it.label }, cur.layout.ordinal) {
                            vm.setSlideLayout(SlideLayout.values()[it])
                        }
                    }
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ToolButton(
                            Icons.Default.Image,
                            if (cur.image.isBlank()) "Илова кардани сурат" else "Иваз кардани сурат"
                        ) { picker.launch(arrayOf("image/*")) }
                        if (cur.image.isNotBlank()) {
                            ToolButton(Icons.Default.Delete, "Бе сурат") { vm.removeSlideImage() }
                        }
                    }
                    val needsImage = cur.layout == SlideLayout.IMG_RIGHT || cur.layout == SlideLayout.IMG_LEFT || cur.layout == SlideLayout.FULL
                    if (needsImage && cur.image.isBlank()) {
                        Text(
                            "Ин тарҳбандӣ сурат талаб мекунад. То сурат илова нашавад, слайд бо тарҳи оддӣ нишон дода мешавад.",
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedTextField(
                        value = cur.title,
                        onValueChange = { vm.updateSlide(title = it) },
                        label = { Text(if (cur.layout == SlideLayout.TITLE) "Мавзӯи презентатсия" else "Сарлавҳаи слайд") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    OutlinedTextField(
                        value = cur.body,
                        onValueChange = { vm.updateSlide(body = it) },
                        label = {
                            Text(if (cur.layout == SlideLayout.TITLE) "Зерсарлавҳа (масалан, номи шумо)" else "Матн (ҳар сатр = як банд)")
                        },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    OutlinedTextField(
                        value = cur.notes,
                        onValueChange = { vm.updateSlide(notes = it) },
                        label = { Text("Ёддошти баромадкунанда (дар чопи «A4 + ёддошт»)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            }
            Text(
                "Маслиҳат: ҳуҷҷат ё шаблон созед ва «Аз ҳуҷҷат»-ро пахш кунед, то слайдҳо худкор сохта шаванд. Баъд сурат илова кунед.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    chooser?.let { kind ->
        AlertDialog(
            onDismissRequest = { chooser = null },
            title = { Text(if (kind == "pdf") "PDF — формат" else "Чоп — формат A4") },
            text = {
                Column {
                    HandoutMode.values().filter { kind == "pdf" || !it.screen }.forEach { m ->
                        Text(
                            m.label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    chooser = null
                                    if (kind == "pdf") vm.exportSlidesPdf(m) else vm.printSlides(ctx, m)
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { chooser = null }) { Text("Бекор") } }
        )
    }
}
