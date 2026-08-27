package ru.itdo.app.ui.pixelbattle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.itdo.app.core.AppContainer

private val PALETTE = listOf(
    "#FFFFFF", "#000000", "#FF4500", "#FFD635", "#00CC78", "#2450A4",
    "#811E9F", "#FF99AA", "#94B3FF", "#6D482F"
)

/**
 * Совместное битвенное полотно (см. api/pixelbattle/board.php и place.php).
 * Реализован простой поллинг; в вебе, вероятно, используется WS
 * (см. api/ws/) — для live-обновлений стоит подключить WebSocket отдельно.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PixelBattleScreen(container: AppContainer) {
    var pixels by remember { mutableStateOf<List<String>>(emptyList()) }
    var width by remember { mutableIntStateOf(0) }
    var selectedColor by remember { mutableStateOf(PALETTE[2]) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        runCatching { container.repository.pixelBoard() }
            .onSuccess { pixels = it.pixels; width = it.width }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(topBar = { TopAppBar(title = { Text("Pixel Battle") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(Modifier.padding(8.dp)) {
                PALETTE.forEach { hex ->
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(28.dp)
                            .background(Color(android.graphics.Color.parseColor(hex)))
                            .clickable { selectedColor = hex }
                    )
                }
            }
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
            } else if (width > 0) {
                LazyVerticalGrid(columns = GridCells.Fixed(width), modifier = Modifier.fillMaxSize()) {
                    items(pixels.size) { idx ->
                        val hex = pixels.getOrNull(idx) ?: "#FFFFFF"
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .clickable {
                                    val x = idx % width
                                    val y = idx / width
                                    scope.launch {
                                        runCatching { container.repository.placePixel(x, y, selectedColor) }
                                        load()
                                    }
                                }
                        )
                    }
                }
            } else {
                Text("Не удалось загрузить поле", modifier = Modifier.padding(16.dp))
            }
        }
    }
}
