package com.Vectura AI.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.Vectura AI.android.data.AndroidReviewedPackageLoader
import com.Vectura AI.android.ar.AndroidHapticManager
import com.Vectura AI.android.navigation.ArCameraFlowViewModel
import com.Vectura AI.android.navigation.AndroidNavigationFlowModel
import com.Vectura AI.designsystem.AnimatedGradientNumber
import com.Vectura AI.designsystem.AnimatedNumber
import com.Vectura AI.designsystem.AuroraBackground
import com.Vectura AI.designsystem.CategoryBadge
import com.Vectura AI.designsystem.GradientText
import com.Vectura AI.designsystem.IconChip
import com.Vectura AI.designsystem.SectionHeader
import com.Vectura AI.designsystem.Spacing
import com.Vectura AI.designsystem.StatPill
import com.Vectura AI.designsystem.Vectura AIBrush
import com.Vectura AI.designsystem.Vectura AICard
import com.Vectura AI.designsystem.Vectura AIColors
import com.Vectura AI.designsystem.Vectura AIFilterChip
import com.Vectura AI.designsystem.Vectura AIHapticsGate
import com.Vectura AI.designsystem.Vectura AIPrimaryButton
import com.Vectura AI.designsystem.Vectura AISecondaryButton
import com.Vectura AI.designsystem.Vectura AIShapes
import com.Vectura AI.designsystem.Vectura AITheme
import com.Vectura AI.designsystem.Vectura AITypography
import com.Vectura AI.designsystem.Vectura AITap
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.min

