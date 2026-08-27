package ru.itdo.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Int,
    val username: String,
    @SerialName("display_name") val displayName: String? = null,
    val avatar: String? = null,
    val bio: String? = null,
    @SerialName("is_admin") val isAdmin: Boolean = false,
    @SerialName("followers_count") val followersCount: Int = 0,
    @SerialName("following_count") val followingCount: Int = 0,
    @SerialName("is_following") val isFollowing: Boolean = false
)

@Serializable
data class Post(
    val id: Int,
    @SerialName("author") val author: User? = null,
    val text: String? = null,
    val media: List<String>? = null,
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("comments_count") val commentsCount: Int = 0,
    @SerialName("reposts_count") val repostsCount: Int = 0,
    @SerialName("liked_by_me") val likedByMe: Boolean = false,
    @SerialName("liked") val liked: Boolean = false,
    val bookmarked: Boolean = false,
    @SerialName("my_reaction") val myReaction: String? = null,
    @SerialName("created_at") val createdAt: Long = 0
)

@Serializable
data class FeedResponse(
    val posts: List<Post> = emptyList(),
    val page: Int = 1,
    val total: Int = 0,
    val error: String? = null
)

@Serializable
data class Comment(
    val id: Int,
    val author: User? = null,
    val text: String? = null,
    val media: List<String>? = null,
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("comments_count") val commentsCount: Int = 0,
    val liked: Boolean = false,
    @SerialName("my_reaction") val myReaction: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class CommentsResponse(
    val comments: List<Comment> = emptyList(),
    val total: Int = 0,
    val error: String? = null
)

@Serializable
data class UserListResponse(
    val users: List<User> = emptyList(),
    val total: Int = 0,
    val error: String? = null
)

@Serializable
data class SearchResponse(
    val users: List<User> = emptyList(),
    val posts: List<Post> = emptyList(),
    val error: String? = null
)

@Serializable
data class AppNotification(
    val id: Int,
    val type: String? = null,
    @SerialName("from_user") val fromUser: User? = null,
    val text: String? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class NotificationsResponse(
    val notifications: List<AppNotification> = emptyList(),
    val total: Int = 0,
    val error: String? = null
)

@Serializable
data class UnreadCountResponse(
    val count: Int = 0,
    val error: String? = null
)

@Serializable
data class GroupInfo(
    val id: Int,
    val title: String? = null,
    val description: String? = null,
    val avatar: String? = null,
    @SerialName("created_by") val createdBy: Int = 0,
    val members: List<User> = emptyList()
)

@Serializable
data class GroupInfoResponse(
    val group: GroupInfo? = null,
    val error: String? = null
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
    @SerialName("totp_code") val totpCode: String? = null,
    @SerialName("h-captcha-response") val hcaptchaResponse: String? = null
)

@Serializable
data class AuthResponse(
    val user: User? = null,
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    val error: String? = null,
    @SerialName("two_factor_required") val twoFactorRequired: Boolean = false,
    val banned: Boolean = false
)

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    @SerialName("h-captcha-response") val hcaptchaResponse: String? = null
)

@Serializable
data class Conversation(
    val id: Int,
    @SerialName("peer") val peer: User? = null,
    val title: String? = null,
    @SerialName("last_message") val lastMessage: Message? = null,
    @SerialName("unread_count") val unreadCount: Int = 0,
    @SerialName("is_group") val isGroup: Boolean = false
)

@Serializable
data class ConversationsResponse(
    val conversations: List<Conversation> = emptyList(),
    val error: String? = null
)

@Serializable
data class Message(
    val id: Int,
    @SerialName("conversation_id") val conversationId: Int = 0,
    @SerialName("sender_id") val senderId: Int = 0,
    val text: String? = null,
    @SerialName("created_at") val createdAt: Long = 0
)

@Serializable
data class MessagesResponse(
    val messages: List<Message> = emptyList(),
    val error: String? = null
)

@Serializable
data class SendMessageRequest(
    @SerialName("conversation_id") val conversationId: Int,
    val text: String
)

@Serializable
data class PixelBoardResponse(
    val width: Int = 0,
    val height: Int = 0,
    val pixels: List<String> = emptyList(),
    val error: String? = null
)

@Serializable
data class PlacePixelRequest(
    val x: Int,
    val y: Int,
    val color: String
)

@Serializable
data class SimpleOk(
    val ok: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)
