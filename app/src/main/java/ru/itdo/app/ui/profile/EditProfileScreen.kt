package ru.itdo.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import ru.itdo.app.core.AppContainer
import ru.itdo.app.data.model.User
import ru.itdo.app.ui.components.PinBadges

/**
 * EditProfile screen — 1:1 with iOS EditProfileView.
 */
@Composable
fun EditProfileScreen(
    container: AppContainer,
    currentUser: User,
    onNavigateBack: () -> Unit = {}
) {
    var name by remember { mutableStateOf(currentUser.name ?: "") }
    var bio by remember { mutableStateOf(currentUser.bio ?: "") }
    var saving by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редактировать профиль") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        scope.launch {
                            saving = true
                            runCatching {
                                container.repository.updateProfile(
                                    name = name.takeIf { it.isNotBlank() },
                                    bio = bio.takeIf { it.isNotBlank() }
                                )
                            }
                                .onSuccess { saveMessage = "Сохранено" }
                                .onFailure { saveMessage = it.message }
                            saving = false
                        }
                    }) {
                        if (saving) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        } else {
                            Text("Сохранить", color = MaterialTheme.colorScheme.primary)
                        }
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
            // Avatar
            Box(
                Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0F0F2)),
                contentAlignment = Alignment.Center
            ) {
                if (currentUser.avatar != null) {
                    AsyncImage(model = currentUser.avatar, contentDescription = null, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0x8C8C8C))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Изменить аватар", color = MaterialTheme.colorScheme.primary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)

            Spacer(Modifier.height(24.dp))

            // Name
            Text("Имя", color = Color(0x8C8C8C), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF0080FF),
                    unfocusedBorderColor = Color(0x1F000000)
                )
            )

            Spacer(Modifier.height(16.dp))

            // Username (read-only)
            Text("Логин", color = Color(0x8C8C8C), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = "@${currentUser.username}",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF0080FF),
                    unfocusedBorderColor = Color(0x1F000000),
                    disabledTextColor = Color(0xFF0A0A0A)
                )
            )

            Spacer(Modifier.height(16.dp))

            // Bio
            Text("О себе", color = Color(0x8C8C8C), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF0080FF),
                    unfocusedBorderColor = Color(0x1F000000)
                )
            )

            // Current badges
            Spacer(Modifier.height(24.dp))
            Text("Ваши бейджи:", color = Color(0xFF0A0A0A), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            PinBadges(
                isVerified = currentUser.isVerified,
                isNuksta = currentUser.isNuksta,
                isBanned = currentUser.isBanned,
                pinChoice = currentUser.pinChoice
            )

            saveMessage?.let { msg ->
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF00BA7C).copy(alpha = 0.1f))
                ) {
                    Text(
                        msg,
                        color = Color(0xFF00BA7C),
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
