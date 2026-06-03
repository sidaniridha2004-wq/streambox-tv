package com.antigravity.iptv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.compose.animation.*
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.SportsTennis
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.antigravity.iptv.data.local.AppPreferences
import com.antigravity.iptv.ui.MainViewModel
import com.antigravity.iptv.ui.screens.*
import com.antigravity.iptv.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder
import javax.inject.Inject

data class BottomNavItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("home", R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem("live_tv", R.string.nav_live_tv, Icons.Filled.Tv, Icons.Outlined.Tv),
    BottomNavItem("movies", R.string.nav_movies, Icons.Filled.Movie, Icons.Outlined.Movie),
    BottomNavItem("series", R.string.nav_series, Icons.Filled.PlayCircle, Icons.Outlined.PlayCircleOutline),
    BottomNavItem("sports", R.string.nav_sports, Icons.Filled.SportsTennis, Icons.Outlined.SportsTennis)
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appPreferences: AppPreferences

    private var keepSplash = true
    private var initialRoute = "select_playlist"
    private var allowPip = false

    override fun attachBaseContext(newBase: android.content.Context) {
        val locale = com.antigravity.iptv.data.local.LocaleManager.getLocale(newBase)
        super.attachBaseContext(com.antigravity.iptv.data.local.LocaleManager.setLocale(newBase, locale))
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (allowPip && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            enterPictureInPictureMode(android.app.PictureInPictureParams.Builder().build())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Hold the splash screen until we've read the DataStore once
        splashScreen.setKeepOnScreenCondition { keepSplash }

        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val isDarkMode by mainViewModel.isDarkMode.collectAsState()
            
            var isReady by remember { mutableStateOf(false) }
            
            LaunchedEffect(Unit) {
                val hasSelectedLanguage = appPreferences.hasSelectedLanguage.first()
                val hasCompletedOnboarding = appPreferences.hasCompletedOnboarding.first()
                val activated = appPreferences.isActivated.first()
                
                if (!hasSelectedLanguage) {
                    initialRoute = "language_selection"
                } else if (!hasCompletedOnboarding) {
                    initialRoute = "onboarding"
                } else if (!activated) {
                    initialRoute = "activation"
                } else {
                    val activeId = appPreferences.activePlaylistId.first()
                    if (activeId != null) {
                        initialRoute = "home"
                    } else {
                        initialRoute = "select_playlist"
                    }
                }
                isReady = true
                keepSplash = false
            }

            IPTVPlayerTheme(darkTheme = isDarkMode) {
                if (!isReady) {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                } else {
                    val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val activePlaylistId by mainViewModel.activePlaylistId.collectAsState()
                val playlists by mainViewModel.playlists.collectAsState()

                // Hide bottom bar on player, add screens, activation, or selection screen
                val showBottomBar = currentRoute != null &&
                    !currentRoute.startsWith("player/") &&
                    !currentRoute.startsWith("add") &&
                    currentRoute != "select_playlist" &&
                    currentRoute != "switch_playlist" &&
                    currentRoute != "activation" &&
                    currentRoute != "language_selection" &&
                    currentRoute != "onboarding"

                allowPip = currentRoute?.startsWith("player/") == true

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp
                            ) {
                                bottomNavItems.forEach { item ->
                                    val isSelected = currentRoute == item.route
                                    val labelText = stringResource(id = item.labelRes)
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = {
                                            if (currentRoute != item.route) {
                                                navController.navigate(item.route) {
                                                    popUpTo("home") { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = labelText
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = labelText,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            indicatorColor = Color.Transparent
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = initialRoute,
                        modifier = Modifier.padding(innerPadding),
                        enterTransition = { 
                            fadeIn(tween(200))
                        },
                        exitTransition = { 
                            fadeOut(tween(150))
                        },
                        popEnterTransition = { 
                            fadeIn(tween(200))
                        },
                        popExitTransition = { 
                            fadeOut(tween(150))
                        }
                    ) {
                        composable("language_selection") {
                            LanguageSelectionScreen(
                                onLanguageSelected = { localeCode ->
                                    lifecycleScope.launch {
                                        appPreferences.setAppLanguage(localeCode)
                                        appPreferences.setHasSelectedLanguage(true)
                                        com.antigravity.iptv.data.local.LocaleManager.saveLocale(this@MainActivity, localeCode)
                                        // Activity restart required to apply locale at base context level
                                        // Moved inside the coroutine to ensure DataStore write finishes first
                                        val intent = intent
                                        finish()
                                        startActivity(intent)
                                    }
                                }
                            )
                        }
                        composable("onboarding") {
                            OnboardingScreen(
                                onFinish = {
                                    lifecycleScope.launch {
                                        appPreferences.setHasCompletedOnboarding(true)
                                        val activated = appPreferences.isActivated.first()
                                        if (activated) {
                                            val activeId = appPreferences.activePlaylistId.first()
                                            if (activeId != null) {
                                                navController.navigate("home") { popUpTo(0) }
                                            } else {
                                                navController.navigate("select_playlist") { popUpTo(0) }
                                            }
                                        } else {
                                            navController.navigate("activation") { popUpTo(0) }
                                        }
                                    }
                                }
                            )
                        }
                        composable("activation") {
                            ActivationScreen(
                                onActivated = {
                                    mainViewModel.activate()
                                    mainViewModel.checkRemoteConfig()
                                    navController.navigate("home") {
                                        popUpTo(0)
                                    }
                                }
                            )
                        }
                        composable("select_playlist") {
                            PlaylistSelectionScreen(
                                playlists = playlists,
                                onSelectPlaylist = { id ->
                                    mainViewModel.setActivePlaylist(id)
                                    navController.navigate("home") {
                                        popUpTo(0) // clear backstack
                                    }
                                },
                                onAddPlaylist = { navController.navigate("add") },
                                onEditPlaylist = { id -> navController.navigate("add?playlistId=$id") },
                                onDeletePlaylist = { playlist -> mainViewModel.deletePlaylist(playlist) }
                            )
                        }
                        composable("switch_playlist") {
                            // Hide the remote playlist from user-visible playlist management
                            val remotePlaylistId by mainViewModel.remotePlaylistId.collectAsState()
                            val userPlaylists = remember(playlists, remotePlaylistId) {
                                playlists.filter { it.id != remotePlaylistId }
                            }
                            PlaylistSelectionScreen(
                                title = "Switch Playlist",
                                playlists = userPlaylists,
                                onSelectPlaylist = { id ->
                                    mainViewModel.setActivePlaylist(id)
                                    navController.popBackStack()
                                },
                                onAddPlaylist = { navController.navigate("add") },
                                onEditPlaylist = { id -> navController.navigate("add?playlistId=$id") },
                                onDeletePlaylist = { playlist -> mainViewModel.deletePlaylist(playlist) },
                                onClose = { navController.popBackStack() }
                            )
                        }
                        composable("home") {
                            LaunchedEffect(Unit) {
                                mainViewModel.checkRemoteConfig()
                            }
                            
                            HomeScreen(
                                viewModel = mainViewModel,
                                onChannelClick = { streamUrl, channelName ->
                                    val encodedUrl = URLEncoder.encode(streamUrl, "UTF-8")
                                    val encodedName = URLEncoder.encode(channelName, "UTF-8")
                                    navController.navigate("player/$encodedUrl/$encodedName")
                                },
                                onSwitchPlaylist = {
                                    navController.navigate("switch_playlist")
                                },
                                onSettingsClick = {
                                    navController.navigate("settings")
                                },
                                onNavigateToCategory = { category ->
                                    navController.navigate(category) {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                        composable("live_tv") {
                            PlaylistDetailsScreen(
                                playlistId = activePlaylistId ?: -1,
                                viewModel = mainViewModel,
                                onBack = { navController.popBackStack() },
                                onChannelClick = { streamUrl, channelName ->
                                    val encodedUrl = URLEncoder.encode(streamUrl, "UTF-8")
                                    val encodedName = URLEncoder.encode(channelName, "UTF-8")
                                    navController.navigate("player/$encodedUrl/$encodedName")
                                }
                            )
                        }
                        composable("movies") {
                            CategoryScreen(
                                viewModel = mainViewModel,
                                categoryFilter = "movies",
                                title = "Movies",
                                onChannelClick = { streamUrl, channelName ->
                                    val encodedUrl = URLEncoder.encode(streamUrl, "UTF-8")
                                    val encodedName = URLEncoder.encode(channelName, "UTF-8")
                                    navController.navigate("player/$encodedUrl/$encodedName")
                                }
                            )
                        }
                        composable("series") {
                            CategoryScreen(
                                viewModel = mainViewModel,
                                categoryFilter = "series",
                                title = "Series",
                                onChannelClick = { streamUrl, channelName ->
                                    val encodedUrl = URLEncoder.encode(streamUrl, "UTF-8")
                                    val encodedName = URLEncoder.encode(channelName, "UTF-8")
                                    navController.navigate("player/$encodedUrl/$encodedName")
                                }
                            )
                        }
                        composable("sports") {
                            // Placeholder for Sports category screen
                            CategoryScreen(
                                viewModel = mainViewModel,
                                categoryFilter = "sports",
                                title = "Sports",
                                onChannelClick = { streamUrl, channelName ->
                                    val encodedUrl = URLEncoder.encode(streamUrl, "UTF-8")
                                    val encodedName = URLEncoder.encode(channelName, "UTF-8")
                                    navController.navigate("player/$encodedUrl/$encodedName")
                                }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                onBack = { navController.popBackStack() },
                                onManagePlaylists = { navController.navigate("switch_playlist") },
                                onLanguageSettingsClick = { navController.navigate("language_selection") }
                            )
                        }
                        composable(
                            route = "add?playlistId={playlistId}",
                            arguments = listOf(navArgument("playlistId") { type = NavType.IntType; defaultValue = -1 })
                        ) { backStackEntry ->
                            val playlistId = backStackEntry.arguments?.getInt("playlistId") ?: -1
                            AddPlaylistScreen(
                                playlistId = playlistId,
                                viewModel = mainViewModel,
                                onBack = { navController.popBackStack() },
                                onSuccess = { 
                                    navController.navigate("home") { popUpTo(0) }
                                }
                            )
                        }
                        composable(
                            route = "player/{streamUrl}/{channelName}",
                            arguments = listOf(
                                navArgument("streamUrl") { type = NavType.StringType },
                                navArgument("channelName") { type = NavType.StringType }
                            ),
                            enterTransition = { fadeIn(tween(150)) },
                            exitTransition = { fadeOut(tween(150)) }
                        ) { backStackEntry ->
                            val streamUrl = backStackEntry.arguments?.getString("streamUrl") ?: ""
                            val channelName = backStackEntry.arguments?.getString("channelName") ?: ""
                            val decodedUrl = URLDecoder.decode(streamUrl, "UTF-8")
                            val decodedName = URLDecoder.decode(channelName, "UTF-8")
                            VideoPlayerScreen(
                                streamUrl = decodedUrl,
                                channelName = decodedName,
                                viewModel = mainViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }

                // Remote Config Syncing Overlay
                val isRemoteSyncing by mainViewModel.isRemoteSyncing.collectAsState()
                val remoteConfigStatus by mainViewModel.remoteConfigStatus.collectAsState()
                
                AnimatedVisibility(
                    visible = isRemoteSyncing,
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(500))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF06091A))
                            .clickable(enabled = false, onClick = {}), // Block all touches
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(20.dp)
                        ) {
                            // Logo
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Aura ", color = AuraCyan, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                Text("TV", color = AuraPurple, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(Color.White, androidx.compose.foundation.shape.RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            CircularProgressIndicator(
                                color = AuraCyan,
                                modifier = Modifier.size(52.dp),
                                strokeWidth = 3.dp
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = remoteConfigStatus ?: "Loading...",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Setting up your content, please wait...",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
            }
        }
    }
}
