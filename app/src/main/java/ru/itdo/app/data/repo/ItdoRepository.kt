package ru.itdo.app.data.repo

import com.google.gson.Gson
import com.google.gson.JsonParseException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import ru.itdo.app.data.api.ItdoApi
import ru.itdo.app.data.model.*
import ru.itdo.app.core.TokenStore

class ItdoRepository(
    private val api: ItdoApi,
    private val tokenStore: TokenStore,
    private val gson: Gson,
    private val appContext: android.content.Context
) {
    // Единая точка защиты от "сервер отдал не то, что ожидалось" —
    // 401/403/5xx без JSON-тела, битый/усечённый JSON на 2xx (нестабильный
    // PHP-бэкенд), обрыв соединения и т.п. Без этого Retrofit на не-2xx
    // просто бросает HttpException (для suspend-методов, возвращающих тип
    // напрямую, а не Response<T>), а Gson на некорректном теле — JsonSyntax/
    // JsonParseException, и оба вылета улетают из репозитория как обычное
    // необработанное исключение. Раньше защита держалась только на том,
    // что КАЖДЫЙ вызов на экране не забыт обёрнут в runCatching — один
    // забытый вызов = краш всего приложения. Теперь репозиторий сам не
    // бросает такие исключения наружу: любой эндпоинт всегда возвращает
    // валидный объект своей модели с заполненным error при сбое.
    private suspend fun <T> safeCall(fallback: (String) -> T, block: suspend () -> T): T {
        return try {
            block()
        } catch (e: HttpException) {
            // Тело ошибки часто вообще не JSON (html страница nginx/php-fpm
            // на 401/403/502, пустой body на рейт-лимите и т.д.) — тело
            // сюда даже не пытаемся парсить, только код и общий текст.
            fallback(httpErrorMessage(e))
        } catch (e: JsonParseException) {
            fallback("Сервер прислал некорректные данные")
        } catch (e: IOException) {
            fallback("Ошибка сети")
        } catch (e: Exception) {
            // Подстраховка от всего остального (NPE в самописных Lenient*
            // TypeAdapter на неожиданном типе поля и т.п.) — лучше показать
            // пользователю ошибку, чем уронить приложение.
            fallback(e.message ?: "Неизвестная ошибка")
        }
    }

    private fun httpErrorMessage(e: HttpException): String = when (e.code()) {
        401 -> "Сессия истекла, войдите заново"
        403 -> "Доступ запрещён"
        404 -> "Не найдено"
        429 -> "Слишком много запросов, попробуйте позже"
        in 500..599 -> "Ошибка сервера (${e.code()})"
        else -> "Ошибка запроса (${e.code()})"
    }

    // Бэкенд шлёт содержательный JSON и на ошибочных HTTP-кодах — не только
    // на 200 (two_factor_required -> 401, banned -> 403, невалидный
    // hCaptcha -> 403, рейт-лимит -> 429; см. api/auth/login.php,
    // requireAuth() в api/config.php). При обычном suspend-возврате
    // Retrofit на не-2xx бросает HttpException и тело теряется — поэтому
    // auth-эндпоинты в ItdoApi возвращают Response<AuthResponse>, а тело
    // (успешное или ошибочное) разбираем здесь вручную.
    //
    // Но даже так: errorBody()?.string() может кинуть IOException (обрыв
    // соединения на чтении тела), а сам body может оказаться не тем JSON,
    // который ждёт Gson (html страница ошибки хостинга, пустая строка,
    // urlencoded вместо JSON и т.п.) — gson.fromJson тогда бросает
    // JsonSyntaxException. Всё это ловится тут же, без исключений наружу.
    private fun parseAuth(resp: Response<AuthResponse>): AuthResponse {
        resp.body()?.let { return it }
        val raw = runCatching { resp.errorBody()?.string() }.getOrNull()
        if (!raw.isNullOrBlank()) {
            runCatching { gson.fromJson(raw, AuthResponse::class.java) }.getOrNull()?.let { return it }
        }
        return AuthResponse(error = "Ошибка сервера (${resp.code()})")
    }

    // ================================================================
    //  AUTH
    // ================================================================

    suspend fun login(username: String, password: String, captcha: String?, totp: String?): AuthResponse =
        safeCall({ AuthResponse(error = it) }) {
            val resp = parseAuth(api.login(LoginRequest(username, password, totp, captcha)))
            if (resp.accessToken != null) {
                tokenStore.save(resp.accessToken, resp.refreshToken)
            }
            resp
        }

    suspend fun register(username: String, email: String, password: String, captcha: String?): AuthResponse =
        safeCall({ AuthResponse(error = it) }) {
            val resp = parseAuth(api.register(RegisterRequest(username, email, password, captcha)))
            if (resp.accessToken != null) {
                tokenStore.save(resp.accessToken, resp.refreshToken)
            }
            resp
        }

    // Актуальный публичный sitekey hCaptcha + флаг открытой регистрации
    // (см. api/auth/registration_status.php). Используется, чтобы виджет
    // hCaptcha на экранах входа/регистрации не зависел от захардкоженного
    // в BuildConfig значения.
    suspend fun registrationStatus(): RegistrationStatusResponse =
        safeCall({ RegistrationStatusResponse(error = it) }) { api.registrationStatus() }

    suspend fun logout() {
        runCatching { api.logout() }
        runCatching { tokenStore.clear() }
    }

    suspend fun isLoggedIn(): Boolean = runCatching { tokenStore.accessTokenOrNull() != null }.getOrDefault(false)

    suspend fun me(): AuthResponse = safeCall({ AuthResponse(error = it) }) { parseAuth(api.me()) }

    // Ручной рефреш access_token по refresh_token (api/auth/refresh.php).
    // Обычно вызывать это напрямую не нужно: TokenAuthenticator в
    // NetworkModule сам перехватывает 401 на любом запросе и делает то же
    // самое прозрачно. Этот метод оставлен для явного вызова (например, из
    // экрана логина/дебага), если понадобится.
    suspend fun refresh(refreshToken: String): AuthResponse =
        safeCall({ AuthResponse(error = it) }) {
            val resp = parseAuth(api.refresh(mapOf("refresh_token" to refreshToken)))
            if (resp.accessToken != null) {
                tokenStore.save(resp.accessToken, resp.refreshToken)
            }
            resp
        }

    /** Список активных сессий (auth/sessions.php). */
    suspend fun sessions(): SessionsResponse =
        safeCall({ SessionsResponse(error = it) }) { api.sessions() }

    /** Переключиться на другой аккаунт (auth/switch.php). */
    suspend fun switchAccount(userId: Int): AuthResponse =
        safeCall({ AuthResponse(error = it) }) {
            val resp = parseAuth(api.switchAccount(mapOf("user_id" to userId.toString())))
            if (resp.accessToken != null) {
                tokenStore.save(resp.accessToken, resp.refreshToken)
            }
            resp
        }

    /** Удалить аккаунт из списка сохранённых (auth/remove_account.php). */
    suspend fun removeAccount(userId: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.removeAccount(mapOf("user_id" to userId.toString())) }

    // ================================================================
    //  FEED / POSTS
    // ================================================================

    suspend fun feed(page: Int, tab: String): FeedResponse =
        safeCall({ FeedResponse(error = it) }) { api.getFeed(page, tab) }

    suspend fun like(postId: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.likePost(mapOf("post_id" to postId)) }

    suspend fun unlike(postId: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.unlikePost(mapOf("post_id" to postId)) }

    suspend fun createPost(text: String, mediaUrls: List<String> = emptyList(), replyTo: Int? = null): SimpleOk =
        safeCall({ SimpleOk(error = it) }) {
            val body = mutableMapOf<String, @JvmSuppressWildcards Any>("text" to text)
            if (mediaUrls.isNotEmpty()) body["media_urls"] = mediaUrls
            if (replyTo != null) body["reply_to"] = replyTo
            api.createPost(body)
        }

    suspend fun deletePost(postId: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.deletePost(mapOf("post_id" to postId)) }

    suspend fun editPost(postId: Int, text: String): SimpleOk =
        safeCall({ SimpleOk(error = it) }) {
            api.editPost(mapOf("post_id" to postId, "text" to text))
        }

    suspend fun repost(postId: Int, text: String = ""): SimpleOk =
        safeCall({ SimpleOk(error = it) }) {
            api.repost(mapOf("post_id" to postId, "text" to text))
        }

    suspend fun unrepost(postId: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.unrepost(mapOf("post_id" to postId)) }

    suspend fun toggleBookmark(postId: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.toggleBookmark(mapOf("post_id" to postId)) }

    suspend fun bookmarks(page: Int = 1, limit: Int = 30): FeedResponse =
        safeCall({ FeedResponse(error = it) }) { api.getBookmarks(page, limit) }

    suspend fun react(postId: Int, emoji: String): SimpleOk =
        safeCall({ SimpleOk(error = it) }) {
            api.reactToPost(mapOf("post_id" to postId.toString(), "emoji" to emoji))
        }

    /** Закрепить/открепить пост на странице автора. */
    suspend fun pinPost(postId: Int): PinPostResponse =
        safeCall({ PinPostResponse(error = it) }) { api.pinPost(mapOf("post_id" to postId)) }

    /** Перевести текст поста. */
    suspend fun translatePost(postId: Int, targetLang: String = "ru"): TranslateResponse =
        safeCall({ TranslateResponse(error = it) }) {
            api.translatePost(mapOf("post_id" to postId, "target_lang" to targetLang))
        }

    // ================================================================
    //  COMMENTS
    // ================================================================

    suspend fun comments(postId: Int, page: Int = 1, sort: String = "new"): CommentsResponse =
        safeCall({ CommentsResponse(error = it) }) { api.getComments(postId, page, sort) }

    suspend fun addComment(postId: Int, text: String): SimpleOk =
        safeCall({ SimpleOk(error = it) }) {
            api.addComment(mapOf("post_id" to postId, "text" to text))
        }

    // ================================================================
    //  PROFILE / USERS
    // ================================================================

    // id принимает как числовой идентификатор, так и username — как в users/profile.php
    suspend fun user(id: String): User? =
        safeCall({ null as User? }) { api.getUser(id = id) }

    suspend fun followUser(userId: Int): FollowResponse =
        safeCall({ FollowResponse(error = it) }) { api.followUser(mapOf("user_id" to userId)) }

    suspend fun unfollowUser(userId: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.unfollowUser(mapOf("user_id" to userId)) }

    suspend fun followers(id: String, page: Int = 1): UserListResponse =
        safeCall({ UserListResponse(error = it) }) { api.getFollowers(id, page) }

    suspend fun following(id: String, page: Int = 1): UserListResponse =
        safeCall({ UserListResponse(error = it) }) { api.getFollowing(id, page) }

    /** Обновить профиль (name, username, email). */
    suspend fun updateProfile(name: String? = null, username: String? = null, email: String? = null): UserProfileResponse =
        safeCall({ UserProfileResponse(error = it) }) {
            val body = mutableMapOf<String, @JvmSuppressWildcards Any>()
            if (name != null) body["name"] = name
            if (username != null) body["username"] = username
            if (email != null) body["email"] = email
            api.updateProfile(body)
        }

    /** Загрузить аватар (multipart). */
    suspend fun uploadAvatar(bytes: ByteArray, fileName: String = "avatar.jpg"): UploadResponse =
        safeCall({ UploadResponse(error = it) }) {
            val body = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val part = okhttp3.MultipartBody.Part.createFormData("avatar", fileName, body)
            api.uploadAvatar(part)
        }

    /** Загрузить аватар из Uri. */
    suspend fun uploadAvatarFromUri(uri: android.net.Uri): UploadResponse {
        val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return UploadResponse(error = "Не удалось прочитать файл")
        val name = queryDisplayName(uri) ?: "avatar_${System.currentTimeMillis()}.jpg"
        return uploadAvatar(bytes, name)
    }

    /** Загрузить баннер (multipart). */
    suspend fun uploadBanner(bytes: ByteArray, fileName: String = "banner.jpg"): UploadResponse =
        safeCall({ UploadResponse(error = it) }) {
            val body = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val part = okhttp3.MultipartBody.Part.createFormData("banner", fileName, body)
            api.uploadBanner(part)
        }

    /** Загрузить баннер из Uri. */
    suspend fun uploadBannerFromUri(uri: android.net.Uri): UploadResponse {
        val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return UploadResponse(error = "Не удалось прочитать файл")
        val name = queryDisplayName(uri) ?: "banner_${System.currentTimeMillis()}.jpg"
        return uploadBanner(bytes, name)
    }

    /** Удалить аккаунт. */
    suspend fun deleteAccount(): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.deleteAccount() }

    /** Посты, которые лайкнул текущий пользователь. */
    suspend fun fetchLikedPosts(page: Int = 1): FeedResponse =
        safeCall({ FeedResponse(error = it) }) { api.fetchLikedPosts(page) }

    /** Посты конкретного пользователя (для вкладки «Посты» в профиле). */
    suspend fun fetchUserPosts(userId: Int, page: Int = 1): UserProfileResponse =
        safeCall({ UserProfileResponse(error = it) }) { api.fetchUserPosts(userId, page) }

    /** Заблокировать пользователя. */
    suspend fun blockUser(userId: Int): BlockResponse =
        safeCall({ BlockResponse(error = it) }) { api.blockUser(mapOf("user_id" to userId)) }

    /** Разблокировать пользователя. */
    suspend fun unblockUser(userId: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.unblockUser(mapOf("user_id" to userId)) }

    /** Рекомендуемые пользователи. */
    suspend fun fetchSuggestions(limit: Int = 20): SuggestionsResponse =
        safeCall({ SuggestionsResponse(error = it) }) { api.fetchSuggestions(limit) }

    /** Онлайн-список. */
    suspend fun fetchOnlineList(): OnlineListResponse =
        safeCall({ OnlineListResponse(error = it) }) { api.fetchOnlineList() }

    /** Отметиться онлайн. */
    suspend fun setOnline(): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.setOnline() }

    /** Отметиться оффлайн. */
    suspend fun setOffline(): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.setOffline() }

    /** Скрыть автора из ленты (toggle). */
    suspend fun hideAuthor(userId: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.hideAuthor(mapOf("user_id" to userId)) }

    /** Раскрыть автора (toggle, тот же эндпоинт). */
    suspend fun unhideAuthor(userId: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.unhideAuthor(mapOf("user_id" to userId)) }

    /** Список скрытых авторов. */
    suspend fun fetchHiddenAuthors(): UserListResponse =
        safeCall({ UserListResponse(error = it) }) { api.fetchHiddenAuthors() }

    /** Список подписчиков/подписок. */
    suspend fun fetchFollowList(userId: Int, page: Int = 1): UserListResponse =
        safeCall({ UserListResponse(error = it) }) { api.fetchFollowList(userId, page) }

    /** Пожаловаться на пользователя. */
    suspend fun reportUser(userId: Int, reason: String): SimpleOk =
        safeCall({ SimpleOk(error = it) }) {
            api.reportUser(mapOf("user_id" to userId, "reason" to reason))
        }

    /** Обновить тему. */
    suspend fun updateTheme(themeCustom: ThemeCustom?): SimpleOk =
        safeCall({ SimpleOk(error = it) }) {
            api.updateTheme(mapOf("theme_custom" to (themeCustom ?: "")))
        }

    /** Реферальная информация. */
    suspend fun fetchReferral(): ReferralResponse =
        safeCall({ ReferralResponse(error = it) }) { api.fetchReferral() }

    // ================================================================
    //  SEARCH
    // ================================================================

    suspend fun search(query: String, type: String = "all", page: Int = 1): SearchResponse =
        safeCall({ SearchResponse(error = it) }) { api.search(query, type, page) }

    // ================================================================
    //  NOTIFICATIONS
    // ================================================================

    suspend fun notifications(limit: Int = 20, offset: Int = 0, unreadOnly: Boolean = false): NotificationsResponse =
        safeCall({ NotificationsResponse(error = it) }) {
            api.getNotifications(limit, offset, if (unreadOnly) 1 else 0)
        }

    suspend fun markNotificationsRead(): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.markNotificationsRead() }

    suspend fun unreadNotificationsCount(): UnreadCountResponse =
        safeCall({ UnreadCountResponse(error = it) }) { api.getUnreadNotificationsCount() }

    // ================================================================
    //  MESSAGES
    // ================================================================

    suspend fun conversations(): ConversationsResponse =
        safeCall({ ConversationsResponse(error = it) }) { api.getConversations() }

    suspend fun messages(conversationId: Int, page: Int = 1): MessagesResponse =
        safeCall({ MessagesResponse(error = it) }) { api.getMessages(conversationId, page) }

    suspend fun sendMessage(
        conversationId: Int,
        text: String? = null,
        mediaUrl: String? = null,
        mediaType: String? = null,
        duration: Int? = null,
        replyTo: Int? = null
    ): SendMessageResponse =
        safeCall({ SendMessageResponse(error = it) }) {
            api.sendMessage(SendMessageRequest(conversationId, text, mediaUrl, mediaType, duration, replyTo))
        }

    suspend fun reactToMessage(messageId: Int, emoji: String): SimpleOk =
        safeCall({ SimpleOk(error = it) }) {
            api.reactToMessage(mapOf("message_id" to messageId.toString(), "emoji" to emoji))
        }

    /** Начать диалог с пользователем — возвращает conv_id. */
    suspend fun startConversation(userId: Int): StartConversationResponse =
        safeCall({ StartConversationResponse(error = it) }) {
            api.startConversation(mapOf("user_id" to userId))
        }

    /** Загрузить медиа для сообщения (multipart). */
    suspend fun uploadMessageMedia(bytes: ByteArray, fileName: String = "media.jpg", mimeType: String = "image/jpeg"): UploadResponse =
        safeCall({ UploadResponse(error = it) }) {
            val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val part = okhttp3.MultipartBody.Part.createFormData("file", fileName, body)
            api.uploadMessageMedia(part)
        }

    /** Загрузить медиа для сообщения из Uri. */
    suspend fun uploadMessageMediaFromUri(uri: android.net.Uri, mimeType: String = "image/jpeg"): UploadResponse {
        val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return UploadResponse(error = "Не удалось прочитать файл")
        val name = queryDisplayName(uri) ?: "media_${System.currentTimeMillis()}"
        return uploadMessageMedia(bytes, name, mimeType)
    }

    /** Загрузить голосовое сообщение (multipart). */
    suspend fun uploadVoiceMessage(bytes: ByteArray, fileName: String = "voice.m4a"): UploadResponse =
        safeCall({ UploadResponse(error = it) }) {
            val body = bytes.toRequestBody("audio/mp4".toMediaTypeOrNull())
            val part = okhttp3.MultipartBody.Part.createFormData("file", fileName, body)
            api.uploadVoiceMessage(part)
        }

    /** Пометить переписку прочитанной. */
    suspend fun markConversationRead(convId: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) {
            // ВАЖНО: бэкенд читает и "conv_id", и "conversation_id" —
            // но messages/mark_read.php использует оба варианта.
            api.markConversationRead(mapOf("conversation_id" to convId))
        }

    /** Сигнал "я печатаю". */
    suspend fun sendTyping(convId: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.sendTyping(mapOf("conv_id" to convId)) }

    /** Список переписок, где собеседник печатает. */
    suspend fun typingStatus(): TypingStatusResponse =
        safeCall({ TypingStatusResponse(error = it) }) { api.typingStatus() }

    /** Редактировать сообщение. */
    suspend fun editMessage(messageId: Int, text: String): SimpleOk =
        safeCall({ SimpleOk(error = it) }) {
            api.editMessage(mapOf("message_id" to messageId, "text" to text))
        }

    /** Удалить сообщение. */
    suspend fun deleteMessage(messageId: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.deleteMessage(mapOf("message_id" to messageId)) }

    /** Переслать сообщения. */
    suspend fun forwardMessages(messageIds: List<Int>, convIds: List<Int>): SimpleOk =
        safeCall({ SimpleOk(error = it) }) {
            api.forwardMessages(mapOf("message_ids" to messageIds, "conv_ids" to convIds))
        }

    /** Удалить диалог. */
    suspend fun deleteConversation(convId: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.deleteConversation(mapOf("conv_id" to convId)) }

    /** Покинуть диалог. */
    suspend fun leaveConversation(convId: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.leaveConversation(mapOf("conv_id" to convId)) }

    /** Запросы на сообщения (от незнакомцев). */
    suspend fun fetchMessageRequests(): MessageRequestsResponse =
        safeCall({ MessageRequestsResponse(error = it) }) { api.fetchMessageRequests() }

    /** Принять запрос на сообщение. */
    suspend fun acceptMessageRequest(convId: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.acceptMessageRequest(mapOf("conv_id" to convId)) }

    /** Отклонить запрос на сообщение. */
    suspend fun declineMessageRequest(convId: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.declineMessageRequest(mapOf("conv_id" to convId)) }

    /** Архивировать диалог. */
    suspend fun archiveConversation(convId: Int): ArchiveConversationResponse =
        safeCall({ ArchiveConversationResponse(error = it) }) { api.archiveConversation(mapOf("conv_id" to convId)) }

    // ================================================================
    //  GROUPS
    // ================================================================

    suspend fun createGroup(title: String, memberIds: List<Int>): CreateGroupResponse =
        safeCall({ CreateGroupResponse(error = it) }) {
            api.createGroup(mapOf("title" to title, "member_ids" to memberIds))
        }

    suspend fun groupInfo(convId: Int): GroupInfoResponse =
        safeCall({ GroupInfoResponse(error = it) }) { api.getGroupInfo(convId) }

    suspend fun joinGroup(inviteCode: String): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.joinGroup(mapOf("invite_code" to inviteCode)) }

    suspend fun leaveGroup(convId: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.leaveGroup(mapOf("conv_id" to convId)) }

    suspend fun addGroupMembers(convId: Int, userIds: List<Int>): SimpleOk =
        safeCall({ SimpleOk(error = it) }) {
            api.addGroupMembers(mapOf("conv_id" to convId, "user_ids" to userIds))
        }

    /** Обновить название/описание группы. */
    suspend fun updateGroupInfo(convId: Int, title: String? = null, description: String? = null): SimpleOk =
        safeCall({ SimpleOk(error = it) }) {
            val body = mutableMapOf<String, @JvmSuppressWildcards Any>("conv_id" to convId)
            if (title != null) body["title"] = title
            if (description != null) body["description"] = description
            api.updateGroupInfo(body)
        }

    /** Назначить роль участнику. */
    suspend fun setGroupMemberRole(convId: Int, userId: Int, role: String): SimpleOk =
        safeCall({ SimpleOk(error = it) }) {
            api.setGroupMemberRole(mapOf("conv_id" to convId, "user_id" to userId, "role" to role))
        }

    /** Удалить участника из группы. */
    suspend fun removeGroupMember(convId: Int, userId: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) {
            api.removeGroupMember(mapOf("conv_id" to convId, "user_id" to userId))
        }

    /** Передать владение группой. */
    suspend fun transferGroupOwnership(convId: Int, userId: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) {
            api.transferGroupOwnership(mapOf("conv_id" to convId, "user_id" to userId))
        }

    /** Получить ссылку-приглашение. */
    suspend fun fetchGroupInviteLink(convId: Int): GroupInviteLinkResponse =
        safeCall({ GroupInviteLinkResponse(error = it) }) {
            api.fetchGroupInviteLink(mapOf("conv_id" to convId))
        }

    /** Перегенерировать ссылку-приглашение. */
    suspend fun regenerateGroupInviteLink(convId: Int): GroupInviteLinkResponse =
        safeCall({ GroupInviteLinkResponse(error = it) }) {
            api.regenerateGroupInviteLink(mapOf("conv_id" to convId, "regenerate" to true))
        }

    /** Удалить группу. */
    suspend fun deleteGroup(convId: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.deleteGroup(mapOf("conv_id" to convId)) }

    // ================================================================
    //  CALLS
    // ================================================================

    /** Начать звонок. */
    suspend fun startCall(convId: Int, type: String = "audio"): CallResponse =
        safeCall({ CallResponse(error = it) }) {
            api.startCall(mapOf("conv_id" to convId, "type" to type))
        }

    /** Ответить на звонок. */
    suspend fun answerCall(callId: Int, action: String = "accept"): SimpleOk =
        safeCall({ SimpleOk(error = it) }) {
            api.answerCall(mapOf("call_id" to callId, "action" to action))
        }

    /** Завершить звонок. */
    suspend fun endCall(callId: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.endCall(mapOf("call_id" to callId)) }

    /** Проверить входящий звонок. */
    suspend fun fetchIncomingCall(): CallResponse =
        safeCall({ CallResponse(error = it) }) { api.fetchIncomingCall() }

    /** История звонков. */
    suspend fun fetchCallHistory(): CallHistoryResponse =
        safeCall({ CallHistoryResponse(error = it) }) { api.fetchCallHistory() }

    // ================================================================
    //  STREAMS
    // ================================================================

    /** Список трансляций. */
    suspend fun fetchStreams(page: Int = 0, username: String? = null): StreamListResponse =
        safeCall({ StreamListResponse(error = it) }) { api.fetchStreams(page, username) }

    /** Создать трансляцию. */
    suspend fun createStream(title: String, description: String = ""): CreateStreamResponse =
        safeCall({ CreateStreamResponse(error = it) }) {
            api.createStream(mapOf("title" to title, "description" to description))
        }

    /** Отправить сообщение в чат эфира. */
    suspend fun sendStreamChatMessage(room: String, text: String): SimpleOk =
        safeCall({ SimpleOk(error = it) }) {
            api.sendStreamChatMessage(mapOf("room" to room, "text" to text))
        }

    /** Получить чат эфира. */
    suspend fun fetchStreamChat(room: String, sinceId: Int = 0): StreamChatListResponse =
        safeCall({ StreamChatListResponse(error = it) }) { api.fetchStreamChat(room, sinceId) }

    /** Отправить донат в эфир. */
    suspend fun sendStreamDonate(streamId: Int, room: String, amount: Int): StreamDonateResponse =
        safeCall({ StreamDonateResponse(error = it) }) {
            api.sendStreamDonate(mapOf("stream_id" to streamId, "room" to room, "amount" to amount))
        }

    /** Лайкнуть трансляцию. */
    suspend fun likeStream(streamId: Int): StreamLikeResponse =
        safeCall({ StreamLikeResponse(error = it) }) { api.likeStream(mapOf("stream_id" to streamId)) }

    /** Пинг зрителя трансляции. */
    suspend fun pingStreamViewer(key: String, action: String): StreamViewersResponse =
        safeCall({ StreamViewersResponse(error = it) }) {
            api.pingStreamViewer(mapOf("key" to key, "action" to action))
        }

    // ================================================================
    //  GIFTS
    // ================================================================

    /** Каталог подарков. */
    suspend fun fetchGiftCatalog(): GiftCatalogResponse =
        safeCall({ GiftCatalogResponse(error = it) }) { api.fetchGiftCatalog() }

    /** Полученные подарки. */
    suspend fun fetchReceivedGifts(userId: Int): ReceivedGiftsResponse =
        safeCall({ ReceivedGiftsResponse(error = it) }) { api.fetchReceivedGifts(userId) }

    /** Отправить подарок. */
    suspend fun sendGift(giftId: String, toUserId: Int, message: String = ""): SendGiftResponse =
        safeCall({ SendGiftResponse(error = it) }) {
            api.sendGift(mapOf("gift_id" to giftId, "to_user_id" to toUserId, "message" to message))
        }

    // ================================================================
    //  PLAYLISTS
    // ================================================================

    /** Список плейлистов. */
    suspend fun fetchPlaylists(): PlaylistsResponse =
        safeCall({ PlaylistsResponse(error = it) }) { api.fetchPlaylists() }

    /** Получить плейлист. */
    suspend fun fetchPlaylist(id: Int): PlaylistResponse =
        safeCall({ PlaylistResponse(error = it) }) { api.fetchPlaylist(id) }

    /** Создать плейлист. */
    suspend fun createPlaylist(name: String, description: String = "", type: String = "mixed"): PlaylistResponse =
        safeCall({ PlaylistResponse(error = it) }) {
            api.createPlaylist(mapOf("name" to name, "description" to description, "type" to type))
        }

    /** Удалить плейлист. */
    suspend fun deletePlaylist(playlistId: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) {
            api.deletePlaylist(mapOf("playlist_id" to playlistId))
        }

    /** Добавить в плейлист. */
    suspend fun addToPlaylist(playlistId: Int, itemType: String, itemId: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) {
            api.addToPlaylist(mapOf("playlist_id" to playlistId, "item_type" to itemType, "item_id" to itemId))
        }

    /** Удалить из плейлиста. */
    suspend fun removeFromPlaylist(playlistId: Int, itemType: String, itemId: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) {
            api.removeFromPlaylist(mapOf("playlist_id" to playlistId, "item_type" to itemType, "item_id" to itemId))
        }

    /** Понравившиеся видео. */
    suspend fun fetchLikedVideos(): LikedVideosResponse =
        safeCall({ LikedVideosResponse(error = it) }) { api.fetchLikedVideos() }

    // ================================================================
    //  SUPPORT
    // ================================================================

    /** Список тикетов. */
    suspend fun fetchSupportTickets(): SupportTicketsResponse =
        safeCall({ SupportTicketsResponse(error = it) }) { api.fetchSupportTickets() }

    /** Получить тикет. */
    suspend fun fetchSupportTicket(ticketId: String): SupportTicketDetailResponse =
        safeCall({ SupportTicketDetailResponse(error = it) }) { api.fetchSupportTicket(ticketId) }

    /** Создать тикет. */
    suspend fun createSupportTicket(subject: String, message: String): SupportCreateResponse =
        safeCall({ SupportCreateResponse(error = it) }) {
            api.createSupportTicket(mapOf("subject" to subject, "message" to message))
        }

    /** Ответить на тикет. */
    suspend fun replySupportTicket(ticketId: String, message: String): SimpleOk =
        safeCall({ SimpleOk(error = it) }) {
            api.replySupportTicket(mapOf("ticket_id" to ticketId, "message" to message))
        }

    // ================================================================
    //  ARTICLES
    // ================================================================

    /** Список статей. */
    suspend fun fetchArticles(page: Int = 1, tag: String? = null, userId: Int? = null): ArticlesResponse =
        safeCall({ ArticlesResponse(error = it) }) { api.fetchArticles(page, tag, userId) }

    /** Получить статью. */
    suspend fun fetchArticle(id: Int): ArticleResponse =
        safeCall({ ArticleResponse(error = it) }) { api.fetchArticle(id) }

    /** Лайкнуть статью. */
    suspend fun likeArticle(articleId: Int): ArticleLikeResponse =
        safeCall({ ArticleLikeResponse(error = it) }) { api.likeArticle(mapOf("article_id" to articleId)) }

    // ================================================================
    //  ANNOUNCEMENTS
    // ================================================================

    /** Получить объявления. */
    suspend fun fetchAnnouncements(): AnnouncementsResponse =
        safeCall({ AnnouncementsResponse(error = it) }) { api.fetchAnnouncements() }

    /** Скрыть объявление. */
    suspend fun dismissAnnouncement(id: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.dismissAnnouncement(mapOf("id" to id)) }

    // ================================================================
    //  LEADERBOARD
    // ================================================================

    suspend fun fetchLeaderboard(): LeaderboardResponse =
        safeCall({ LeaderboardResponse(error = it) }) { api.fetchLeaderboard() }

    // ================================================================
    //  QUESTS
    // ================================================================

    /** Список квестов. */
    suspend fun fetchQuests(): QuestsResponse =
        safeCall({ QuestsResponse(error = it) }) { api.fetchQuests() }

    /** Забрать награду за квест. */
    suspend fun claimQuest(questId: String): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.claimQuest(mapOf("quest_id" to questId)) }

    // ================================================================
    //  NUKSTA
    // ================================================================

    /** Статус подписки Нукста. */
    suspend fun fetchNuksta(): NukstaResponse =
        safeCall({ NukstaResponse(error = it) }) { api.fetchNuksta() }

    /** Оформить подписку Нукста за монеты. */
    suspend fun subscribeNuksta(): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.subscribeNuksta() }

    /** Получить тему Нукста. */
    suspend fun fetchNukstaTheme(): NukstaThemeResponse =
        safeCall({ NukstaThemeResponse(error = it) }) { api.fetchNukstaTheme() }

    // ================================================================
    //  WALLET / COINS
    // ================================================================

    suspend fun fetchWallet(): WalletResponse =
        safeCall({ WalletResponse(error = it) }) { api.fetchWallet() }

    // ================================================================
    //  VERIFICATION
    // ================================================================

    /** Запрос на верификацию. */
    suspend fun requestVerification(reason: String = ""): SimpleOk =
        safeCall({ SimpleOk(error = it) }) {
            api.requestVerification(if (reason.isNotEmpty()) mapOf("reason" to reason) else emptyMap())
        }

    /** Статус верификации. */
    suspend fun fetchVerificationStatus(): VerificationResponse =
        safeCall({ VerificationResponse(error = it) }) { api.fetchVerificationStatus() }

    // ================================================================
    //  REPORTS
    // ================================================================

    /** Пожаловаться на пост. */
    suspend fun reportPost(postId: Int, reason: String, details: String = ""): SimpleOk =
        safeCall({ SimpleOk(error = it) }) {
            api.reportPost(mapOf("post_id" to postId, "reason" to reason, "details" to details))
        }

    /** Список жалоб. */
    suspend fun fetchReports(): ReportsResponse =
        safeCall({ ReportsResponse(error = it) }) { api.fetchReports() }

    // ================================================================
    //  EXPLORE / TRENDING
    // ================================================================

    suspend fun fetchTrending(): TrendingResponse =
        safeCall({ TrendingResponse(error = it) }) { api.fetchTrending() }

    // ================================================================
    //  VERSION
    // ================================================================

    suspend fun fetchVersion(): VersionResponse =
        safeCall({ VersionResponse(error = it) }) { api.fetchVersion() }

    // ================================================================
    //  AGENT (AI-чат)
    // ================================================================

    /** Список бесед с агентом. */
    suspend fun fetchAgentConversations(): AgentConversationsResponse =
        safeCall({ AgentConversationsResponse(error = it) }) { api.fetchAgentConversations() }

    /** Сообщения беседы с агентом. */
    suspend fun fetchAgentMessages(conversationId: Int): AgentMessagesResponse =
        safeCall({ AgentMessagesResponse(error = it) }) { api.fetchAgentMessages(conversationId) }

    /** Удалить беседу с агентом. */
    suspend fun deleteAgentConversation(id: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.deleteAgentConversation(mapOf("id" to id)) }

    /** Переименовать беседу с агентом. */
    suspend fun renameAgentConversation(id: Int, title: String): SimpleOk =
        safeCall({ SimpleOk(error = it) }) {
            api.renameAgentConversation(mapOf("id" to id, "title" to title))
        }

    // ================================================================
    //  PIXEL BATTLE
    // ================================================================

    suspend fun pixelBoard(): PixelBoardResponse =
        safeCall({ PixelBoardResponse(error = it) }) { api.getPixelBoard() }

    suspend fun placePixel(x: Int, y: Int, color: String): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.placePixel(PlacePixelRequest(x, y, color)) }

    // ================================================================
    //  CLIPS
    // ================================================================

    suspend fun clips(page: Int = 1, userId: Int? = null): ClipsResponse =
        safeCall({ ClipsResponse(error = it) }) { api.getClips(page, 30, userId) }

    // type: "like" | "dislike" — как в iOS APIClient.likeClip/dislikeClip,
    // оба идут через один и тот же clips/vote.php.
    suspend fun voteClip(clipId: Int, type: String): ClipVoteResponse =
        safeCall({ ClipVoteResponse(error = it) }) {
            api.voteClip(mapOf("clip_id" to clipId, "type" to type))
        }

    suspend fun clipComments(clipId: Int): ClipCommentsResponse =
        safeCall({ ClipCommentsResponse(error = it) }) { api.getClipComments(clipId) }

    suspend fun addClipComment(clipId: Int, text: String): SimpleOk =
        safeCall({ SimpleOk(error = it) }) {
            api.addClipComment(mapOf("clip_id" to clipId, "text" to text))
        }

    suspend fun uploadClip(bytes: ByteArray, fileName: String, title: String, description: String): ClipUploadResponse =
        safeCall({ ClipUploadResponse(error = it) }) {
            val body = bytes.toRequestBody("video/*".toMediaTypeOrNull())
            val part = okhttp3.MultipartBody.Part.createFormData("video", fileName, body)
            api.uploadClip(
                part,
                title.toRequestBody("text/plain".toMediaTypeOrNull()),
                description.toRequestBody("text/plain".toMediaTypeOrNull())
            )
        }

    // Файлпикер в Compose (rememberLauncherForActivityResult) отдаёт
    // content:// Uri, а не файл на диске — читаем байты через
    // ContentResolver и переиспользуем uploadClip(bytes, ...) выше.
    suspend fun uploadClipFromUri(uri: android.net.Uri, title: String, description: String): ClipUploadResponse {
        val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return ClipUploadResponse(error = "Не удалось прочитать файл")
        val name = queryDisplayName(uri) ?: "clip_${System.currentTimeMillis()}.mp4"
        return uploadClip(bytes, name, title, description)
    }

    // ================================================================
    //  WS INFO
    // ================================================================

    /** Получить WebSocket URL. */
    suspend fun fetchWsInfo(): WsInfoResponse =
        safeCall({ WsInfoResponse(error = it) }) { api.fetchWsInfo() }

    // ================================================================
    //  HELPERS
    // ================================================================

    private fun queryDisplayName(uri: android.net.Uri): String? = runCatching {
        appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }
    }.getOrNull()
}
