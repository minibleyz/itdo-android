package ru.itdo.app.data.api

import ru.itdo.app.data.model.*
import retrofit2.http.*

// Отражает эндпоинты бэкенда itdo (см. файлы api/*.php в исходниках сайта).
// Аутентификация: Bearer access_token (совпадает с cookie-based на вебе,
// см. bearerToken() в api/config.php).
//
// ВАЖНО: имена полей JSON в реальном ответе PHP могут отличаться от тех,
// что заведены в data/model/Models.kt — сверьте по факту (например, через
// curl/Postman к своему бэкенду) и поправьте @SerialName при необходимости.
interface ItdoApi {

    // ---- Auth ----
    @POST("auth/login.php")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("auth/register.php")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("auth/logout.php")
    suspend fun logout(): SimpleOk

    @GET("auth/me.php")
    suspend fun me(): AuthResponse

    @POST("auth/refresh.php")
    suspend fun refresh(@Body body: Map<String, String>): AuthResponse

    // ---- Feed / Posts ----
    @GET("feed/get.php")
    suspend fun getFeed(
        @Query("page") page: Int = 1,
        @Query("tab") tab: String = "for_you",
        @Query("sort") sort: String = "new"
    ): FeedResponse

    @POST("posts/create.php")
    suspend fun createPost(@Body body: Map<String, String>): SimpleOk

    @POST("posts/like.php")
    suspend fun likePost(@Body body: Map<String, Int>): SimpleOk

    @POST("posts/unlike.php")
    suspend fun unlikePost(@Body body: Map<String, Int>): SimpleOk

    @GET("posts/get.php")
    suspend fun getPost(@Query("id") id: Int): Post

    @POST("posts/delete.php")
    suspend fun deletePost(@Body body: Map<String, Int>): SimpleOk

    @POST("posts/edit.php")
    suspend fun editPost(@Body body: Map<String, String>): SimpleOk

    @POST("posts/repost.php")
    suspend fun repost(@Body body: Map<String, String>): SimpleOk

    @POST("posts/bookmark.php")
    suspend fun toggleBookmark(@Body body: Map<String, Int>): SimpleOk

    @GET("posts/bookmarks.php")
    suspend fun getBookmarks(@Query("page") page: Int = 1): FeedResponse

    @POST("posts/react.php")
    suspend fun reactToPost(@Body body: Map<String, String>): SimpleOk

    // ---- Comments (posts/reply.php создаёт пост-комментарий, у которого
    // reply_to = id родительского поста; posts/comments.php их вычитывает) ----
    @GET("posts/comments.php")
    suspend fun getComments(@Query("post_id") postId: Int, @Query("page") page: Int = 1): CommentsResponse

    @POST("posts/reply.php")
    suspend fun addComment(@Body body: Map<String, String>): SimpleOk

    // ---- Profile ----
    // ВНИМАНИЕ: реального users/get.php на бэкенде нет — правильный путь
    // users/profile.php, параметр называется id (принимает и числовой id,
    // и username — см. users/profile.php: $_GET['id'] ?? $_GET['user_id']).
    @GET("users/profile.php")
    suspend fun getUser(@Query("id") id: String): User

    @POST("users/follow.php")
    suspend fun followUser(@Body body: Map<String, Int>): SimpleOk

    @POST("users/unfollow.php")
    suspend fun unfollowUser(@Body body: Map<String, Int>): SimpleOk

    @GET("users/followers.php")
    suspend fun getFollowers(@Query("id") id: String, @Query("page") page: Int = 1): UserListResponse

    @GET("users/following.php")
    suspend fun getFollowing(@Query("id") id: String, @Query("page") page: Int = 1): UserListResponse

    // ---- Search ----
    @GET("search/search.php")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String = "all",
        @Query("page") page: Int = 1
    ): SearchResponse

    // ---- Notifications ----
    @GET("notifications/get.php")
    suspend fun getNotifications(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("unread_only") unreadOnly: Int = 0
    ): NotificationsResponse

    @POST("notifications/mark_read.php")
    suspend fun markNotificationsRead(): SimpleOk

    @GET("notifications/unread_count.php")
    suspend fun getUnreadNotificationsCount(): UnreadCountResponse

    // ---- Messages ----
    @GET("messages/conversations.php")
    suspend fun getConversations(): ConversationsResponse

    @GET("messages/get.php")
    suspend fun getMessages(@Query("conversation_id") conversationId: Int, @Query("page") page: Int = 1): MessagesResponse

    @POST("messages/send.php")
    suspend fun sendMessage(@Body body: SendMessageRequest): SimpleOk

    @POST("messages/react.php")
    suspend fun reactToMessage(@Body body: Map<String, String>): SimpleOk

    // ---- Groups (групповые чаты поверх messages/*) ----
    @POST("messages/create_group.php")
    suspend fun createGroup(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleOk

    @GET("groups/info.php")
    suspend fun getGroupInfo(@Query("conv_id") convId: Int): GroupInfoResponse

    @POST("groups/join.php")
    suspend fun joinGroup(@Body body: Map<String, String>): SimpleOk

    @POST("messages/leave.php")
    suspend fun leaveGroup(@Body body: Map<String, Int>): SimpleOk

    @POST("groups/add_members.php")
    suspend fun addGroupMembers(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleOk

    // ---- Pixel battle ----
    @GET("pixelbattle/board.php")
    suspend fun getPixelBoard(): PixelBoardResponse

    @POST("pixelbattle/place.php")
    suspend fun placePixel(@Body body: PlacePixelRequest): SimpleOk

    // ---- Admin (в исходнике admin.html на порядок больше эндпоинтов —
    // logs/coins/ip_bans/device_bans/mail/automod и т.д. Добавляйте по мере надобности) ----
    @GET("admin/logs.php")
    suspend fun adminLogs(@Query("page") page: Int = 1): Map<String, kotlinx.serialization.json.JsonElement>

    @GET("admin/posts.php")
    suspend fun adminPosts(@Query("page") page: Int = 1): FeedResponse
}
