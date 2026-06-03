package com.antigravity.iptv.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "channels",
    indices = [
        Index(value = ["playlistId", "sourceType"]),
        Index(value = ["playlistId", "groupName"]),
        Index(value = ["playlistId", "sourceType", "groupName"]),
        Index(value = ["playlistId", "name"]),
        Index(value = ["playlistId", "sourceType", "orderIndex"])
    ]
)
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playlistId: Int,
    val name: String,
    val streamUrl: String,
    val logoUrl: String?,
    val groupName: String,
    val isFavorite: Boolean = false,
    val sourceType: String = "live", // "live", "movie", "series"
    val orderIndex: Int = 1 // 0 for beIN sports, 1 for others
)
