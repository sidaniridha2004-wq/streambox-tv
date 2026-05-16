package com.streambox.tv.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.streambox.tv.ui.auth.LoginChooserScreen
import com.streambox.tv.ui.auth.M3uFormScreen
import com.streambox.tv.ui.auth.SplashScreen
import com.streambox.tv.ui.auth.StalkerFormScreen
import com.streambox.tv.ui.auth.SyncingScreen
import com.streambox.tv.ui.auth.WelcomeScreen
import com.streambox.tv.ui.epg.EpgScreen
import com.streambox.tv.ui.favorites.FavoritesScreen
import com.streambox.tv.ui.home.HomeScreen
import com.streambox.tv.ui.livetv.LiveTvScreen
import com.streambox.tv.ui.movies.MovieDetailsScreen
import com.streambox.tv.ui.movies.MoviesScreen
import com.streambox.tv.ui.player.PlayerScreen
import com.streambox.tv.ui.profile.ProvidersScreen
import com.streambox.tv.ui.search.SearchScreen
import com.streambox.tv.ui.series.SeriesDetailsScreen
import com.streambox.tv.ui.series.SeriesScreen
import com.streambox.tv.ui.settings.SettingsScreen

@Composable
fun AppNavHost(nav: NavHostController) {
    NavHost(navController = nav, startDestination = Routes.Splash) {
        composable(Routes.Splash) { SplashScreen(onContinue = { nav.navigate(Routes.Welcome) { popUpTo(Routes.Splash) { inclusive = true } } }) }
        composable(Routes.Welcome) { WelcomeScreen(onAddPlaylist = { nav.navigate(Routes.LoginChooser) }, onSkip = { nav.navigate(Routes.Home) { popUpTo(Routes.Welcome) { inclusive = true } } }) }
        composable(Routes.LoginChooser) {
            LoginChooserScreen(
                onM3u = { nav.navigate(Routes.M3uForm) },
                onStalker = { nav.navigate(Routes.StalkerForm) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.M3uForm) {
            M3uFormScreen(
                onSubmit = { nav.navigate(Routes.Syncing) { popUpTo(Routes.LoginChooser) { inclusive = true } } },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.StalkerForm) {
            StalkerFormScreen(
                onSubmit = { nav.navigate(Routes.Syncing) { popUpTo(Routes.LoginChooser) { inclusive = true } } },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.Syncing) {
            SyncingScreen(
                onDone = { nav.navigate(Routes.Home) { popUpTo(Routes.Splash) { inclusive = true } } },
                onError = { nav.popBackStack(Routes.LoginChooser, inclusive = false) },
            )
        }

        composable(Routes.Home) { HomeScreen(nav) }
        composable(Routes.LiveTv) { LiveTvScreen(nav) }
        composable(Routes.Epg) { EpgScreen(nav) }
        composable(Routes.Movies) { MoviesScreen(nav) }
        composable(Routes.Series) { SeriesScreen(nav) }
        composable(Routes.Search) { SearchScreen(nav) }
        composable(Routes.Favorites) { FavoritesScreen(nav) }
        composable(Routes.Providers) { ProvidersScreen(nav) }
        composable(Routes.Settings) { SettingsScreen(nav) }

        composable(
            Routes.MovieDetails,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { backStack ->
            MovieDetailsScreen(nav, backStack.arguments?.getString("id") ?: "")
        }
        composable(
            Routes.SeriesDetails,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { backStack ->
            SeriesDetailsScreen(nav, backStack.arguments?.getString("id") ?: "")
        }
        composable(
            Routes.Player,
            arguments = listOf(navArgument("streamUrl") { type = NavType.StringType }),
        ) { backStack ->
            val encoded = backStack.arguments?.getString("streamUrl") ?: ""
            val url = java.net.URLDecoder.decode(encoded, "UTF-8")
            PlayerScreen(nav, url)
        }
    }
}
