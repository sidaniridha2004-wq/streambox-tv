package com.antigravity.iptv.data.parser

import com.antigravity.iptv.data.local.entity.ChannelEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.Scanner

object M3uParser {
    /**
     * Parse an M3U file and return a list of ChannelEntity.
     * @param inputStream The M3U file input stream
     * @param playlistId The playlist ID to assign to each channel
     * @param defaultSourceType The source type to assign: "live", "movie", or "series".
     *   If "vod", the parser will auto-detect from URL path and group names.
     */
    suspend fun parse(
        inputStream: InputStream,
        playlistId: Int,
        defaultSourceType: String = "live"
    ): List<ChannelEntity> = withContext(Dispatchers.IO) {
        val channels = mutableListOf<ChannelEntity>()
        
        // Read entire content into memory. Fast and efficient.
        val content = inputStream.bufferedReader().use { it.readText() }
        
        var currentIndex = content.indexOf("#EXTINF:")
        
        while (currentIndex != -1) {
            val nextIndex = content.indexOf("#EXTINF:", currentIndex + 8)
            
            // The bounds for the current entry
            val endOfEntry = if (nextIndex != -1) nextIndex else content.length
            
            // Extract the entry without creating a giant array
            val entryStart = currentIndex + 8
            if (entryStart >= endOfEntry) {
                currentIndex = nextIndex
                continue
            }
            
            val entry = content.substring(entryStart, endOfEntry).trim()
            if (entry.isEmpty()) {
                currentIndex = nextIndex
                continue
            }
            
            val commaIndex = entry.indexOf(',')
            if (commaIndex == -1) {
                currentIndex = nextIndex
                continue
            }
            
            val attributesPart = entry.substring(0, commaIndex)
            val nameAndUrlPart = entry.substring(commaIndex + 1).trim()
            
            // Extract group-title
            val groupRegex = """group-title="(.*?)"""".toRegex()
            val currentGroup = groupRegex.find(attributesPart)?.groups?.get(1)?.value?.trim() ?: "Uncategorized"
            
            // Extract tvg-logo or stream_icon
            val logoRegex = """tvg-logo="(.*?)"""".toRegex()
            val streamIconRegex = """stream_icon="(.*?)"""".toRegex()
            val currentLogo = logoRegex.find(attributesPart)?.groups?.get(1)?.value?.trim()
                ?: streamIconRegex.find(attributesPart)?.groups?.get(1)?.value?.trim()
            
            var currentName = ""
            var streamUrl = ""
            
            // Locate URL start
            val httpIndex = nameAndUrlPart.indexOf("http://")
            val httpsIndex = nameAndUrlPart.indexOf("https://")
            
            val urlStart = when {
                httpIndex != -1 && httpsIndex != -1 -> minOf(httpIndex, httpsIndex)
                httpIndex != -1 -> httpIndex
                httpsIndex != -1 -> httpsIndex
                else -> -1
            }
            
            if (urlStart != -1) {
                // Name is everything before the URL
                val rawName = nameAndUrlPart.substring(0, urlStart).trim()
                currentName = rawName.split('\n').firstOrNull { !it.trim().startsWith("#") }?.trim() ?: rawName
                
                // Extract URL (stop at first space or newline)
                val rawUrl = nameAndUrlPart.substring(urlStart).trim()
                streamUrl = rawUrl.split("\\s+".toRegex()).firstOrNull() ?: rawUrl
            } else {
                // Fallback if URL doesn't use http/https
                val lines = nameAndUrlPart.split('\n').map { it.trim() }
                currentName = lines.firstOrNull { !it.startsWith("#") && it.isNotEmpty() } ?: "Unknown"
                streamUrl = lines.lastOrNull { !it.startsWith("#") && it.isNotEmpty() } ?: ""
            }
            
            // Clean up channel name
            currentName = currentName.replace("\r", "").replace("\n", "").trim()
            
            if (currentName.isNotEmpty() && streamUrl.isNotEmpty()) {
                val sourceType = when {
                    defaultSourceType == "vod" -> detectTypeFromUrl(streamUrl, currentGroup, currentName)
                    else -> defaultSourceType
                }
                
                val orderIndex = if (currentName.lowercase().contains("bein")) 0 else 1
                
                channels.add(
                    ChannelEntity(
                        playlistId = playlistId,
                        name = currentName,
                        streamUrl = streamUrl,
                        logoUrl = currentLogo,
                        groupName = currentGroup,
                        sourceType = sourceType,
                        orderIndex = orderIndex
                    )
                )
            }
            
            currentIndex = nextIndex
        }
        
        return@withContext channels
    }

    /**
     * Detect content type using the stream URL path first (most reliable),
     * then fall back to group name / channel name patterns.
     * 
     * Xtream Codes URL patterns:
     *   /live/user/pass/id.ts    → live
     *   /movie/user/pass/id.mkv  → movie
     *   /series/user/pass/id.mkv → series
     */
    private fun detectTypeFromUrl(streamUrl: String, groupName: String, channelName: String): String {
        val urlLower = streamUrl.lowercase()
        
        // 1. URL path detection (most reliable — xtream codes standard)
        if (urlLower.contains("/movie/")) return "movie"
        if (urlLower.contains("/series/")) return "series"
        if (urlLower.contains("/live/")) return "live"
        
        // 2. File extension hints
        val movieExtensions = listOf(".mkv", ".mp4", ".avi", ".mov")
        val liveExtensions = listOf(".ts", ".m3u8")
        val hasMovieExt = movieExtensions.any { urlLower.endsWith(it) }
        val hasLiveExt = liveExtensions.any { urlLower.endsWith(it) }
        
        // 3. Group name / channel name patterns
        val groupLower = groupName.lowercase()
        val nameLower = channelName.lowercase()
        
        // Series detection
        val seriesGroupPatterns = listOf(
            "series", "série", "show", "episode", "saison", "season", "tv show"
        )
        if (seriesGroupPatterns.any { groupLower.contains(it) }) return "series"
        
        val episodeRegex = Regex("""[Ss]\d+[Ee]\d+""")
        if (episodeRegex.containsMatchIn(channelName)) return "series"
        
        // Movie detection from group name
        val movieGroupPatterns = listOf(
            "movie", "film", "vod", "cinema", "cinéma"
        )
        if (movieGroupPatterns.any { groupLower.contains(it) }) return "movie"
        
        // If has movie file extension and no live indicators → movie
        if (hasMovieExt) return "movie"
        
        // Everything else is live (channels, sports, news, etc.)
        return "live"
    }
}
