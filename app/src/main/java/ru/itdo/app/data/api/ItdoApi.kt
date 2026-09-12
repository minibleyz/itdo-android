package ru.itdo.app.data.api

import retrofit2.Response
import ru.itdo.app.data.model.*
import retrofit2.http.*

// Отражает эндпоинты бэкенда itdo (см. файлы api/*.php в исходниках сайта).
// Аутентификация: Bearer access_token (совпадает с cookie-based на вебе,
// см. bearerToken() в api/config.php).
//
// ВАЖНО: имена полей JSON в реальном ответе PHP могут отличаться от тех,
// что заведены в data/model/Models.kt — сверьте по факту (например, через
// curl/Postman к своему бэкенду) и поправьте @SerializedName при необходимости.
interface ItdoApi {

    // ================================================================
    //  AUTH (auth/*.php)
    // ================================================================

    // Response<AuthResponse>, а не AuthResponse напрямую: бэкенд отдаёт
    // содержательный JSON и на не-2xx кодах (two_factor_required — 401,
    // banned — 403, невалидный hCaptcha — 403, рейт-лимит — 429; см.
    // api/auth/login.php и requireAuth() в api/config.php). При обычном
    // suspend-возврате Retrofit на не-2xx бросает HttpException и тело
    // теряется — поэтому парсим его вручную в ItdoRepository.parseAuth().
    @POST("auth/login.php")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @POST("auth/register.php")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    @POST("auth/logout.php")
    suspend fun logout(): SimpleOk

    @GET("auth/me.php")
    suspend fun me(): Response<AuthResponse>

    @POST("auth/refresh.php")
    suspend fun refresh(@Body body: Map<String, String>): Response<AuthResponse>

    // Публичный sitekey hCaptcha (+ включена ли регистрация). Используется,
    // чтобы не хардкодить sitekey в клиенте и подхватывать его смену на
    // сервере без обновления приложения (см. api/auth/registration_status.php).
    @GET("auth/registration_status.php")
    suspend fun registrationStatus(): RegistrationStatusResponse

    /** Список активных сессий (auth/sessions.php). */
    @GET("auth/sessions.php")
    suspend fun sessions(): SessionsResponse

    /** Переключение на другой аккаунт (auth/switch.php). */
    @POST("auth/switch.php")
    suspend fun switchAccount(@Body body: Map<String, String>): Response<AuthResponse>

    /** Удаление аккаунта из списка сохранённых (auth/remove_account.php). */
    @POST("auth/remove_account.php")
    suspend fun removeAccount(@Body body: Map<String, String>): SimpleOk

    // ================================================================
    //  FEED / POSTS (feed/*.php, posts/*.php)
    // ================================================================

    @GET("feed/get.php")
    suspend fun getFeed(
        @Query("page") page: Int = 1,
        @Query("tab") tab: String = "for_you",
        @Query("sort") sort: String = "new"
    ): FeedResponse

