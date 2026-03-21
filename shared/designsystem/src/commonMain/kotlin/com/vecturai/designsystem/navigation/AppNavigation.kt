package com.vecturai.designsystem.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.ExperimentalMaterial3Api
import com.vecturai.designsystem.screens.HomeScreen
import com.vecturai.designsystem.screens.SearchScreen
import com.vecturai.designsystem.screens.ImportScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    onNavigateToAr: (String?) -> Unit = {},
    onPickImportFile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    when (currentScreen) {
        Screen.Home -> HomeScreen(
            onNavigateToSearch = { onNavigate(Screen.Search) },
            onNavigateToAr = { onNavigateToAr(null) },
            modifier = modifier
        )
        Screen.Search -> SearchScreen(
            onRoomSelected = { roomName ->
                onNavigateToAr(roomName)
            },
            modifier = modifier
        )
        Screen.Import -> ImportScreen(
            onPickFile = onPickImportFile,
            modifier = modifier
        )
        else -> HomeScreen(
            onNavigateToSearch = { onNavigate(Screen.Search) },
            onNavigateToAr = { onNavigateToAr(null) },
            modifier = modifier
        )
    }
}
