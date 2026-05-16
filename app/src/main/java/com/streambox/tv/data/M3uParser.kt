package com.streambox.tv.data

/**
 * Minimal M3U / extended-M3U parser. Returns [Channel] objects.
 * Supports tvg-name, tvg-id, tvg-logo and group-title attributes commonly
 * used by IPTV providers (including Xtream Codes output).
 */
object M3uParser {

    fun parse(input: String): List<Channel> {
        // Strip UTF-8 BOM that some servers emit
        val text = input.removePrefix("\uFEFF")
        val lines = text.lineSequence().map { it.trim() }.toList()
        val channels = mutableListOf<Channel>()
        var pending: Meta? = null
        var idx = 1
        for (line in lines) {
            when {
                line.isEmpty() -> Unit
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    val name = line.substringAfter(",", missingDelimiterValue = "")
                        .trim()
                        .ifBlank { extractAttr(line, "tvg-name") ?: "Channel $idx" }
                    pending = Meta(
                        name = name,
                        logo = extractAttr(line, "tvg-logo"),
                        group = extractAttr(line, "group-title") ?: "General",
                    )
                }
                line.startsWith("#") -> Unit  // ignore other directives (#EXTGRP, #EXTVLCOPT, etc.)
                pending != null -> {
                    val meta = pending!!
                    channels += Channel(
                        id = "m3u-$idx",
                        number = idx,
                        name = meta.name,
                        group = meta.group,
                        logoUrl = meta.logo,
                        streamUrl = line,
                        quality = guessQuality(meta.name),
                    )
                    idx++
                    pending = null
                }
            }
        }
        return channels
    }

    private fun extractAttr(line: String, key: String): String? {
        // Handles both attr="value" and attr=value (without quotes, rare)
        val quoted = """$key="([^"]*)"""".toRegex(RegexOption.IGNORE_CASE)
        quoted.find(line)?.let { return it.groupValues[1] }
        val bare = """$key=([^,\s]+)""".toRegex(RegexOption.IGNORE_CASE)
        return bare.find(line)?.groupValues?.get(1)
    }

    private fun guessQuality(name: String): String {
        val u = name.uppercase()
        return when {
            "4K" in u || "UHD" in u -> "4K"
            "FHD" in u || "1080" in u -> "FHD"
            "HD" in u || "720" in u -> "HD"
            else -> "SD"
        }
    }

    private data class Meta(val name: String, val logo: String?, val group: String)
}
