package com.vecturai.designsystem.navigation

import androidx.compose.runtime.Composable
import androidx.compose.material3.ExperimentalMaterial3Api
import com.vecturai.designsystem.screens.HomeScreen
import com.vecturai.designsystem.screens.SearchScreen
import com.vecturai.designsystem.screens.ImportScreen

/**
 * Main navigation controller for the Vecturai app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    onNavigateToAr: (String?) -> Unit = {},
    onPickImportFile: () -> Unit = {}
) {
    when (currentScreen) {
        Screen.Home -> HomeScreen(
            onFindRoomClick = { onNavigate(Screen.Search) },
            onStartArClick = { onNavigateToAr(null) }
        )
        Screen.Search -> SearchScreen(
            onRoomSelected = { roomName ->
                onNavigateToAr(roomName)
            }
        )
        Screen.Import -> ImportScreen(
            onPickFile = onPickImportFile
        )
        else -> HomeScreen(
            onFindRoomClick = { onNavigate(Screen.Search) },
            onStartArClick = { onNavigateToAr(null) }
        )
    }
}
