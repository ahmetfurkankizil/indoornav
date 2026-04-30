package com.vecturai.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
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
    onStartNavigation: () -> Unit,
) {
    var showAdminTools by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070D18)),
    ) {
        DotGridBackground()

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 8.dp, end = 16.dp)
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF121A28).copy(alpha = 0.92f))
                .border(1.dp, Color(0xFF253149), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(
                onClick = { showAdminTools = true },
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Admin Tools",
                    tint = Color(0xFF8C99AD),
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF148BFF)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(43.dp),
                    tint = Color.White,
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "VecturAI",
                color = Color.White,
                fontSize = 31.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 34.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Find any room in seconds.",
                color = Color(0xFF8994A6),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FeaturePill("Live AR", Modifier.weight(1f))
                FeaturePill("Smart Routes", Modifier.weight(1f))
                FeaturePill("Indoor Maps", Modifier.weight(1f))
            }

            Spacer(Modifier.weight(1f))

            WelcomePrimaryButton(onClick = onStartNavigation)

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Scan the QR code at any building entrance",
                color = Color(0xFF465164),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showAdminTools) {
        ModalBottomSheet(
            onDismissRequest = { showAdminTools = false },
            containerColor = Color(0xFF101827),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF071C33)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color(0xFF168BFF),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Admin Tools",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            "Draft jobs are not available on this device.",
                            color = Color(0xFF8A95A8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF151F31),
                    border = BorderStroke(1.dp, Color(0xFF233149)),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Visitor navigation is ready. Admin review tools stay separate from the demo flow.",
                            color = Color(0xFFB6BFCE),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 18.sp,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DotGridBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val spacing = 22.dp.toPx()
        val radius = 0.85.dp.toPx()
        var x = 10.dp.toPx()
        while (x < size.width) {
            var y = 14.dp.toPx()
            while (y < size.height) {
                drawCircle(
                    color = Color(0xFF263750).copy(alpha = 0.36f),
                    radius = radius,
                    center = Offset(x, y),
                )
                y += spacing
            }
            x += spacing
        }
    }
}

