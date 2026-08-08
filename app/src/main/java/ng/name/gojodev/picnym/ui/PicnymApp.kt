package ng.name.gojodev.picnym.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import kotlinx.coroutines.launch
import ng.name.gojodev.picnym.BuildConfig
import ng.name.gojodev.picnym.data.AuthRepository
import ng.name.gojodev.picnym.data.PicnymApi
import ng.name.gojodev.picnym.data.SessionStore
import ng.name.gojodev.picnym.ui.screens.AccountScreen
import ng.name.gojodev.picnym.ui.screens.AuthScreen
import ng.name.gojodev.picnym.ui.screens.DashboardScreen
import ng.name.gojodev.picnym.ui.screens.HomeScreen
import ng.name.gojodev.picnym.ui.screens.PollScreen
import ng.name.gojodev.picnym.ui.screens.PublicInboxScreen
import ng.name.gojodev.picnym.ui.screens.PublicProfileScreen
import ng.name.gojodev.picnym.ui.theme.AppThemeState
import ng.name.gojodev.picnym.ui.theme.ThemeMode

@Composable
fun PicnymApp() {
    val context = LocalContext.current.applicationContext
    val store = remember { SessionStore(context) }
    val auth = remember { AuthRepository(store) }
    val api = remember { PicnymApi(store, auth) }
    val scope = rememberCoroutineScope()
    val systemDark = isSystemInDarkTheme()
    val signedIn by produceState<Boolean?>(initialValue = null, store) { value = store.current().signedIn }

    LaunchedEffect(store) {
        store.themeFlow.collect { AppThemeState.mode = ThemeMode.from(it) }
    }

    if (signedIn == null) {
        LoadingScreen("Starting PICNYM…")
        return
    }

    val nav = rememberNavController()
    val start = if (signedIn == true) "home" else "auth"

    fun home() {
        nav.navigate("home") { popUpTo(nav.graph.startDestinationId) { inclusive = false }; launchSingleTop = true }
    }
    fun account() { nav.navigate("account") { launchSingleTop = true } }
    fun dashboard(slug: String) { nav.navigate("dashboard/$slug") }
    fun publicInbox(slug: String) { nav.navigate("send/$slug") }
    fun profile(username: String) { if (username.isNotBlank()) nav.navigate("profile/$username") }
    fun poll(slug: String) { if (slug.isNotBlank()) nav.navigate("poll/$slug") }
    fun quickTheme() {
        val currentlyDark = when (AppThemeState.mode) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
        }
        val next = if (currentlyDark) ThemeMode.LIGHT else ThemeMode.DARK
        AppThemeState.mode = next
        scope.launch { store.saveTheme(next.key) }
    }
    fun signedOut() {
        nav.navigate("auth") { popUpTo(0) }
    }

    NavHost(navController = nav, startDestination = start) {
        composable("auth") {
            AuthScreen(auth) {
                nav.navigate("home") { popUpTo("auth") { inclusive = true } }
            }
        }
        composable("home") {
            HomeScreen(api, onAccount = ::account, onDashboard = ::dashboard, onPublicInbox = ::publicInbox, onThemeToggle = ::quickTheme)
        }
        composable("account") {
            AccountScreen(api, auth, store, onHome = ::home, onDashboard = ::dashboard, onProfile = ::profile, onSignedOut = ::signedOut)
        }
        composable(
            route = "dashboard/{slug}",
            arguments = listOf(navArgument("slug") { type = NavType.StringType })
        ) { entry ->
            DashboardScreen(api, entry.arguments?.getString("slug").orEmpty(), onBack = { nav.popBackStack() }, onOpenPoll = ::poll, onOpenProfile = ::profile)
        }
        composable(
            route = "send/{slug}",
            arguments = listOf(navArgument("slug") { type = NavType.StringType }),
            deepLinks = listOf(
                navDeepLink { uriPattern = BuildConfig.SITE_URL + "/u/{slug}" },
                navDeepLink { uriPattern = "picnym://u/{slug}" }
            )
        ) { entry ->
            PublicInboxScreen(
                api = api,
                auth = auth,
                slug = entry.arguments?.getString("slug").orEmpty(),
                onBack = { nav.popBackStack() },
                onProfile = ::profile,
                onPollCreated = ::poll
            )
        }
        composable(
            route = "profile/{username}",
            arguments = listOf(navArgument("username") { type = NavType.StringType }),
            deepLinks = listOf(
                navDeepLink { uriPattern = BuildConfig.SITE_URL + "/profile/{username}" },
                navDeepLink { uriPattern = "picnym://profile/{username}" }
            )
        ) { entry ->
            PublicProfileScreen(api, auth, entry.arguments?.getString("username").orEmpty(), onBack = { nav.popBackStack() })
        }
        composable(
            route = "poll/{slug}",
            arguments = listOf(navArgument("slug") { type = NavType.StringType }),
            deepLinks = listOf(
                navDeepLink { uriPattern = BuildConfig.SITE_URL + "/poll/{slug}" },
                navDeepLink { uriPattern = "picnym://poll/{slug}" }
            )
        ) { entry ->
            PollScreen(api, entry.arguments?.getString("slug").orEmpty(), onBack = { nav.popBackStack() })
        }
    }
}
