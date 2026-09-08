package ru.itdo.app.data.model

import com.google.gson.annotations.SerializedName
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

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
    val email: String? = null,
    val avatar: String? = null,
    val banner: String? = null,
    val bio: String? = null,
    val role: String? = null,
    @SerializedName("is_admin") val isAdmin: Boolean = false,
    @SerializedName("is_verified") val isVerified: Boolean = false,
    @SerializedName("is_nuksta") val isNuksta: Boolean = false,
    @SerializedName("is_banned") val isBanned: Boolean = false,
    @SerializedName("pin_choice") val pinChoice: String? = null,
    val coins: Int = 0,
    @SerializedName("posts_count") val postsCount: Int = 0,
    @SerializedName("followers_count") val followersCount: Int = 0,
    @SerializedName("following_count") val followingCount: Int = 0,
    @SerializedName("is_following") val isFollowing: Boolean = false,
    @SerializedName("is_blocked") val isBlocked: Boolean = false,
    @SerializedName("theme_custom") val themeCustom: ThemeCustom? = null
) {
    /** Отображаемое имя с фолбэком на username — как displayName у iOS PostAuthor. */
    val displayName: String get() = name?.takeIf { it.isNotBlank() } ?: username
}

// ---- Сессии (auth/sessions.php) ----

data class Session(
    @SerializedName("session_id") val sessionId: String = "",
    @SerializedName("is_current") val isCurrent: Boolean = false,
    val ip: String? = null,
    @SerializedName("user_agent") val userAgent: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("expires_at") val expiresAt: Int? = null
)

data class SessionsResponse(
    val sessions: List<Session> = emptyList(),
    val error: String? = null
)

// ---- Тема / кастомизация ----

data class ThemeCustom(
    val vars: Map<String, String>? = null,
    val gradient: ThemeGradient? = null
)

data class ThemeGradient(
    val c1: String = "",
    val c2: String = "",
    val angle: Int = 0
)

// ---- Посты / лента ----

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

// ---- Комментарии ----

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

// ---- Поиск / пользователи ----

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

// ---- Уведомления ----

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

// ---- Группы ----

data class GroupInfo(
    val id: Int,
    val title: String? = null,
    val description: String? = null,
    val avatar: String? = null,
    @SerializedName("created_by") val createdBy: Int = 0,
    val members: List<User> = emptyList()
)

/** Расширенный ответ groups/info.php — содержит my_role, owner_id, invite_link. */
data class GroupInfoResponse(
    val group: GroupInfo? = null,
    val title: String? = null,
    val description: String? = null,
    val members: List<GroupMember>? = null,
    @SerializedName("my_role") val myRole: String? = null,
    @SerializedName("owner_id") val ownerId: Int? = null,
    @SerializedName("invite_link") val inviteLink: String? = null,
    val error: String? = null
)

data class GroupMember(
    val id: Int,
    val username: String = "",
    val name: String? = null,
    val avatar: String? = null,
    val role: String? = null,
    @SerializedName("is_nuksta") val isNuksta: Boolean = false,
    @SerializedName("is_verified") val isVerified: Boolean = false
)

// ---- Авторизация ----

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
    @SerializedName("registration_disabled") val registrationDisabled: Boolean? = null,
    @SerializedName("hide_register_link") val hideRegisterLink: Boolean? = null,
    val error: String? = null
)

// ---- Диалоги / сообщения ----

