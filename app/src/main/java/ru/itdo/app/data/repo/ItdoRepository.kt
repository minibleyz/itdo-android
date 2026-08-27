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

    suspend fun logout() {
        runCatching { api.logout() }
        tokenStore.clear()
    }

    suspend fun isLoggedIn(): Boolean = tokenStore.accessTokenOrNull() != null

    suspend fun me(): AuthResponse = api.me()

    suspend fun feed(page: Int, tab: String) = api.getFeed(page, tab)

    suspend fun like(postId: Int) = api.likePost(mapOf("post_id" to postId))
    suspend fun unlike(postId: Int) = api.unlikePost(mapOf("post_id" to postId))
    suspend fun createPost(text: String) = api.createPost(mapOf("text" to text))

    suspend fun user(username: String) = api.getUser(username = username)

    suspend fun conversations() = api.getConversations()
    suspend fun messages(conversationId: Int) = api.getMessages(conversationId)
    suspend fun sendMessage(conversationId: Int, text: String) =
        api.sendMessage(SendMessageRequest(conversationId, text))

    suspend fun pixelBoard() = api.getPixelBoard()
    suspend fun placePixel(x: Int, y: Int, color: String) = api.placePixel(PlacePixelRequest(x, y, color))
}
