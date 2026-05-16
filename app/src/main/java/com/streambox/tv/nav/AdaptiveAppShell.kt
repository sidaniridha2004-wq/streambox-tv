package com.streambox.tv.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.streambox.tv.ui.components.NavItem
import com.streambox.tv.ui.components.PhoneBottomNav
import com.streambox.tv.ui.components.TvNavRail

private val MAIN_TABS = listOf(
    NavItem(Routes.Home, "Home", Icons.Default.Home),
    NavItem(Routes.LiveTv, "Live TV", Icons.Default.LiveTv),
    NavItem(Routes.Epg, "Guide", Icons.Default.CalendarMonth),
    NavItem(Routes.Movies, "Movies", Icons.Default.Movie),
    NavItem(Routes.Series, "Series", Icons.Default.Tv),
    NavItem(Routes.Search, "Search", Icons.Default.Search),
    NavItem(Routes.Favorites, "Favorites", Icons.Default.Favorite),
    NavItem(Routes.Providers, "Providers", Icons.Default.AccountCircle),
    NavItem(Routes.Settings, "Settings", Icons.Default.Settings),
)

private val PHONE_TABS = listOf(
    NavItem(Routes.Home, "Home", Icons.Default.Home),
    NavItem(Routes.LiveTv, "Live", Icons.Default.LiveTv),
    NavItem(Routes.Movies, "Movies", Icons.Default.Movie),
    NavItem(Routes.Search, "Search", Icons.Default.Search),
    NavItem(Routes.Settings, "More", Icons.Default.Settings),
)

@Composable
fun AdaptiveAppShell(windowSize: WindowSizeClass, isTv: Boolean) {
    val nav = rememberNavController()
    val useRail = isTv || windowSize.widthSizeClass != WindowWidthSizeClass.Compact

    Box(Modifier.fillMaxSize()) {
        if (useRail) {
            ShellWithRail(nav)
        } else {
            ShellWithBottomNav(nav)
        }
    }
}

@Composable
private fun ShellWithRail(nav: NavHostController) {
    val current by nav.currentBackStackEntryAsState()
    val route = current?.destination?.route ?: Routes.Splash
    val showRail = route in setOf(
        Routes.Home, Routes.LiveTv, Routes.Epg, Routes.Movies, Routes.Series,
        Routes.Search, Routes.Favorites, Routes.Providers, Routes.Settings,
    )
    Row(Modifier.fillMaxSize()) {
        if (showRail) {
            TvNavRail(
                items = MAIN_TABS,
                current = route,
                onSelect = { dest -> if (dest != route) nav.navigate(dest) { launchSingleTop = true } },
            )
        }
        Box(Modifier.weight(1f).fillMaxSize()) {
            AppNavHost(nav)
        }
    }
}

@Composable
private fun ShellWithBottomNav(nav: NavHostController) {
    val current by nav.currentBackStackEntryAsState()
    val route = current?.destination?.route ?: Routes.Splash
    val showBottom = route in setOf(
        Routes.Home, Routes.LiveTv, Routes.Movies, Routes.Search, Routes.Settings,
    )
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxSize()) {
            AppNavHost(nav)
        }
        if (showBottom) {
            PhoneBottomNav(
                items = PHONE_TABS,
                current = route,
                onSelect = { dest -> if (dest != route) nav.navigate(dest) { launchSingleTop = true } },
            )
        }
    }
}