@Composable
fun AndroidNavigationApp(
    flowModel: AndroidNavigationFlowModel,
    onStartNavigation: () -> Unit,
) {
    val state by flowModel.state.collectAsState()

    Vectura AITheme {
        Vectura AIHapticsGate(enabled = AndroidHapticManager.HapticsEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                when (val current = state) {
                    AndroidNavigationFlowModel.HomeState.Home -> HomeScreen(onStartNavigation = onStartNavigation)
                    is AndroidNavigationFlowModel.HomeState.PackageError -> PackageErrorScreen(
                        message = current.message,
                        onRetry = flowModel::retryPackageLoad,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(onStartNavigation: () -> Unit) {
    var showAdminTools by remember { mutableStateOf(false) }
    var gearArmed by remember { mutableStateOf(false) }
    var activePill by remember { mutableIntStateOf(0) }
    val reduceMotion = rememberReduceMotion()
    val intensity = rememberAuroraIntensity()
    val gearRotation by animateFloatAsState(
        targetValue = if (gearArmed) 30f else 0f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "gearRotation",
    )

    LaunchedEffect(reduceMotion) {
        if (reduceMotion) return@LaunchedEffect
        while (true) {
            delay(2_000)
            activePill = (activePill + 1) % HomePillLabels.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Vectura AIColors.SurfaceCanvas),
    ) {
        AuroraBackground(intensity = intensity)

        IconChip(
            icon = Icons.Default.Settings,
            contentDescription = "Admin Tools",
            onClick = {
                gearArmed = true
                showAdminTools = true
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = Spacing.xs, end = Spacing.md)
                .graphicsLayer { rotationZ = gearRotation },
            tint = Vectura AIColors.TextMuted,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            BrandMark(
                pulsing = !reduceMotion,
                modifier = Modifier.size(88.dp),
            )

            Spacer(Modifier.height(Spacing.lg))

            GradientText(
                text = "Vectura AI",
                style = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "Find your way indoors",
                color = Vectura AIColors.TextMuted,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(Spacing.xxl))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HomePillLabels.forEachIndexed { index, label ->
                    FeaturePill(
                        text = label,
                        active = activePill == index,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Vectura AIPrimaryButton(
                text = "Scan Entrance Code",
                leadingIcon = Icons.Default.QrCodeScanner,
                onClick = onStartNavigation,
            )

            Spacer(Modifier.height(Spacing.md))

            Text(
                text = "Scan the entrance poster to begin",
                color = Vectura AIColors.TextDisabled,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.sm))
            DemoBuildingStatus()
            Spacer(Modifier.height(Spacing.xl))
        }
    }

    if (showAdminTools) {
        ModalBottomSheet(
            onDismissRequest = {
                showAdminTools = false
                gearArmed = false
            },
            containerColor = Vectura AIColors.SurfaceElevated,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BrandMark(pulsing = false, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.width(Spacing.sm))
                    Column(Modifier.weight(1f)) {
                        Text("Admin Tools", color = Vectura AIColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Draft jobs are not available on this device.",
                            color = Vectura AIColors.TextMuted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Vectura AICard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(Spacing.sm))
                        Text(
                            "Visitor navigation is ready. Admin review tools stay separate from the demo flow.",
                            color = Vectura AIColors.TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.md))
            }
        }
    }
}

@Composable
private fun PackageErrorScreen(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Vectura AIColors.SurfaceCanvas),
        contentAlignment = Alignment.Center,
    ) {
        AuroraBackground(intensity = rememberAuroraIntensity())
        Vectura AICard(
            modifier = Modifier.padding(Spacing.xl),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(Vectura AIShapes.Large)
                        .background(Vectura AIColors.AccentAmber.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Unable to load navigation data",
                        modifier = Modifier.size(32.dp),
                        tint = Vectura AIColors.AccentAmber,
                    )
                }
                Text(
                    "Unable to load navigation data",
                    color = Vectura AIColors.TextPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                Text(
                    message,
                    textAlign = TextAlign.Center,
                    color = Vectura AIColors.TextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Vectura AIPrimaryButton(text = "Try Again", onClick = onRetry)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DestinationSelectScreen(
    flowModel: ArCameraFlowViewModel,
    onCancel: () -> Unit,
) {
    val session by flowModel.session.collectAsState()
    var searchText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    val listState = rememberLazyListState()
    val collapse = (listState.firstVisibleItemScrollOffset / 160f).coerceIn(0f, 1f)
    val searchHeight by animateDpAsState(
        targetValue = if (collapse > 0.45f) 44.dp else 54.dp,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "searchCollapse",
    )
    val rooms = flowModel.availableRooms
    val orderedRooms = remember(rooms) { rooms.sortedBy { destinationSortIndex(it.id) } }
    
    // Filter out the "other" point (if selecting destination, hide origin; if selecting origin, hide destination)
    val filteredRooms = orderedRooms.filter { room ->
        val isOtherPoint = if (session.selectingOrigin) {
            room.id == session.selectedRoom?.id
        } else {
            // Hide the active origin: either the manually selected room or the scanned entrance node
            room.id == session.selectedOriginRoom?.id || 
            room.id == session.validatedEntranceMarker?.startNodeId ||
            room.category?.lowercase() == "entrance"
        }

        val matchesSearch = searchText.isBlank() ||
            room.displayName.contains(searchText, ignoreCase = true) ||
            room.description?.contains(searchText, ignoreCase = true) == true ||
            room.category?.contains(searchText, ignoreCase = true) == true
        
        val matchesCategory = selectedCategory == "All" || 
            displayNameForCategory(room.category ?: "").equals(selectedCategory, ignoreCase = true)
        
        !isOtherPoint && matchesSearch && matchesCategory
    }

    val dynamicCategories = remember(rooms) {
        listOf("All") + rooms.mapNotNull { it.category?.let { c -> displayNameForCategory(c) } }.distinct().sorted()
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
    val originName = session.selectedOriginRoom?.displayName ?: session.confirmedEntrance.ifBlank { "Main Entrance" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Vectura AIColors.SurfaceCanvas),
    ) {
        // AuroraBackground removed for better scrolling performance

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.xl),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.sm),
            ) {
                Spacer(Modifier.height(Spacing.md))
                FlowProgressStrip(activeStep = 1)
                Spacer(Modifier.height(Spacing.lg))
                DestinationHeader(
                    originName = originName,
                    selectingOrigin = session.selectingOrigin,
                    collapse = collapse,
                    onCancel = onCancel,
                    onChangeOrigin = { flowModel.toggleSelectingOrigin(true) }
                )
                Spacer(Modifier.height(Spacing.md))
                SearchField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    height = searchHeight,
                )
                Spacer(Modifier.height(Spacing.sm))
                DestinationFilterRow(
                    categories = dynamicCategories,
                    selectedCategory = selectedCategory,
                    onSelect = { selectedCategory = it },
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = Spacing.xxl),
            ) {
                item {
                if (rooms.isEmpty()) {
                    Spacer(Modifier.height(Spacing.xl))
                    SkeletonDestinationRows()
                }

                if (searchText.isBlank() && selectedCategory == "All" && recentRooms.isNotEmpty()) {
                    Spacer(Modifier.height(Spacing.xl))
                    SectionHeader("RECENTLY VISITED")
                    Spacer(Modifier.height(Spacing.sm))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        items(recentRooms, key = { it.id }) { room ->
                            RecentDestinationCard(
                                room = room,
                                routeSummary = routeSummaries[room.id],
                                onClick = { flowModel.selectDestination(room) },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.xl))
                SectionHeader(
                    title = if (searchText.isBlank()) {
                        if (session.selectingOrigin) "STARTING POINT" else "LOCATIONS"
                    } else "SEARCH RESULTS",
                    trailing = "${filteredRooms.size} places",
                )
                Spacer(Modifier.height(Spacing.xs))
            }

            if (filteredRooms.isEmpty() && rooms.isNotEmpty()) {
                item {
                    EmptyRoomsState(searchText, selectedCategory)
                }
            } else if (groupedFilteredRooms.isNotEmpty()) {
                groupedFilteredRooms.forEach { (category, categoryRooms) ->
                    item(key = "header-$category") {
                        Spacer(Modifier.height(Spacing.sm))
                        SectionHeader(displayNameForCategory(category).uppercase())
                        Spacer(Modifier.height(Spacing.xs))
                    }
                    items(categoryRooms, key = { it.id }) { room ->
                        DestinationRow(
                            room = room,
                            routeSummary = if (session.selectingOrigin) null else routeSummaries[room.id],
                            onClick = { 
                                if (session.selectingOrigin) flowModel.selectOrigin(room)
                                else flowModel.selectDestination(room)
                            },
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
}

@Composable
fun RoutePreviewScreen(flowModel: ArCameraFlowViewModel) {
    val session by flowModel.session.collectAsState()
    val routePackage = session.routePackage
    val distance = routePackage?.totalDistance ?: 0.0
    val routeStepCount = routePackage?.routeNodeIds?.let { (it.size - 1).coerceAtLeast(1) } ?: 1
    val destinationName = session.selectedRoom?.prettyDestinationName().orEmpty()
    val originName = session.selectedOriginRoom?.displayName ?: session.confirmedEntrance.ifBlank { "Main Entrance" }
    val distanceText = if (distance > 0.0) "${distance.formatMeters()} m" else "--"
    val walkingTimeText = if (distance > 0.0) formatWalkingTime(distance / 1.2) else "< 1 min"
    val walkingMinutes = if (distance > 0.0) ceil((distance / 1.2) / 60.0).toInt().coerceAtLeast(1) else 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Vectura AIColors.SurfaceCanvas),
    ) {
        AuroraBackground(intensity = rememberAuroraIntensity())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.xl),
        ) {
            Spacer(Modifier.height(Spacing.md))
            FlowProgressStrip(activeStep = 2)
            Spacer(Modifier.height(Spacing.lg))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconChip(
                    icon = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    onClick = flowModel::goBackToDestinationSelect,
                )
                Spacer(Modifier.width(Spacing.sm))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Route to $destinationName",
                        color = Vectura AIColors.TextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val buildingName = session.reviewedConfig?.manifest?.buildingName ?: "Building"
                    val floorName = session.selectedRoom?.floorName ?: "Floor"
                    Text(
                        text = "$buildingName - $floorName",
                        color = Vectura AIColors.TextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                StatPill(text = session.selectedRoom?.floorName ?: "Floor")
            }

            Spacer(Modifier.height(Spacing.xl))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RouteHeroCard(
                    session = session,
                    originName = originName,
                    destinationName = destinationName,
                    distanceText = distanceText,
                    walkingTimeText = walkingTimeText,
                    walkingMinutes = walkingMinutes,
                    stepCount = routeStepCount,
                    routePoints = routePackage?.routePoints.orEmpty(),
                )

                Spacer(Modifier.height(Spacing.md))

                RouteTimelineCard(
                    originName = originName,
                    destinationName = destinationName,
                    distanceText = distanceText,
                )

                Spacer(Modifier.height(Spacing.md))

                RouteReadyCard()
                Spacer(Modifier.height(Spacing.lg))
            }

            Vectura AIPrimaryButton(
                text = "Start AR Navigation",
                leadingIcon = Icons.Default.Place,
                onClick = flowModel::startNavigation,
            )

            Spacer(Modifier.height(Spacing.xl))
        }
    }
}

@Composable
fun EntranceConfirmedSheet(
    entranceName: String,
    buildingName: String,
    floorName: String,
    onContinue: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220, easing = FastOutSlowInEasing)),
        exit = fadeOut(tween(220, easing = FastOutSlowInEasing)),
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Vectura AIColors.SurfaceCanvas),
        ) {
            AuroraBackground(intensity = rememberAuroraIntensity())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(Spacing.md))
                FlowProgressStrip(activeStep = 0)
                Spacer(Modifier.weight(1f))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedCheckMark()

                    Spacer(Modifier.height(Spacing.lg))

                    StatPill(text = "$buildingName - $floorName", color = Vectura AIColors.AccentGreen)

                    Spacer(Modifier.height(Spacing.md))

                    Text(
                        text = "Entrance confirmed",
                        color = Vectura AIColors.TextPrimary,
                        style = MaterialTheme.typography.displayMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = "$entranceName - $buildingName - $floorName",
                        color = Vectura AIColors.TextMuted,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.height(Spacing.xxl))

                    Vectura AICard {
                        SectionHeader(title = "STARTING POINT")
                        Spacer(Modifier.height(Spacing.sm))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(Vectura AIShapes.Medium)
                                    .background(Vectura AIColors.AccentGreen.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Vectura AIColors.AccentGreen,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            Spacer(Modifier.width(Spacing.sm))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = entranceName,
                                    color = Vectura AIColors.TextPrimary,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "$floorName - $buildingName",
                                    color = Vectura AIColors.TextMuted,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                )
                            }
                            StatPill(text = "Confirmed", color = Vectura AIColors.AccentGreen)
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                Vectura AIPrimaryButton(text = "Choose Destination", onClick = onContinue)

                Spacer(Modifier.height(Spacing.xl))
            }
        }
    }
}

