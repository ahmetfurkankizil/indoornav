package com.example.vecturai.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DestinationPicker(
    modifier: Modifier,
    state: NavigationUiState,
    onSelect: (String) -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 10.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Pick Destination", style = MaterialTheme.typography.titleMedium)
            if (state.destinationNodes.isEmpty()) {
                Text("No room labels exist in this graph. Return to mapping and tag anchors.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.destinationNodes) { node ->
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onSelect(node.id) }
                        ) {
                            Text(node.label ?: node.id.take(8))
                        }
                    }
                }
            }
        }
    }
}