    @POST("posts/create.php")
    suspend fun createPost(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleOk

    @POST("posts/like.php")
    suspend fun likePost(@Body body: Map<String, Int>): SimpleOk

    @POST("posts/unlike.php")
    suspend fun unlikePost(@Body body: Map<String, Int>): SimpleOk

    @GET("posts/get.php")
    suspend fun getPost(@Query("id") id: Int): Post

    @POST("posts/delete.php")
    suspend fun deletePost(@Body body: Map<String, Int>): SimpleOk

    @POST("posts/edit.php")
    suspend fun editPost(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleOk

    @POST("posts/repost.php")
    suspend fun repost(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleOk

    @POST("posts/unrepost.php")
    suspend fun unrepost(@Body body: Map<String, Int>): SimpleOk

    @POST("posts/bookmark.php")
    suspend fun toggleBookmark(@Body body: Map<String, Int>): SimpleOk

    @GET("posts/bookmarks.php")
    suspend fun getBookmarks(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30,
        @Query("offset") offset: Int = 0
    ): FeedResponse

    @POST("posts/react.php")
    suspend fun reactToPost(@Body body: Map<String, String>): SimpleOk

    /** Закрепить/открепить пост на странице автора (posts/pin.php). */
    @POST("posts/pin.php")
    suspend fun pinPost(@Body body: Map<String, Int>): PinPostResponse

    /** Перевести текст поста (posts/translate.php). */
    @POST("posts/translate.php")
    suspend fun translatePost(@Body body: Map<String, @JvmSuppressWildcards Any>): TranslateResponse

    // ---- Comments (posts/reply.php создаёт пост-комментарий, у которого
    // reply_to = id родительского поста; posts/comments.php их вычитывает) ----

    @GET("posts/comments.php")
    suspend fun getComments(
        @Query("post_id") postId: Int,
        @Query("page") page: Int = 1,
        @Query("sort") sort: String = "new"
    ): CommentsResponse

    @POST("posts/reply.php")
    suspend fun addComment(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleOk

    // ================================================================
    //  PROFILE / USERS (users/*.php)
    // ================================================================

    // ВНИМАНИЕ: реального users/get.php на бэкенде нет — правильный путь
    // users/profile.php, параметр называется id (принимает и числовой id,
    // и username — см. users/profile.php: $_GET['id'] ?? $_GET['user_id']).
    @GET("users/profile.php")
    suspend fun getUser(@Query("id") id: String): User

    @POST("users/follow.php")
    suspend fun followUser(@Body body: Map<String, Int>): FollowResponse

    @POST("users/unfollow.php")
    suspend fun unfollowUser(@Body body: Map<String, Int>): SimpleOk

    @GET("users/followers.php")
    suspend fun getFollowers(@Query("id") id: String, @Query("page") page: Int = 1): UserListResponse

    @GET("users/following.php")
    suspend fun getFollowing(@Query("id") id: String, @Query("page") page: Int = 1): UserListResponse

    /** Обновить профиль (name, username, email) — users/update.php. */
    @POST("users/update.php")
    suspend fun updateProfile(@Body body: Map<String, @JvmSuppressWildcards Any>): UserProfileResponse

    /** Загрузить аватар — users/upload_avatar.php (multipart). */
    @Multipart
    @POST("users/upload_avatar.php")
    suspend fun uploadAvatar(@Part avatar: okhttp3.MultipartBody.Part): UploadResponse

    /** Загрузить баннер — users/upload_banner.php (multipart). */
    @Multipart
    @POST("users/upload_banner.php")
    suspend fun uploadBanner(@Part banner: okhttp3.MultipartBody.Part): UploadResponse

    /** Удалить аккаунт — users/delete_account.php. */
    @POST("users/delete_account.php")
    suspend fun deleteAccount(): SimpleOk

    /** Посты, которые лайкнул текущий пользователь — users/liked.php. */
    @GET("users/liked.php")
    suspend fun fetchLikedPosts(@Query("page") page: Int = 1): FeedResponse

    /** Посты конкретного пользователя — users/profile.php (posts в ответе). */
    @GET("users/profile.php")
    suspend fun fetchUserPosts(
        @Query("user_id") userId: Int,
        @Query("page") page: Int = 1
    ): UserProfileResponse

    /** Заблокировать пользователя — users/block.php. */
    @POST("users/block.php")
    suspend fun blockUser(@Body body: Map<String, Int>): BlockResponse

    /** Разблокировать пользователя — users/unblock.php. */
    @POST("users/unblock.php")
    suspend fun unblockUser(@Body body: Map<String, Int>): SimpleOk

    /** Рекомендуемые пользователи — users/suggestions.php. */
    @GET("users/suggestions.php")
    suspend fun fetchSuggestions(@Query("limit") limit: Int = 20): SuggestionsResponse

    /** Онлайн-список — users/online_list.php. */
    @GET("users/online_list.php")
    suspend fun fetchOnlineList(): OnlineListResponse

    /** Отметиться онлайн — users/online.php. */
    @POST("users/online.php")
    suspend fun setOnline(): SimpleOk

    /** Отметиться оффлайн — users/offline.php. */
    @POST("users/offline.php")
    suspend fun setOffline(): SimpleOk

    /** Скрыть автора из ленты — users/hide_author.php (toggle). */
    @POST("users/hide_author.php")
    suspend fun hideAuthor(@Body body: Map<String, Int>): SimpleOk

    /** Раскрыть автора — users/hide_author.php (toggle, тот же эндпоинт). */
    @POST("users/hide_author.php")
    suspend fun unhideAuthor(@Body body: Map<String, Int>): SimpleOk

    /** Список скрытых авторов — users/hidden_authors_list.php. */
    @GET("users/hidden_authors_list.php")
    suspend fun fetchHiddenAuthors(): UserListResponse

    /** Список подписчиков/подписок — users/followers.php. */
    @GET("users/followers.php")
    suspend fun fetchFollowList(
        @Query("user_id") userId: Int,
        @Query("page") page: Int = 1
    ): UserListResponse

    /** Пожаловаться на пользователя — reports/report_user.php. */
    @POST("reports/report_user.php")
    suspend fun reportUser(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleOk

    /** Обновить тему — users/update_theme.php. */
    @POST("users/update_theme.php")
    suspend fun updateTheme(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleOk

    /** Реферальная информация — users/referral.php. */
    @GET("users/referral.php")
    suspend fun fetchReferral(): ReferralResponse

    // ================================================================
    //  SEARCH (search/*.php)
    // ================================================================

    @GET("search/search.php")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String = "all",
        @Query("page") page: Int = 1
    ): SearchResponse

    // ================================================================
    //  NOTIFICATIONS (notifications/*.php)
    // ================================================================

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

    // ================================================================
    //  MESSAGES (messages/*.php)
    // ================================================================

    @GET("messages/conversations.php")
    suspend fun getConversations(): ConversationsResponse

    @GET("messages/get.php")
    suspend fun getMessages(
        @Query("conversation_id") conversationId: Int,
        @Query("page") page: Int = 1
    ): MessagesResponse

    @POST("messages/send.php")
    suspend fun sendMessage(@Body body: SendMessageRequest): SendMessageResponse

    @POST("messages/react.php")
    suspend fun reactToMessage(@Body body: Map<String, String>): SimpleOk

    /** Начать диалог с пользователем — messages/start.php. */
    @POST("messages/start.php")
    suspend fun startConversation(@Body body: Map<String, Int>): StartConversationResponse

    /** Загрузить медиа для сообщения — messages/upload_media.php (multipart). */
    @Multipart
    @POST("messages/upload_media.php")
    suspend fun uploadMessageMedia(@Part file: okhttp3.MultipartBody.Part): UploadResponse

    /** Загрузить голосовое сообщение — messages/upload_voice.php (multipart). */
    @Multipart
    @POST("messages/upload_voice.php")
    suspend fun uploadVoiceMessage(@Part file: okhttp3.MultipartBody.Part): UploadResponse

    /** Пометить переписку прочитанной — messages/mark_read.php. */
    @POST("messages/mark_read.php")
    suspend fun markConversationRead(@Body body: Map<String, Int>): SimpleOk

    /** Сигнал "я печатаю" — messages/typing.php. */
    @POST("messages/typing.php")
    suspend fun sendTyping(@Body body: Map<String, Int>): SimpleOk

    /** Список переписок, где собеседник печатает — messages/typing_status.php. */
    @GET("messages/typing_status.php")
    suspend fun typingStatus(): TypingStatusResponse

    /** Редактировать сообщение — messages/edit.php. */
    @POST("messages/edit.php")
    suspend fun editMessage(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleOk

    /** Удалить сообщение — messages/delete.php. */
    @POST("messages/delete.php")
    suspend fun deleteMessage(@Body body: Map<String, Int>): SimpleOk

    /** Переслать сообщения — messages/forward.php. */
    @POST("messages/forward.php")
    suspend fun forwardMessages(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleOk

    /** Удалить диалог — messages/delete_conv.php. */
    @POST("messages/delete_conv.php")
    suspend fun deleteConversation(@Body body: Map<String, Int>): SimpleOk

    /** Покинуть диалог — messages/leave.php. */
    @POST("messages/leave.php")
    suspend fun leaveConversation(@Body body: Map<String, Int>): SimpleOk

    /** Запросы на сообщения (от незнакомцев) — messages/requests.php. */
    @GET("messages/requests.php")
    suspend fun fetchMessageRequests(): MessageRequestsResponse

    /** Принять запрос на сообщение — messages/accept_request.php. */
    @POST("messages/accept_request.php")
    suspend fun acceptMessageRequest(@Body body: Map<String, Int>): SimpleOk

    /** Отклонить запрос на сообщение — messages/decline_request.php. */
    @POST("messages/decline_request.php")
    suspend fun declineMessageRequest(@Body body: Map<String, Int>): SimpleOk

    /** Архивировать диалог — messages/archive.php. */
    @POST("messages/archive.php")
    suspend fun archiveConversation(@Body body: Map<String, Int>): ArchiveConversationResponse

    // ================================================================
    //  GROUPS (groups/*.php, messages/create_group.php)
    // ================================================================

    @POST("messages/create_group.php")
    suspend fun createGroup(@Body body: Map<String, @JvmSuppressWildcards Any>): CreateGroupResponse

    @GET("groups/info.php")
    suspend fun getGroupInfo(@Query("conv_id") convId: Int): GroupInfoResponse

    @POST("groups/join.php")
    suspend fun joinGroup(@Body body: Map<String, String>): SimpleOk

    @POST("groups/leave.php")
    suspend fun leaveGroup(@Body body: Map<String, Int>): SimpleOk

    @POST("groups/add_members.php")
    suspend fun addGroupMembers(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleOk

    /** Обновить название/описание группы — groups/update_info.php. */
    @POST("groups/update_info.php")
    suspend fun updateGroupInfo(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleOk

    /** Назначить роль участнику — groups/set_role.php. */
    @POST("groups/set_role.php")
    suspend fun setGroupMemberRole(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleOk

    /** Удалить участника из группы — groups/remove_member.php. */
    @POST("groups/remove_member.php")
    suspend fun removeGroupMember(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleOk

    /** Передать владение группой — groups/transfer_ownership.php. */
    @POST("groups/transfer_ownership.php")
    suspend fun transferGroupOwnership(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleOk

    /** Получить ссылку-приглашение — groups/invite_link.php. */
    @POST("groups/invite_link.php")
    suspend fun fetchGroupInviteLink(@Body body: Map<String, @JvmSuppressWildcards Any>): GroupInviteLinkResponse

    /** Перегенерировать ссылку-приглашение — groups/invite_link.php (regenerate=true). */
    @POST("groups/invite_link.php")
    suspend fun regenerateGroupInviteLink(@Body body: Map<String, @JvmSuppressWildcards Any>): GroupInviteLinkResponse

    /** Удалить группу — groups/delete.php. */
    @POST("groups/delete.php")
    suspend fun deleteGroup(@Body body: Map<String, Int>): SimpleOk

    // ================================================================
    //  CALLS (calls/*.php)
    // ================================================================

    /** Начать звонок — calls/start.php. */
    @POST("calls/start.php")
    suspend fun startCall(@Body body: Map<String, @JvmSuppressWildcards Any>): CallResponse

    /** Ответить на звонок — calls/answer.php. */
    @POST("calls/answer.php")
    suspend fun answerCall(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleOk

    /** Завершить звонок — calls/end.php. */
    @POST("calls/end.php")
    suspend fun endCall(@Body body: Map<String, Int>): SimpleOk

    /** Проверить входящий звонок — calls/incoming.php. */
    @GET("calls/incoming.php")
    suspend fun fetchIncomingCall(): CallResponse

    /** История звонков — calls/history.php. */
    @GET("calls/history.php")
    suspend fun fetchCallHistory(): CallHistoryResponse

    // ================================================================
    //  STREAMS (streams/*.php)
    // ================================================================

    /** Список трансляций — streams/list.php. */
    @GET("streams/list.php")
    suspend fun fetchStreams(
        @Query("page") page: Int = 0,
        @Query("username") username: String? = null
    ): StreamListResponse

    /** Создать трансляцию — streams/create.php. */
    @POST("streams/create.php")
    suspend fun createStream(@Body body: Map<String, @JvmSuppressWildcards Any>): CreateStreamResponse

    /** Отправить сообщение в чат эфира — streams/chat_send.php. */
    @POST("streams/chat_send.php")
    suspend fun sendStreamChatMessage(@Body body: Map<String, String>): SimpleOk

    /** Получить чат эфира — streams/chat_list.php. */
    @GET("streams/chat_list.php")
    suspend fun fetchStreamChat(
        @Query("room") room: String,
        @Query("since_id") sinceId: Int = 0
    ): StreamChatListResponse

    /** Отправить донат в эфир — streams/donate.php. */
    @POST("streams/donate.php")
    suspend fun sendStreamDonate(@Body body: Map<String, @JvmSuppressWildcards Any>): StreamDonateResponse

    /** Лайкнуть трансляцию — streams/like.php. */
    @POST("streams/like.php")
    suspend fun likeStream(@Body body: Map<String, Int>): StreamLikeResponse

    /** Пинг зрителя трансляции — streams/viewers.php. */
    @POST("streams/viewers.php")
    suspend fun pingStreamViewer(@Body body: Map<String, String>): StreamViewersResponse

    // ================================================================
    //  GIFTS (gifts/*.php)
    // ================================================================

    /** Каталог подарков — gifts/catalog.php. */
    @GET("gifts/catalog.php")
    suspend fun fetchGiftCatalog(): GiftCatalogResponse

    /** Полученные подарки — gifts/received.php. */
    @GET("gifts/received.php")
    suspend fun fetchReceivedGifts(@Query("user_id") userId: Int): ReceivedGiftsResponse

    /** Отправить подарок — gifts/send.php. */
    @POST("gifts/send.php")
    suspend fun sendGift(@Body body: Map<String, @JvmSuppressWildcards Any>): SendGiftResponse

    // ================================================================
    //  PLAYLISTS (playlists/*.php)
    // ================================================================

    /** Список плейлистов — playlists/list.php. */
    @GET("playlists/list.php")
    suspend fun fetchPlaylists(): PlaylistsResponse

    /** Получить плейлист — playlists/get.php. */
    @GET("playlists/get.php")
    suspend fun fetchPlaylist(@Query("id") id: Int): PlaylistResponse

    /** Создать плейлист — playlists/create.php. */
    @POST("playlists/create.php")
    suspend fun createPlaylist(@Body body: Map<String, @JvmSuppressWildcards Any>): PlaylistResponse

    /** Удалить плейлист — playlists/delete.php. */
    @POST("playlists/delete.php")
    suspend fun deletePlaylist(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleOk

    /** Добавить в плейлист — playlists/add.php. */
    @POST("playlists/add.php")
    suspend fun addToPlaylist(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleOk

    /** Удалить из плейлиста — playlists/remove.php. */
    @POST("playlists/remove.php")
    suspend fun removeFromPlaylist(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleOk

    /** Понравившиеся видео — playlists/liked.php. */
    @GET("playlists/liked.php")
    suspend fun fetchLikedVideos(): LikedVideosResponse

    // ================================================================
    //  SUPPORT (support/*.php)
    // ================================================================

    /** Список тикетов — support/list.php. */
    @GET("support/list.php")
    suspend fun fetchSupportTickets(): SupportTicketsResponse

    /** Получить тикет — support/get.php. */
    @GET("support/get.php")
    suspend fun fetchSupportTicket(@Query("ticket_id") ticketId: String): SupportTicketDetailResponse

    /** Создать тикет — support/create.php. */
    @POST("support/create.php")
    suspend fun createSupportTicket(@Body body: Map<String, String>): SupportCreateResponse

    /** Ответить на тикет — support/reply.php. */
    @POST("support/reply.php")
    suspend fun replySupportTicket(@Body body: Map<String, String>): SimpleOk

    // ================================================================
    //  ARTICLES (articles/*.php)
    // ================================================================

    /** Список статей — articles/list.php. */
    @GET("articles/list.php")
    suspend fun fetchArticles(
        @Query("page") page: Int = 1,
        @Query("tag") tag: String? = null,
        @Query("user_id") userId: Int? = null
    ): ArticlesResponse

    /** Получить статью — articles/get.php. */
    @GET("articles/get.php")
    suspend fun fetchArticle(@Query("id") id: Int): ArticleResponse

    /** Лайкнуть статью — articles/like.php. */
    @POST("articles/like.php")
    suspend fun likeArticle(@Body body: Map<String, Int>): ArticleLikeResponse

    // ================================================================
    //  ANNOUNCEMENTS (announcements/*.php)
    // ================================================================

    /** Получить объявления — announcements/get.php. */
    @GET("announcements/get.php")
    suspend fun fetchAnnouncements(): AnnouncementsResponse

    /** Скрыть объявление — announcements/dismiss.php. */
    @POST("announcements/dismiss.php")
    suspend fun dismissAnnouncement(@Body body: Map<String, Int>): SimpleOk

    // ================================================================
    //  LEADERBOARD (leaderboard/get.php)
    // ================================================================

    @GET("leaderboard/get.php")
    suspend fun fetchLeaderboard(): LeaderboardResponse

    // ================================================================
    //  QUESTS (quests/*.php)
    // ================================================================

    /** Список квестов — quests/get.php. */
    @GET("quests/get.php")
    suspend fun fetchQuests(): QuestsResponse

    /** Забрать награду за квест — quests/claim.php. */
    @POST("quests/claim.php")
    suspend fun claimQuest(@Body body: Map<String, String>): SimpleOk

    // ================================================================
    //  NUKSTA (nuksta/*.php)
    // ================================================================

    /** Статус подписки Нукста — nuksta/api.php?action=status. */
    @GET("nuksta/api.php")
    suspend fun fetchNuksta(@Query("action") action: String = "status"): NukstaResponse

    /** Оформить подписку Нукста за монеты — nuksta/subscribe_coins.php. */
    @POST("nuksta/subscribe_coins.php")
    suspend fun subscribeNuksta(@Body body: Map<String, String> = emptyMap()): SimpleOk

    /** Получить тему Нукста — nuksta/theme.css?format=json. */
    @GET("nuksta/theme.css")
    suspend fun fetchNukstaTheme(@Query("format") format: String = "json"): NukstaThemeResponse

    // ================================================================
    //  WALLET / COINS (coins/get.php)
    // ================================================================

    @GET("coins/get.php")
    suspend fun fetchWallet(): WalletResponse

    // ================================================================
    //  VERIFICATION (verification/*.php)
    // ================================================================

    /** Запрос на верификацию — verification/request.php. */
    @POST("verification/request.php")
    suspend fun requestVerification(@Body body: Map<String, String> = emptyMap()): SimpleOk

    /** Статус верификации — verification/status.php. */
    @GET("verification/status.php")
    suspend fun fetchVerificationStatus(): VerificationResponse

    // ================================================================
    //  REPORTS (reports/*.php)
    // ================================================================

    /** Пожаловаться на пост — reports/create.php. */
    @POST("reports/create.php")
    suspend fun reportPost(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleOk

    /** Список жалоб — reports/list.php. */
    @GET("reports/list.php")
    suspend fun fetchReports(): ReportsResponse

    // ================================================================
    //  EXPLORE / TRENDING (explore/trending.php)
    // ================================================================

    @GET("explore/trending.php")
    suspend fun fetchTrending(): TrendingResponse

    // ================================================================
    //  VERSION (version.php)
    // ================================================================

    @GET("version.php")
    suspend fun fetchVersion(): VersionResponse

    // ================================================================
    //  AGENT — AI-чат (agent/*.php)
    // ================================================================

    /** Список бесед с агентом — agent/conversations.php. */
    @GET("agent/conversations.php")
    suspend fun fetchAgentConversations(): AgentConversationsResponse

    /** Сообщения беседы с агентом — agent/messages.php. */
    @GET("agent/messages.php")
    suspend fun fetchAgentMessages(@Query("conversation_id") conversationId: Int): AgentMessagesResponse

    /** Удалить беседу с агентом — agent/conversations.php (DELETE). */
    @HTTP(method = "DELETE", path = "agent/conversations.php", hasBody = true)
    suspend fun deleteAgentConversation(@Body body: Map<String, Int>): SimpleOk

    /** Переименовать беседу с агентом — agent/conversations.php (PATCH). */
    @HTTP(method = "PATCH", path = "agent/conversations.php", hasBody = true)
    suspend fun renameAgentConversation(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleOk

    // ================================================================
    //  PIXEL BATTLE (pixelbattle/*.php)
    // ================================================================

    @GET("pixelbattle/board.php")
    suspend fun getPixelBoard(): PixelBoardResponse

    @POST("pixelbattle/place.php")
    suspend fun placePixel(@Body body: PlacePixelRequest): SimpleOk

    // ================================================================
    //  ADMIN (admin/*.php)
    // ================================================================

    @GET("admin/logs.php")
    suspend fun adminLogs(@Query("page") page: Int = 1): Map<String, @JvmSuppressWildcards Any>

    @GET("admin/posts.php")
    suspend fun adminPosts(@Query("page") page: Int = 1): FeedResponse

    // ================================================================
    //  CLIPS (clips/*.php)
    // ================================================================

    @GET("clips/list.php")
    suspend fun getClips(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30,
        @Query("user_id") userId: Int? = null
    ): ClipsResponse

    @POST("clips/vote.php")
    suspend fun voteClip(@Body body: Map<String, @JvmSuppressWildcards Any>): ClipVoteResponse

    @GET("clips/comments.php")
    suspend fun getClipComments(@Query("clip_id") clipId: Int): ClipCommentsResponse

    @POST("clips/comment.php")
    suspend fun addClipComment(@Body body: Map<String, @JvmSuppressWildcards Any>): SimpleOk

    @Multipart
    @POST("clips/upload.php")
    suspend fun uploadClip(
        @Part video: okhttp3.MultipartBody.Part,
        @Part("title") title: okhttp3.RequestBody,
        @Part("description") description: okhttp3.RequestBody
    ): ClipUploadResponse

    // ================================================================
    //  WS INFO (ws/info.php)
    // ================================================================

    /** Получить WebSocket URL — ws/info.php. */
    @GET("ws/info.php")
    suspend fun fetchWsInfo(): WsInfoResponse
}

/** Ответ ws/info.php — адрес WebSocket-сервера. */

