package ru.itdo.app.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import ru.itdo.app.data.model.Comment
import ru.itdo.app.data.model.Post

/**
 * Post detail screen — full view of a single post with comments.
 * 1:1 with iOS PostDetailView.
 */
@Composable
fun PostDetailScreen(
    post: Post,
    onNavigateBack: () -> Unit = {},
    onOpenComments: () -> Unit = {},
    onOpenAuthor: (Int) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Пост") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            item {
                PostCardFull(post, onOpenAuthor = onOpenAuthor)
            }
        }
    }
}

@Composable
private fun PostCardFull(post: Post, onOpenAuthor: (Int) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, Color(0x1F000000), RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Author header
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = post.author?.avatar,
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape).clickable { post.author?.id?.let(onOpenAuthor) }
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        post.author?.displayName ?: "",
                        color = Color(0xFF0A0A0A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { post.author?.id?.let(onOpenAuthor) }
                    )
                    if (post.author?.isVerified == true) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(14.dp))
                    }
                }
                Text("@${post.author?.username ?: ""}", color = Color(0x8C8C8C), fontSize = 14.sp)
            }
        }

        // Text
        post.text?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = Color(0xFF0A0A0A), fontSize = 15.sp, lineHeight = 20.sp)
        }

        // Media
        post.media?.takeIf { it.isNotEmpty() }?.let { media ->
            Spacer(Modifier.height(10.dp))
            if (media.size == 1) {
                AsyncImage(
                    model = media[0].url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp))
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    media.take(4).forEach { item ->
                        AsyncImage(
                            model = item.url,
                            contentDescription = null,
                            modifier = Modifier.weight(1f).height(140.dp).clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
            }
        }

        // Actions
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionIcon(Icons.Default.Favorite, post.likesCount.toString(), Color(0xFFF91880))
            ActionIcon(Icons.Default.ChatBubbleOutline, post.commentsCount.toString(), Color(0xFF0A0A0A))
            ActionIcon(Icons.Default.Repeat, post.repostsCount.toString(), Color(0xFF00BA7C))
            ActionIcon(Icons.Default.Visibility, post.viewsCount.toString(), Color(0x8C8C8C))
        }
    }
}

@Composable
private fun ActionIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, count: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text(count, color = tint, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
