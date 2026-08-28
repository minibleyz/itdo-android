package ru.itdo.app.data.repo

import ru.itdo.app.data.api.ItdoApi
import ru.itdo.app.data.model.*
import ru.itdo.app.core.TokenStore

class ItdoRepository(
    private val api: ItdoApi,
    private val tokenStore: TokenStore
) {
    suspend fun login(username: String, password: String, captcha: String?, totp: String?): AuthResponse {
        val resp = api.login(LoginRequest(username, password, totp, captcha))
        if (resp.accessToken != null) {
            tokenStore.save(resp.accessToken, resp.refreshToken)
        }
        return resp
    }

    suspend fun register(username: String, email: String, password: String, captcha: String?): AuthResponse {
        val resp = api.register(RegisterRequest(username, email, password, captcha))
        if (resp.accessToken != null) {
            tokenStore.save(resp.accessToken, resp.refreshToken)
        }
        return resp
    }

    // Актуальный публичный sitekey hCaptcha + флаг открытой регистрации
    // (см. api/auth/registration_status.php). Используется, чтобы виджет
    // hCaptcha на экранах входа/регистрации не зависел от захардкоженного
    // в BuildConfig значения.
    suspend fun registrationStatus(): RegistrationStatusResponse = api.registrationStatus()

    suspend fun logout() {
        runCatching { api.logout() }
        tokenStore.clear()
    }

    suspend fun isLoggedIn(): Boolean = tokenStore.accessTokenOrNull() != null

    suspend fun me(): AuthResponse = api.me()

    // ---- Feed / Posts ----
    suspend fun feed(page: Int, tab: String) = api.getFeed(page, tab)

    suspend fun like(postId: Int) = api.likePost(mapOf("post_id" to postId))
    suspend fun unlike(postId: Int) = api.unlikePost(mapOf("post_id" to postId))
    suspend fun createPost(text: String) = api.createPost(mapOf("text" to text))
    suspend fun deletePost(postId: Int) = api.deletePost(mapOf("post_id" to postId))
    suspend fun editPost(postId: Int, text: String) =
        api.editPost(mapOf("post_id" to postId.toString(), "text" to text))
    suspend fun repost(postId: Int, text: String = "") =
        api.repost(mapOf("post_id" to postId.toString(), "text" to text))
    suspend fun toggleBookmark(postId: Int) = api.toggleBookmark(mapOf("post_id" to postId))
    suspend fun bookmarks(page: Int = 1) = api.getBookmarks(page)
    suspend fun react(postId: Int, emoji: String) =
        api.reactToPost(mapOf("post_id" to postId.toString(), "emoji" to emoji))

    // ---- Comments ----
    suspend fun comments(postId: Int, page: Int = 1) = api.getComments(postId, page)
    suspend fun addComment(postId: Int, text: String) =
        api.addComment(mapOf("post_id" to postId.toString(), "text" to text))

    // ---- Profile / social graph ----
    // id принимает как числовой идентификатор, так и username — как в users/profile.php
    suspend fun user(id: String) = api.getUser(id = id)
    suspend fun followUser(userId: Int) = api.followUser(mapOf("user_id" to userId))
    suspend fun unfollowUser(userId: Int) = api.unfollowUser(mapOf("user_id" to userId))
    suspend fun followers(id: String, page: Int = 1) = api.getFollowers(id, page)
    suspend fun following(id: String, page: Int = 1) = api.getFollowing(id, page)

    // ---- Search ----
    suspend fun search(query: String, type: String = "all", page: Int = 1) = api.search(query, type, page)

    // ---- Notifications ----
    suspend fun notifications(limit: Int = 20, offset: Int = 0, unreadOnly: Boolean = false) =
        api.getNotifications(limit, offset, if (unreadOnly) 1 else 0)
    suspend fun markNotificationsRead() = api.markNotificationsRead()
    suspend fun unreadNotificationsCount() = api.getUnreadNotificationsCount()

    // ---- Messages ----
    suspend fun conversations() = api.getConversations()
    suspend fun messages(conversationId: Int) = api.getMessages(conversationId)
    suspend fun sendMessage(conversationId: Int, text: String) =
        api.sendMessage(SendMessageRequest(conversationId, text))
    suspend fun reactToMessage(messageId: Int, emoji: String) =
        api.reactToMessage(mapOf("message_id" to messageId.toString(), "emoji" to emoji))

    // ---- Groups ----
    suspend fun createGroup(title: String, memberIds: List<Int>) =
        api.createGroup(mapOf("title" to title, "member_ids" to memberIds))
    suspend fun groupInfo(convId: Int) = api.getGroupInfo(convId)
    suspend fun joinGroup(inviteCode: String) = api.joinGroup(mapOf("invite_code" to inviteCode))
    suspend fun leaveGroup(convId: Int) = api.leaveGroup(mapOf("conv_id" to convId))
    suspend fun addGroupMembers(convId: Int, userIds: List<Int>) =
        api.addGroupMembers(mapOf("conv_id" to convId, "user_ids" to userIds))

    // ---- Pixel battle ----
    suspend fun pixelBoard() = api.getPixelBoard()
    suspend fun placePixel(x: Int, y: Int, color: String) = api.placePixel(PlacePixelRequest(x, y, color))
}
