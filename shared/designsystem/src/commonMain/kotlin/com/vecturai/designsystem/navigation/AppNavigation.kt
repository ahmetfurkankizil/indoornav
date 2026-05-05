package com.VecturAI.designsystem.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.VecturAI.designsystem.screens.*

/**
 * Main app navigation scaffold with bottom navigation bar.
 *
 * Hosts all non-AR screens in a single navigation structure.
 * The AR screen is launched separately as a native activity/view.
 *
 * @param onNavigateToAr Callback to launch the native AR screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    onNavigateToAr: () -> Unit = {},
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentScreen == Screen.Home,
                    onClick = { currentScreen = Screen.Home },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.Search,
                    onClick = { currentScreen = Screen.Search },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    label = { Text("Search") },
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.RoutePreview,
                    onClick = { currentScreen = Screen.RoutePreview },
                    icon = { Icon(Icons.Default.Map, contentDescription = "Preview") },
                    label = { Text("Preview") },
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.History,
                    onClick = { currentScreen = Screen.History },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History") },
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.Settings,
                    onClick = { currentScreen = Screen.Settings },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                )
            }
        },
    ) { paddingValues ->
        val modifier = Modifier.padding(paddingValues)

        when (currentScreen) {
            Screen.Home -> HomeScreen(
                modifier = modifier,
                onNavigateToSearch = { currentScreen = Screen.Search },
                onNavigateToAr = onNavigateToAr,
            )
            Screen.Search -> SearchScreen(modifier = modifier)
            Screen.RoutePreview -> RoutePreviewScreen(modifier = modifier)
            Screen.History -> HistoryScreen(modifier = modifier)
            Screen.Settings -> SettingsScreen(modifier = modifier)
        }
    }
}
