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
 * @param onRoomSelected Callback when a room is tapped, receives the room name
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    onRoomSelected: (String) -> Unit = {},
) {
    var searchQuery by remember { mutableStateOf("") }

    // Demo room data
    val allRooms = remember {
        listOf(
            "Conference Room A" to "Ground Floor • Meeting Room",
            "Conference Room B" to "Ground Floor • Meeting Room",
            "Kitchen" to "Ground Floor • Facility",
            "Reception" to "Ground Floor • Lobby",
            "Office 101" to "1st Floor • Office",
            "Office 102" to "1st Floor • Office",
            "Office 103" to "1st Floor • Office",
            "Server Room" to "Basement • Utility",
            "Break Room" to "Ground Floor • Facility",
            "Restroom A" to "Ground Floor • Facility",
            "Restroom B" to "1st Floor • Facility",
            "Storage" to "Basement • Utility",
        )
    }

    val filteredRooms = if (searchQuery.isEmpty()) {
        allRooms
    } else {
        allRooms.filter { (name, desc) ->
            name.contains(searchQuery, ignoreCase = true) ||
                desc.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Search bar
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = { /* search is live-filtered */ },
            active = false,
            onActiveChange = { },
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

        if (filteredRooms.isEmpty()) {
            VecturaiEmptyState(
                icon = Icons.Default.SearchOff,
                title = "No rooms found",
                description = "Try a different search term.",
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredRooms.size) { index ->
                    val (roomName, roomDesc) = filteredRooms[index]
                    VecturaiCard(onClick = { onRoomSelected(roomName) }) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Icon(
                                imageVector = Icons.Default.Room,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = roomName,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = roomDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
