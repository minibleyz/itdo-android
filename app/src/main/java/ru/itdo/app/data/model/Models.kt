package ru.itdo.app.data.model

import com.google.gson.annotations.SerializedName

// ВАЖНО: `authorFields()` в api/config.php отдаёт объект автора поста с
// полем "name" (реальное отображаемое имя из БД) — раньше здесь было
// @SerializedName("display_name"), которого в этом JSON просто не
// существует, поэтому имя автора никогда не парсилось и везде подставлялся
// username. Смотри также users/profile.php — там та же БД-запись напрямую,
// тоже поле "name", не "display_name".
data class User(
    val id: Int,
    val username: String,
    @SerializedName("name") val name: String? = null,
    val avatar: String? = null,
    val bio: String? = null,
    @SerializedName("is_admin") val isAdmin: Boolean = false,
    @SerializedName("is_verified") val isVerified: Boolean = false,
    @SerializedName("is_nuksta") val isNuksta: Boolean = false,
    @SerializedName("is_banned") val isBanned: Boolean = false,
    @SerializedName("pin_choice") val pinChoice: String? = null,
    @SerializedName("followers_count") val followersCount: Int = 0,
    @SerializedName("following_count") val followingCount: Int = 0,
    @SerializedName("is_following") val isFollowing: Boolean = false,
    @SerializedName("is_blocked") val isBlocked: Boolean = false
) {
    /** Отображаемое имя с фолбэком на username — как displayName у iOS PostAuthor. */
    val displayName: String get() = name?.takeIf { it.isNotBlank() } ?: username
}

data class PostMedia(
    val type: String? = null,
    val url: String? = null
)

data class PollOption(
    val id: Int,
    val text: String,
    val votes: Int = 0
)

data class PostPoll(
    val options: List<PollOption> = emptyList(),
    @SerializedName("ends_at") val endsAt: String? = null,
    /** id варианта, за который проголосовал текущий юзер, либо null. */
    val voted: Int? = null
) {
    val totalVotes: Int get() = options.sumOf { it.votes }
}

/** Прикреплённый трек — бэкенд шлёт его под ключом "music". */
data class PostTrack(
    val title: String? = null,
    val artist: String? = null,
    val url: String? = null,
    val cover: String? = null,
    val duration: Int = 0
)

data class PostQuote(
    val id: Int,
    val text: String? = null,
    val media: List<PostMedia>? = null,
    val music: PostTrack? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    val author: User? = null
)

data class Reaction(
    val emoji: String,
    val count: Int = 0,
    val mine: Boolean = false
)

data class Post(
    val id: Int,
    @SerializedName("author") val author: User? = null,
    val text: String? = null,
    val media: List<PostMedia>? = null,
    /** Ключ в JSON — "music", маппим на track (как track на iOS). */
    @SerializedName("music") val track: PostTrack? = null,
    val poll: PostPoll? = null,
    val quote: PostQuote? = null,
    val reactions: List<Reaction> = emptyList(),
    @SerializedName("likes_count") val likesCount: Int = 0,
    @SerializedName("comments_count") val commentsCount: Int = 0,
    @SerializedName("reposts_count") val repostsCount: Int = 0,
    @SerializedName("views_count") val viewsCount: Int = 0,
    val liked: Boolean = false,
    val reposted: Boolean = false,
    val bookmarked: Boolean = false,
    @SerializedName("my_reaction") val myReaction: String? = null,
    @SerializedName("is_pinned") val isPinned: Boolean = false,
    @SerializedName("admin_pinned") val adminPinned: Boolean = false,
    @SerializedName("created_at") val createdAt: String? = null
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
    // Бэкенд может отдать токен и как access_token, и как token (см. iOS
    // AuthResponse.resolvedToken) — принимаем оба через alternate-имя.
    @SerializedName(value = "access_token", alternate = ["token"]) val accessToken: String? = null,
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

// ---- Clips (api/clips/*.php) — поля списком из list.php (плоский объект,
// не authorFields()): username/avatar/is_verified прямо на клипе. ----
data class Clip(
    val id: Int,
    @SerializedName("user_id") val userId: Int = 0,
    val username: String = "",
    val avatar: String? = null,
    @SerializedName("is_verified") val isVerified: Boolean = false,
    val title: String? = null,
    val description: String? = null,
    @SerializedName("video_url") val videoUrl: String = "",
    val likes: Int = 0,
    val dislikes: Int = 0,
    val views: Int = 0,
    @SerializedName("comments_count") val commentsCount: Int = 0,
    val liked: Boolean = false,
    val disliked: Boolean = false,
    @SerializedName("created_at") val createdAt: String? = null
)

data class ClipsResponse(
    val clips: List<Clip> = emptyList(),
    val error: String? = null
)

// comments.php отдаёт "author": authorFields(user) вложенным объектом,
// а не username/avatar плоско на комментарии — в отличие от iOS-модели
// ClipComment, которая (в самом iOS-клиенте) ждёт их плоско и из-за этого
// у них комментарии к клипам никогда не парсились. Здесь сделано по факту
// ответа сервера, чтобы реально работало.
data class ClipComment(
    val id: Int,
    val text: String = "",
    @SerializedName("created_at") val createdAt: String? = null,
    val author: User? = null
)

data class ClipCommentsResponse(
    val comments: List<ClipComment> = emptyList(),
    val error: String? = null
)

data class ClipVoteResponse(
    val success: Boolean = false,
    val vote: String? = null,
    val likes: Int = 0,
    val dislikes: Int = 0,
    val error: String? = null
)

data class ClipUploadResponse(
    val success: Boolean = false,
    @SerializedName("clip_id") val clipId: Int? = null,
    @SerializedName("video_url") val videoUrl: String? = null,
    val error: String? = null
)
