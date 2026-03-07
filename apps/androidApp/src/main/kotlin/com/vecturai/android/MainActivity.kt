package com.vecturai.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.vecturai.designsystem.VecturaiAppContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VecturaiAppContent(
                onNavigateToAr = {
                    // TODO: Launch ArNavigationActivity when AR navigation is triggered
                    // val intent = Intent(this, ArNavigationActivity::class.java)
                    // startActivity(intent)
                }
            )
        }
    }
}
