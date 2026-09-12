package ru.itdo.app.ui.profile

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.itdo.app.core.AppContainer

/**
 * Verification screen — 1:1 with iOS VerificationView.
 */
@Composable
fun VerificationScreen(
    container: AppContainer,
    isVerified: Boolean,
    onNavigateBack: () -> Unit = {}
) {
    var status by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var requesting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun loadStatus() {
        loading = true
        runCatching { container.repository.fetchVerificationStatus() }
            .onSuccess {
                status = it.status
                error = it.error
            }
            .onFailure { error = it.message }
        loading = false
    }

    LaunchedEffect(Unit) { loadStatus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Верификация") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Icon
            Box(
                Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0080FF).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Verified,
                    contentDescription = null,
                    tint = Color(0xFF0080FF),
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text("Верификация аккаунта", color = Color(0xFF0A0A0A), fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text("Подтвердите свою личность для получения синей галочки верификации", color = Color(0x8C8C8C), fontSize = 15.sp)

            Spacer(Modifier.height(24.dp))

            when {
                loading -> {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                isVerified -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF00BA7C).copy(alpha = 0.1f))
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00BA7C))
                                Spacer(Modifier.width(8.dp))
                                Text("Аккаунт верифицирован", color = Color(0xFF00BA7C), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                error != null -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF91880).copy(alpha = 0.1f))
                    ) {
                        Text(error!!, color = Color(0xFFF91880), modifier = Modifier.padding(16.dp))
                    }
                }
                else -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F7))
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Требования для верификации:", color = Color(0xFF0A0A0A), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            VerificationRequirement("Заполненный профиль с аватаром")
                            VerificationRequirement("Биография (о себе)")
                            VerificationRequirement("Активность в течение 30+ дней")
                            VerificationRequirement("Наличие постов (10+)")
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                requesting = true
                                runCatching { container.repository.requestVerification() }
                                    .onSuccess { loadStatus() }
                                    .onFailure { error = it.message }
                                requesting = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !requesting
                    ) {
                        if (requesting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Text("Запросить верификацию", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerificationRequirement(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF0080FF), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color(0xFF0A0A0A), fontSize = 14.sp)
    }
    Spacer(Modifier.height(6.dp))
}
