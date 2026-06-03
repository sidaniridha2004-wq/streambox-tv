package com.antigravity.iptv.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.antigravity.iptv.domain.model.PlaylistType

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: PlaylistType,
    val url: String? = null, // Used for M3U and Stalker Portal
    val macAddress: String? = null // Used for Stalker
)
