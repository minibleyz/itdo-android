package ru.itdo.app.core

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "itdo_prefs")

class TokenStore(private val context: Context) {
    private val ACCESS = stringPreferencesKey("access_token")
    private val REFRESH = stringPreferencesKey("refresh_token")

    val accessTokenFlow: Flow<String?> = context.dataStore.data.map { it[ACCESS] }

    suspend fun accessTokenOrNull(): String? = context.dataStore.data.first()[ACCESS]
    suspend fun refreshTokenOrNull(): String? = context.dataStore.data.first()[REFRESH]

    suspend fun save(access: String, refresh: String?) {
        context.dataStore.edit {
            it[ACCESS] = access
            if (refresh != null) it[REFRESH] = refresh
        }
    }

    suspend fun clear() {
        context.dataStore.edit {
            it.remove(ACCESS)
            it.remove(REFRESH)
        }
    }
}
