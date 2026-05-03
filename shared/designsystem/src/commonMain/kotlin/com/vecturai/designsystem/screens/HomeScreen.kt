package com.Vectura AI.designsystem.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.Vectura AI.designsystem.Vectura AIButton
import com.Vectura AI.designsystem.Vectura AICard
import com.Vectura AI.designsystem.Vectura AISectionHeader

/**
 * Home screen — the main landing screen of Vectura AI.
 *
 * Displays a welcome message, quick actions, and building information.
 *
 * TODO: Load actual building data from repository
 * TODO: Show cached building status
 * TODO: Display recent destinations for quick re-navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToSearch: () -> Unit = {},
    onNavigateToAr: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // Header
        Text(
            text = "Vectura AI",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Indoor Navigation",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))

        // Quick Actions
        Vectura AISectionHeader("Quick Actions")
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Vectura AIButton(
                text = "Find Room",
                onClick = onNavigateToSearch,
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Search,
            )
            Vectura AIButton(
                text = "Start AR",
                onClick = onNavigateToAr,
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CameraAlt,
            )
        }

        Spacer(Modifier.height(24.dp))

        // Building Info Card
        Vectura AISectionHeader("Current Building")
        Spacer(Modifier.height(8.dp))

        Vectura AICard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Demo Building",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Ground Floor • 12 rooms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // TODO: Show real building data
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Status
        Vectura AISectionHeader("Status")
        Spacer(Modifier.height(8.dp))

        Vectura AICard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Building data cached and ready",
                    style = MaterialTheme.typography.bodyMedium,
                )
                // TODO: Show actual cache status
            }
        }
    }
}
