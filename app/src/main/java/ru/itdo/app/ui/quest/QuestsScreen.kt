package ru.itdo.app.ui.quest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.launch
import ru.itdo.app.core.AppContainer
import ru.itdo.app.data.model.Quest

/**
 * Quests screen — 1:1 with iOS QuestsView.
 */
@Composable
fun QuestsScreen(
    container: AppContainer,
    quests: List<Quest> = emptyList(),
    coins: Int = 0,
    onNavigateBack: () -> Unit = {}
) {
    var claiming by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Квесты") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFD4A017), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("$coins", color = Color(0xFFD4A017), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (quests.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(quests, key = { it.id }) { quest ->
                        QuestCard(quest, onClaim = {
                            if (!quest.completed || quest.claimed) return@QuestCard
                            scope.launch {
                                claiming = quest.id
                                runCatching { container.repository.claimQuest(quest.id) }
                                claiming = null
                            }
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestCard(quest: Quest, onClaim: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                // Icon
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(quest.icon ?: "🎯", fontSize = 24.sp)
                }

                Spacer(Modifier.width(12.dp))

                // Info
                Column(Modifier.weight(1f)) {
                    Text(quest.title, color = Color(0xFF0A0A0A), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    quest.description?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, color = Color(0x8C8C8C), fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(8.dp))

                    // Progress bar
                    LinearProgressIndicator(
                        progress = if (quest.target > 0) quest.progress.toFloat() / quest.target else 0f,
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        trackColor = Color(0xFFF0F0F2),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("${quest.progress} / ${quest.target}", color = Color(0x8C8C8C), fontSize = 12.sp)
                }
            }

            // Reward and claim button
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFD4A017), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("+${quest.reward}", color = Color(0xFFD4A017), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                if (quest.claimed) {
                    Text("Получено", color = Color(0x8C8C8C), fontSize = 13.sp)
                } else if (quest.completed) {
                    Button(
                        onClick = onClaim,
                        enabled = claiming != quest.id,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BA7C))
                    ) {
                        if (claiming == quest.id) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Text("Получить", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    Text("В процессе", color = Color(0x8C8C8C), fontSize = 13.sp)
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
            Icons.Default.SportsEsports,
            contentDescription = null,
            tint = Color(0x668C8C8C),
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text("Квестов пока нет", color = Color(0x8C8C8C))
    }
}