@Composable
private fun DestinationHeader(
    originName: String,
    selectingOrigin: Boolean,
    collapse: Float,
    onCancel: () -> Unit,
    onChangeOrigin: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconChip(
            icon = Icons.Default.ArrowBack,
            contentDescription = "Back",
            onClick = onCancel,
        )
        Spacer(Modifier.width(Spacing.sm))
        Column(Modifier.weight(1f)) {
            Text(
                text = "Where to?",
                color = Vectura AIColors.TextPrimary,
                style = MaterialTheme.typography.headlineMedium,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Vectura AIColors.AccentGreen)
                        .semantics { contentDescription = "Origin status indicator: connected" },
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    text = if (selectingOrigin) "Select starting point" else "From $originName",
                    color = if (selectingOrigin) Vectura AIColors.AccentCyan else Vectura AIColors.TextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (!selectingOrigin) {
                    Text(
                        text = "Change",
                        color = Vectura AIColors.AccentCyan,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onChangeOrigin() }
                            .padding(horizontal = Spacing.sm, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    height: androidx.compose.ui.unit.Dp,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Vectura AIColors.TextPrimary),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Search destinations" },
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .height(height)
                    .fillMaxWidth()
                    .clip(Vectura AIShapes.Medium)
                    .background(Vectura AIColors.SurfaceElevated.copy(alpha = 0.94f))
                    .border(BorderStroke(1.dp, Vectura AIColors.BorderSubtle), Vectura AIShapes.Medium)
                    .padding(horizontal = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Vectura AIColors.TextDisabled,
                    modifier = Modifier.size(21.dp),
                )
                Spacer(Modifier.width(Spacing.sm))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = "Search rooms, labs, facilities...",
                            color = Vectura AIColors.TextDisabled,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
                if (value.isNotEmpty()) {
                    IconChip(
                        icon = Icons.Default.Close,
                        contentDescription = "Clear search",
                        onClick = { onValueChange("") },
                        modifier = Modifier.size(48.dp),
                        tint = Vectura AIColors.TextMuted,
                    )
                }
            }
        },
    )
}

