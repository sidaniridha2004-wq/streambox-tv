package com.antigravity.iptv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.antigravity.iptv.data.local.entity.ChannelEntity
import com.antigravity.iptv.data.local.entity.WatchHistoryEntity
import com.antigravity.iptv.data.local.entity.ChannelWithProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: WatchHistoryEntity)

    @Query("SELECT * FROM watch_history WHERE playlistId = :playlistId AND channelId = :channelId LIMIT 1")
    suspend fun getHistoryForChannel(playlistId: Int, channelId: Int): WatchHistoryEntity?

    @Query("UPDATE watch_history SET timestamp = :timestamp WHERE id = :id")
    suspend fun updateHistoryTimestamp(id: Int, timestamp: Long)

    @Query("UPDATE watch_history SET progressMs = :progressMs, durationMs = :durationMs, timestamp = :timestamp WHERE id = :id")
    suspend fun updateHistoryProgress(id: Int, progressMs: Long, durationMs: Long, timestamp: Long)

    @Query("""
        SELECT c.*, h.progressMs, h.durationMs, h.timestamp FROM channels c 
        INNER JOIN watch_history h ON c.id = h.channelId 
        WHERE h.playlistId = :playlistId 
        ORDER BY h.timestamp DESC 
        LIMIT :limit
    """)
    fun getRecentChannels(playlistId: Int, limit: Int = 15): Flow<List<ChannelWithProgress>>

    @Query("DELETE FROM watch_history WHERE playlistId = :playlistId")
    suspend fun deleteHistoryForPlaylist(playlistId: Int)
}
