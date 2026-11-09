package tj.soni.listoffice.feature.dashboard

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import tj.soni.listoffice.MainViewModel
import tj.soni.listoffice.R
import tj.soni.listoffice.core.theme.UltraColors
import tj.soni.listoffice.core.ui.EmptyState
import tj.soni.listoffice.core.ui.GradientHeader
import tj.soni.listoffice.core.ui.IconBadge
import tj.soni.listoffice.core.ui.PremiumCard
import tj.soni.listoffice.core.ui.SectionTitle
import java.util.Calendar

private val tips = listOf(
    "Матнро дар «Ҳуҷҷат» нависед, баъд бо як пахш ба дастнавис ё слайд табдил диҳед.",
    "Дар «Дастнавис» ҳошияҳои чап, рост, боло ва поёнро бо миллиметр танзим кунед.",
    "Барои чоп аввал PDF созед: дар PDF viewer тугмаи чоп ва мубодила ҳаст.",
    "Шаблони реферат/КМД сохтор месозад; матни аслиро худатон менависед.",
    "Ҳуҷҷатҳо худкор нигоҳ дошта мешаванд, барномаро бепарво пӯшед."
)

@Composable
fun DashboardScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val files by vm.files.collectAsState()
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = when {
        hour < 5 -> "Шаб ба хайр"
        hour < 11 -> "Субҳ ба хайр"
        hour < 17 -> "Рӯз ба хайр"
        hour < 22 -> "Бегоҳ ба хайр"
        else -> "Шаб ба хайр"
    }
    val tip = remember { tips[((System.currentTimeMillis() / 86_400_000L) % tips.size).toInt()] }
    fun count(ext: String) = files.count { it.extension.equals(ext, true) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        GradientHeader(
            title = greeting,
            subtitle = "ListSoz",
            leading = {
                Image(painterResource(R.drawable.ic_fg), contentDescription = "ListSoz", modifier = Modifier.size(56.dp))
            },
            trailing = {
                IconButton(onClick = { vm.showSettings = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "Танзимот", tint = Color.White)
                }
            }
        )
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PremiumCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Stat("Ҳамаги", files.size)
                    Stat("PDF", count("pdf"))
                    Stat("DOCX", count("docx"))
                    Stat("PPTX", count("pptx"))
                }
            }

            SectionTitle("Модулҳо")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModuleTile(
                    Icons.Default.Description, "Ҳуҷҷат", "Матн, PDF, DOCX",
                    listOf(UltraColors.GreenDeep, UltraColors.Green), Modifier.weight(1f)
                ) { vm.tab = 1 }
                ModuleTile(
                    Icons.Default.Star, "Реферат / КМД", "Шаблони тайёр",
                    listOf(UltraColors.BlueDeep, UltraColors.Blue), Modifier.weight(1f)
                ) { vm.newDoc(); vm.templateRequest = true; vm.tab = 1 }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModuleTile(
                    Icons.Default.Slideshow, "Слайдҳо", "PPTX ва PDF",
                    listOf(Color(0xFF7A3F0E), UltraColors.Gold), Modifier.weight(1f)
                ) { vm.tab = 2 }
                ModuleTile(
                    Icons.Default.Create, "Дастнавис", "Ручкаи кабуд",
                    listOf(Color(0xFF2A2A7A), Color(0xFF5B6CFF)), Modifier.weight(1f)
                ) { vm.tab = 3 }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModuleTile(
                    Icons.Default.Folder, "Файлҳо", "Кушодан ва мубодила",
                    listOf(Color(0xFF244A3A), Color(0xFF3FBF8A)), Modifier.weight(1f)
                ) { vm.tab = 4 }
                ModuleTile(
                    Icons.Default.Print, "Чоп", "Принтери Android",
                    listOf(Color(0xFF5A1F2A), UltraColors.Red), Modifier.weight(1f)
                ) { vm.tab = 4 }
            }

            SectionTitle("Охирин ҳуҷҷатҳо")
            PremiumCard(Modifier.fillMaxWidth()) {
                if (files.isEmpty()) {
                    EmptyState(
                        Icons.Default.Description,
                        "Ҳоло файл нест",
                        "Ҳуҷҷати нав созед ё шаблони реферат/КМД-ро истифода баред."
                    )
                } else {
                    files.take(5).forEach { f ->
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.openFile(ctx, f) }.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IconBadge(iconFor(f.extension), colorsFor(f.extension), 38.dp)
                            Column(Modifier.weight(1f)) {
                                Text(f.name, maxLines = 1)
                                Text(
                                    "${f.extension.uppercase()} • ${f.length() / 1024} KB",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Маслиҳат", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(tip, modifier = Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}

fun iconFor(ext: String): ImageVector = when (ext.lowercase()) {
    "pptx" -> Icons.Default.Slideshow
    "pdf" -> Icons.Default.Print
    else -> Icons.Default.Description
}

fun colorsFor(ext: String): List<Color> = when (ext.lowercase()) {
    "pdf" -> listOf(Color(0xFF5A1F2A), UltraColors.Red)
    "docx" -> listOf(UltraColors.BlueDeep, UltraColors.Blue)
    "pptx" -> listOf(Color(0xFF7A3F0E), UltraColors.Gold)
    else -> listOf(Color(0xFF2A3A33), Color(0xFF5E7A6C))
}

@Composable
private fun ModuleTile(
    icon: ImageVector,
    title: String,
    sub: String,
    colors: List<Color>,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            IconBadge(icon, colors)
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: Int) {
    val shown by animateIntAsState(targetValue = value, label = "stat")
    Column {
        Text(shown.toString(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