@Composable
private fun DestinationFilterRow(
    categories: List<String>,
    selectedCategory: String,
    onSelect: (String) -> Unit,
) {
    val density = LocalDensity.current
    val chipBounds = remember { mutableStateMapOf<String, Pair<Int, Int>>() }
    val target = chipBounds[selectedCategory]
    
    val animatedOffsetPx by animateIntAsState(
        targetValue = target?.first ?: 0,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "filterIndicatorOffset",
    )
    val animatedWidthPx by animateIntAsState(
        targetValue = target?.second ?: 0,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "filterIndicatorWidth",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            categories.forEach { category ->
                Vectura AIFilterChip(
                    text = category,
                    selected = selectedCategory == category,
                    onClick = { onSelect(category) },
                    modifier = Modifier.onGloballyPositioned { coords ->
                        val parent = coords.parentLayoutCoordinates ?: return@onGloballyPositioned
                        val origin = parent.localPositionOf(coords, Offset.Zero)
                        chipBounds[category] = origin.x.roundToInt() to coords.size.width
                    },
                )
            }
        }
        Spacer(Modifier.height(Spacing.xxs))
        if (target != null) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(animatedOffsetPx, 0) }
                    .width(with(density) { animatedWidthPx.toDp() })
                    .height(3.dp)
                    .clip(Vectura AIShapes.Pill)
                    .background(Vectura AIBrush.Primary),
            )
        } else {
            Spacer(Modifier.height(3.dp))
        }
    }
}

@Composable
private fun RecentDestinationCard(
    room: AndroidReviewedPackageLoader.PackageRoom,
    routeSummary: ArCameraFlowViewModel.RouteSummary?,
    onClick: () -> Unit,
) {
    val accent = destinationAccent(room)
    Surface(
        modifier = Modifier
            .width(190.dp)
            .height(72.dp)
            .clip(Vectura AIShapes.Medium)
            .Vectura AITap(onClick = onClick),
        shape = Vectura AIShapes.Medium,
        color = Vectura AIColors.SurfaceCard.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, Vectura AIColors.BorderSubtle),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryIcon(room.category, accent)
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(
                    text = room.prettyDestinationName(),
                    color = Vectura AIColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = routeSummary?.walkTimeText() ?: displayNameForCategory(room.category ?: "other"),
                    color = Vectura AIColors.TextMuted,
                    style = MaterialTheme.typography.bodySmall,
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
            .clip(Vectura AIShapes.Large)
            .background(Vectura AIColors.SurfaceCard.copy(alpha = 0.74f))
            .border(BorderStroke(1.dp, Vectura AIColors.BorderSubtle.copy(alpha = 0.58f)), Vectura AIShapes.Large)
            .Vectura AITap(onClick = onClick)
            .padding(end = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(72.dp)
                .clip(Vectura AIShapes.Pill)
                .background(accent),
        )
        Spacer(Modifier.width(Spacing.sm))
        CategoryIcon(room.category, accent)
        Spacer(Modifier.width(Spacing.sm))
        Column(Modifier.weight(1f)) {
            Text(
                text = room.prettyDestinationName(),
                color = Vectura AIColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = room.locationSubtitle(),
                color = Vectura AIColors.TextMuted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            WalkTimePill(routeSummary)
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                text = routeSummary?.distanceText() ?: displayNameForCategory(room.category ?: "other"),
                color = Vectura AIColors.TextDisabled,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(Spacing.xs))
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = Vectura AIColors.TextDisabled,
            modifier = Modifier.size(20.dp),
        )
    }
    Spacer(Modifier.height(Spacing.xs))
}

