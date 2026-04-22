package com.vecturai.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.vecturai.android.ar.AndroidArNavigationViewModel
import com.vecturai.android.navigation.AndroidNavigationFlowModel
import com.vecturai.android.ui.AndroidNavigationApp
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val flowModel: AndroidNavigationFlowModel by viewModel()
    private val arViewModel: AndroidArNavigationViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidNavigationApp(
                flowModel = flowModel,
                arViewModel = arViewModel,
            )
        }
    }
}
