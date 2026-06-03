package com.antigravity.iptv.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.antigravity.iptv.data.local.dao.ChannelDao
import com.antigravity.iptv.data.local.dao.PlaylistDao
import com.antigravity.iptv.data.local.entity.ChannelEntity
import com.antigravity.iptv.data.local.entity.PlaylistEntity

import com.antigravity.iptv.data.local.dao.WatchHistoryDao
import com.antigravity.iptv.data.local.entity.WatchHistoryEntity

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [PlaylistEntity::class, ChannelEntity::class, WatchHistoryEntity::class], version = 6, exportSchema = false)
abstract class IptvDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun channelDao(): ChannelDao
    abstract fun watchHistoryDao(): WatchHistoryDao

    companion object {
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE watch_history ADD COLUMN progressMs INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE watch_history ADD COLUMN durationMs INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
