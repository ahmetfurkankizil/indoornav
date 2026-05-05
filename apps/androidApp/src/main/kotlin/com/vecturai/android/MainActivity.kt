package com.VecturAI.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.VecturAI.android.ar.ArCameraActivity
import com.VecturAI.android.navigation.AndroidNavigationFlowModel
import com.VecturAI.android.ui.AndroidNavigationApp
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val flowModel: AndroidNavigationFlowModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidNavigationApp(
                flowModel = flowModel,
                onStartNavigation = {
                    startActivity(Intent(this, ArCameraActivity::class.java))
                },
            )
        }
    }
}
