package com.streambox.tv.data

/**
 * Xtream Codes is the most common IPTV provider format. Authentication is by
 * (host, username, password) and the playlist URL is built deterministically.
 *
 * Reference endpoints (typical Xtream Codes panel):
 *   • Playlist M3U:  <host>/get.php?username=U&password=P&type=m3u_plus&output=m3u8
 *   • EPG XMLTV:     <host>/xmltv.php?username=U&password=P
 *   • Player API:    <host>/player_api.php?username=U&password=P&action=...
 */
object XtreamClient {

    fun playlistUrl(host: String, user: String, pass: String, output: String = "m3u8"): String {
        val cleanHost = host.trim().trimEnd('/')
        return "$cleanHost/get.php?username=${enc(user)}&password=${enc(pass)}&type=m3u_plus&output=${enc(output)}"
    }

    fun epgUrl(host: String, user: String, pass: String): String {
        val cleanHost = host.trim().trimEnd('/')
        return "$cleanHost/xmltv.php?username=${enc(user)}&password=${enc(pass)}"
    }

    /** Helper for direct stream URLs once a channel id is known via the panel API. */
    fun liveStreamUrl(host: String, user: String, pass: String, streamId: Int, ext: String = "m3u8"): String {
        val cleanHost = host.trim().trimEnd('/')
        return "$cleanHost/live/${enc(user)}/${enc(pass)}/$streamId.$ext"
    }

    private fun enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8")
}
