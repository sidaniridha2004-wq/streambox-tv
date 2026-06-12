package com.streambox.tv.data

/**
 * Hardcoded sample data so the entire UI is populated even before a real
 * M3U / Stalker portal is connected. Numbers are realistic for an IPTV app.
 */
object SampleData {

    val providers = listOf(
        Provider("p1", "MyIPTV Pro", ProviderType.XTREAM, "http://my-host.tv:8080/get.php?username=demo&password=demo&type=m3u_plus&output=m3u8", ProviderStatus.OK, "2 min ago", 4862, 8120, 2470),
        Provider("p2", "Stalker Home", ProviderType.STALKER, "http://portal.example.tv/stalker_portal/c/", ProviderStatus.SYNCING, "syncing…", 0, 0, 0),
        Provider("p3", "Backup Line", ProviderType.M3U, "https://example.org/backup.m3u8", ProviderStatus.EXPIRED, "3 days ago", 1200, 0, 0),
    )

    val groups = listOf("Sports", "News", "Movies", "Kids", "Arabic", "French", "Entertainment", "Documentary", "Music")

    val channels: List<Channel> = listOf(
        Channel("c1", 1, "BeIN Sports 1 HD", "Sports", null, "https://example.com/c1.m3u8", "FHD"),
        Channel("c2", 2, "BeIN Sports 2 HD", "Sports", null, "https://example.com/c2.m3u8", "FHD"),
        Channel("c3", 3, "Al Jazeera English", "News", null, "https://example.com/c3.m3u8", "HD"),
        Channel("c4", 4, "France 24", "News", null, "https://example.com/c4.m3u8", "HD"),
        Channel("c5", 5, "MBC 1", "Arabic", null, "https://example.com/c5.m3u8", "HD"),
        Channel("c6", 6, "MBC 2", "Movies", null, "https://example.com/c6.m3u8", "HD"),
        Channel("c7", 7, "MBC Action", "Movies", null, "https://example.com/c7.m3u8", "HD"),
        Channel("c8", 8, "Cartoon Network", "Kids", null, "https://example.com/c8.m3u8", "HD"),
        Channel("c9", 9, "Disney Channel", "Kids", null, "https://example.com/c9.m3u8", "HD"),
        Channel("c10", 10, "TF1", "French", null, "https://example.com/c10.m3u8", "FHD"),
        Channel("c11", 11, "Canal+", "French", null, "https://example.com/c11.m3u8", "FHD"),
        Channel("c12", 12, "National Geographic", "Documentary", null, "https://example.com/c12.m3u8", "4K"),
        Channel("c13", 13, "Discovery HD", "Documentary", null, "https://example.com/c13.m3u8", "HD"),
        Channel("c14", 14, "MTV", "Music", null, "https://example.com/c14.m3u8", "HD"),
        Channel("c15", 15, "Sky News", "News", null, "https://example.com/c15.m3u8", "HD"),
        Channel("c16", 16, "Fox Sports", "Sports", null, "https://example.com/c16.m3u8", "FHD"),
    )

    val epg: List<EpgProgram> = buildList {
        val grid = listOf(
            "c1" to listOf("La Liga: Real Madrid vs Barcelona" to 120, "Football Tonight" to 60, "Champions Recap" to 60),
            "c2" to listOf("Premier League Live" to 120, "Match of the Day" to 60, "Tennis Open" to 90),
            "c3" to listOf("News at 5" to 60, "Inside Story" to 30, "Documentary: Cities" to 60),
            "c4" to listOf("Le Journal" to 60, "Politique Live" to 60, "Reportage" to 60),
            "c5" to listOf("Morning Show" to 60, "Drama Series" to 60, "Top of the Hour" to 30),
            "c6" to listOf("Movie: Inception" to 150, "Movie: Tenet" to 150, "Trailers" to 30),
            "c7" to listOf("Action Hour" to 60, "Movie: Mad Max" to 120, "Late Night" to 60),
            "c8" to listOf("Cartoons" to 30, "Adventure Time" to 30, "Ben 10" to 30),
            "c9" to listOf("Mickey Mouse" to 30, "Phineas & Ferb" to 30, "DuckTales" to 30),
            "c10" to listOf("JT 20h" to 60, "Téléfilm" to 90, "Cinéma" to 120),
            "c11" to listOf("Le Petit Journal" to 60, "Série française" to 50, "Cinema+" to 110),
            "c12" to listOf("Wild Africa" to 60, "Cosmos" to 60, "Megastructures" to 60),
            "c13" to listOf("MythBusters" to 60, "Gold Rush" to 60, "Deadliest Catch" to 60),
            "c14" to listOf("Top of the Pops" to 60, "MTV Live" to 60, "Unplugged" to 60),
            "c15" to listOf("Breaking News" to 30, "Business Live" to 30, "World Tonight" to 60),
            "c16" to listOf("MLB Tonight" to 60, "NBA Game" to 150, "Sports Center" to 60),
        )
        grid.forEach { (channelId, blocks) ->
            var t = -30 // first block started 30 minutes ago
            blocks.forEachIndexed { idx, (title, dur) ->
                add(
                    EpgProgram(
                        id = "$channelId-$idx",
                        channelId = channelId,
                        title = title,
                        description = "Sample program description for $title.",
                        startMinute = t,
                        durationMinutes = dur,
                    )
                )
                t += dur
            }
        }
    }

