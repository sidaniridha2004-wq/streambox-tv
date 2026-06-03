package com.antigravity.iptv.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofitBuilder(okHttpClient: OkHttpClient): retrofit2.Retrofit.Builder {
        return retrofit2.Retrofit.Builder()
            .client(okHttpClient)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
    }

    @Provides
    @Singleton
    fun provideStalkerApiService(retrofitBuilder: retrofit2.Retrofit.Builder): com.antigravity.iptv.data.remote.StalkerApiService {
        return retrofitBuilder.baseUrl("http://localhost/").build().create(com.antigravity.iptv.data.remote.StalkerApiService::class.java)
    }
}
