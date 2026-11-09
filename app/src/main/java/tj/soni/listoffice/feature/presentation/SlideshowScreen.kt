package tj.soni.listoffice.feature.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import tj.soni.listoffice.MainViewModel

/** Намоиши слайдҳо дар экрани пурра: ламс дар тарафи рост = баъд, дар тарафи чап = пеш. */
@Composable
fun SlideshowScreen(vm: MainViewModel) {
    BackHandler { vm.slideshow = false }
    val n = vm.slides.size
    var i by rememberSaveable { mutableStateOf(vm.slideIdx.coerceIn(0, (n - 1).coerceAtLeast(0))) }
    val cur = vm.slides.getOrNull(i)
    val bmp = remember(cur, i, vm.presPalette, vm.presNumbers) {
        cur?.let { vm.renderSlide(it, i, 1280) }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(n) {
                detectTapGestures { offset ->
                    if (offset.x > size.width / 2) {
                        if (i < n - 1) i++ else vm.slideshow = false
                    } else if (i > 0) {
                        i--
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            "${i + 1} / $n",
            color = Color(0x99FFFFFF),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
        )
    }
}
