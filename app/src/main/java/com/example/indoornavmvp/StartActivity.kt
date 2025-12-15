package com.example.indoornavmvp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.indoornavmvp.ui.theme.IndoorNavMvpTheme

/**
 * Launcher activity with mode selection buttons.
 */
class StartActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            IndoorNavMvpTheme {
                StartScreen(
                    onAdminClick = {
                        startActivity(Intent(this, AdminActivity::class.java))
                    },
                    onUserClick = {
                        startActivity(Intent(this, UserArActivity::class.java))
                    }
                )
            }
        }
    }
}

@Composable
private fun StartScreen(
    onAdminClick: () -> Unit,
    onUserClick: () -> Unit
) {
    // Gradient background colors
    val gradientColors = listOf(
        Color(0xFF1A1A2E),
        Color(0xFF16213E),
        Color(0xFF0F3460)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Title
            Text(
                text = "Indoor Nav",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "MVP",
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
                color = Color(0xFF00D9FF),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "AR Navigation Prototype",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 48.dp)
            )

            // Admin Mode Button
            ModeButton(
                text = "Admin Mode",
                subtitle = "Place navigation nodes",
                color = Color(0xFF4CAF50),
                onClick = onAdminClick
            )

            Spacer(modifier = Modifier.height(20.dp))

            // User Mode Button
            ModeButton(
                text = "User Mode",
                subtitle = "Navigate to destination",
                color = Color(0xFF2196F3),
                onClick = onUserClick
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Footer info
            Text(
                text = "Single-floor • Same-device • No backend",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun ModeButton(
    text: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(260.dp)
            .height(72.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.9f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 2.dp
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = text,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

