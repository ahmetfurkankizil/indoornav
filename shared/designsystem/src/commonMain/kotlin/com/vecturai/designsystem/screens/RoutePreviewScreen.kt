package com.Vectura AI.designsystem.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.Vectura AI.designsystem.Vectura AIButton
import com.Vectura AI.designsystem.Vectura AICard
import com.Vectura AI.designsystem.Vectura AIEmptyState
import com.Vectura AI.designsystem.Vectura AISectionHeader

/**
 * Route Preview screen showing a 2D preview of the navigation route.
 *
 * Displays step-by-step instructions, total distance, estimated time,
 * and a "Start AR Navigation" button.
 *
 * TODO: Connect to RoutePreviewUseCase
 * TODO: Add 2D map visualization
 * TODO: Show turn-by-turn instruction cards
 * TODO: Launch AR navigation on button press
 */
@Composable
fun RoutePreviewScreen(
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Map,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Route Preview",
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        // TODO: Replace with actual route data
        Vectura AIEmptyState(
            icon = Icons.Default.Route,
            title = "No route selected",
            description = "Search for a destination first to preview the navigation route.",
            modifier = Modifier.weight(1f),
        )

        // Start Navigation button (always visible at bottom)
        Vectura AIButton(
            text = "Start AR Navigation",
            onClick = { /* TODO: Launch AR navigation */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            icon = Icons.Default.Navigation,
            enabled = false, // TODO: Enable when route is available
        )
    }
}
