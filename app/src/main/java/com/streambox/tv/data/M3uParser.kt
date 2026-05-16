package com.streambox.tv.data

/**
 * Minimal M3U / extended-M3U parser. Returns Channel objects.
 * Supports tvg-name, tvg-logo, group-title attributes commonly used by IPTV providers.
 */
object M3uParser {

    fun parse(text: String): List<Channel> {
        val lines = text.lineSequence().map { it.trim() }.toList()
        val channels = mutableListOf<Channel>()
        var pendingMeta: Triple<String, String?, String>? = null // name, logo, group
        var idx = 1
        for (line in lines) {
            when {
                line.startsWith("#EXTINF") -> {
                    val name = line.substringAfter(",").ifBlank { "Channel $idx" }
                    val logo = extractAttr(line, "tvg-logo")
                    val group = extractAttr(line, "group-title") ?: "General"
                    pendingMeta = Triple(name, logo, group)
                }
                line.isNotEmpty() && !line.startsWith("#") && pendingMeta != null -> {
                    val (name, logo, group) = pendingMeta!!
                    channels += Channel(
                        id = "m3u-$idx",
                        number = idx,
                        name = name,
                        group = group,
                        logoUrl = logo,
                        streamUrl = line,
                        quality = guessQuality(name),
                    )
                    idx++
                    pendingMeta = null
                }
            }
        }
        return channels
    }

    private fun extractAttr(line: String, key: String): String? {
        val regex = "$key=\"([^\"]*)\"".toRegex()
        return regex.find(line)?.groupValues?.get(1)
    }

    private fun guessQuality(name: String): String = when {
        "4K" in name.uppercase() -> "4K"
        "FHD" in name.uppercase() -> "FHD"
        "HD" in name.uppercase() -> "HD"
        else -> "SD"
    }
}
