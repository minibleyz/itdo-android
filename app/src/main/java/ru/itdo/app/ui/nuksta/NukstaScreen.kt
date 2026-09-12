package ru.itdo.app.ui.nuksta

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.itdo.app.core.AppContainer

/**
 * Nuksta / ITDO Pro screen — updated from "ИТДО ШЛЁП" to "ITDO PRO".
 * 1:1 with iOS NukstaView but with updated branding and design.
 */
@Composable
fun NukstaScreen(
    container: AppContainer,
    onNavigateBack: () -> Unit = {}
) {
    var data by remember { mutableStateOf<NukstaData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showingSubscribe by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val planPrice = 299
    val planDays = 30
    val features = listOf(
        "✨ Уникальный бейдж ITDO PRO",
        "🎨 Эксклюзивные темы оформления",
        "📌 Закрепление постов",
        "💎 Приоритет в рекомендациях",
        "🚫 Отсутствие рекламы",
        "📊 Расширенная статистика",
        "🎁 Доступ к закрытым функциям",
        "💬 Приоритетная поддержка"
    )

    suspend fun load() {
        loading = true
        runCatching { container.repository.fetchNuksta() }
            .onSuccess {
                data = NukstaData(
                    isActive = it.isActive,
                    daysLeft = it.daysLeft,
                    error = it.error
                )
                error = it.error
            }
            .onFailure { error = it.message }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ITDO PRO") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
                }
                error != null -> {
                    Text(error!!, color = Color(0xFFF91880), modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    Column(
                        Modifier.fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Header with gradient
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFF0080FF), Color(0xFF8B5CF6))
                                    )
                                )
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "ITDO PRO",
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Премиум-подписка для настоящих ценителей",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // Features
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(Modifier.padding(20.dp)) {
                                Text(
                                    "Что входит в подписку:",
                                    color = Color(0xFF0A0A0A),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(12.dp))
                                features.forEach { feature ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF00BA7C),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(feature, color = Color(0xFF0A0A0A), fontSize = 14.sp)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // Subscription status
                        data?.let { nukstaData ->
                            if (nukstaData.isActive) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF00BA7C).copy(alpha = 0.1f))
                                ) {
                                    Column(Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF00BA7C))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Подписка активна", color = Color(0xFF00BA7C), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        nukstaData.daysLeft?.let { days ->
                                            Spacer(Modifier.height(4.dp))
                                            Text("Осталось дней: $days", color = Color(0x8C8C8C), fontSize = 13.sp)
                                        }
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                // Renew button
                                Button(
                                    onClick = { showingSubscribe = true },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0080FF))
                                ) {
                                    Text("Продлить за $planPrice монет", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                Button(
                                    onClick = { showingSubscribe = true },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0080FF))
                                ) {
                                    Text("Подписаться за $planPrice монет", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Subscribe dialog
    if (showingSubscribe) {
        AlertDialog(
            onDismissRequest = { showingSubscribe = false },
            title = { Text("Оформить ITDO PRO?") },
            text = { Text("С вашего баланса будет списано $planPrice монет на $planDays дней.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        runCatching { container.repository.subscribeNuksta() }
                            .onSuccess { load() }
                        showingSubscribe = false
                    }
                }) {
                    Text("Подписаться", color = Color(0xFF0080FF))
                }
            },
            dismissButton = {
                TextButton(onClick = { showingSubscribe = false }) {
                    Text("Отмена", color = Color(0x8C8C8C))
                }
            }
        )
    }
}

data class NukstaData(
    val isActive: Boolean = false,
    val daysLeft: Int? = null,
    val error: String? = null
)