@Composable
private fun WalkTimePill(routeSummary: ArCameraFlowViewModel.RouteSummary?) {
    Surface(
        shape = Vectura AIShapes.Pill,
        color = Vectura AIColors.SurfaceOverlay,
        border = BorderStroke(1.dp, Vectura AIColors.BorderStrong),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsWalk,
                contentDescription = null,
                tint = Vectura AIColors.TextMuted,
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(Spacing.xxs))
            if (routeSummary != null) {
                AnimatedNumber(
                    value = routeSummary.walkMinutes(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Vectura AIColors.TextSecondary,
                )
                Text(" min", color = Vectura AIColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
            } else {
                Text("Route", color = Vectura AIColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun EmptyRoomsState(searchText: String, selectedCategory: String) {
    val message = if (searchText.isBlank()) {
        if (selectedCategory == "All") "No destinations found" else "No $selectedCategory found here"
    } else {
        "No results for \"$searchText\""
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(196.dp)
            .padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BrandMark(
            pulsing = false,
            muted = true,
            modifier = Modifier.size(52.dp),
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = Vectura AIColors.TextMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SkeletonDestinationRows() {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val shimmer by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Restart),
        label = "skeletonShimmer",
    )
    val brush = androidx.compose.ui.graphics.Brush.linearGradient(
        listOf(
            Vectura AIColors.SurfaceElevated.copy(alpha = 0.45f),
            Vectura AIColors.SurfaceOverlay.copy(alpha = 0.95f),
            Vectura AIColors.SurfaceElevated.copy(alpha = 0.45f),
        ),
        start = Offset(shimmer * -500f, 0f),
        end = Offset(shimmer * 500f, 0f),
    )
    repeat(6) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(Vectura AIShapes.Large)
                .background(brush),
        )
        Spacer(Modifier.height(Spacing.xs))
    }
}

@Composable
private fun RouteReadyCard() {
    Vectura AICard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(Vectura AIShapes.Medium)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Ready for AR guidance",
                    color = Vectura AIColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun RouteHeroCard(
    session: ArCameraFlowViewModel.SessionData,
    originName: String,
    destinationName: String,
    distanceText: String,
    walkingTimeText: String,
    walkingMinutes: Int,
    stepCount: Int,
    routePoints: List<Pair<Double, Double>>,
) {
    Vectura AICard {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedGradientNumber(
                value = walkingMinutes,
                suffix = if (walkingTimeText.contains("min")) " min" else "",
                style = Vectura AITypography.numericDisplay(),
                textAlign = TextAlign.Center,
            )
            val floorName = session.selectedRoom?.floorName ?: "Floor"
            Text(
                text = "$distanceText - $stepCount ${stepCount.stepLabel().lowercase()} - $floorName",
                color = Vectura AIColors.TextMuted,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.lg))

            RoutePlanView(
                session = session,
                routePoints = routePoints,
            )

            Spacer(Modifier.height(Spacing.lg))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("From", color = Vectura AIColors.TextMuted, style = Vectura AITypography.overline())
                    Text(
                        text = originName,
                        color = Vectura AIColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Vectura AIColors.TextDisabled,
                    modifier = Modifier
                        .padding(horizontal = Spacing.sm)
                        .size(20.dp),
                )
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("To", color = Vectura AIColors.TextMuted, style = Vectura AITypography.overline())
                    Text(
                        text = destinationName,
                        color = Vectura AIColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun RoutePlanView(
    session: ArCameraFlowViewModel.SessionData,
    routePoints: List<Pair<Double, Double>>,
) {
    val config = session.reviewedConfig ?: return
    val currentFloorId = session.selectedRoom?.floorId
    
    // Filter nodes and edges for the current floor to draw the "map"
    val floorNodes = config.nodes.filter { it.floorId == currentFloorId || currentFloorId == null }
    val floorEdges = config.edges.filter { edge ->
        val fromNode = config.nodes.find { it.id == edge.from }
        fromNode?.floorId == currentFloorId || currentFloorId == null
    }

    if (floorNodes.isEmpty()) {
        RouteMiniStrip(routePoints) // Fallback if no floor data
        return
    }

    // Bounds calculation for auto-scaling - EXTREME ZOOM to route
    val padding = 0.5 // Minimal padding for maximum zoom
    val (minX, maxX, minZ, maxZ) = if (routePoints.isNotEmpty()) {
        val rMinX = routePoints.minOf { it.first }
        val rMaxX = routePoints.maxOf { it.first }
        val rMinZ = routePoints.minOf { it.second }
        val rMaxZ = routePoints.maxOf { it.second }
        
        // Expansion factor: very low to keep it tightly centered
        val dx = (rMaxX - rMinX).coerceAtLeast(2.0)
        val dz = (rMaxZ - rMinZ).coerceAtLeast(2.0)
        
        listOf(rMinX - dx * 0.05, rMaxX + dx * 0.05, rMinZ - dz * 0.05, rMaxZ + dz * 0.05)
    } else {
        listOf(floorNodes.minOf { it.x }, floorNodes.maxOf { it.x }, floorNodes.minOf { it.z }, floorNodes.maxOf { it.z })
    }
    
    val mapWidth = (maxX - minX).coerceAtLeast(1.0)
    val mapHeight = (maxZ - minZ).coerceAtLeast(1.0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(Vectura AIShapes.Large)
            .background(Vectura AIColors.SurfaceElevated)
            .border(1.dp, Vectura AIColors.BorderSubtle, Vectura AIShapes.Large),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            val canvasW = size.width
            val canvasH = size.height
            
            // Tighter scale calculation
            val scale = kotlin.math.min(canvasW / (mapWidth + padding), canvasH / (mapHeight + padding)).toFloat()
            
            val offsetX = (canvasW - mapWidth.toFloat() * scale) / 2f - minX.toFloat() * scale
            val offsetZ = (canvasH - mapHeight.toFloat() * scale) / 2f - minZ.toFloat() * scale

            fun project(x: Double, z: Double): Offset {
                return Offset(
                    x.toFloat() * scale + offsetX,
                    z.toFloat() * scale + offsetZ
                )
            }

            // 1. Draw "Corridors" / Walls (Edges)
            floorEdges.forEach { edge ->
                val from = config.nodes.find { it.id == edge.from }
                val to = config.nodes.find { it.id == edge.to }
                if (from != null && to != null) {
                    drawLine(
                        color = Vectura AIColors.BorderStrong.copy(alpha = 0.35f),
                        start = project(from.x, from.z),
                        end = project(to.x, to.z),
                        strokeWidth = 12.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // 2. Draw "Room Spaces" and Objects
            floorNodes.forEach { node ->
                val pos = project(node.x, node.z)
                when (node.type) {
                    "room" -> {
                        drawCircle(
                            color = Vectura AIColors.SurfaceElevated,
                            radius = 10.dp.toPx(),
                            center = pos
                        )
                        drawCircle(
                            color = Vectura AIColors.BorderSubtle,
                            radius = 10.dp.toPx(),
                            center = pos,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                    "waypoint" -> {
                        drawCircle(
                            color = Vectura AIColors.TextMuted.copy(alpha = 0.2f),
                            radius = 2.dp.toPx(),
                            center = pos
                        )
                    }
                }
            }

            // 3. Draw the Active Route
            if (routePoints.size > 1) {
                val routePath = androidx.compose.ui.graphics.Path().apply {
                    val start = project(routePoints[0].first, routePoints[0].second)
                    moveTo(start.x, start.y)
                    for (i in 1 until routePoints.size) { project(routePoints[i].first, routePoints[i].second).let { lineTo(it.x, it.y) } }
                }
                
                // Route Glow
                drawPath(
                    path = routePath,
                    color = Vectura AIColors.GradientMid.copy(alpha = 0.3f),
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                
                // Route Main Line
                drawPath(
                    path = routePath,
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        listOf(Vectura AIColors.GradientStart, Vectura AIColors.GradientEnd)
                    ),
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // 4. Start and End Markers
            if (routePoints.isNotEmpty()) {
                val startPos = project(routePoints.first().first, routePoints.first().second)
                val endPos = project(routePoints.last().first, routePoints.last().second)
                
                // Origin
                drawCircle(Vectura AIColors.AccentGreen, radius = 6.dp.toPx(), center = startPos)
                drawCircle(Color.White, radius = 2.dp.toPx(), center = startPos)
                
                // Destination
                drawCircle(Vectura AIColors.AccentAmber, radius = 7.dp.toPx(), center = endPos)
                drawCircle(Color.White, radius = 3.dp.toPx(), center = endPos)
            }
        }
        
        // Map Overlay Labels
        Box(Modifier.fillMaxSize().padding(Spacing.sm)) {
            Icon(
                Icons.Default.Map,
                contentDescription = null,
                tint = Vectura AIColors.TextMuted.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp).align(Alignment.TopEnd)
            )
            Text(
                text = session.selectedRoom?.floorName ?: "Floor Plan",
                style = MaterialTheme.typography.labelSmall,
                color = Vectura AIColors.TextMuted,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}

@Composable
private fun RouteMiniStrip(routePoints: List<Pair<Double, Double>>) {
    val reduceMotion = rememberReduceMotion()
    val transition = rememberInfiniteTransition(label = "routeStrip")
    val markerProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduceMotion) 0f else 1f,
        animationSpec = infiniteRepeatable(tween(1_800, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "routeStripMarker",
    )
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(Vectura AIShapes.Medium)
            .background(Vectura AIColors.SurfaceElevated),
    ) {
        val left = 24.dp.toPx()
        val right = size.width - 24.dp.toPx()
        val midY = size.height / 2f
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(left, midY)
            cubicTo(size.width * 0.32f, 10.dp.toPx(), size.width * 0.62f, size.height - 10.dp.toPx(), right, midY)
        }
        drawPath(
            path = path,
            color = Vectura AIColors.BorderStrong,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
        )
        drawPath(
            path = path,
            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                listOf(
                    Vectura AIColors.GradientStart,
                    Vectura AIColors.GradientMid,
                    Vectura AIColors.GradientEnd,
                ),
            ),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
        )
        drawCircle(Vectura AIColors.AccentGreen, radius = 6.dp.toPx(), center = Offset(left, midY))
        drawCircle(Vectura AIColors.AccentAmber, radius = 6.dp.toPx(), center = Offset(right, midY))
        val markerX = left + (right - left) * markerProgress
        val markerY = midY + kotlin.math.sin(markerProgress * Math.PI).toFloat() * 16.dp.toPx()
        drawCircle(Color.White, radius = 4.dp.toPx(), center = Offset(markerX, markerY))
        if (routePoints.size > 1) {
            drawCircle(
                Vectura AIColors.AccentCyan.copy(alpha = 0.45f),
                radius = 2.dp.toPx(),
                center = Offset(size.width / 2f, midY),
            )
        }
    }
}

@Composable
private fun RouteTimelineCard(
    originName: String,
    destinationName: String,
    distanceText: String,
) {
    Vectura AICard {
        SectionHeader(title = "STEPS")
        Spacer(Modifier.height(Spacing.sm))
        TimelineStep(number = 1, title = "Start from $originName", detail = "Starting point", first = true)
        TimelineStep(number = 2, title = "Follow the highlighted route", detail = "$distanceText total")
        TimelineStep(number = 3, title = "Arrive at $destinationName", detail = "Destination", last = true)
    }
}

@Composable
private fun TimelineStep(
    number: Int,
    title: String,
    detail: String,
    first: Boolean = false,
    last: Boolean = false,
) {
    val reduceMotion = rememberReduceMotion()
    var appeared by remember { mutableStateOf(reduceMotion) }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            appeared = true
        } else {
            delay(number * 80L)
            appeared = true
        }
    }
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.5f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "timelineDot",
    )
    Row(verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (!first) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(12.dp)
                        .background(Vectura AIColors.BorderStrong),
                )
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.48f)), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedNumber(
                    value = number,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (!last) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(28.dp)
                        .background(Vectura AIColors.BorderStrong),
                )
            }
        }
        Spacer(Modifier.width(Spacing.sm))
        Column(Modifier.padding(top = if (first) 2.dp else Spacing.sm)) {
            Text(
                text = title,
                color = Vectura AIColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = detail,
                color = Vectura AIColors.TextMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun CategoryIcon(category: String?, accent: Color) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(Vectura AIShapes.Medium)
            .background(accent.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = iconForCategory(category),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun BrandMark(
    pulsing: Boolean,
    modifier: Modifier = Modifier,
    muted: Boolean = false,
) {
    val transition = rememberInfiniteTransition(label = "brandMark")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (pulsing) 1.04f else 1f,
        animationSpec = infiniteRepeatable(tween(2_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "brandPulse",
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pulse
                scaleY = pulse
                alpha = if (muted) 0.45f else 1f
            }
            .clip(Vectura AIShapes.XLarge)
            .background(if (muted) Vectura AIColors.SurfaceElevated else Vectura AIColors.SurfaceOverlay)
            .border(BorderStroke(1.dp, if (muted) Vectura AIColors.BorderStrong else MaterialTheme.colorScheme.primary), Vectura AIShapes.XLarge),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Vectura AIBrandIcon,
            contentDescription = null,
            tint = if (muted) Vectura AIColors.TextMuted else Color.White,
            modifier = Modifier.size(48.dp),
        )
    }
}

@Composable
private fun FeaturePill(text: String, active: Boolean, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(
        targetValue = if (active) 1.04f else 1f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "featurePill",
    )
    Surface(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        shape = Vectura AIShapes.Pill,
        color = if (active) Vectura AIColors.AccentCyan.copy(alpha = 0.16f) else Vectura AIColors.SurfaceElevated.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, if (active) Vectura AIColors.AccentCyan.copy(alpha = 0.58f) else Vectura AIColors.BorderSubtle),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            color = if (active) Vectura AIColors.AccentCyan else Vectura AIColors.TextMuted,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun DemoBuildingStatus() {
    Surface(
        shape = Vectura AIShapes.Pill,
        color = Vectura AIColors.SurfaceElevated.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, Vectura AIColors.BorderSubtle),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Vectura AIColors.AccentGreen),
            )
            Spacer(Modifier.width(Spacing.xs))
            Text(
                text = "Dynamic building data active",
                color = Vectura AIColors.TextMuted,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun FlowProgressStrip(activeStep: Int) {
    val steps = listOf("Entrance", "Destination", "Navigate")
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        steps.forEachIndexed { index, label ->
            val active = index <= activeStep
            Surface(
                shape = Vectura AIShapes.Pill,
                color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Vectura AIColors.SurfaceElevated.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.48f) else Vectura AIColors.BorderSubtle),
            ) {
                Text(
                    text = if (index < activeStep) "$label done" else label,
                    modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
                    color = if (active) Vectura AIColors.TextPrimary else Vectura AIColors.TextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
            if (index != steps.lastIndex) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(if (index < activeStep) MaterialTheme.colorScheme.primary else Vectura AIColors.BorderSubtle),
                )
            }
        }
    }
}

@Composable
fun AnimatedCheckMark(modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 104.dp) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "checkPath",
    )
    val ring by animateFloatAsState(
        targetValue = if (started) 1f else 0.45f,
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label = "checkRing",
    )
    Canvas(modifier = modifier.size(size)) {
        val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
        drawCircle(Vectura AIColors.AccentGreen.copy(alpha = 0.16f * (1f - ring * 0.4f)), radius = size.toPx() * 0.5f * ring, center = center)
        drawCircle(Vectura AIColors.AccentGreen.copy(alpha = 0.28f), radius = size.toPx() * 0.36f, center = center)
        drawCircle(Vectura AIColors.AccentGreen.copy(alpha = 0.88f), radius = size.toPx() * 0.25f, center = center)
        val start = Offset(size.toPx() * 0.36f, size.toPx() * 0.52f)
        val mid = Offset(size.toPx() * 0.47f, size.toPx() * 0.63f)
        val end = Offset(size.toPx() * 0.68f, size.toPx() * 0.39f)
        val firstProgress = min(progress / 0.5f, 1f)
        val secondProgress = max((progress - 0.5f) / 0.5f, 0f)
        drawLine(
            color = Color.White,
            start = start,
            end = Offset(
                x = start.x + (mid.x - start.x) * firstProgress,
                y = start.y + (mid.y - start.y) * firstProgress,
            ),
            strokeWidth = 5.dp.toPx(),
            cap = StrokeCap.Round,
        )
        if (progress > 0.5f) {
            drawLine(
                color = Color.White,
                start = mid,
                end = Offset(
                    x = mid.x + (end.x - mid.x) * secondProgress,
                    y = mid.y + (end.y - mid.y) * secondProgress,
                ),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

private val HomePillLabels = listOf("Live AR", "Smart Routes", "Indoor Maps")

private val Vectura AIBrandIcon: ImageVector = ImageVector.Builder(
    name = "Vectura AIBrandMark",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(4f, 12.7f)
        lineTo(13.7f, 3f)
        curveTo(14.25f, 2.45f, 15.2f, 2.84f, 15.2f, 3.62f)
        lineTo(15.2f, 8.3f)
        lineTo(20.3f, 8.3f)
        curveTo(21.05f, 8.3f, 21.46f, 9.17f, 20.98f, 9.74f)
        lineTo(11.3f, 21.05f)
        curveTo(10.8f, 21.64f, 9.82f, 21.28f, 9.82f, 20.5f)
        lineTo(9.82f, 15.45f)
        lineTo(4.66f, 15.45f)
        curveTo(3.9f, 15.45f, 3.47f, 13.24f, 4f, 12.7f)
        close()
    }
    path(fill = SolidColor(Color.White)) {
        moveTo(17.2f, 17.1f)
        curveTo(17.2f, 15.55f, 18.45f, 14.3f, 20f, 14.3f)
        curveTo(21.55f, 14.3f, 22.8f, 15.55f, 22.8f, 17.1f)
        curveTo(22.8f, 18.65f, 21.55f, 19.9f, 20f, 19.9f)
        curveTo(18.45f, 19.9f, 17.2f, 18.65f, 17.2f, 17.1f)
        close()
    }
}.build()

private fun AndroidReviewedPackageLoader.PackageRoom.prettyDestinationName(): String =
    displayName.replace("EA 102", "EA102")

private fun AndroidReviewedPackageLoader.PackageRoom.locationSubtitle(): String {
    val floor = floorName ?: "Unknown Floor"
    return description?.takeIf { it.isNotBlank() } ?: "$floor - ${displayNameForCategory(category ?: "other")}"
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
    // Group by Floor Name/ID ALWAYS to satisfy the floor-based categorization requirement
    return rooms.sortedWith(compareBy({ it.floorId }, { it.category }, { it.displayName }))
        .groupBy { it.floorName ?: it.floorId?.let { id -> "Floor $id" } ?: "Other" }
        .flatMap { (floorName, floorRooms) ->
            floorRooms.groupBy { it.category ?: "other" }
                .map { (cat, catRooms) -> 
                    val catDisplay = displayNameForCategory(cat)
                    "$floorName • $catDisplay" to catRooms 
                }
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

private fun displayNameForCategory(category: String): String = when (category.lowercase()) {
    "room" -> "Rooms"
    "classroom" -> "Classrooms"
    "lab" -> "Laboratories"
    "cafe" -> "Cafe"
    "vertical_transport", "elevator", "stairs" -> "Elevators/Stairs"
    "toilet", "restroom" -> "Restrooms"
    "office" -> "Offices"
    "entrance" -> "Entrances"
    else -> category.replaceFirstChar { it.uppercase() }
}

private fun formatWalkingTime(seconds: Double): String {
    if (seconds < 60.0) return "< 1 min"
    return "~${ceil(seconds / 60.0).toInt()} min"
}

private fun ArCameraFlowViewModel.RouteSummary.walkTimeText(): String =
    formatWalkingTime(distanceMeters / 1.2)

private fun ArCameraFlowViewModel.RouteSummary.walkMinutes(): Int =
    ceil((distanceMeters / 1.2) / 60.0).toInt().coerceAtLeast(1)

private fun ArCameraFlowViewModel.RouteSummary.distanceText(): String =
    "${distanceMeters.formatMeters()} m"

private fun Int.stepLabel(): String = if (this == 1) "Step" else "Steps"

private fun Double.formatMeters(): String = "%.0f".format(this)
