package com.vecturai.designsystem

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.vecturai.designsystem.navigation.AppNavigation
import com.vecturai.designsystem.navigation.Screen

@Composable
fun VecturaiAppContent(
    onNavigateToAr: (String?) -> Unit = {},
    onPickImportFile: () -> Unit = {}
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    VecturaiTheme {
        Scaffold(
            bottomBar = {
                VecturaiBottomBar(
                    currentScreen = currentScreen,
                    onScreenSelected = { currentScreen = it }
                )
            }
        ) { paddingValues ->
            AppNavigation(
                currentScreen = currentScreen,
                onNavigate = { currentScreen = it },
                onNavigateToAr = onNavigateToAr,
                onPickImportFile = onPickImportFile,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}
