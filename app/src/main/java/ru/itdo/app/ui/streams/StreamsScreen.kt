package ru.itdo.app.ui.streams

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
import ru.itdo.app.data.model.LiveStream

/**
 * Streams screen — list of live streams.
 * 1:1 with iOS StreamsListView.
 */
@Composable
fun StreamsScreen(
    onNavigateBack: () -> Unit = {},
    onOpenStream: (Int) -> Unit = {}
) {
    var streams by remember { mutableStateOf<List<LiveStream>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Эфиры") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (loading && streams.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
            } else if (streams.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(streams, key = { it.id }) { stream ->
                        StreamCard(stream, onClick = { onOpenStream(stream.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamCard(stream: LiveStream, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            // Stream thumbnail / banner
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Color(0xFF1C1C1C)),
                contentAlignment = Alignment.Center
            ) {
                if (stream.isLive) {
                    Box(
                        Modifier
                            .padding(12.dp)
                            .background(Color(0xFFFF0000), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Circle, contentDescription = null, tint = Color.White, modifier = Modifier.size(8.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("LIVE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Stream info
            Row(verticalAlignment = Alignment.Top) {
                AsyncImage(
                    model = stream.avatar,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stream.title,
                        color = Color(0xFF0A0A0A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2
                    )
                    stream.description?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = Color(0x8C8C8C), fontSize = 13.sp, maxLines = 1)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (stream.viewers > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFFF91880), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("${stream.viewers}", color = Color(0x8C8C8C), fontSize = 12.sp)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFF91880), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${stream.likes}", color = Color(0x8C8C8C), fontSize = 12.sp)
                        }
                    }
                }
            }
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
            Icons.Default.Radio,
            contentDescription = null,
            tint = Color(0x668C8C8C),
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text("Эфиров пока нет", color = Color(0x8C8C8C))
    }
}