data class Conversation(
    val id: Int,
    @SerializedName("peer") val peer: User? = null,
    val title: String? = null,
    @SerializedName("last_message") val lastMessage: Message? = null,
    @SerializedName("unread_count") val unreadCount: Int = 0,
    @SerializedName("is_group") val isGroup: Boolean = false,
    // Расширенные поля (как в iOS Conversation)
    @SerializedName("partner_id") val partnerId: Int? = null,
    val name: String? = null,
    val username: String? = null,
    val avatar: String? = null,
    @SerializedName("is_nuksta") val isNuksta: Boolean = false,
    @SerializedName("is_verified") val isVerified: Boolean = false,
    @SerializedName("member_count") val memberCount: Int? = null,
    @SerializedName("last_message_time") val lastMessageTime: String? = null,
    @SerializedName("last_message_mine") val lastMessageMine: Boolean = false,
    @SerializedName("last_message_read") val lastMessageRead: Boolean = false,
    val unread: Int = 0,
    val online: Boolean = false,
    @SerializedName("last_seen") val lastSeen: Int? = null,
    @SerializedName("pending_sent") val pendingSent: Boolean = false,
    @SerializedName("blocked_by_me") val blockedByMe: Boolean = false,
    @SerializedName("blocked_me") val blockedMe: Boolean = false,
    val archived: Boolean = false,
    @SerializedName("is_bot") val isBot: Boolean = false
) {
    /** Отображаемое имя диалога — название группы или имя собеседника. */
    val displayName: String
        get() = if (isGroup) {
            title?.takeIf { it.isNotBlank() } ?: name?.takeIf { it.isNotBlank() } ?: "Группа"
        } else {
            name?.takeIf { it.isNotBlank() } ?: username?.takeIf { it.isNotBlank() } ?: "Чат"
        }
}

data class ConversationsResponse(
    val conversations: List<Conversation> = emptyList(),
    val error: String? = null
)

/** Превью ответа на сообщение (reply). */
data class MessageReplyPreview(
    val id: Int = 0,
    @SerializedName("sender_name") val senderName: String = "",
    val text: String = "",
    @SerializedName("media_type") val mediaType: String? = null
)

data class Message(
    val id: Int = 0,
    @SerializedName("conversation_id") val conversationId: Int = 0,
    @SerializedName("sender_id") val senderId: Int = 0,
    @SerializedName("sender_name") val senderName: String? = null,
    @SerializedName("sender_is_deleted") val senderIsDeleted: Boolean = false,
    val text: String? = null,
    @SerializedName("media_url") val mediaUrl: String? = null,
    @SerializedName("media_type") val mediaType: String? = null,
    @SerializedName("media_title") val mediaTitle: String? = null,
    val duration: Int? = null,
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("reply_to") val replyTo: Int? = null,
    val reply: MessageReplyPreview? = null,
    val edited: Boolean = false,
    @SerializedName("forward_from_name") val forwardFromName: String? = null,
    val read: Boolean = false,
    @SerializedName("is_outgoing") val isOutgoing: Boolean = false,
    // Поля для карточек звонков (kind == "call")
    val kind: String = "message",
    @SerializedName("call_type") val callType: String? = null,
    @SerializedName("call_status") val callStatus: String? = null
)

data class MessagesResponse(
    val messages: List<Message> = emptyList(),
    val error: String? = null
)

data class SendMessageRequest(
    @SerializedName("conv_id") val conversationId: Int,
    val text: String? = null,
    @SerializedName("media_url") val mediaUrl: String? = null,
    @SerializedName("media_type") val mediaType: String? = null,
    val duration: Int? = null,
    @SerializedName("reply_to") val replyTo: Int? = null
)

/** Ответ messages/send.php — id нового сообщения. */
data class SendMessageResponse(
    val id: Int = 0,
    val ok: Boolean = false,
    val error: String? = null
)

/** Ответ messages/typing_status.php — список conv_id, в которых собеседник печатает. */
data class TypingStatusResponse(
    val typing: List<Int> = emptyList(),
    val error: String? = null
)

