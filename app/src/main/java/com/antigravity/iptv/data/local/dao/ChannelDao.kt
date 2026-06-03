package com.antigravity.iptv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.antigravity.iptv.data.local.entity.ChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels WHERE playlistId = :playlistId")
    fun getChannelsByPlaylist(playlistId: Int): Flow<List<ChannelEntity>>

    // Movies: ONLY sourceType = 'movie' — clean separation
    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND sourceType = 'movie' LIMIT :limit")
    fun getMovies(playlistId: Int, limit: Int): Flow<List<ChannelEntity>>

    // Series: ONLY sourceType = 'series' — clean separation
    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND sourceType = 'series' LIMIT :limit")
    fun getSeries(playlistId: Int, limit: Int): Flow<List<ChannelEntity>>

    // Live TV: ONLY sourceType = 'live', sorted with beIN Sports first
    @Query("""SELECT * FROM channels WHERE playlistId = :playlistId AND sourceType = 'live' 
              ORDER BY orderIndex ASC, name ASC 
              LIMIT :limit""")
    fun getLiveTv(playlistId: Int, limit: Int): Flow<List<ChannelEntity>>

    // Categories for ALL channels in a playlist
    @Query("SELECT DISTINCT groupName FROM channels WHERE playlistId = :playlistId")
    fun getCategoriesByPlaylist(playlistId: Int): Flow<List<String>>

    // Categories filtered by sourceType (e.g. only live channel categories)
    @Query("SELECT DISTINCT groupName FROM channels WHERE playlistId = :playlistId AND sourceType = :sourceType")
    fun getCategoriesBySourceType(playlistId: Int, sourceType: String): Flow<List<String>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND groupName = :groupName")
    fun getChannelsByCategory(playlistId: Int, groupName: String): Flow<List<ChannelEntity>>

    // Channels by category AND sourceType, sorted with beIN Sports first
    @Query("""SELECT * FROM channels WHERE playlistId = :playlistId AND groupName = :groupName AND sourceType = :sourceType
              ORDER BY orderIndex ASC, name ASC""")
    fun getChannelsByCategoryAndType(playlistId: Int, groupName: String, sourceType: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND (LOWER(groupName) LIKE '%sport%' OR LOWER(name) LIKE '%sport%')")
    fun getSportsChannels(playlistId: Int): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND name LIKE '%' || :baseName || '%'")
    fun getChannelsByBaseName(playlistId: Int, baseName: String): Flow<List<ChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Query("UPDATE channels SET isFavorite = :isFavorite WHERE id = :channelId")
    suspend fun updateFavoriteStatus(channelId: Int, isFavorite: Boolean)

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    suspend fun deleteChannelsByPlaylist(playlistId: Int)

    // Delete only channels with a specific sourceType (for partial re-sync)
    @Query("DELETE FROM channels WHERE playlistId = :playlistId AND sourceType = :sourceType")
    suspend fun deleteChannelsBySourceType(playlistId: Int, sourceType: String)

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND name LIKE '%' || :query || '%'")
    fun searchChannels(playlistId: Int, query: String): Flow<List<ChannelEntity>>

    @Query("SELECT COUNT(*) FROM channels WHERE playlistId = :playlistId")
    fun getChannelCountByPlaylist(playlistId: Int): Flow<Int>
}
