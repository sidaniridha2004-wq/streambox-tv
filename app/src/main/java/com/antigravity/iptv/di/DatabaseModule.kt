package com.antigravity.iptv.di

import android.content.Context
import androidx.room.Room
import com.antigravity.iptv.data.local.IptvDatabase
import com.antigravity.iptv.data.local.dao.ChannelDao
import com.antigravity.iptv.data.local.dao.PlaylistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideIptvDatabase(@ApplicationContext context: Context): IptvDatabase {
        return Room.databaseBuilder(
            context,
            IptvDatabase::class.java,
            "iptv_database"
        ).addMigrations(IptvDatabase.MIGRATION_5_6).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun providePlaylistDao(database: IptvDatabase): PlaylistDao {
        return database.playlistDao()
    }

    @Provides
    fun provideChannelDao(database: IptvDatabase): ChannelDao {
        return database.channelDao()
    }

    @Provides
    fun provideWatchHistoryDao(database: IptvDatabase): com.antigravity.iptv.data.local.dao.WatchHistoryDao {
        return database.watchHistoryDao()
    }
}
