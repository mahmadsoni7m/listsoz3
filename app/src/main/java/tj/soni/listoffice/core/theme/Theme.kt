package tj.soni.listoffice.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object UltraColors {
    val Blue = Color(0xFF2F80FF)
    val BlueDeep = Color(0xFF16408F)
    val Green = Color(0xFF1FA45B)
    val GreenDeep = Color(0xFF0F3D26)
    val Night = Color(0xFF050B08)
    val Gold = Color(0xFFE0A93B)
    val Red = Color(0xFFE5675F)
}

private val UltraScheme = darkColorScheme(
    primary = UltraColors.Blue,
    onPrimary = Color.White,
    secondary = UltraColors.Green,
    onSecondary = Color.White,
    secondaryContainer = UltraColors.BlueDeep,
    onSecondaryContainer = Color.White,
    background = UltraColors.Night,
    onBackground = Color(0xFFE6F0EA),
    surface = Color(0xFF0C1712),
    onSurface = Color(0xFFE6F0EA),
    surfaceVariant = Color(0xFF14231B),
    onSurfaceVariant = Color(0xFFB9CBC0),
    primaryContainer = UltraColors.GreenDeep,
    onPrimaryContainer = Color(0xFFD6F5E3),
    outline = Color(0xFF3A5246),
    error = Color(0xFFFF6B6B)
)

private val UltraTypography = Typography(
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
)

private val UltraShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun ListOfficeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = UltraScheme, typography = UltraTypography, shapes = UltraShapes, content = content)
}
