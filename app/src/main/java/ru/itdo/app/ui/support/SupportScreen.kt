package ru.itdo.app.ui.support

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
 * Support screen — 1:1 with iOS SupportView.
 */
@Composable
fun SupportScreen(
    container: AppContainer,
    onNavigateBack: () -> Unit = {}
) {
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var success by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Поддержка") },
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
            // Header
            Box(
                Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0080FF).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.QuestionMark,
                    contentDescription = null,
                    tint = Color(0xFF0080FF),
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text("Как мы можем помочь?", color = Color(0xFF0A0A0A), fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text("Опишите вашу проблему и мы свяжемся с вами", color = Color(0x8C8C8C), fontSize = 15.sp)

            Spacer(Modifier.height(24.dp))

            // Subject
            Text("Тема", color = Color(0x8C8C8C), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Кратко опишите проблему") }
            )

            Spacer(Modifier.height(16.dp))

            // Message
            Text("Сообщение", color = Color(0x8C8C8C), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Подробно опишите вашу проблему...") }
            )

            Spacer(Modifier.height(24.dp))

            // Error
            error?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF91880).copy(alpha = 0.1f))
                ) {
                    Text(it, color = Color(0xFFF91880), modifier = Modifier.padding(12.dp))
                }
                Spacer(Modifier.height(16.dp))
            }

            // Success
            if (success) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF00BA7C).copy(alpha = 0.1f))
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00BA7C))
                            Spacer(Modifier.width(8.dp))
                            Text("Тикет создан!", color = Color(0xFF00BA7C), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Submit button
            Button(
                onClick = {
                    scope.launch {
                        sending = true
                        runCatching {
                            container.repository.createSupportTicket(subject, message)
                        }
                            .onSuccess { success = true; subject = ""; message = "" }
                            .onFailure { error = it.message }
                        sending = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !sending && subject.isNotBlank() && message.isNotBlank()
            ) {
                if (sending) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text("Отправить", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
