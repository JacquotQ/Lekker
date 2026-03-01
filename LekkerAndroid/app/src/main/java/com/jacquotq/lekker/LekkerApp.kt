package com.jacquotq.lekker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.jacquotq.lekker.data.AppSettings
import com.jacquotq.lekker.data.AppStorage
import com.jacquotq.lekker.service.AIService
import com.jacquotq.lekker.service.TTSService
import com.jacquotq.lekker.ui.screens.CollectionScreen
import com.jacquotq.lekker.ui.screens.FlashcardScreen
import com.jacquotq.lekker.ui.screens.SearchScreen
import com.jacquotq.lekker.ui.screens.SettingsScreen
import com.jacquotq.lekker.ui.theme.*

sealed class Screen(val route: String) {
    object Search     : Screen("search")
    object SearchWord : Screen("search/{word}") {
        fun withWord(word: String) = "search/${java.net.URLEncoder.encode(word, "UTF-8")}"
    }
    object Flashcard  : Screen("flashcard")
    object Collection : Screen("collection")
    object Settings   : Screen("settings")
}

/** The four bottom-nav destinations (no SearchWord — it's an overlay of Search). */
private val bottomNavTabs = listOf(
    Screen.Search, Screen.Flashcard, Screen.Collection, Screen.Settings
)

@Composable
fun LekkerApp(storage: AppStorage, tts: TTSService, ai: AIService) {
    // Reactive settings — changes here re-compose the whole shell (bottom bar + theme)
    var settings by remember { mutableStateOf(storage.loadSettings()) }

    fun onSettingsChanged(newSettings: AppSettings) {
        storage.saveSettings(newSettings)
        settings = newSettings
    }

    LekkerTheme(theme = settings.theme) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // Pick colours from the active theme so the nav bar always matches
        val navBg     = MaterialTheme.colorScheme.surface
        val navBorder = MaterialTheme.colorScheme.outline
        val bgColor   = MaterialTheme.colorScheme.background

        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = navBg,
                    tonalElevation = 0.dp
                ) {
                    bottomNavTabs.forEach { screen ->
                        // Highlight Search tab for both "search" and "search/{word}"
                        val selected = when (screen) {
                            Screen.Search -> currentRoute == Screen.Search.route ||
                                    currentRoute == Screen.SearchWord.route
                            else -> currentRoute == screen.route
                        }
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = when (screen) {
                                        Screen.Search     -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
                                        Screen.Flashcard  -> if (selected) Icons.Filled.BookmarkAdd else Icons.Outlined.BookmarkBorder
                                        Screen.Collection -> if (selected) Icons.AutoMirrored.Filled.LibraryBooks else Icons.AutoMirrored.Outlined.LibraryBooks
                                        Screen.Settings   -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
                                        else              -> Icons.Outlined.Home
                                    },
                                    contentDescription = when (screen) {
                                        Screen.Search     -> "Search"
                                        Screen.Flashcard  -> "Flashcard"
                                        Screen.Collection -> "Collection"
                                        Screen.Settings   -> "Settings"
                                        else              -> ""
                                    }
                                )
                            },
                            label = null,
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Search.route) { saveState = false }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = navBg
                            )
                        )
                    }
                }
                HorizontalDivider(color = navBorder)
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Search.route,
                modifier = Modifier
                    .padding(innerPadding)
                    .background(bgColor)
            ) {
                // Plain search home
                composable(Screen.Search.route) {
                    SearchScreen(storage, tts, ai, settings)
                }

                // Search with a pre-loaded word (deep-link from flashcard)
                composable(
                    route = Screen.SearchWord.route,
                    arguments = listOf(navArgument("word") { type = NavType.StringType })
                ) { backStackEntry ->
                    val word = backStackEntry.arguments?.getString("word")
                        ?.let { java.net.URLDecoder.decode(it, "UTF-8") }
                    SearchScreen(storage, tts, ai, settings, initialQuery = word)
                }

                composable(Screen.Flashcard.route) {
                    FlashcardScreen(
                        storage = storage,
                        tts = tts,
                        onNavigateToWord = { word ->
                            navController.navigate(Screen.SearchWord.withWord(word)) {
                                popUpTo(Screen.Search.route) { saveState = false }
                                launchSingleTop = true
                                restoreState = false
                            }
                        }
                    )
                }

                composable(Screen.Collection.route) {
                    CollectionScreen(storage, tts, settings)
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        storage  = storage,
                        settings = settings,
                        onSettingsChanged = ::onSettingsChanged
                    )
                }
            }
        }
    }
}
