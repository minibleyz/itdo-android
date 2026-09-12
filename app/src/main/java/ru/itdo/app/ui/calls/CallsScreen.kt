package ru.itdo.app.ui.calls

import androidx.compose.foundation.background
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
import ru.itdo.app.data.model.Call

/**
 * Calls screen — call history.
 * 1:1 with iOS CallsView.
 */
@Composable
fun CallsScreen(
    calls: List<Call> = emptyList(),
    onNavigateBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Звонки") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (calls.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(calls, key = { it.id }) { call ->
                        CallRow(call)
                        Divider(color = Color(0x1F000000))
                    }
                }
            }
        }
    }
}

@Composable
private fun CallRow(call: Call) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFFF0F0F2)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                when {
                    // Поле в модели Call называется "type", а не "callType".
                    call.type == "video" -> Icons.Default.Videocam
                    call.status == "missed" -> Icons.Default.CallEnd
                    else -> Icons.Default.Call
                },
                contentDescription = null,
                tint = when {
                    call.status == "missed" -> Color(0xFFF91880)
                    call.status == "declined" -> Color(0xFFFF8C00)
                    else -> Color(0xFF00BA7C)
                },
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        // Info
        Column(Modifier.weight(1f)) {
            Text(
                call.peer?.name ?: call.peer?.username ?: "Неизвестный",
                color = Color(0xFF0A0A0A),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                getCallLabel(call),
                color = Color(0x8C8C8C),
                fontSize = 13.sp
            )
        }

        // Duration
        if (call.duration > 0) {
            Text(
                formatDuration(call.duration),
                color = Color(0x8C8C8C),
                fontSize = 13.sp
            )
        }
    }
}

private fun getCallLabel(call: Call): String {
    return when (call.status) {
        "missed" -> if (call.isOutgoing) "Исходящий (пропущен)" else "Входящий (пропущен)"
        "declined" -> if (call.isOutgoing) "Исходящий (отклонён)" else "Входящий (отклонён)"
        else -> if (call.isOutgoing) "Исходящий" else "Входящий"
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "${m}:${String.format("%02d", s)}"
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Call,
            contentDescription = null,
            tint = Color(0x668C8C8C),
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text("История звонков пуста", color = Color(0x8C8C8C))
    }
}