    val movies: List<Movie> = listOf(
        Movie("m1", "Dune: Part Two", "2024", "Sci-Fi", 166, 8.6, null, null,
            "Paul Atreides unites with the Fremen to seek revenge against those who destroyed his family.",
            "https://example.com/m1.mp4"),
        Movie("m2", "Oppenheimer", "2023", "Drama", 180, 8.4, null, null,
            "The story of J. Robert Oppenheimer and the development of the atomic bomb.",
            "https://example.com/m2.mp4"),
        Movie("m3", "The Batman", "2022", "Action", 176, 7.9, null, null,
            "Batman ventures into Gotham's underworld when a sadistic killer leaves a trail of clues.",
            "https://example.com/m3.mp4"),
        Movie("m4", "Interstellar", "2014", "Sci-Fi", 169, 8.7, null, null,
            "A team of explorers travel through a wormhole in space to ensure humanity's survival.",
            "https://example.com/m4.mp4"),
        Movie("m5", "Inception", "2010", "Sci-Fi", 148, 8.8, null, null,
            "A thief who steals corporate secrets through dream-sharing technology.",
            "https://example.com/m5.mp4"),
        Movie("m6", "Tenet", "2020", "Action", 150, 7.4, null, null, "Armed with one word—Tenet—the protagonist fights for survival of the world.",
            "https://example.com/m6.mp4"),
        Movie("m7", "Mad Max: Fury Road", "2015", "Action", 120, 8.1, null, null, "In a post-apocalyptic wasteland, a woman rebels against a tyrannical ruler.",
            "https://example.com/m7.mp4"),
        Movie("m8", "Parasite", "2019", "Drama", 132, 8.5, null, null, "Greed and class discrimination threaten a symbiotic relationship.",
            "https://example.com/m8.mp4"),
        Movie("m9", "Joker", "2019", "Drama", 122, 8.4, null, null, "A mentally troubled comedian descends into madness.",
            "https://example.com/m9.mp4"),
        Movie("m10", "Spider-Man: Across the Spider-Verse", "2023", "Animation", 140, 8.7, null, null,
            "Miles Morales catapults across the Multiverse.",
            "https://example.com/m10.mp4"),
        Movie("m11", "Top Gun: Maverick", "2022", "Action", 130, 8.3, null, null, "Pete Mitchell trains a new generation of pilots.",
            "https://example.com/m11.mp4"),
        Movie("m12", "Everything Everywhere All at Once", "2022", "Sci-Fi", 139, 7.8, null, null,
            "A laundromat owner connects with parallel universe versions of herself.",
            "https://example.com/m12.mp4"),
    )

    val series: List<Series> = listOf(
        Series("s1", "House of the Dragon", "2022", "Fantasy", 8.4, null, null,
            "200 years before A Song of Ice and Fire, House Targaryen begins to unravel.",
            seasons = listOf(
                Season(1, (1..10).map { Episode("s1-1-$it", it, "Episode $it", 60, watchedRatio = if (it < 4) 1f else if (it == 4) 0.42f else 0f, "https://example.com/s1-1-$it.mp4") }),
                Season(2, (1..8).map { Episode("s1-2-$it", it, "Episode $it", 60, watchedRatio = 0f, "https://example.com/s1-2-$it.mp4") }),
            ),
        ),
        Series("s2", "The Last of Us", "2023", "Drama", 8.7, null, null,
            "After a global pandemic destroys civilization, a hardened survivor escorts a teenager.",
            seasons = listOf(
                Season(1, (1..9).map { Episode("s2-1-$it", it, "Episode $it", 55, watchedRatio = if (it == 1) 1f else 0f, "https://example.com/s2-1-$it.mp4") }),
            ),
        ),
        Series("s3", "Succession", "2018", "Drama", 8.8, null, null,
            "The Roy family controls one of the biggest media and entertainment companies in the world.",
            seasons = listOf(Season(1, (1..10).map { Episode("s3-1-$it", it, "Episode $it", 55, 0f, "https://example.com/s3-1-$it.mp4") })),
        ),
        Series("s4", "Severance", "2022", "Mystery", 8.7, null, null,
            "Mark leads a team whose work memories are surgically severed from their personal ones.",
            seasons = listOf(Season(1, (1..9).map { Episode("s4-1-$it", it, "Episode $it", 50, 0f, "https://example.com/s4-1-$it.mp4") })),
        ),
        Series("s5", "Andor", "2022", "Sci-Fi", 8.4, null, null,
            "Cassian Andor's journey to becoming a rebel hero.",
            seasons = listOf(Season(1, (1..12).map { Episode("s5-1-$it", it, "Episode $it", 50, 0f, "https://example.com/s5-1-$it.mp4") })),
        ),
        Series("s6", "Better Call Saul", "2015", "Drama", 8.9, null, null,
            "The trials and tribulations of criminal lawyer Jimmy McGill.",
            seasons = listOf(Season(1, (1..10).map { Episode("s6-1-$it", it, "Episode $it", 50, 0f, "https://example.com/s6-1-$it.mp4") })),
        ),
    )

    val continueWatching = listOf(
        ContinueWatching("House of the Dragon", "S1 · E4 · 24:18 left", null, 0.42f),
        ContinueWatching("Dune: Part Two", "1h 04m left", null, 0.37f),
        ContinueWatching("BeIN Sports 1 HD", "Live · La Liga", null, 0.65f),
    )
}
