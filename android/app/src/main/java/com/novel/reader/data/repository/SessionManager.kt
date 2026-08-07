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
        private val FONT_SIZE_KEY = intPreferencesKey("reader_font_size")
        private val NIGHT_MODE_KEY = booleanPreferencesKey("reader_night_mode")
        private val READER_BG_KEY = intPreferencesKey("reader_bg_index")
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

    suspend fun saveReaderPrefs(fontSize: Int, nightMode: Boolean, bgIndex: Int) {
        dataStore.edit {
            it[FONT_SIZE_KEY] = fontSize
            it[NIGHT_MODE_KEY] = nightMode
            it[READER_BG_KEY] = bgIndex
        }
    }

    val tokenFlow: Flow<String?> = dataStore.data.map { it[TOKEN_KEY] }
    val userFlow: Flow<User?> = dataStore.data.map { prefs ->
        prefs[USER_KEY]?.let { jsonStr ->
            try {
                Json.decodeFromString(User.serializer(), jsonStr)
            } catch (_: Exception) { null }
        }
    }
    val fontSizeFlow: Flow<Int> = dataStore.data.map { it[FONT_SIZE_KEY] ?: 18 }
    val nightModeFlow: Flow<Boolean> = dataStore.data.map { it[NIGHT_MODE_KEY] ?: false }
    val readerBgFlow: Flow<Int> = dataStore.data.map { it[READER_BG_KEY] ?: 0 }

    fun getTokenSync(prefs: Preferences): String? = prefs[TOKEN_KEY]
}
