package ru.itdo.app.data.api

import ru.itdo.app.data.model.*
import retrofit2.http.*

/**
 * Отражает эндпоинты бэкенда itdo (см. /api/*.php в исходниках сайта).
 * Аутентификация: Bearer access_token (совпадает с cookie-based на вебе,
 * см. bearerToken() в api/config.php).
 *
 * ВАЖНО: имена полей JSON в реальном ответе PHP могут отличаться от тех,
 * что заведены в data/model/Models.kt — сверьте по факту (например, через
 * curl/Postman к своему бэкенду) и поправьте @SerialName при необходимости.
 */
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

    // ---- Profile ----
    @GET("users/get.php")
    suspend fun getUser(@Query("username") username: String? = null, @Query("id") id: Int? = null): User

    // ---- Messages ----
    @GET("messages/conversations.php")
    suspend fun getConversations(): ConversationsResponse

    @GET("messages/get.php")
    suspend fun getMessages(@Query("conversation_id") conversationId: Int, @Query("page") page: Int = 1): MessagesResponse

    @POST("messages/send.php")
    suspend fun sendMessage(@Body body: SendMessageRequest): SimpleOk

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
