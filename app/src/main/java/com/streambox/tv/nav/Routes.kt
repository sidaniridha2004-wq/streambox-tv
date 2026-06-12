package com.streambox.tv.nav

object Routes {
    const val Splash = "splash"
    const val Welcome = "welcome"
    const val LoginChooser = "login_chooser"
    const val M3uForm = "m3u_form"
    const val StalkerForm = "stalker_form"
    const val Syncing = "syncing"

    const val Home = "home"
    const val LiveTv = "live_tv"
    const val Epg = "epg"
    const val Movies = "movies"
    const val MovieDetails = "movie/{id}"
    const val Series = "series"
    const val SeriesDetails = "series/{id}"
    const val Player = "player/{streamUrl}"
    const val Favorites = "favorites"
    const val Search = "search"
    const val Settings = "settings"
    const val Providers = "providers"

    fun movieDetails(id: String) = "movie/$id"
    fun seriesDetails(id: String) = "series/$id"
    fun player(streamUrl: String) = "player/${java.net.URLEncoder.encode(streamUrl, "UTF-8")}"
}