@Composable
private fun FeaturePill(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = Color(0xFF071C33).copy(alpha = 0.72f),
        border = BorderStroke(1.dp, Color(0xFF0B60AE)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = Color(0xFF238CFF),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun WelcomePrimaryButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF168BFF))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "Scan Entrance Code",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun PackageErrorScreen(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070D18)),
        contentAlignment = Alignment.Center,
    ) {
        DotGridBackground()
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF151F31),
            border = BorderStroke(1.dp, Color(0xFF233149)),
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF3B2A08)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color(0xFFF59E0B),
                    )
                }
                Text(
                    "Unable to load navigation data",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    message,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF8A95A8),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF168BFF))
                        .clickable(role = Role.Button, onClick = onRetry),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Try Again",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
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
    var selectedFilter by remember { mutableStateOf(DestinationFilter.All) }
    val rooms = flowModel.availableRooms
    val orderedRooms = remember(rooms) { rooms.sortedBy { destinationSortIndex(it.id) } }
    val filteredRooms = orderedRooms.filter { room ->
        val matchesSearch = searchText.isBlank() ||
            room.displayName.contains(searchText, ignoreCase = true) ||
            room.description?.contains(searchText, ignoreCase = true) == true ||
            room.category?.contains(searchText, ignoreCase = true) == true
        matchesSearch && selectedFilter.matches(room)
    }
    val groupedFilteredRooms = remember(filteredRooms, searchText) {
        if (searchText.isBlank()) groupedRooms(filteredRooms) else emptyList()
    }
    val routeSummaries = remember(orderedRooms, session.reviewedConfig) {
        orderedRooms.associate { room -> room.id to flowModel.routeSummaryFor(room) }
    }
    val recentRooms = remember(rooms) {
        listOfNotNull(
            rooms.firstOrNull { it.id == "cs-lab" },
            rooms.firstOrNull { it.id == "fameo-cafe" },
        ).ifEmpty { rooms.take(2) }
    }
    val originName = session.confirmedEntrance.ifBlank { "Main Entrance" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070D18)),
    ) {
        DotGridBackground()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp),
            contentPadding = PaddingValues(bottom = 28.dp),
        ) {
            item {
                Spacer(Modifier.height(18.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(Color(0xFF121A28))
                            .border(1.dp, Color(0xFF24334A), RoundedCornerShape(13.dp))
                            .clickable(role = Role.Button, onClick = onCancel),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF168BFF),
                            modifier = Modifier.size(21.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Where to?",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 26.sp,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10D978)),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "From $originName",
                                color = Color(0xFF7F8A9D),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                SearchField(
                    value = searchText,
                    onValueChange = { searchText = it },
                )

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    DestinationFilter.entries.forEach { filter ->
                        DestinationFilterChip(
                            filter = filter,
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                        )
                    }
                }

                if (searchText.isBlank() && selectedFilter == DestinationFilter.All && recentRooms.isNotEmpty()) {
                    Spacer(Modifier.height(18.dp))
                    SectionLabel("RECENTLY VISITED")
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        recentRooms.forEach { room ->
                            RecentDestinationCard(
                                room = room,
                                routeSummary = routeSummaries[room.id],
                                modifier = Modifier.weight(1f),
                                onClick = { flowModel.selectDestination(room) },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel(if (searchText.isBlank()) "LOCATIONS" else "SEARCH RESULTS")
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${filteredRooms.size} places",
                        color = Color(0xFF5C6779),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            if (filteredRooms.isEmpty()) {
                item {
                    EmptyRoomsState(searchText)
                }
            } else if (groupedFilteredRooms.isNotEmpty()) {
                groupedFilteredRooms.forEach { (category, categoryRooms) ->
                    item(key = "header-$category") {
                        Spacer(Modifier.height(10.dp))
                        SectionLabel(displayNameForCategory(category).uppercase())
                        Spacer(Modifier.height(2.dp))
                    }
                    items(categoryRooms, key = { it.id }) { room ->
                        DestinationRow(
                            room = room,
                            routeSummary = routeSummaries[room.id],
                            onClick = { flowModel.selectDestination(room) },
                        )
                    }
                }
            } else {
                items(filteredRooms, key = { it.id }) { room ->
                    DestinationRow(
                        room = room,
                        routeSummary = routeSummaries[room.id],
                        onClick = { flowModel.selectDestination(room) },
                    )
                }
            }
        }
    }
}

private enum class DestinationFilter(val label: String) {
    All("All"),
    Rooms("Rooms"),
    Labs("Labs"),
    Food("Food"),
    Services("Services");

    fun matches(room: AndroidReviewedPackageLoader.PackageRoom): Boolean = when (this) {
        All -> true
        Rooms -> room.category == "classroom" || room.category == "office"
        Labs -> room.category == "lab"
        Food -> room.category == "cafe" || room.category == "kitchen"
        Services -> room.category == "toilet" || room.category == "vertical_transport"
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Search destinations" },
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .height(50.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF121A28))
                    .border(1.dp, Color(0xFF1E2B40), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF657185),
                    modifier = Modifier.size(21.dp),
                )
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = "Search rooms, labs, facilities...",
                            color = Color(0xFF667286),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    innerTextField()
                }
                if (value.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.Button) { onValueChange("") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = Color(0xFF657185),
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun DestinationFilterChip(
    filter: DestinationFilter,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) Color(0xFF168BFF) else Color(0xFF121A28)
    val textColor = if (selected) Color.White else Color(0xFF8994A6)
    Surface(
        modifier = Modifier
            .height(40.dp)
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(50),
        color = background,
        border = if (selected) null else BorderStroke(1.dp, Color(0xFF1E2B40)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = filter.label,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = Color(0xFF6D7788),
        fontSize = 13.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.sp,
    )
}

@Composable
private fun RecentDestinationCard(
    room: AndroidReviewedPackageLoader.PackageRoom,
    routeSummary: ArCameraFlowViewModel.RouteSummary?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val accent = destinationAccent(room)
    Surface(
        modifier = modifier
            .height(60.dp)
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = Color(0xFF151F31),
        border = BorderStroke(1.dp, Color(0xFF233149)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(accent.copy(alpha = 0.17f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = iconForCategory(room.category),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = room.prettyDestinationName(),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = routeSummary?.walkTimeText() ?: displayNameForCategory(room.category ?: "other"),
                    color = Color(0xFF6F7B8E),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DestinationRow(
    room: AndroidReviewedPackageLoader.PackageRoom,
    routeSummary: ArCameraFlowViewModel.RouteSummary?,
    onClick: () -> Unit,
) {
    val accent = destinationAccent(room)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = iconForCategory(room.category),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = room.prettyDestinationName(),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = room.locationSubtitle(),
                color = Color(0xFF6F7B8E),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF151D2B),
                border = BorderStroke(1.dp, Color(0xFF273247)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsWalk,
                        contentDescription = null,
                        tint = Color(0xFF8E99AA),
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = routeSummary?.walkTimeText() ?: "Route",
                        color = Color(0xFFB5BECC),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = routeSummary?.distanceText() ?: displayNameForCategory(room.category ?: "other"),
                color = Color(0xFF657185),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF354156),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun EmptyRoomsState(searchText: String) {
    val message = if (searchText.isBlank()) {
        "No destinations in this filter"
    } else {
        "No rooms match \"$searchText\""
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun RoutePreviewScreen(flowModel: ArCameraFlowViewModel) {
    val session by flowModel.session.collectAsState()
    val routePackage = session.routePackage
    val distance = routePackage?.totalDistance ?: 0.0
    val routeStepCount = routePackage?.routeNodeIds?.let { (it.size - 1).coerceAtLeast(1) } ?: 1
    val destinationName = session.selectedRoom?.prettyDestinationName().orEmpty()
    val originName = session.confirmedEntrance.ifBlank { "Main Entrance" }
    val distanceText = if (distance > 0.0) "${distance.formatMeters()} m" else "--"
    val walkingTimeText = if (distance > 0.0) formatWalkingTime(distance / 1.2) else "< 1 min"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070D18)),
    ) {
        DotGridBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color(0xFF121A28))
                        .border(1.dp, Color(0xFF24334A), RoundedCornerShape(13.dp))
                        .clickable(role = Role.Button, onClick = flowModel::goBackToDestinationSelect),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF168BFF),
                        modifier = Modifier.size(21.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Route to $destinationName",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 22.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Building A - Floor G",
                        color = Color(0xFF6F7B8E),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF071C33).copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, Color(0xFF0B60AE)),
                ) {
                    Text(
                        text = "Floor G",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        color = Color(0xFF168BFF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RouteSummaryCard(
                    originName = originName,
                    destinationName = destinationName,
                    distanceText = distanceText,
                    walkingTimeText = walkingTimeText,
                    stepCount = routeStepCount,
                )

                Spacer(Modifier.height(18.dp))

                RouteStepsCard(
                    originName = originName,
                    destinationName = destinationName,
                    distanceText = distanceText,
                )

                Spacer(Modifier.height(18.dp))

                RouteReadyCard()
                Spacer(Modifier.height(20.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF168BFF))
                    .clickable(role = Role.Button, onClick = flowModel::startNavigation),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Start AR Navigation",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            Spacer(
                modifier = Modifier
                    .height(28.dp)
                    .navigationBarsPadding(),
            )
        }
    }
}

@Composable
private fun RouteReadyCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF111B2B),
        border = BorderStroke(1.dp, Color(0xFF213047)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color(0xFF0A3A66)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = Color(0xFF168BFF),
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Ready for AR guidance",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Start at the entrance marker",
                    color = Color(0xFF7B8698),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun RouteSummaryCard(
    originName: String,
    destinationName: String,
    distanceText: String,
    walkingTimeText: String,
    stepCount: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF151F31),
        border = BorderStroke(1.dp, Color(0xFF233149)),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = walkingTimeText,
                        color = Color.White,
                        fontSize = 31.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 34.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$distanceText - $stepCount ${stepCount.stepLabel().lowercase()} - Ground Floor",
                        color = Color(0xFF6F7B8E),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = Color(0xFF0A3A66),
                    border = BorderStroke(1.dp, Color(0xFF0B60AE)),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            tint = Color(0xFF168BFF),
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Route",
                            color = Color(0xFF168BFF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF263247)),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "From",
                        color = Color(0xFF7D8899),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = originName,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF667286),
                    modifier = Modifier
                        .padding(horizontal = 14.dp)
                        .size(20.dp),
                )
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        text = "To",
                        color = Color(0xFF7D8899),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = destinationName,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteStepsCard(
    originName: String,
    destinationName: String,
    distanceText: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF151F31),
        border = BorderStroke(1.dp, Color(0xFF233149)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Text(
                text = "STEPS",
                color = Color(0xFF687486),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.sp,
            )
            Spacer(Modifier.height(14.dp))
            RouteStepRow(number = 1, title = "Start from $originName", distance = "Entrance")
            Spacer(Modifier.height(14.dp))
            RouteStepRow(
                number = 2,
                title = "Follow the highlighted route",
                distance = "$distanceText total",
            )
            Spacer(Modifier.height(14.dp))
            RouteStepRow(
                number = 3,
                title = "Arrive at $destinationName",
                distance = "Destination",
            )
        }
    }
}

