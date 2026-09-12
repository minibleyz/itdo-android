package ru.itdo.app.ui.articles

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import coil.compose.AsyncImage
import ru.itdo.app.data.model.Article

/**
 * Articles screen — 1:1 with iOS ArticlesView.
 */
@Composable
fun ArticlesScreen(
    articles: List<Article> = emptyList(),
    onNavigateBack: () -> Unit = {},
    onOpenArticle: (Int) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Статьи") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (articles.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(articles, key = { it.id }) { article ->
                        ArticleCard(article, onClick = { onOpenArticle(article.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleCard(article: Article, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            // Cover
            article.cover?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                )
                Spacer(Modifier.height(12.dp))
            }

            // Author
            article.author?.let { author ->
                Row(
                    Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = author.avatar,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp).clip(androidx.compose.foundation.shape.CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        author.name ?: author.username,
                        color = Color(0xFF0A0A0A),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // Title
            Text(
                article.title,
                color = Color(0xFF0A0A0A),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Stats
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                StatChip(Icons.Default.Favorite, "${article.likesCount}")
                StatChip(Icons.Default.Visibility, "${article.viewsCount}")
                StatChip(Icons.Default.ChatBubbleOutline, "${article.commentsCount}")
            }
        }
    }
}

@Composable
private fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, count: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color(0x8C8C8C), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(count, color = Color(0x8C8C8C), fontSize = 12.sp)
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
            Icons.Default.Article,
            contentDescription = null,
            tint = Color(0x668C8C8C),
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text("Статей пока нет", color = Color(0x8C8C8C))
    }
}
