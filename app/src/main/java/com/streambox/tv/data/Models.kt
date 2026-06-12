package com.streambox.tv.data

enum class ProviderType { M3U, XTREAM, STALKER }

enum class ProviderStatus { OK, SYNCING, FAILED, EXPIRED }

data class Provider(
    val id: String,
    val name: String,
    val type: ProviderType,
    val endpoint: String, // M3U URL or portal URL
    val status: ProviderStatus,
    val lastSync: String,
    val channelCount: Int,
    val movieCount: Int,
    val seriesCount: Int,
)

data class Channel(
    val id: String,
    val number: Int,
    val name: String,
    val group: String,
    val logoUrl: String?,
    val streamUrl: String,
    val quality: String = "HD",
    val isFavorite: Boolean = false,
)

data class EpgProgram(
    val id: String,
    val channelId: String,
    val title: String,
    val description: String,
    val startMinute: Int, // minutes from "now anchor" (negative = past)
    val durationMinutes: Int,
    val category: String = "",
)

data class Movie(
    val id: String,
    val title: String,
    val year: String,
    val genre: String,
    val durationMin: Int,
    val rating: Double,
    val posterUrl: String?,
    val backdropUrl: String?,
    val description: String,
    val streamUrl: String,
    val isFavorite: Boolean = false,
)

data class Episode(
    val id: String,
    val number: Int,
    val title: String,
    val durationMin: Int,
    val watchedRatio: Float, // 0..1
    val streamUrl: String,
)

data class Season(
    val number: Int,
    val episodes: List<Episode>,
)

data class Series(
    val id: String,
    val title: String,
    val year: String,
    val genre: String,
    val rating: Double,
    val posterUrl: String?,
    val backdropUrl: String?,
    val description: String,
    val seasons: List<Season>,
    val isFavorite: Boolean = false,
)

data class ContinueWatching(
    val title: String,
    val subtitle: String,
    val imageUrl: String?,
    val progress: Float,
)
