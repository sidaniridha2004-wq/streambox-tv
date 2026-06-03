package com.antigravity.iptv.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playlistId: Int,
    val channelId: Int,
    val timestamp: Long,
    val progressMs: Long = 0L,
    val durationMs: Long = 0L
)

data class ChannelWithProgress(
    @androidx.room.Embedded val channel: ChannelEntity,
    val progressMs: Long,
    val durationMs: Long,
    val timestamp: Long
)
