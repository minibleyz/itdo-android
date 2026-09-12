package ru.itdo.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * PinBadgesView — бейджи верификации, Nuksta, бан, pin choice.
 * 1:1 с iOS PinBadgesView (ITDOApp/Views/Components/PinBadgesView.swift).
 */
@Composable
fun PinBadges(
    isVerified: Boolean = false,
    isNuksta: Boolean = false,
    isBanned: Boolean = false,
    pinChoice: String? = null
) {
    val badges = mutableListOf<Badge>()

    if (isVerified) {
        badges.add(Badge.Verified)
    }
    if (isNuksta) {
        badges.add(Badge.Nuksta)
    }
    if (isBanned) {
        badges.add(Badge.Banned)
    }
    if (!pinChoice.isNullOrEmpty()) {
        badges.add(Badge.PinChoice(pinChoice))
    }

    if (badges.isEmpty()) return

    Row(verticalAlignment = Alignment.CenterVertically) {
        badges.forEachIndexed { index, badge ->
            if (index > 0) Spacer(Modifier.width(4.dp))
            BadgeView(badge)
        }
    }
}

private sealed interface Badge {
    object Verified : Badge
    object Nuksta : Badge
    object Banned : Badge
    data class PinChoice(val choice: String) : Badge
}

@Composable
private fun BadgeView(badge: Badge) {
    when (badge) {
        is Badge.Verified -> {
            Icon(
                Icons.Default.Verified,
                contentDescription = "Верифицирован",
                tint = Color(0xFF0080FF),
                modifier = Modifier.width(14.dp)
            )
        }
        is Badge.Nuksta -> {
            Text(
                "⭐",
                fontSize = 12.sp
            )
        }
        is Badge.Banned -> {
            Text(
                "🚫",
                fontSize = 12.sp
            )
        }
        is Badge.PinChoice -> {
            // Pin choice colors (like iOS)
            val color = when (badge.choice.lowercase()) {
                "blue" -> Color(0xFF0080FF)
                "purple" -> Color(0xFF8B5CF6)
                "orange" -> Color(0xFFFF8C00)
                "green" -> Color(0xFF00BA7C)
                "pink" -> Color(0xFFF91880)
                "gold" -> Color(0xFFFFD700)
                else -> Color(0xFF8C8C8C)
            }
            Text(
                "●",
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
