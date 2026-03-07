package com.vecturai.designsystem.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vecturai.designsystem.VecturaiCard
import com.vecturai.designsystem.VecturaiEmptyState

/**
 * Search screen for finding rooms within the building.
 *
 * TODO: Connect to SearchUseCase for actual room search
 * TODO: Add room category filters
 * TODO: Show search suggestions from history
 * TODO: Navigate to Route Preview on room selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        // Search bar
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = { /* TODO: Trigger search */ },
            active = false,
            onActiveChange = { /* TODO: Handle search active state */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search rooms, offices, facilities...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
        ) {}

        if (searchQuery.isEmpty()) {
            // Show categories when not searching
            VecturaiEmptyState(
                icon = Icons.Default.SearchOff,
                title = "Search for a destination",
                description = "Type a room name, office number, or facility type to find your way.",
                modifier = Modifier.weight(1f),
            )
        } else {
            // TODO: Show real search results from SearchUseCase
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Placeholder results
                items(3) { index ->
                    VecturaiCard(onClick = { /* TODO: Navigate to route preview */ }) {
                        Text(
                            text = "Room ${index + 101}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Ground Floor • Office",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
