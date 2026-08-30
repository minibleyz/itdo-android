package ru.itdo.app.data.repo

import com.google.gson.Gson
import com.google.gson.JsonParseException
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import ru.itdo.app.data.api.ItdoApi
import ru.itdo.app.data.model.*
import ru.itdo.app.core.TokenStore

class ItdoRepository(
    private val api: ItdoApi,
    private val tokenStore: TokenStore,
    private val gson: Gson
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

    // ---- Feed / Posts ----
    suspend fun feed(page: Int, tab: String): FeedResponse =
        safeCall({ FeedResponse(error = it) }) { api.getFeed(page, tab) }

    suspend fun like(postId: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.likePost(mapOf("post_id" to postId)) }

    suspend fun unlike(postId: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.unlikePost(mapOf("post_id" to postId)) }

    suspend fun createPost(text: String): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.createPost(mapOf("text" to text)) }

    suspend fun deletePost(postId: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.deletePost(mapOf("post_id" to postId)) }

    suspend fun editPost(postId: Int, text: String): SimpleOk =
        safeCall({ SimpleOk(error = it) }) {
            api.editPost(mapOf("post_id" to postId.toString(), "text" to text))
        }

    suspend fun repost(postId: Int, text: String = ""): SimpleOk =
        safeCall({ SimpleOk(error = it) }) {
            api.repost(mapOf("post_id" to postId.toString(), "text" to text))
        }

    suspend fun toggleBookmark(postId: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.toggleBookmark(mapOf("post_id" to postId)) }

    suspend fun bookmarks(page: Int = 1): FeedResponse =
        safeCall({ FeedResponse(error = it) }) { api.getBookmarks(page) }

    suspend fun react(postId: Int, emoji: String): SimpleOk =
        safeCall({ SimpleOk(error = it) }) {
            api.reactToPost(mapOf("post_id" to postId.toString(), "emoji" to emoji))
        }

    // ---- Comments ----
    suspend fun comments(postId: Int, page: Int = 1): CommentsResponse =
        safeCall({ CommentsResponse(error = it) }) { api.getComments(postId, page) }

    suspend fun addComment(postId: Int, text: String): SimpleOk =
        safeCall({ SimpleOk(error = it) }) {
            api.addComment(mapOf("post_id" to postId.toString(), "text" to text))
        }

    // ---- Profile / social graph ----
    // id принимает как числовой идентификатор, так и username — как в users/profile.php
    suspend fun user(id: String): User? =
        safeCall({ null as User? }) { api.getUser(id = id) }

    suspend fun followUser(userId: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.followUser(mapOf("user_id" to userId)) }

    suspend fun unfollowUser(userId: Int): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.unfollowUser(mapOf("user_id" to userId)) }

    suspend fun followers(id: String, page: Int = 1): UserListResponse =
        safeCall({ UserListResponse(error = it) }) { api.getFollowers(id, page) }

    suspend fun following(id: String, page: Int = 1): UserListResponse =
        safeCall({ UserListResponse(error = it) }) { api.getFollowing(id, page) }

    // ---- Search ----
    suspend fun search(query: String, type: String = "all", page: Int = 1): SearchResponse =
        safeCall({ SearchResponse(error = it) }) { api.search(query, type, page) }

    // ---- Notifications ----
    suspend fun notifications(limit: Int = 20, offset: Int = 0, unreadOnly: Boolean = false): NotificationsResponse =
        safeCall({ NotificationsResponse(error = it) }) {
            api.getNotifications(limit, offset, if (unreadOnly) 1 else 0)
        }

    suspend fun markNotificationsRead(): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.markNotificationsRead() }

    suspend fun unreadNotificationsCount(): UnreadCountResponse =
        safeCall({ UnreadCountResponse(error = it) }) { api.getUnreadNotificationsCount() }

    // ---- Messages ----
    suspend fun conversations(): ConversationsResponse =
        safeCall({ ConversationsResponse(error = it) }) { api.getConversations() }

    suspend fun messages(conversationId: Int): MessagesResponse =
        safeCall({ MessagesResponse(error = it) }) { api.getMessages(conversationId) }

    suspend fun sendMessage(conversationId: Int, text: String): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.sendMessage(SendMessageRequest(conversationId, text)) }

    suspend fun reactToMessage(messageId: Int, emoji: String): SimpleOk =
        safeCall({ SimpleOk(error = it) }) {
            api.reactToMessage(mapOf("message_id" to messageId.toString(), "emoji" to emoji))
        }

    // ---- Groups ----
    suspend fun createGroup(title: String, memberIds: List<Int>): SimpleOk =
        safeCall({ SimpleOk(error = it) }) {
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

    // ---- Pixel battle ----
    suspend fun pixelBoard(): PixelBoardResponse =
        safeCall({ PixelBoardResponse(error = it) }) { api.getPixelBoard() }

    suspend fun placePixel(x: Int, y: Int, color: String): SimpleOk =
        safeCall({ SimpleOk(error = it) }) { api.placePixel(PlacePixelRequest(x, y, color)) }
}