/** Запрос на сообщение (от незнакомца). */
data class MessageRequest(
    @SerializedName("conv_id") val conversationId: Int = 0,
    val user: User? = null,
    val text: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class MessageRequestsResponse(
    val requests: List<MessageRequest> = emptyList(),
    val error: String? = null
)

// ---- Звонки ----

data class Call(
    val id: Int = 0,
    @SerializedName("conv_id") val conversationId: Int = 0,
    @SerializedName("caller_id") val callerId: Int = 0,
    @SerializedName("callee_id") val calleeId: Int = 0,
    val type: String = "audio",
    val status: String = "ended",
    @SerializedName("started_at") val startedAt: String? = null,
    @SerializedName("connected_at") val connectedAt: String? = null,
    @SerializedName("ended_at") val endedAt: String? = null,
    val duration: Int = 0,
    @SerializedName("is_outgoing") val isOutgoing: Boolean = false,
    val peer: CallPeer? = null
)

data class CallPeer(
    val id: Int = 0,
    val name: String? = null,
    val username: String? = null,
    val avatar: String? = null
)

data class CallResponse(
    val call: Call? = null,
    @SerializedName("call_id") val callId: Int? = null,
    val existing: Boolean? = null,
    val error: String? = null
)

data class CallHistoryResponse(
    val calls: List<Call> = emptyList(),
    val error: String? = null
)

// ---- Пиксель-баттл ----

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

// ---- Простой ответ ok/success ----

data class SimpleOk(
    val ok: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)

// ---- Клипы (api/clips/*.php) ----

data class Clip(
    val id: Int,
    @SerializedName("user_id") val userId: Int = 0,
    val username: String = "",
    val avatar: String? = null,
    @SerializedName("is_verified") val isVerified: Boolean = false,
    val title: String? = null,
    val description: String? = null,
    @SerializedName("video_url") val videoUrl: String = "",
    @SerializedName("thumbnail_url") val thumbnailUrl: String? = null,
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
    val page: Int = 1,
    val total: Int = 0,
    val error: String? = null
)

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

// ============================================================
// Новые модели — синхронизация с iOS (Models.swift)
// ============================================================

// ---- Трансляции (streams/*.php) ----

data class LiveStream(
    val id: Int = 0,
    @SerializedName("user_id") val userId: Int = 0,
    val username: String = "",
    val avatar: String? = null,
    val title: String = "",
    val description: String? = null,
    @SerializedName("is_live") val isLive: Boolean = false,
    @SerializedName("stream_key") val streamKey: String = "",
    @SerializedName("hls_url") val hlsUrl: String = "",
    @SerializedName("recording_url") val recordingUrl: String? = null,
    val likes: Int = 0,
    val viewers: Int = 0,
    @SerializedName("created_at") val createdAt: String? = null
)

data class StreamListResponse(
    val streams: List<LiveStream> = emptyList(),
    val error: String? = null
)

data class StreamResponse(
    val stream: LiveStream? = null,
    val error: String? = null
)

/** Сообщение чата эфира. */
data class StreamChatMessage(
    val id: Int = 0,
    val type: String? = null,
    val name: String? = null,
    val username: String? = null,
    val text: String? = null,
    val amount: Int? = null,
    @SerializedName("is_nuksta") val isNuksta: Boolean = false
) {
    val isDonate: Boolean get() = type == "donate"
    val displayName: String get() = name ?: username ?: ""
}

data class StreamChatListResponse(
    val messages: List<StreamChatMessage> = emptyList(),
    val error: String? = null
)

data class StreamDonateResponse(
    @SerializedName("new_balance") val newBalance: Int = 0,
    val error: String? = null
)

data class StreamLikeResponse(
    val liked: Boolean = false,
    val likes: Int = 0,
    val error: String? = null
)

data class StreamViewersResponse(
    val viewers: Int = 0,
    val error: String? = null
)

data class CreateStreamResponse(
    @SerializedName("stream_id") val streamId: Int? = null,
    @SerializedName("stream_key") val streamKey: String? = null,
    @SerializedName("rtmp_url") val rtmpUrl: String? = null,
    @SerializedName("hls_url") val hlsUrl: String? = null,
    val error: String? = null
)

// ---- Агент (AI-чат, agent/*.php) ----

data class AgentConversation(
    val id: Int = 0,
    @SerializedName("user_id") val userId: Int = 0,
    val title: String? = null,
    @SerializedName("last_message_at") val lastMessageAt: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class AgentConversationsResponse(
    val conversations: List<AgentConversation> = emptyList(),
    val error: String? = null
)

/** Результат вызова инструмента ИИ-агента. */
data class ToolEventResult(
    val error: String? = null
)

/** Один вызов инструмента ИИ-агента. */
data class ToolEvent(
    val name: String = "",
    val result: ToolEventResult? = null
) {
    val isError: Boolean get() = !result?.error.isNullOrEmpty()
}

data class AgentMessage(
    val id: Int = 0,
    @SerializedName("conversation_id") val conversationId: Int = 0,
    val role: String = "",
    val content: String = "",
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("tool_events") val toolEvents: List<ToolEvent>? = null
)

data class AgentMessagesResponse(
    val conversation: AgentConversation? = null,
    val messages: List<AgentMessage> = emptyList(),
    val error: String? = null
)

// ---- Explore / Trending (explore/trending.php) ----

data class TrendingHashtag(
    val tag: String = "",
    val count: Int = 0
)

data class TrendingResponse(
    val posts: List<Post> = emptyList(),
    val hashtags: List<TrendingHashtag> = emptyList(),
    val error: String? = null
)

// ---- Перевод (posts/translate.php) ----

data class TranslateResponse(
    val text: String? = null,
    val translated: String? = null,
    val lang: String? = null,
    val error: String? = null
) {
    /** Берём первое непустое — translated или text. */
    val resolvedText: String?
        get() = translated?.takeIf { it.isNotEmpty() } ?: text?.takeIf { it.isNotEmpty() }
}

// ---- Подарки (gifts/*.php) ----

data class GiftCatalogItem(
    val id: String = "",
    val emoji: String = "",
    val name: String = "",
    val price: Int = 0
)

data class GiftCatalogResponse(
    val gifts: List<GiftCatalogItem> = emptyList(),
    val error: String? = null
)

data class ReceivedGift(
    val id: Int = 0,
    val emoji: String = "",
    val name: String = "",
    val message: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("from_id") val fromId: Int = 0,
    @SerializedName("from_name") val fromName: String? = null,
    @SerializedName("from_username") val fromUsername: String? = null
)

data class ReceivedGiftsResponse(
    val gifts: List<ReceivedGift> = emptyList(),
    @SerializedName("total_count") val totalCount: Int = 0,
    val error: String? = null
)

data class SendGiftResponse(
    val success: Boolean = false,
    @SerializedName("new_balance") val newBalance: Int? = null,
    val error: String? = null
)

// ---- Плейлисты (playlists/*.php) ----

data class PlaylistItem(
    @SerializedName("item_type") val itemType: String = "",
    @SerializedName("item_id") val itemId: Int = 0,
    @SerializedName("added_at") val addedAt: String? = null
)

data class Playlist(
    val id: Int = 0,
    @SerializedName("user_id") val userId: Int? = null,
    val name: String = "",
    val description: String? = null,
    val type: String? = null,
    @SerializedName("is_default") val isDefault: Boolean = false,
    val items: List<PlaylistItem>? = null,
    val cover: String? = null
)

data class PlaylistsResponse(
    val playlists: List<Playlist> = emptyList(),
    val error: String? = null
)

data class PlaylistResponse(
    val playlist: Playlist? = null,
    val error: String? = null
)

data class LikedVideosResponse(
    val videos: List<Post> = emptyList(),
    val clips: List<Clip> = emptyList(),
    val error: String? = null
)

// ---- Поддержка (support/*.php) ----

data class SupportTicket(
    @SerializedName("ticket_id") val ticketId: String = "",
    val subject: String = "",
    val status: String = "",
    @SerializedName("created_at") val createdAt: String? = null
)

data class SupportMessage(
    @SerializedName("ticket_id") val ticketId: String = "",
    @SerializedName("user_id") val userId: Int? = null,
    val username: String? = null,
    val message: String = "",
    @SerializedName("is_admin") val isAdmin: Boolean = false,
    @SerializedName("created_at") val createdAt: String? = null
)

data class SupportTicketsResponse(
    val tickets: List<SupportTicket> = emptyList(),
    val error: String? = null
)

data class SupportTicketDetailResponse(
    val ticket: SupportTicket? = null,
    val messages: List<SupportMessage> = emptyList(),
    val error: String? = null
)

data class SupportCreateResponse(
    val ok: Boolean = false,
    @SerializedName("ticket_id") val ticketId: String = "",
    val error: String? = null
)

// ---- Статьи (articles/*.php) ----

data class ArticleAuthor(
    val id: Int = 0,
    val username: String = "",
    val name: String? = null,
    val avatar: String? = null,
    @SerializedName("is_verified") val isVerified: Boolean = false
)

data class Article(
    val id: Int = 0,
    val title: String = "",
    val content: String = "",
    val cover: String? = null,
    val tags: List<String>? = null,
    @SerializedName("likes_count") val likesCount: Int = 0,
    @SerializedName("views_count") val viewsCount: Int = 0,
    @SerializedName("comments_count") val commentsCount: Int = 0,
    @SerializedName("created_at") val createdAt: String? = null,
    val author: ArticleAuthor? = null
)

data class ArticlesResponse(
    val articles: List<Article> = emptyList(),
    val page: Int = 1,
    val total: Int = 0,
    val error: String? = null
)

data class ArticleResponse(
    val article: Article? = null,
    val error: String? = null
)

data class ArticleLikeResponse(
    val liked: Boolean = false,
    val error: String? = null
)

// ---- Объявления (announcements/*.php) ----

data class Announcement(
    val id: Int = 0,
    val title: String? = null,
    val text: String? = null,
    val message: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
) {
    /** Текст объявления — text или message, что есть. */
    val body: String get() = text ?: message ?: ""
}

data class AnnouncementsResponse(
    val announcements: List<Announcement> = emptyList(),
    val error: String? = null
)

// ---- Кошелёк / монеты (coins/get.php) ----

data class WalletResponse(
    val balance: Int = 0,
    val transactions: List<CoinTransaction>? = null,
    val error: String? = null
)

data class CoinTransaction(
    val id: Int = 0,
    val amount: Int = 0,
    val reason: String? = null,
    @SerializedName("balance_after") val balanceAfter: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null
) {
    /** Бэкенд не шлёт type — выводим из знака amount. */
    val type: String get() = if (amount >= 0) "earn" else "spend"
    val description: String? get() = reason
}

// ---- Лидерборд (leaderboard/get.php) ----

data class LeaderboardEntry(
    val id: Int = 0,
    val username: String = "",
    val name: String? = null,
    val avatar: String? = null,
    @SerializedName("is_verified") val isVerified: Boolean = false,
    @SerializedName("is_nuksta") val isNuksta: Boolean = false,
    val coins: Int = 0,
    val followers: Int = 0,
    val posts: Int = 0,
    @SerializedName("is_me") val isMe: Boolean = false,
    val rank: Int = 0
)

data class LeaderboardSection(
    val top: List<LeaderboardEntry> = emptyList(),
    @SerializedName("my_rank") val myRank: Int? = null,
    val me: LeaderboardEntry? = null,
    val total: Int = 0
)

data class LeaderboardResponse(
    val coins: LeaderboardSection? = null,
    val followers: LeaderboardSection? = null,
    val posts: LeaderboardSection? = null,
    val error: String? = null
)

// ---- Квесты (quests/*.php) ----

data class Quest(
    val id: String = "",
    val title: String = "",
    @SerializedName("desc") val description: String? = null,
    val icon: String? = null,
    val reward: Int = 0,
    val progress: Int = 0,
    @SerializedName("goal") val target: Int = 0,
    val completed: Boolean = false,
    val claimed: Boolean = false,
    @SerializedName("expires_at") val expiresAt: String? = null
)

data class QuestsResponse(
    val coins: Int = 0,
    val quests: List<Quest> = emptyList(),
    val error: String? = null
)

// ---- Нукста (nuksta/*.php) ----

data class NukstaResponse(
    @SerializedName("is_active") val isActive: Boolean = false,
    @SerializedName("expires_at") val expiresAt: String? = null,
    @SerializedName("days_left") val daysLeft: Int? = null,
    val error: String? = null
) {
    val subscribed: Boolean get() = isActive
}

// ---- Подписки / блокировка ----

data class FollowResponse(
    val following: Boolean = false,
    val error: String? = null
)

data class BlockResponse(
    val blocked: Boolean = false,
    val error: String? = null
)

data class OnlineListResponse(
    @SerializedName("online_ids") val onlineIds: List<Int>? = null,
    val error: String? = null
)

// ---- Рекомендации (users/suggestions.php) ----

data class UserSuggestion(
    val id: Int = 0,
    val username: String = "",
    val name: String? = null,
    val avatar: String? = null,
    @SerializedName("is_verified") val isVerified: Boolean = false
)

data class SuggestionsResponse(
    val users: List<UserSuggestion> = emptyList(),
    val suggestions: List<UserSuggestion> = emptyList(),
    val error: String? = null
)

// ---- Профиль (users/profile.php) ----

data class UserProfileResponse(
    val user: User? = null,
    val posts: List<Post> = emptyList(),
    val error: String? = null
)

// ---- Верификация (verification/*.php) ----

data class VerificationResponse(
    @SerializedName("is_verified") val isVerified: Boolean = false,
    val status: String? = null,
    val error: String? = null
)

// ---- Реферальная система (users/referral.php) ----

data class ReferralResponse(
    @SerializedName("referral_code") val referralCode: String = "",
    @SerializedName("referral_link") val referralLink: String = "",
    @SerializedName("invited_count") val invitedCount: Int = 0,
    @SerializedName("invited_users") val invitedUsers: List<UserSuggestion>? = null,
    val error: String? = null
)

// ---- Загрузка файлов ----

data class UploadResponse(
    val url: String = "",
    val type: String? = null,
    val title: String? = null,
    val error: String? = null
)

// ---- Версия приложения (version.php) ----

data class VersionResponse(
    val version: String = "",
    @SerializedName("min_version") val minVersion: String? = null,
    val error: String? = null
)

// ---- Жалобы (reports/*.php) ----

data class Report(
    val id: Int = 0,
    @SerializedName("reporter_id") val reporterId: Int = 0,
    @SerializedName("target_id") val targetId: Int = 0,
    @SerializedName("target_type") val targetType: String = "",
    val reason: String? = null,
    val status: String = "",
    @SerializedName("created_at") val createdAt: String? = null
)

data class ReportsResponse(
    val reports: List<Report> = emptyList(),
    val error: String? = null
)

// ---- Ответ на закреп поста (posts/pin.php) ----

data class PinPostResponse(
    @SerializedName("is_pinned") val isPinned: Boolean = false,
    val error: String? = null
)

// ---- Ответ invite_link группы (groups/invite_link.php) ----

data class GroupInviteLinkResponse(
    val link: String = "",
    val error: String? = null
)

// ---- Ответ archive диалога (messages/archive.php) ----

data class ArchiveConversationResponse(
    val archived: Boolean = false,
    val error: String? = null
)

// ---- Ответ start диалога (messages/start.php) ----

data class StartConversationResponse(
    @SerializedName("conv_id") val convId: Int = 0,
    val error: String? = null
)

// ---- Ответ create_group (messages/create_group.php) ----

data class CreateGroupResponse(
    @SerializedName("conv_id") val convId: Int = 0,
    val error: String? = null
)

// ---- Ответ nuksta/theme.css?format=json ----

data class NukstaThemeResponse(
    val theme: String = "",
    val error: String? = null
)

// ---- WS Info (api/ws/info.php) ----
data class WsInfoResponse(
    val url: String = "",
    val port: Int = 9502,
    val protocol: String = "wss",
    val error: String? = null
)
