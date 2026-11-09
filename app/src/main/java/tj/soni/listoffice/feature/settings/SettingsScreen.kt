package tj.soni.listoffice.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import tj.soni.listoffice.MainViewModel
import tj.soni.listoffice.core.theme.UltraColors
import tj.soni.listoffice.core.ui.ChipRow
import tj.soni.listoffice.core.ui.GradientHeader
import tj.soni.listoffice.core.ui.PremiumCard
import tj.soni.listoffice.core.ui.ToolButton

@Composable
fun SettingsScreen(vm: MainViewModel) {
    BackHandler { vm.showSettings = false }
    val styles = listOf(
        "natural.ttf" to "Natural Student",
        "beautiful.ttf" to "Beautiful",
        "clean.ttf" to "Clean / Custom",
        "soni.ttf" to "SONI (шрифти шумо)"
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        GradientHeader(
            title = "Танзимот",
            subtitle = "Шрифтҳо, пешфарзҳо ва маълумот",
            trailing = {
                IconButton(onClick = { vm.showSettings = false }) {
                    Icon(Icons.Default.Close, contentDescription = "Пӯшидан", tint = Color.White)
                }
            }
        )
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PremiumCard(Modifier.fillMaxWidth()) {
                Text("Шрифтҳои дастнавис", style = MaterialTheme.typography.titleMedium)
                styles.forEach { (file, label) ->
                    val ok = vm.fontStatus[file]
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label)
                        Text(
                            when (ok) {
                                true -> "омода"
                                false -> "нест (хати система)"
                                null -> "..."
                            },
                            color = if (ok == true) UltraColors.Green else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    "Шрифтҳо дар вақти build аз GitHub Actions зеркашӣ мешаванд. Шрифти худатонро ба assets/fonts гузоред.",
                    modifier = Modifier.padding(top = 10.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            PremiumCard(Modifier.fillMaxWidth()) {
                Text("Андозаи ҳарф дар PDF/DOCX", style = MaterialTheme.typography.titleMedium)
                Column(Modifier.padding(top = 8.dp)) {
                    ChipRow(vm.docFontSizes.map { "$it pt" }, vm.docFontSize) { vm.docFontSize = it }
                }
            }

            PremiumCard(Modifier.fillMaxWidth()) {
                Text("Нигоҳдорӣ", style = MaterialTheme.typography.titleMedium)
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToolButton(Icons.Default.Check, "Тоза кардани кэши чоп") { vm.clearCache() }
                    ToolButton(Icons.Default.Create, "Бозгашти танзимоти дастнавис") { vm.resetHandwritingSettings() }
                }
            }

            PremiumCard(Modifier.fillMaxWidth()) {
                Text("Дар бораи барнома", style = MaterialTheme.typography.titleMedium)
                Text(
                    "ListSoz 1.0.0 • Android 8.0+\nҲама чиз дар худи дастгоҳ кор мекунад: бе сервер, бе интернет ва бе API-и пулакӣ.",
                    modifier = Modifier.padding(top = 6.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
