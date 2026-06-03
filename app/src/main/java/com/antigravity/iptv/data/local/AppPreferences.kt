package com.antigravity.iptv.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class AppPreferences @Inject constructor(@ApplicationContext private val context: Context) {

    private val ACTIVE_PLAYLIST_ID = intPreferencesKey("active_playlist_id")
    private val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
    private val IS_ACTIVATED = booleanPreferencesKey("is_activated")
    private val REMOTE_CONFIG_VERSION = intPreferencesKey("remote_config_version")
    private val REMOTE_PLAYLIST_ID = intPreferencesKey("remote_playlist_id")
    private val VOD_CONFIG_VERSION = intPreferencesKey("vod_config_version")
    private val VOD_PLAYLIST_ID = intPreferencesKey("vod_playlist_id")
    private val APP_LANGUAGE = stringPreferencesKey("app_language")
    private val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
    private val HAS_SELECTED_LANGUAGE = booleanPreferencesKey("has_selected_language")

    val activePlaylistId: Flow<Int?> = context.dataStore.data.map { preferences ->
        val id = preferences[ACTIVE_PLAYLIST_ID]
        if (id == -1 || id == 0) null else id
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_DARK_MODE] ?: true // Default to true (dark theme)
    }

    val isActivated: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_ACTIVATED] ?: false
    }

    val remoteConfigVersion: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[REMOTE_CONFIG_VERSION] ?: 0
    }

    val remotePlaylistId: Flow<Int?> = context.dataStore.data.map { preferences ->
        val id = preferences[REMOTE_PLAYLIST_ID]
        if (id == null || id == -1 || id == 0) null else id
    }

    suspend fun setActivePlaylistId(id: Int) {
        context.dataStore.edit { preferences ->
            preferences[ACTIVE_PLAYLIST_ID] = id
        }
    }

    suspend fun setDarkMode(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_DARK_MODE] = isDark
        }
    }

    suspend fun setActivated(activated: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_ACTIVATED] = activated
        }
    }

    suspend fun setRemoteConfigVersion(version: Int) {
        context.dataStore.edit { preferences ->
            preferences[REMOTE_CONFIG_VERSION] = version
        }
    }

    suspend fun setRemotePlaylistId(id: Int) {
        context.dataStore.edit { preferences ->
            preferences[REMOTE_PLAYLIST_ID] = id
        }
    }

    val vodConfigVersion: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[VOD_CONFIG_VERSION] ?: 0
    }

    val vodPlaylistId: Flow<Int?> = context.dataStore.data.map { preferences ->
        val id = preferences[VOD_PLAYLIST_ID]
        if (id == null || id == -1 || id == 0) null else id
    }

    suspend fun setVodConfigVersion(version: Int) {
        context.dataStore.edit { preferences ->
            preferences[VOD_CONFIG_VERSION] = version
        }
    }

    suspend fun setVodPlaylistId(id: Int) {
        context.dataStore.edit { preferences ->
            preferences[VOD_PLAYLIST_ID] = id
        }
    }

    val appLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[APP_LANGUAGE] ?: "en"
    }

    suspend fun setAppLanguage(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[APP_LANGUAGE] = languageCode
        }
    }

    val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HAS_COMPLETED_ONBOARDING] ?: false
    }

    suspend fun setHasCompletedOnboarding(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAS_COMPLETED_ONBOARDING] = completed
        }
    }

    val hasSelectedLanguage: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HAS_SELECTED_LANGUAGE] ?: false
    }

    suspend fun setHasSelectedLanguage(selected: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAS_SELECTED_LANGUAGE] = selected
        }
    }

    suspend fun clearActivePlaylistId() {
        context.dataStore.edit { preferences ->
            preferences.remove(ACTIVE_PLAYLIST_ID)
        }
    }

    fun getLastSyncTime(playlistId: Int): Flow<Long> = context.dataStore.data.map { preferences ->
        val key = longPreferencesKey("sync_time_$playlistId")
        preferences[key] ?: 0L
    }

    suspend fun setLastSyncTime(playlistId: Int, timeMs: Long) {
        context.dataStore.edit { preferences ->
            val key = longPreferencesKey("sync_time_$playlistId")
            preferences[key] = timeMs
        }
    }
}
