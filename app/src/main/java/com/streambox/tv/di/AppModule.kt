package com.streambox.tv.di

import com.streambox.tv.data.IptvRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideIptvRepository(): IptvRepository = IptvRepository()
}