@Composable
private fun RouteStepRow(number: Int, title: String, distance: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(27.dp)
                .clip(CircleShape)
                .background(Color(0xFF0A3A66))
                .border(1.dp, Color(0xFF0B60AE), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                color = Color(0xFF168BFF),
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 17.sp,
            )
            Text(
                text = distance,
                color = Color(0xFF6F7B8E),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun EntranceConfirmedSheet(entranceName: String, onContinue: () -> Unit) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070D18)),
        ) {
            DotGridBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.weight(1f))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF063D24))
                            .border(2.dp, Color(0xFF0E7E45), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0B5836).copy(alpha = 0.9f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF13D06D),
                                modifier = Modifier.size(42.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF073E27),
                        border = BorderStroke(1.dp, Color(0xFF0C7040)),
                    ) {
                        Text(
                            text = "Building A - Floor G",
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
                            color = Color(0xFF12CF69),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "Entrance confirmed",
                        color = Color.White,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 31.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = "$entranceName - Building A - Floor G",
                        color = Color(0xFF8D98AA),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.height(32.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF151F31),
                        border = BorderStroke(1.dp, Color(0xFF233149)),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 17.dp),
                        ) {
                            Text(
                                text = "STARTING POINT",
                                color = Color(0xFF687486),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.sp,
                            )
                            Spacer(Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(13.dp))
                                        .background(Color(0xFF0B5A3C).copy(alpha = 0.55f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF14D978),
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                                Spacer(Modifier.width(13.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = entranceName,
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "Ground Floor - Building A",
                                        color = Color(0xFF8A95A8),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFF0C6B42).copy(alpha = 0.82f),
                                ) {
                                    Text(
                                        text = "Confirmed",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        color = Color(0xFF28E07E),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF168BFF))
                        .clickable(role = Role.Button, onClick = onContinue),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Choose Destination",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }

                Spacer(Modifier.height(24.dp))
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
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size((20 * pressedScale).dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

private fun AndroidReviewedPackageLoader.PackageRoom.prettyDestinationName(): String =
    displayName.replace("EA 102", "EA102")

private fun AndroidReviewedPackageLoader.PackageRoom.locationSubtitle(): String = when (id) {
    "ea101" -> "Ground Floor - East Wing"
    "ea102" -> "Ground Floor - East Wing"
    "cs-lab" -> "Ground Floor - Computer Science"
    "me-lab" -> "First Floor - Northeast Wing"
    "fameo-cafe" -> "Ground Floor - East Corridor"
    "elevators" -> "Ground Floor - Main Core"
    "west-men-wc", "west-women-wc" -> "Ground Floor - West Wing"
    "east-men-wc", "east-women-wc" -> "Ground Floor - East Wing"
    else -> description?.takeIf { it.isNotBlank() } ?: displayNameForCategory(category ?: "other")
}

private fun destinationSortIndex(id: String): Int = when (id) {
    "ea101" -> 0
    "ea102" -> 1
    "cs-lab" -> 2
    "me-lab" -> 3
    "fameo-cafe" -> 4
    "elevators" -> 5
    "west-men-wc" -> 6
    "west-women-wc" -> 7
    "east-men-wc" -> 8
    "east-women-wc" -> 9
    else -> 100
}

private fun destinationAccent(room: AndroidReviewedPackageLoader.PackageRoom): Color = when (room.id) {
    "me-lab" -> Color(0xFF00A878)
    else -> categoryColor(room.category)
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

private fun ArCameraFlowViewModel.RouteSummary.walkTimeText(): String =
    formatWalkingTime(distanceMeters / 1.2)

private fun ArCameraFlowViewModel.RouteSummary.distanceText(): String =
    "${distanceMeters.formatMeters()} m"

private fun Int.stepLabel(): String = if (this == 1) "Step" else "Steps"

private fun Double.formatMeters(): String = "%.0f".format(this)
