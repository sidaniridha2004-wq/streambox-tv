package com.streambox.tv.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.providerDataStore by preferencesDataStore(name = "auratv_providers")

/**
 * Persistent record of a provider. Stored as JSON in DataStore so the app
 * remembers your IPTV accounts across launches.
 *
 * We don't persist "fetched channels" — those re-download on launch (a few KB
 * to a few MB depending on provider). That keeps DataStore small and avoids
 * stale channel lists.
 */
@Serializable
data class StoredProvider(
    val id: String,
    val name: String,
    val type: String,           // "M3U" | "XTREAM" | "STALKER"
    val endpoint: String,       // M3U / playlist URL (for Xtream this is the get.php URL)
    val username: String? = null,
    val password: String? = null,
    val epgUrl: String? = null,
    val xtreamHost: String? = null,
    val xtreamOutput: String? = null,
)

@Serializable
private data class StoreSnapshot(
    val providers: List<StoredProvider> = emptyList(),
    val activeProviderId: String? = null,
)

@Singleton
class ProviderStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val key = stringPreferencesKey("snapshot_v1")

    val flow = context.providerDataStore.data.map { prefs ->
        prefs[key]?.let { runCatching { json.decodeFromString<StoreSnapshot>(it) }.getOrNull() }
            ?: StoreSnapshot()
    }

    suspend fun load(): Pair<List<StoredProvider>, String?> {
        val s = flow.first()
        return s.providers to s.activeProviderId
    }

    suspend fun save(providers: List<StoredProvider>, activeId: String?) {
        context.providerDataStore.edit { prefs ->
            prefs[key] = json.encodeToString(
                StoreSnapshot.serializer(),
                StoreSnapshot(providers, activeId),
            )
        }
    }
}
