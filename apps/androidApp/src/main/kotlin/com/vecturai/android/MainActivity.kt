package com.vecturai.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.vecturai.android.ar.ArNavigationActivity
import com.vecturai.designsystem.VecturaiAppContent

class MainActivity : ComponentActivity() {

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            Toast.makeText(this, "Importing: ${uri.path}", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VecturaiAppContent(
                onNavigateToAr = { roomName ->
                    val intent = Intent(this, ArNavigationActivity::class.java).apply {
                        putExtra("destinationName", roomName)
                    }
                    startActivity(intent)
                },
                onPickImportFile = {
                    pickFileLauncher.launch("*/*")
                },
            )
        }
    }
}
