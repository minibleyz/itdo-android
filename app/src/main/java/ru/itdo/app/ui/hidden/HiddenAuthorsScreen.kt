package ru.itdo.app.ui.hidden

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import ru.itdo.app.core.AppContainer

/**
 * Hidden authors screen — 1:1 with iOS HiddenAuthorsView.
 */
@Composable
fun HiddenAuthorsScreen(
    container: AppContainer,
    authors: List<HiddenAuthor> = emptyList(),
    onNavigateBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Скрытые авторы") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (authors.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(authors, key = { it.id }) { author ->
                        HiddenAuthorRow(author) {
                            scope.launch {
                                runCatching { container.repository.unhideAuthor(author.id) }
                            }
                        }
                        Divider(color = Color(0x1F000000))
                    }
                }
            }
        }
    }
}

data class HiddenAuthor(
    val id: Int,
    val username: String,
    val name: String? = null,
    val avatar: String? = null
)

@Composable
private fun HiddenAuthorRow(author: HiddenAuthor, onUnhide: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = author.avatar,
            contentDescription = null,
            modifier = Modifier.size(44.dp).clip(CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                author.name ?: author.username,
                color = Color(0xFF0A0A0A),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text("@${author.username}", color = Color(0x8C8C8C), fontSize = 13.sp)
        }
        TextButton(onClick = onUnhide) {
            Text("Показать", color = Color(0xFF0080FF))
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.VisibilityOff,
            contentDescription = null,
            tint = Color(0x668C8C8C),
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text("Скрытых авторов нет", color = Color(0x8C8C8C))
    }
}
