package ru.itdo.app.data.model

import com.google.gson.annotations.SerializedName

data class User(
    val id: Int,
    val username: String,
    @SerializedName("display_name") val displayName: String? = null,
    val avatar: String? = null,
    val bio: String? = null,
    @SerializedName("is_admin") val isAdmin: Boolean = false,
    @SerializedName("followers_count") val followersCount: Int = 0,
    @SerializedName("following_count") val followingCount: Int = 0,
    @SerializedName("is_following") val isFollowing: Boolean = false
)

data class Post(
    val id: Int,
    @SerializedName("author") val author: User? = null,
    val text: String? = null,
    val media: List<String>? = null,
    @SerializedName("likes_count") val likesCount: Int = 0,
    @SerializedName("comments_count") val commentsCount: Int = 0,
    @SerializedName("reposts_count") val repostsCount: Int = 0,
    @SerializedName("liked_by_me") val likedByMe: Boolean = false,
    @SerializedName("liked") val liked: Boolean = false,
    val bookmarked: Boolean = false,
    @SerializedName("my_reaction") val myReaction: String? = null,
    @SerializedName("created_at") val createdAt: Long = 0
)

data class FeedResponse(
    val posts: List<Post> = emptyList(),
    val page: Int = 1,
    val total: Int = 0,
    val error: String? = null
)

data class Comment(
    val id: Int,
    val author: User? = null,
    val text: String? = null,
    val media: List<String>? = null,
    @SerializedName("likes_count") val likesCount: Int = 0,
    @SerializedName("comments_count") val commentsCount: Int = 0,
    val liked: Boolean = false,
    @SerializedName("my_reaction") val myReaction: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class CommentsResponse(
    val comments: List<Comment> = emptyList(),
    val total: Int = 0,
    val error: String? = null
)

data class UserListResponse(
    val users: List<User> = emptyList(),
    val total: Int = 0,
    val error: String? = null
)

data class SearchResponse(
    val users: List<User> = emptyList(),
    val posts: List<Post> = emptyList(),
    val error: String? = null
)

data class AppNotification(
    val id: Int,
    val type: String? = null,
    @SerializedName("from_user") val fromUser: User? = null,
    val text: String? = null,
    @SerializedName("is_read") val isRead: Boolean = false,
    @SerializedName("created_at") val createdAt: String? = null
)

data class NotificationsResponse(
    val notifications: List<AppNotification> = emptyList(),
    val total: Int = 0,
    val error: String? = null
)

data class UnreadCountResponse(
    val count: Int = 0,
    val error: String? = null
)

data class GroupInfo(
    val id: Int,
    val title: String? = null,
    val description: String? = null,
    val avatar: String? = null,
    @SerializedName("created_by") val createdBy: Int = 0,
    val members: List<User> = emptyList()
)

data class GroupInfoResponse(
    val group: GroupInfo? = null,
    val error: String? = null
)

data class LoginRequest(
    val username: String,
    val password: String,
    @SerializedName("totp_code") val totpCode: String? = null,
    // ВАЖНО: бэкенд читает именно поле hcaptcha_token, а не "h-captcha-response"
    // (см. api/auth/login.php -> requireLoginCaptcha($body) -> hcaptchaVerify
    // читает $body['hcaptcha_token']; см. api/lib/security.php).
    @SerializedName("hcaptcha_token") val hcaptchaToken: String? = null
)

data class AuthResponse(
    val user: User? = null,
    @SerializedName("access_token") val accessToken: String? = null,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    val error: String? = null,
    @SerializedName("two_factor_required") val twoFactorRequired: Boolean = false,
    val banned: Boolean = false
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    // ВАЖНО: см. комментарий в LoginRequest — бэкенд ждёт hcaptcha_token
    // (см. api/auth/register.php).
    @SerializedName("hcaptcha_token") val hcaptchaToken: String? = null
)

/** Ответ api/auth/registration_status.php — актуальный публичный sitekey hCaptcha. */
data class RegistrationStatusResponse(
    @SerializedName("captcha_provider") val captchaProvider: String? = null,
    @SerializedName("hcaptcha_sitekey") val hcaptchaSitekey: String? = null,
    @SerializedName("registration_open") val registrationOpen: Boolean = true,
    val error: String? = null
)

data class Conversation(
    val id: Int,
    @SerializedName("peer") val peer: User? = null,
    val title: String? = null,
    @SerializedName("last_message") val lastMessage: Message? = null,
    @SerializedName("unread_count") val unreadCount: Int = 0,
    @SerializedName("is_group") val isGroup: Boolean = false
)

data class ConversationsResponse(
    val conversations: List<Conversation> = emptyList(),
    val error: String? = null
)

data class Message(
    val id: Int,
    @SerializedName("conversation_id") val conversationId: Int = 0,
    @SerializedName("sender_id") val senderId: Int = 0,
    val text: String? = null,
    @SerializedName("created_at") val createdAt: Long = 0
)

data class MessagesResponse(
    val messages: List<Message> = emptyList(),
    val error: String? = null
)

data class SendMessageRequest(
    @SerializedName("conversation_id") val conversationId: Int,
    val text: String
)

data class PixelBoardResponse(
    val width: Int = 0,
    val height: Int = 0,
    val pixels: List<String> = emptyList(),
    val error: String? = null
)

data class PlacePixelRequest(
    val x: Int,
    val y: Int,
    val color: String
)

data class SimpleOk(
    val ok: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)
