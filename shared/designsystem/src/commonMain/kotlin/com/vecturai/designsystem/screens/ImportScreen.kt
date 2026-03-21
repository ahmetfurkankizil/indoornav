package com.vecturai.designsystem.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vecturai.designsystem.VecturaiButton
import com.vecturai.designsystem.VecturaiCard
import com.vecturai.designsystem.VecturaiSectionHeader

/**
 * Import screen for importing new floor plans into VecturAI.
 *
 * Supports PLY, XYZ, PTS, CSV, and GLB/GLTF formats.
 */
@Composable
fun ImportScreen(
    modifier: Modifier = Modifier,
    onPickFile: (() -> Unit)? = null,
) {
    var selectedFormat by remember { mutableStateOf<String?>(null) }
    var importStatus by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.FileUpload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "Import Floor Plan",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "Add a new scanned building map",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Supported Formats
        VecturaiSectionHeader("Supported Formats")
        Spacer(Modifier.height(8.dp))

        val formats = listOf(
            Triple("PLY", "Point Cloud (Polygon)", "ASCII & Binary LE/BE supported"),
            Triple("XYZ", "Point Cloud (XYZ)", "Space or tab delimited, x y z per line"),
            Triple("PTS", "Leica Point Cloud", "With point count header"),
            Triple("CSV", "Comma-Separated Values", "Auto-detects x, y, z columns"),
            Triple("GLB / GLTF", "3D Model", "Native format, full mesh support"),
        )

        formats.forEach { (name, type, detail) ->
            VecturaiCard(
                onClick = { selectedFormat = name },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = if (selectedFormat == name) Icons.Default.CheckCircle else Icons.Default.InsertDriveFile,
                        contentDescription = null,
                        tint = if (selectedFormat == name) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$name — $type",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Unsupported format note
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            ),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "DXF files are not supported (2D CAD format, no 3D point cloud data)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Import Button
        VecturaiButton(
            text = "Select File to Import",
            onClick = {
                if (onPickFile != null) {
                    onPickFile()
                } else {
                    importStatus = "File picker launched — select your scan file"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.FolderOpen,
        )

        Spacer(Modifier.height(12.dp))

        // Status message
        importStatus?.let { status ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // How it works
        VecturaiSectionHeader("How It Works")
        Spacer(Modifier.height(8.dp))

        val steps = listOf(
            "Scan your building with a 3D scanner or LiDAR device",
            "Export the scan in PLY, XYZ, PTS, CSV, or GLB format",
            "Import the file here — VecturAI processes it automatically",
            "The floor plan is analyzed: floor detection, room zones, and navigation paths",
            "Review and start navigating your new building!",
        )

        steps.forEachIndexed { index, step ->
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = step,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
