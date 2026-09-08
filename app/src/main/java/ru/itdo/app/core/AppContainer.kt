package ru.itdo.app.core

import android.content.Context
import ru.itdo.app.data.api.NetworkModule
import ru.itdo.app.data.repo.ItdoRepository
import ru.itdo.app.data.ws.WSClient

/** Простой ручной DI-контейнер (без Hilt/Koin, чтобы не плодить зависимости). */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val tokenStore = TokenStore(appContext)
    private val api = NetworkModule.createApi(tokenStore)
    val repository = ItdoRepository(api, tokenStore, NetworkModule.gson, appContext)

    /** WebSocket-клиент для реального времени (сообщения, звонки, typing, онлайн). */
    val wsClient = WSClient(tokenStore, api, NetworkModule.gson)
}
