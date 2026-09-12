package ru.itdo.app.ui.biometric

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlin.concurrent.thread

/**
 * BiometricPrompt screen — Android equivalent of iOS Face ID / Touch ID.
 * Uses Android BiometricPrompt API (fingerprint + face unlock on supported devices).
 */
@Composable
fun BiometricPromptScreen(
    onAuthenticated: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    // BiometricPrompt требует FragmentActivity, а не произвольный Context —
    // ComponentActivity (база для Compose-активностей) наследуется от него.
    val activity = context as FragmentActivity
    var authResult by remember { mutableStateOf<AuthResult?>(null) }

    LaunchedEffect(Unit) {
        val biometricPrompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    authResult = AuthResult.Success
                    onAuthenticated()
                }

                override fun onAuthenticationFailed() {
                    authResult = AuthResult.Failed
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    authResult = AuthResult.Error(errString.toString())
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        onDismiss()
                    }
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("ITDO PRO")
            .setSubtitle("Войдите с помощью отпечатка или лица")
            .setNegativeButtonText("Отмена")
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                } else {
                    setAllowedAuthenticators(BIOMETRIC_STRONG)
                }
            }
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF000000))) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Fingerprint icon
            Box(
                Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0080FF).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = Color(0xFF0080FF),
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "ITDO PRO",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Используйте отпечаток пальца или Face Unlock",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )

            authResult?.let { result ->
                Spacer(Modifier.height(16.dp))
                when (result) {
                    is AuthResult.Failed -> {
                        Text("Не удалось распознать", color = Color(0xFFF91880), fontSize = 14.sp)
                    }
                    is AuthResult.Error -> {
                        Text(result.message, color = Color(0xFFF91880), fontSize = 14.sp)
                    }
                    else -> {}
                }
            }
        }
    }
}

private sealed interface AuthResult {
    object Success : AuthResult
    object Failed : AuthResult
    data class Error(val message: String) : AuthResult
}

/**
 * Check if biometric authentication is available on the device.
 */
fun isBiometricAvailable(context: android.content.Context): Boolean {
    val biometricManager = BiometricManager.from(context)
    return biometricManager.canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
}
