package ru.itdo.app.data.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
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

object NetworkModule {
    private val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    fun createApi(tokenStore: TokenStore): ItdoApi {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore))
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
