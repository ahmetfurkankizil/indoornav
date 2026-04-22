package com.vecturai.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Elevator
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vecturai.android.data.AndroidReviewedPackageLoader
import com.vecturai.android.navigation.ArCameraFlowViewModel
import com.vecturai.android.navigation.AndroidNavigationFlowModel
import com.vecturai.designsystem.VecturaiTheme
import kotlin.math.ceil

@Composable
fun AndroidNavigationApp(
    flowModel: AndroidNavigationFlowModel,
    onStartNavigation: () -> Unit,
) {
    val state by flowModel.state.collectAsState()

    VecturaiTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val current = state) {
                AndroidNavigationFlowModel.HomeState.Home -> HomeScreen(
                    flowModel = flowModel,
                    onStartNavigation = onStartNavigation,
                )
                is AndroidNavigationFlowModel.HomeState.PackageError -> PackageErrorScreen(
                    message = current.message,
                    onRetry = flowModel::retryPackageLoad,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    flowModel: AndroidNavigationFlowModel,
    onStartNavigation: () -> Unit,
) {
    var showAdminTools by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(onClick = { showAdminTools = true }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Admin Tools",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(
                imageVector = Icons.Default.Map,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("VecturAI", fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Find your way indoors",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.weight(2f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GradientPrimaryButton(
                text = "Scan Entrance Code",
                icon = Icons.Default.QrCodeScanner,
                onClick = onStartNavigation,
            )
            Text(
                "Scan the QR code at the building entrance to begin",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }

    if (showAdminTools) {
        ModalBottomSheet(onDismissRequest = { showAdminTools = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Admin Tools", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "No draft jobs to show on this device.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PackageErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(56.dp), tint = Color(0xFFF59E0B))
        Spacer(Modifier.height(20.dp))
        Text("Unable to Load Navigation Data", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text("Try Again")
        }
    }
}

@Composable
fun DestinationSelectScreen(
    flowModel: ArCameraFlowViewModel,
    onCancel: () -> Unit,
) {
    val session by flowModel.session.collectAsState()
    var searchText by remember { mutableStateOf("") }
    val rooms = flowModel.availableRooms
    val filteredRooms = if (searchText.isBlank()) {
        rooms
    } else {
        rooms.filter {
            it.displayName.contains(searchText, ignoreCase = true) ||
                (it.category?.contains(searchText, ignoreCase = true) == true)
        }
    }
    val groupedRooms = remember(filteredRooms, searchText) {
        if (searchText.isNotBlank()) {
            emptyList()
        } else {
            groupedRooms(filteredRooms)
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        CenteredTopBar(
            title = "Choose Destination",
            onBack = onCancel,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "From: ${session.confirmedEntrance}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { searchText = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            placeholder = { Text("Search rooms...") },
            shape = RoundedCornerShape(10.dp),
        )

        HorizontalDivider()

        if (filteredRooms.isEmpty()) {
            EmptyRoomsState(searchText)
        } else if (searchText.isNotBlank()) {
            LazyColumn(Modifier.fillMaxSize()) {
                items(filteredRooms, key = { it.id }) { room ->
                    RoomRow(room = room, onClick = { flowModel.selectDestination(room) })
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                groupedRooms.forEach { (category, categoryRooms) ->
                    item(key = "header-$category") {
                        Text(
                            displayNameForCategory(category).uppercase(),
                            modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(categoryRooms, key = { it.id }) { room ->
                        RoomRow(room = room, onClick = { flowModel.selectDestination(room) })
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyRoomsState(searchText: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text(
            "No rooms match \"$searchText\"",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RoomRow(
    room: AndroidReviewedPackageLoader.PackageRoom,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val categoryColor = categoryColor(room.category)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(categoryColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = iconForCategory(room.category),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = categoryColor,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(room.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                room.description?.takeIf { it.isNotBlank() } ?: displayNameForCategory(room.category ?: "other"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Surface(
            shape = RoundedCornerShape(50),
            color = categoryColor.copy(alpha = 0.12f),
        ) {
            Text(
                displayNameForCategory(room.category ?: "other"),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = categoryColor,
                maxLines = 1,
            )
        }
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun RoutePreviewScreen(flowModel: ArCameraFlowViewModel) {
    val session by flowModel.session.collectAsState()
    val distance = session.routePackage?.totalDistance ?: 0.0

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        CenteredTopBar(title = "Route Preview", onBack = flowModel::goBackToDestinationSelect)
        HorizontalDivider()

        Spacer(Modifier.weight(1f))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (distance > 0.0) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            formatWalkingTime(distance / 1.2),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("walking", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("~${distance.formatMeters()} m", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(28.dp))
                }

                RoutePointRow(
                    icon = Icons.Default.LocationOn,
                    tint = Color(0xFF10B981),
                    label = "From",
                    value = session.confirmedEntrance,
                )
                Row(Modifier.fillMaxWidth()) {
                    Box(
                        Modifier
                            .padding(start = 15.dp)
                            .width(1.5.dp)
                            .height(28.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                }
                RoutePointRow(
                    icon = Icons.Default.Place,
                    tint = MaterialTheme.colorScheme.primary,
                    label = "To",
                    value = session.selectedRoom?.displayName.orEmpty(),
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GradientPrimaryButton(
                text = "Start AR Navigation",
                icon = Icons.Default.Map,
                onClick = flowModel::startNavigation,
            )
            Text(
                "Point your camera at the entrance sign to begin",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RoutePointRow(icon: ImageVector, tint: Color, label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = tint)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun CenteredTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Back")
        }
        Spacer(Modifier.weight(1f))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        TextButton(onClick = {}, enabled = false) {
            Text("Back", color = Color.Transparent)
        }
    }
}

@Composable
fun EntranceConfirmedSheet(entranceName: String, onContinue: () -> Unit) {
    AnimatedVisibility(
        visible = true,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.weight(1f))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                shadowElevation = 16.dp,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .width(36.dp)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    Spacer(Modifier.height(24.dp))
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(44.dp), tint = Color(0xFF10B981))
                    Spacer(Modifier.height(12.dp))
                    Text("Entrance Confirmed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Starting from $entranceName", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                        Text("Choose Destination")
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun GradientPrimaryButton(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val pressedScale by animateFloatAsState(targetValue = 1f, label = "buttonScale")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)),
                )
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size((20 * pressedScale).dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

private fun groupedRooms(
    rooms: List<AndroidReviewedPackageLoader.PackageRoom>,
): List<Pair<String, List<AndroidReviewedPackageLoader.PackageRoom>>> {
    val grouped = rooms.groupBy { it.category ?: "other" }
    val preferred = listOf(
        "classroom", "lab", "cafe", "vertical_transport", "toilet",
        "kitchen", "living_room", "bedroom", "bathroom", "office", "other",
    )
    val ordered = preferred.filter { grouped.containsKey(it) } +
        grouped.keys.filter { it !in preferred }.sorted()
    return ordered.mapNotNull { category ->
        grouped[category]?.takeIf { it.isNotEmpty() }?.let { category to it }
    }
}

private fun iconForCategory(category: String?): ImageVector = when (category) {
    "classroom" -> Icons.Default.School
    "lab" -> Icons.Default.Science
    "cafe", "kitchen" -> Icons.Default.Restaurant
    "vertical_transport" -> Icons.Default.Elevator
    else -> Icons.Default.MeetingRoom
}

private fun categoryColor(category: String?): Color = when (category) {
    "classroom" -> Color(0xFF2563EB)
    "lab" -> Color(0xFF7C3AED)
    "cafe", "kitchen" -> Color(0xFFF59E0B)
    "vertical_transport" -> Color(0xFF0D9488)
    "toilet" -> Color(0xFF94A3B8)
    "office" -> Color(0xFF10B981)
    else -> Color(0xFF64748B)
}

private fun displayNameForCategory(category: String): String = when (category) {
    "classroom" -> "Classrooms"
    "lab" -> "Laboratories"
    "cafe" -> "Cafe"
    "vertical_transport" -> "Elevators"
    "toilet" -> "Restrooms"
    "kitchen" -> "Kitchen"
    "living_room", "salon" -> "Living Room"
    "bedroom" -> "Bedroom"
    "bathroom" -> "Bathroom"
    "office" -> "Office"
    else -> "Other"
}

private fun formatWalkingTime(seconds: Double): String {
    if (seconds < 60.0) return "< 1 min"
    return "~${ceil(seconds / 60.0).toInt()} min"
}

private fun Double.formatMeters(): String = "%.0f".format(this)
