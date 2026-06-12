# StreamBox TV

A premium-feel **IPTV player for Android phone and Android TV**, built with Kotlin + Jetpack Compose (and Compose for TV). It is a *player utility* — bring your own provider via M3U, Xtream Codes, or a Stalker portal.

## Highlights

- **Three onboarding flows**
  - **M3U URL** — direct `.m3u`/`.m3u8` link with optional EPG and HTTP basic auth.
  - **Xtream Codes** — host + username + password; the app builds the playlist and EPG URLs for you (`/get.php?...&type=m3u_plus`, `/xmltv.php`).
  - **Stalker Portal** — portal URL + MAC + optional Device ID/Serial (MAG/STB style auth).
- **Adaptive shell** — phone uses a bottom navigation; TV / large screens use a left rail with D-pad-friendly focus rings on every focusable surface.
- **Premium dark UI** — teal accent, glass cards, soft strokes, large legible type tuned for TV viewing distance. Latin / Arabic / French friendly system typography (RTL supported).
- **Full screen set** included: Splash, Welcome, Login chooser, M3U/Xtream/Stalker forms, Syncing progress, Home, Live TV, EPG grid, Movies + details, Series + seasons/episodes, Player with overlays + channel drawer, Favorites, Search (Channels / Movies / Series tabs), Settings, Provider management.

## Architecture

```
app/
├── data/         models, in-memory repository, M3U parser, Xtream URL builder, sample data
├── di/           Hilt module
├── nav/          NavHost + AdaptiveAppShell (phone bottom nav vs TV nav rail)
├── ui/
│   ├── theme/    colors, typography, shapes, focus ring helper
│   ├── components/  reusable cards, buttons, inputs, chips, search bar, navigation, states
│   ├── auth/     splash, welcome, chooser, M3U/Xtream form, Stalker form, syncing
│   ├── home/, livetv/, epg/, movies/, series/, player/, favorites/, search/, settings/, profile/
└── …
```

Tech: Compose BOM 2024.09, Compose-for-TV, Material3, Hilt, Navigation-Compose, ExoPlayer (Media3), Coil, Coroutines, Room (deps wired). Dark theme only.

## Build

This is a standard Android Studio project.

```bash
# 1. Open in Android Studio (Hedgehog or newer)
# 2. Let Gradle sync (downloads SDK + dependencies)
# 3. Build → Build Bundle(s) / APK(s) → Build APK(s)
#    or: Build → Generate Signed Bundle / APK for a release build
```

To build from the CLI:

```bash
gradle wrapper        # one-time, generates ./gradlew
./gradlew assembleDebug
# APK will be at: app/build/outputs/apk/debug/app-debug.apk
```

The app installs as a normal phone app **and** shows up in the Android TV launcher (we declare `android.intent.category.LEANBACK_LAUNCHER` and a TV banner).

## Notes for production

- The repository ships with rich sample data so every screen demos with realistic content out of the box. To wire real providers: replace the in-memory state in `IptvRepository` with parsed results from `M3uParser` / a Stalker client / Xtream `player_api.php` calls.
- The `XtreamClient` builds canonical Xtream URLs:
  - playlist: `<host>/get.php?username=…&password=…&type=m3u_plus&output=m3u8`
  - EPG: `<host>/xmltv.php?username=…&password=…`
  - direct stream: `<host>/live/<u>/<p>/<streamId>.m3u8`
- `PlayerScreen` ships with the full overlay UI (top info, bottom controls, side channel drawer, buffering / no-signal states) and a placeholder render surface. To go live, replace the `VideoSurfacePlaceholder` composable with an `AndroidView { PlayerView(it).apply { player = exoPlayer } }`.

## Demo flow

`Splash → Welcome → Login chooser → (M3U/Xtream tabs) | Stalker → Syncing progress → Home`. From Home you can reach every other surface via the rail/nav or the quick action cards.
