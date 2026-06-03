package com.antigravity.iptv.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.http.Url

interface StalkerApiService {

    @GET
    suspend fun handshake(
        @Url url: String,
        @Query("type") type: String = "stb",
        @Query("action") action: String = "handshake",
        @Header("Cookie") macCookie: String
    ): Response<StalkerHandshakeResponse>

    @GET
    suspend fun getProfile(
        @Url url: String,
        @Query("type") type: String = "stb",
        @Query("action") action: String = "get_profile",
        @Header("Cookie") macCookie: String,
        @Header("Authorization") token: String
    ): Response<StalkerProfileResponse>

    @GET
    suspend fun getAllChannels(
        @Url url: String,
        @Query("type") type: String = "itv",
        @Query("action") action: String = "get_all_channels",
        @Header("Cookie") macCookie: String,
        @Header("Authorization") token: String
    ): Response<StalkerChannelsResponse>

    @GET
    suspend fun createLink(
        @Url url: String,
        @Query("type") type: String = "itv",
        @Query("action") action: String = "create_link",
        @Query("cmd") cmd: String,
        @Header("Cookie") macCookie: String,
        @Header("Authorization") token: String
    ): Response<StalkerLinkResponse>
}

data class StalkerHandshakeResponse(
    val js: StalkerHandshakeData?
)

data class StalkerHandshakeData(
    val token: String?
)

data class StalkerProfileResponse(
    val js: Any?
)

data class StalkerChannelsResponse(
    val js: StalkerChannelsData?
)

data class StalkerChannelsData(
    val data: List<StalkerChannelItem>?
)

data class StalkerChannelItem(
    val id: String?,
    val name: String?,
    val cmd: String?,
    val logo: String?,
    val tv_genre_id: String?
)

data class StalkerLinkResponse(
    val js: StalkerLinkData?
)

data class StalkerLinkData(
    val cmd: String?
)
