package com.Vectura AI.designsystem.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.Vectura AI.designsystem.Vectura AICard

/**
 * Settings screen for app configuration.
 *
 * TODO: Connect to actual app preferences
 * TODO: Add dark mode toggle
 * TODO: Add cache management (clear/refresh building data)
 * TODO: Show app version and build info
 * TODO: Add feedback/support link
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(Modifier.height(24.dp))

        // Cache Management
        Vectura AICard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storage, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Building Data Cache", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Manage offline building data",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = { /* TODO: Refresh cache */ }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Dark Mode
        var darkMode by remember { mutableStateOf(false) }
        Vectura AICard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DarkMode, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("Dark Mode", style = MaterialTheme.typography.titleMedium)
                }
                Switch(
                    checked = darkMode,
                    onCheckedChange = { darkMode = it },
                    // TODO: Actually apply dark mode theme
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // AR Settings
        Vectura AICard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ViewInAr, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("AR Settings", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Navigation arrow style, guidance voice",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // TODO: Expand to AR settings sub-screen
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // App info
        Text(
            text = "Vectura AI v0.1.0 (MVP)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}
