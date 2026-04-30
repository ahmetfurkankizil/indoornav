package com.example.vecturai.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.vecturai.ar.CloudAnchorAuthStatus

@Composable
fun ModePickerScreen(
    showDisclosure: Boolean,
    cloudAnchorAuthStatus: CloudAnchorAuthStatus,
    onDisclosureAccepted: () -> Unit,
    onMapBuilding: () -> Unit,
    onNavigate: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Indoor AR Navigation",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = "Map one building with Cloud Anchors, then reopen the app to navigate to tagged rooms.",
            style = MaterialTheme.typography.bodyLarge
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            color = if (cloudAnchorAuthStatus.isConfigured) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        ) {
            Text(
                modifier = Modifier.padding(12.dp),
                text = cloudAnchorAuthStatus.message,
                color = if (cloudAnchorAuthStatus.isConfigured) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(28.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = onMapBuilding
            ) {
                Text("Map")
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onNavigate
            ) {
                Text("Navigate")
            }
        }
    }

    if (showDisclosure) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Cloud Anchor Privacy") },
            text = {
                Text(
                    "This app uses ARCore Cloud Anchors. Visual data from your camera is processed by Google to enable AR features."
                )
            },
            confirmButton = {
                Button(onClick = onDisclosureAccepted) {
                    Text("I Understand")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://policies.google.com/privacy")
                            )
                        )
                    }
                ) {
                    Text("Privacy Policy")
                }
            }
        )
    }
}
