package ru.itdo.app.data.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.itdo.app.BuildConfig
import ru.itdo.app.core.TokenStore

/** Добавляет Authorization: Bearer <access_token> ко всем запросам, кроме auth/login и auth/register. */
class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath
        val skip = path.endsWith("auth/login.php") ||
            path.endsWith("auth/register.php") ||
            path.endsWith("auth/refresh.php")
        if (skip) return chain.proceed(original)

        val token = runBlocking { tokenStore.accessTokenOrNull() }
        val newRequest = if (token != null) {
            original.newBuilder().addHeader("Authorization", "Bearer $token").build()
        } else original
        return chain.proceed(newRequest)
    }
}

/**
 * access_token живёт всего 1 час (см. issueAuthSession() в api/config.php).
 * До этого фикса refresh() нигде не вызывался автоматически — по истечении
 * часа ЛЮБОЙ запрос к API начинал падать с 401, а экраны (лента, чаты,
 * профиль и т.д.) никак это не обрабатывали, т.к. проверка сессии в
 * AppNav.MainTabs выполняется только один раз при старте.
 *
 * Authenticator перехватывает 401 на уровне OkHttp: дёргает
 * auth/refresh.php синхронным клиентом БЕЗ AuthInterceptor (иначе
 * циклическая зависимость: клиенту с интерцептором нужен authenticator,
 * которому нужен клиент для рефреша), сохраняет новую пару токенов и
 * прозрачно повторяет исходный запрос с новым access_token.
 */
class TokenAuthenticator(
    private val tokenStore: TokenStore,
    private val refreshApi: () -> ItdoApi
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        // Не трогаем сами auth-эндпоинты и не уходим в бесконечный цикл ретраев.
        val path = response.request.url.encodedPath
        if (path.endsWith("auth/login.php") ||
            path.endsWith("auth/register.php") ||
            path.endsWith("auth/refresh.php")
        ) return null
        if (responseCount(response) >= 2) return null

        val refreshToken = runBlocking { tokenStore.refreshTokenOrNull() } ?: return null

        val newAccessToken = synchronized(this) {
            // Другой поток мог уже обновить токен, пока мы ждали лока —
            // проверяем, не сменился ли access_token с момента 401, и если
            // да, просто используем его вместо повторного рефреша.
            val current = runBlocking { tokenStore.accessTokenOrNull() }
            val usedToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            if (current != null && current != usedToken) {
                current
            } else {
                runBlocking {
                    runCatching {
                        val resp = refreshApi().refresh(mapOf("refresh_token" to refreshToken))
                        val body = resp.body()
                        if (resp.isSuccessful && body?.accessToken != null) {
                            tokenStore.save(body.accessToken, body.refreshToken)
                            body.accessToken
                        } else {
                            // Рефреш-токен тоже невалиден/протух — разлогиниваем.
                            tokenStore.clear()
                            null
                        }
                    }.getOrNull()
                }
            }
        } ?: return null

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccessToken")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}

object NetworkModule {
    // Публичный, а не private: ItdoRepository использует тот же Gson, чтобы
    // вручную распарсить error body auth-эндпоинтов на не-2xx кодах
    // (см. ItdoApi.login/register/me/refresh -> Response<AuthResponse>).
    //
    // Lenient*TypeAdapter — бэкенд не гарантирует стабильный JSON-тип для
    // bool/int/long полей (см. LenientTypeAdapters.kt), без них ответы
    // с "нестандартным" (но валидным для PHP) представлением падают целиком
    // с JsonSyntaxException.
    val gson: Gson = GsonBuilder()
        .setLenient()
        .registerTypeAdapter(Boolean::class.javaObjectType, LenientBooleanTypeAdapter)
        .registerTypeAdapter(Boolean::class.javaPrimitiveType, LenientBooleanTypeAdapter)
        .registerTypeAdapter(Int::class.javaObjectType, LenientIntTypeAdapter)
        .registerTypeAdapter(Int::class.javaPrimitiveType, LenientIntTypeAdapter)
        .registerTypeAdapter(Long::class.javaObjectType, LenientLongTypeAdapter)
        .registerTypeAdapter(Long::class.javaPrimitiveType, LenientLongTypeAdapter)
        .create()

    fun createApi(tokenStore: TokenStore): ItdoApi {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        // Отдельный клиент/Retrofit без AuthInterceptor и без authenticator'а —
        // только для запроса auth/refresh.php из TokenAuthenticator. Ленивый
        // (lazy), т.к. на момент создания authenticator'а основной api ещё не
        // существует (см. комментарий у TokenAuthenticator).
        val refreshClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
        val refreshApi: ItdoApi by lazy {
            Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .client(refreshClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(ItdoApi::class.java)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore))
            .authenticator(TokenAuthenticator(tokenStore) { refreshApi })
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        return retrofit.create(ItdoApi::class.java)
    }
}
