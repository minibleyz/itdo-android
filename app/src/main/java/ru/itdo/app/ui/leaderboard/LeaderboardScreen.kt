package ru.itdo.app.ui.leaderboard

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
import ru.itdo.app.data.model.LeaderboardEntry

/**
 * Leaderboard screen — 1:1 with iOS LeaderboardView.
 */
@Composable
fun LeaderboardScreen(
    entries: List<LeaderboardEntry> = emptyList(),
    myRank: Int? = null,
    onNavigateBack: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("По монетам", "По подписчикам", "По постам")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Топ") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Tabs
            Row(Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { index, title ->
                    TabSegment(title, selected = selectedTab == index) { selectedTab = index }
                }
            }
            Divider(color = Color(0x1F000000))

            if (entries.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        LeaderboardRow(entry, entries.indexOf(entry) + 1)
                    }
                }
            }
        }
    }
}

// weight() — расширение RowScope, поэтому функция должна быть его extension,
// а не принимать обычный Modifier: иначе .weight(1f) не резолвится.
@Composable
private fun RowScope.TabSegment(title: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            title,
            color = if (selected) Color(0xFF0A0A0A) else Color(0x8C8C8C),
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
        if (selected) {
            Spacer(
                Modifier
                    .fillMaxWidth(0.6f)
                    .height(2.dp)
                    .align(Alignment.BottomCenter)
                    .background(Color(0xFF0080FF))
            )
        }
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntry, rank: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.isMe) Color(0xFF0080FF).copy(alpha = 0.1f) else Color.White
        )
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Text(
                when (rank) {
                    1 -> "🥇"
                    2 -> "🥈"
                    3 -> "🥉"
                    else -> "$rank"
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(30.dp)
            )

            Spacer(Modifier.width(10.dp))

            // Avatar
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0F0F2)),
                contentAlignment = Alignment.Center
            ) {
                if (entry.avatar != null) {
                    AsyncImage(model = entry.avatar, contentDescription = null, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0x8C8C8C))
                }
            }

            Spacer(Modifier.width(10.dp))

            // Name
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        entry.name ?: entry.username,
                        color = Color(0xFF0A0A0A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (entry.isVerified) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF0080FF), modifier = Modifier.size(14.dp))
                    }
                }
                Text("@${entry.username}", color = Color(0x8C8C8C), fontSize = 13.sp)
            }

            // Coins
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFD4A017), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    "${entry.coins}",
                    color = Color(0xFFD4A017),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
