package ru.itdo.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Загрузочный экран в стиле веба, а не generic-спиннер "наобум".
 * Повторяет #app-loading из index.html (вордмарк "ITDO") и знак
 * логотипа — то же разомкнутое кольцо + точка, что в sidebar-logo
 * (index.html) и в ic_launcher_foreground.xml. Анимация — та же
 * skeletonPulse из assets/css/app.css (opacity 1 → 0.45 → 1, 1000ms,
 * ease-in-out), а не своя произвольная.
 */
@Composable
fun ItdoLoadingScreen(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "itdoLoaderPulse")
    val alpha by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "itdoLoaderAlpha"
    )
    val color = MaterialTheme.colorScheme.onBackground

    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(modifier = Modifier.size(44.dp)) {
                val strokeWidth = size.minDimension * 0.09f
                val ringColor = color.copy(alpha = alpha)
                // Разомкнутое кольцо: разрыв внизу-слева, как в оригинальной
                // трассировке лого (см. комментарий в ic_launcher_foreground.xml).
                drawArc(
                    color = ringColor,
                    startAngle = 168f,
                    sweepAngle = 251f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                drawCircle(
                    color = ringColor,
                    radius = size.minDimension * 0.095f,
                    center = Offset(size.width / 2f, size.height / 2f)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "ITDO",
                color = color.copy(alpha = alpha),
                fontWeight = FontWeight.Black,
                fontSize = 26.sp,
                letterSpacing = 1.sp
            )
        }
    }
}
