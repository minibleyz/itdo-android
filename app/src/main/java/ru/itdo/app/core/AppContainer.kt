package ru.itdo.app.core

import android.content.Context
import ru.itdo.app.data.api.NetworkModule
import ru.itdo.app.data.repo.ItdoRepository

/** Простой ручной DI-контейнер (без Hilt/Koin, чтобы не плодить зависимости). */
class AppContainer(context: Context) {
    val tokenStore = TokenStore(context.applicationContext)
    private val api = NetworkModule.createApi(tokenStore)
    val repository = ItdoRepository(api, tokenStore)
}
