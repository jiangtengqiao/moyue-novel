package com.novel.reader.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.novel.reader.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 会话管理 - 使用 DataStore 持久化用户登录状态
 */
@Singleton
class SessionManager @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
        private val USER_KEY = stringPreferencesKey("user_data")
    }

    suspend fun saveToken(token: String) {
        dataStore.edit { it[TOKEN_KEY] = token }
    }

    suspend fun saveUser(user: User) {
        val json = Json { encodeDefaults = true }
        dataStore.edit { it[USER_KEY] = json.encodeToString(User.serializer(), user) }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    val tokenFlow: Flow<String?> = dataStore.data.map { it[TOKEN_KEY] }
    val userFlow: Flow<User?> = dataStore.data.map { prefs ->
        prefs[USER_KEY]?.let { jsonStr ->
            try {
                Json.decodeFromString(User.serializer(), jsonStr)
            } catch (_: Exception) { null }
        }
    }

    fun getTokenSync(prefs: Preferences): String? = prefs[TOKEN_KEY]
}
