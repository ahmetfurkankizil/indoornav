package com.example.vecturai

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.vecturai.ar.CloudAnchorAuthStatus
import com.example.vecturai.persistence.GraphRepository
import com.example.vecturai.ui.ModePickerScreen
import com.example.vecturai.ui.mapping.MappingRoute
import com.example.vecturai.ui.navigation.NavigationRoute
import com.example.vecturai.ui.theme.VecturaiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VecturaiTheme {
                IndoorNavApp()
            }
        }
    }
}

@Composable
private fun IndoorNavApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val graphRepository = remember { GraphRepository(context) }
    val cloudAnchorAuthStatus = remember { CloudAnchorAuthStatus.from(context) }
    val preferences = remember {
        context.getSharedPreferences("indoor_ar_nav", Context.MODE_PRIVATE)
    }
    var disclosureAccepted by remember {
        mutableStateOf(preferences.getBoolean(KEY_CLOUD_ANCHOR_DISCLOSURE, false))
    }

    NavHost(
        navController = navController,
        startDestination = Route.ModePicker.path
    ) {
        composable(Route.ModePicker.path) {
            ModePickerScreen(
                showDisclosure = !disclosureAccepted,
                cloudAnchorAuthStatus = cloudAnchorAuthStatus,
                onDisclosureAccepted = {
                    preferences.edit()
                        .putBoolean(KEY_CLOUD_ANCHOR_DISCLOSURE, true)
                        .apply()
                    disclosureAccepted = true
                },
                onMapBuilding = { navController.navigate(Route.Mapping.path) },
                onNavigate = { navController.navigate(Route.Navigation.path) }
            )
        }
        composable(Route.Mapping.path) {
            MappingRoute(
                graphRepository = graphRepository,
                onExit = { navController.popBackStack() }
            )
        }
        composable(Route.Navigation.path) {
            NavigationRoute(
                graphRepository = graphRepository,
                onExit = { navController.popBackStack() }
            )
        }
    }
}

private enum class Route(val path: String) {
    ModePicker("mode_picker"),
    Mapping("mapping"),
    Navigation("navigation")
}

private const val KEY_CLOUD_ANCHOR_DISCLOSURE = "cloud_anchor_disclosure_accepted"
