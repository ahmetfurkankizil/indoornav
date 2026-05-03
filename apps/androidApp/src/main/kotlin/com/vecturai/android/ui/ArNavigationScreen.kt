package com.vecturai.android.ui

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Straight
import androidx.compose.material.icons.filled.SubdirectoryArrowLeft
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.UTurnLeft
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vecturai.android.ar.AndroidArNavigationViewModel
import com.vecturai.android.ar.ArNavigationUiState
import com.vecturai.android.ar.NavigationActionIcon
import com.vecturai.android.ar.TrackingStatusIcon
import com.vecturai.designsystem.AnimatedNumber
import com.vecturai.designsystem.GradientText
import com.vecturai.designsystem.IconChip
import com.vecturai.designsystem.Spacing
import com.vecturai.designsystem.StatPill
import com.vecturai.designsystem.VecturaiBrush
import com.vecturai.designsystem.VecturaiCard
import com.vecturai.designsystem.VecturaiColors
import com.vecturai.designsystem.VecturaiPrimaryButton
import com.vecturai.designsystem.VecturaiSecondaryButton
import com.vecturai.designsystem.VecturaiShapes
import com.vecturai.designsystem.VecturaiTypography
import com.vecturai.designsystem.vecturaiTap
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArNavigationScreen(
    viewModel: AndroidArNavigationViewModel,
    isEmulator: Boolean,
    onEnd: () -> Unit,
    onRetryActivity: () -> Unit,
    onNavigateElsewhere: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var pendingLegPreview by remember { mutableStateOf<com.vecturai.android.data.AndroidReviewedPackageLoader.RouteLeg?>(null) }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = when {
                uiState.hasArrived -> "arrival"
                uiState.sessionErrorMessage != null -> "session"
                !uiState.isAligned -> "aligning"
                else -> "active"
            },
            transitionSpec = {
                slideInVertically(
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                ) { it / 12 } + fadeIn(tween(220, easing = FastOutSlowInEasing)) togetherWith
                    slideOutVertically(
                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                    ) { -it / 12 } + fadeOut(tween(220, easing = FastOutSlowInEasing))
            },
            label = "arOverlayPhase",
        ) { phase ->
            when (phase) {
                "arrival" -> ArrivalOverlay(uiState, onEnd, onNavigateElsewhere)
                "session" -> SessionErrorOverlay(
                    message = uiState.sessionErrorMessage.orEmpty(),
                    isArCoreInstall = uiState.sessionErrorIsArCoreInstall,
                    onRetry = onRetryActivity,
                    onEnd = onEnd,
                )
                "aligning" -> Column(Modifier.fillMaxSize()) {
                    ArTopBar(uiState = uiState, onEnd = onEnd)
                    Spacer(Modifier.weight(1f))
                    AlignmentOverlay(
                        uiState = uiState,
                        onRetry = onRetryActivity,
                        onCancel = onEnd,
                        onSimulate = viewModel::requestSimulatedAlignment,
                        allowSimulation = isEmulator,
                    )
                }
                else -> ActiveNavigationOverlay(
                    uiState = uiState,
                    onEnd = onEnd,
                    onAdvance = viewModel::advanceProgress,
                )
            }
        }
    }
}

@Composable
private fun ArTopBar(uiState: ArNavigationUiState, onEnd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!uiState.isAligned) {
            StatPill(
                text = uiState.sessionStateLabel,
                color = VecturaiColors.AccentAmber,
            )
        }
        if (uiState.isSimulated) {
            Spacer(Modifier.width(Spacing.xs))
            StatPill(text = "DEMO", color = VecturaiColors.AccentAmber)
        }
        Spacer(Modifier.weight(1f))
        IconChip(
            icon = Icons.Default.Close,
            contentDescription = "End route",
            onClick = onEnd,
            tint = VecturaiColors.TextSecondary,
            modifier = Modifier.clip(CircleShape),
        )
    }
}

@Composable
private fun AlignmentOverlay(
    uiState: ArNavigationUiState,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onSimulate: () -> Unit,
    allowSimulation: Boolean,
) {
    VecturaiCard(
        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxl),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (uiState.alignmentTimedOut) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Alignment timed out",
                        tint = VecturaiColors.AccentAmber,
                        modifier = Modifier.size(32.dp),
                    )
                } else {
                    RadarSweep(modifier = Modifier.size(48.dp))
                }
                Spacer(Modifier.width(Spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (uiState.alignmentTimedOut) uiState.timeoutReasonMessage else "Looking for entrance sign...",
                        color = VecturaiColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (uiState.alignmentTimedOut) uiState.timeoutHintMessage else "Point your camera at the entrance poster",
                        color = VecturaiColors.TextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            AlignmentMiniIllustration()

            if (uiState.alignmentTimedOut) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    VecturaiPrimaryButton(
                        text = "Retry",
                        onClick = onRetry,
                        leadingIcon = Icons.Default.Refresh,
                        modifier = Modifier.weight(1f),
                    )
                    VecturaiSecondaryButton(
                        text = "Cancel",
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (allowSimulation) {
                VecturaiPrimaryButton(
                    text = "Simulate Scan",
                    onClick = onSimulate,
                    leadingIcon = Icons.Default.Navigation,
                )
            }
        }
    }
}

@Composable
private fun ActiveNavigationOverlay(
    uiState: ArNavigationUiState,
    onEnd: () -> Unit,
    onAdvance: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        InstructionBanner(
            uiState,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = Spacing.sm, top = Spacing.xs, end = Spacing.sm),
        )
        CompassStrip(
            bearingDegrees = 0f, // Removed relativeBearingDegrees
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
        )
        
        // DEVIATION WARNING OVERLAY
        androidx.compose.animation.AnimatedVisibility(
            visible = uiState.isOffPath || uiState.isWrongWay,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF7F1D1D).copy(alpha = 0.94f), // Dark red
                border = BorderStroke(1.5.dp, Color(0xFFEF4444))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (uiState.isWrongWay) "WRONG DIRECTION" else "OFF ROUTE",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (uiState.isWrongWay) "Please turn around and follow the arrows." else "Please return to the highlighted path.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        
        Spacer(Modifier.weight(1f))
        BottomHud(uiState = uiState, onEnd = onEnd, onAdvance = onAdvance)
    }
}

@Composable
private fun ArMinimapOverlay(
    uiState: ArNavigationUiState,
    config: com.vecturai.android.data.AndroidReviewedPackageLoader.ReviewedConfig,
    routePoints: List<Pair<Double, Double>>
) {
    Surface(
        modifier = Modifier.size(110.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.85f),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
    ) {
        if (config.nodes.isEmpty()) return@Surface

        // Calculate map bounds
        var minX = Double.MAX_VALUE
        var minZ = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var maxZ = -Double.MAX_VALUE

        for (node in config.nodes) {
            minX = minOf(minX, node.x)
            minZ = minOf(minZ, node.z)
            maxX = maxOf(maxX, node.x)
            maxZ = maxOf(maxZ, node.z)
        }

        // Add padding
        val pad = 2.0
        minX -= pad
        minZ -= pad
        maxX += pad
        maxZ += pad

        val width = maxX - minX
        val depth = maxZ - minZ

        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            val canvasW = size.width
            val canvasH = size.height
            val centerX = canvasW / 2f
            val centerY = canvasH / 2f
            val scale = 15f 

            val userHeadingDeg = Math.toDegrees(uiState.userHeadingRad).toFloat()

            fun bToCanvas(bx: Double, bz: Double): Offset {
                val dx = (bx - uiState.userStableBuildingX).toFloat()
                val dz = (bz - uiState.userStableBuildingZ).toFloat()
                return Offset(centerX + dx * scale, centerY + dz * scale)
            }

            withTransform({
                // Flip by 180 degrees to match AR arrow orientation
                rotate(degrees = -userHeadingDeg + 180f, pivot = Offset(centerX, centerY))
            }) {
                val nodeMap = config.nodes.associateBy { it.id }
                for (edge in config.edges) {
                    val from = nodeMap[edge.from]
                    val to = nodeMap[edge.to]
                    if (from != null && to != null) {
                        drawLine(
                            color = Color(0xFF334155),
                            start = bToCanvas(from.x, from.z),
                            end = bToCanvas(to.x, to.z),
                            strokeWidth = 2.5f
                        )
                    }
                }

                if (routePoints.size > 1) {
                    for (i in 0 until routePoints.size - 1) {
                        val a = routePoints[i]
                        val b = routePoints[i + 1]
                        drawLine(
                            color = Color(0xFF3B82F6),
                            start = bToCanvas(a.first, a.second),
                            end = bToCanvas(b.first, b.second),
                            strokeWidth = 6f
                        )
                    }
                }
            }

            drawCircle(
                color = Color(0xFF3B82F6).copy(alpha = 0.3f),
                radius = 10f,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = Color.White,
                radius = 4f,
                center = Offset(centerX, centerY)
            )
            val cursorPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(centerX, centerY - 8f)
                lineTo(centerX - 5f, centerY - 2f)
                lineTo(centerX + 5f, centerY - 2f)
                close()
            }
            drawPath(cursorPath, Color(0xFF60A5FA))
        }
    }
}

@Composable
private fun InstructionBanner(uiState: ArNavigationUiState, modifier: Modifier = Modifier) {
    val urgent = (uiState.nextActionDistance ?: Double.MAX_VALUE) < 5.0
    val height by animateFloatAsState(
        targetValue = if (urgent) 88f else 76f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "instructionHeight",
    )
    VecturaiCard(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp),
        glass = true,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TurnGlyph(
                icon = uiState.nextActionIcon,
                contentDescription = uiState.nextActionText,
                urgent = urgent,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    uiState.nextActionText,
                    color = VecturaiColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val distance = uiState.nextActionDistance
                if (distance != null && distance < 30.0) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("in ", color = VecturaiColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
                        GradientText("${distance.roundMeters()} m", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            TrackingBadge(uiState)
        }
    }
}

@Composable
private fun TrackingBadge(uiState: ArNavigationUiState) {
    val targetColor = when {
        !uiState.isLowConfidence -> VecturaiColors.AccentGreen
        uiState.trackingStatusIcon == TrackingStatusIcon.HoldSteady -> VecturaiColors.AccentAmber
        else -> VecturaiColors.AccentRed
    }
    val color by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "trackingBadgeColor",
    )
    StatPill(
        text = uiState.trackingStatusLabel,
        color = color,
    )
}

@Composable
private fun BottomHud(uiState: ArNavigationUiState, onEnd: () -> Unit, onAdvance: () -> Unit) {
    VecturaiCard(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Spacing.sm, vertical = Spacing.xl),
        glass = true,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressEtaCluster(uiState)

            if (uiState.isSimulated) {
                IconButton(onClick = onAdvance) {
                    Icon(Icons.Default.FastForward, contentDescription = "Advance", tint = MaterialTheme.colorScheme.primary)
                }
            }

            SwipeToEndRoute(
                onEnd = onEnd,
                modifier = Modifier.width(148.dp),
            )
        }
    }
}

@Composable
private fun RowScope.ProgressEtaCluster(uiState: ArNavigationUiState) {
    val progress = if (uiState.totalDistance > 0.0) {
        (1.0 - uiState.remainingDistance / uiState.totalDistance).coerceIn(0.0, 1.0).toFloat()
    } else {
        0f
    }
    var displayedProgress by remember { mutableFloatStateOf(progress) }
    var lastProgressUpdateMs by remember { mutableStateOf(0L) }
    LaunchedEffect(progress) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastProgressUpdateMs >= 200L || progress == 0f || progress == 1f) {
            displayedProgress = progress
            lastProgressUpdateMs = now
        }
    }
    val arcProgress by animateFloatAsState(
        targetValue = displayedProgress,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "progressArc",
    )
    Box(
        modifier = Modifier
            .size(78.dp)
            .padding(end = Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawArc(
                color = VecturaiColors.BorderStrong,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
            )
            drawArc(
                color = VecturaiColors.AccentCyan,
                startAngle = -90f,
                sweepAngle = 360f * arcProgress,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (uiState.remainingDistance > 0.0) {
                Text(
                    formatEta(uiState.remainingDistance / 1.2),
                    color = VecturaiColors.TextPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
                Text(
                    "${uiState.remainingDistance.roundMeters()} m",
                    color = VecturaiColors.TextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            } else {
                Text(uiState.destinationLabel, color = VecturaiColors.TextPrimary, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
    Column(Modifier.weight(1f)) {
        Text(
            uiState.destinationLabel,
            color = VecturaiColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "Follow the path",
            color = VecturaiColors.TextMuted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun SwipeToEndRoute(onEnd: () -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .height(48.dp)
            .clip(VecturaiShapes.Medium)
            .background(VecturaiColors.AccentRed.copy(alpha = 0.14f))
            .border(BorderStroke(1.dp, VecturaiColors.AccentRed.copy(alpha = 0.34f)), VecturaiShapes.Medium),
    ) {
        val density = LocalDensity.current
        val maxPx = with(density) { maxWidth.toPx() }
        var dragPx by remember { mutableFloatStateOf(0f) }
        val state = rememberDraggableState { delta ->
            dragPx = (dragPx + delta).coerceIn(0f, maxPx)
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .draggable(
                    state = state,
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        if (dragPx >= maxPx * 0.6f) {
                            onEnd()
                        }
                        dragPx = 0f
                    },
                )
                .padding(horizontal = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .graphicsLayer { translationX = dragPx.coerceAtMost(maxPx - 40f) }
                    .clip(CircleShape)
                    .background(VecturaiColors.AccentRed),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(Spacing.xs))
            Text("Swipe to end", color = VecturaiColors.TextSecondary, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

@Composable
private fun ArrivalOverlay(
    uiState: ArNavigationUiState,
    onEnd: () -> Unit,
    onNavigateElsewhere: () -> Unit,
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.5f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "arrivalScale",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VecturaiColors.SurfaceCanvas),
    ) {
        com.vecturai.designsystem.AuroraBackground(intensity = rememberAuroraIntensity())
        ConfettiBurst()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = Spacing.xl, top = Spacing.xl, end = Spacing.xl, bottom = 140.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            ) {
                AnimatedCheckMark(size = 124.dp)
            }

            Spacer(Modifier.height(Spacing.lg))

            Text(
                text = "ARRIVAL CONFIRMED",
                color = VecturaiColors.AccentGreen,
                style = VecturaiTypography.overline(),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "You've arrived",
                color = VecturaiColors.TextPrimary,
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = uiState.destinationLabel,
                color = VecturaiColors.TextSecondary,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(Spacing.xl))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                ArrivalStatCard(
                    value = if (uiState.totalDistance > 0.0) uiState.totalDistance.roundMeters().toIntOrNull() ?: 0 else 0,
                    suffix = " m",
                    label = "Distance",
                    modifier = Modifier.weight(1f),
                )
                ArrivalStatCard(
                    value = if (uiState.totalDistance > 0.0) ceil((uiState.totalDistance / 1.2) / 60.0).toInt().coerceAtLeast(1) else 0,
                    suffix = " min",
                    label = "Time",
                    modifier = Modifier.weight(1f),
                )
                ArrivalStatCard(
                    value = uiState.routeStepCount.coerceAtLeast(1),
                    suffix = "",
                    label = uiState.routeStepCount.coerceAtLeast(1).stepLabel(),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(Spacing.sm))

            ArrivalDestinationCard(
                destination = uiState.destinationLabel,
                location = uiState.arrivalLocationLabel.ifBlank { "Route complete" },
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = Spacing.xl, end = Spacing.xl, bottom = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            VecturaiPrimaryButton(
                text = "Done",
                leadingIcon = Icons.Default.Done,
                onClick = onEnd,
            )
            Spacer(Modifier.height(Spacing.sm))
            VecturaiSecondaryButton(
                text = "Navigate somewhere else",
                leadingIcon = Icons.Default.PlayArrow,
                onClick = onNavigateElsewhere,
            )
        }

        IconChip(
            icon = Icons.Default.Close,
            contentDescription = "Close",
            onClick = onEnd,
            tint = VecturaiColors.TextSecondary,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = Spacing.sm, end = Spacing.md)
                .clip(CircleShape),
        )
    }
}

@Composable
private fun ArrivalStatCard(
    value: Int,
    suffix: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(72.dp),
        shape = VecturaiShapes.Medium,
        color = VecturaiColors.SurfaceCard,
        border = BorderStroke(1.dp, VecturaiColors.BorderSubtle),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                AnimatedNumber(
                    value = value,
                    style = MaterialTheme.typography.titleLarge,
                    color = VecturaiColors.TextPrimary,
                )
                Text(suffix, color = VecturaiColors.TextPrimary, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                text = label,
                color = VecturaiColors.TextMuted,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ArrivalDestinationCard(destination: String, location: String) {
    VecturaiCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(VecturaiShapes.Medium)
                    .background(VecturaiColors.AccentAmber.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (destination.contains("Cafe", ignoreCase = true)) Icons.Default.Restaurant else Icons.Default.Flag,
                    contentDescription = null,
                    tint = VecturaiColors.AccentAmber,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(
                    text = destination,
                    color = VecturaiColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = location,
                    color = VecturaiColors.TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatPill(text = "Arrived", color = VecturaiColors.AccentGreen)
        }
    }
}

@Composable
private fun ConfigErrorOverlay(message: String, onEnd: () -> Unit) {
    ErrorScaffold {
        VecturaiCard {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Icon(Icons.Default.Warning, contentDescription = "Setup needed", modifier = Modifier.size(56.dp), tint = VecturaiColors.AccentRed)
                Text("Setup needed", color = VecturaiColors.TextPrimary, style = MaterialTheme.typography.headlineMedium)
                Text(
                    message,
                    color = VecturaiColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
                VecturaiPrimaryButton(text = "Go Back", onClick = onEnd)
            }
        }
    }
}

@Composable
private fun SessionErrorOverlay(
    message: String,
    isArCoreInstall: Boolean,
    onRetry: () -> Unit,
    onEnd: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    ErrorScaffold {
        VecturaiCard {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Icon(Icons.Default.Warning, contentDescription = "Camera starting", modifier = Modifier.size(52.dp), tint = VecturaiColors.AccentAmber)
                Text("Camera Starting", color = VecturaiColors.TextPrimary, style = MaterialTheme.typography.headlineMedium)
                Text(
                    message,
                    color = VecturaiColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
                VecturaiPrimaryButton(
                    text = if (isArCoreInstall) "Install / Update ARCore" else "Try Again",
                    onClick = {
                        if (isArCoreInstall) {
                            uriHandler.openUri("market://details?id=com.google.ar.core")
                        } else {
                            onRetry()
                        }
                    },
                )
                VecturaiSecondaryButton(text = "Go Back", onClick = onEnd)
            }
        }
    }
}

@Composable
private fun ErrorScaffold(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VecturaiColors.SurfaceCanvas.copy(alpha = 0.94f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        content()
    }
}

@Composable
private fun TurnGlyph(
    icon: NavigationActionIcon,
    contentDescription: String,
    urgent: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = if (urgent) VecturaiColors.AccentAmber else MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .clip(VecturaiShapes.Medium)
            .background(color.copy(alpha = 0.16f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.36f)), VecturaiShapes.Medium),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            actionIcon(icon),
            contentDescription = contentDescription,
            modifier = Modifier.size(34.dp),
            tint = color,
        )
    }
}

@Composable
private fun CompassStrip(bearingDegrees: Float, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp),
        shape = VecturaiShapes.Pill,
        color = VecturaiColors.SurfaceCard.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, VecturaiColors.BorderSubtle.copy(alpha = 0.62f)),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val centerY = size.height / 2f
            val offset = (bearingDegrees / 180f) * size.width * 0.35f
            for (i in -6..6) {
                val x = size.width / 2f + i * 28.dp.toPx() - offset
                drawLine(
                    color = if (i == 0) VecturaiColors.AccentCyan else VecturaiColors.TextDisabled,
                    start = Offset(x, centerY - 5.dp.toPx()),
                    end = Offset(x, centerY + 5.dp.toPx()),
                    strokeWidth = if (i == 0) 2.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            drawCircle(VecturaiColors.AccentCyan, radius = 3.dp.toPx(), center = Offset(size.width / 2f, centerY))
        }
    }
}

@Composable
private fun RadarSweep(modifier: Modifier = Modifier) {
    val reduceMotion = rememberReduceMotion()
    val transition = rememberInfiniteTransition(label = "radar")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduceMotion) 0f else 360f,
        animationSpec = infiniteRepeatable(tween(2_000, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "radarRotation",
    )
    Canvas(modifier) {
        drawCircle(VecturaiColors.AccentCyan.copy(alpha = 0.12f), radius = size.minDimension / 2f)
        drawCircle(
            VecturaiColors.AccentCyan.copy(alpha = 0.42f),
            radius = size.minDimension / 2.2f,
            style = Stroke(width = 2.dp.toPx()),
        )
        drawArc(
            color = VecturaiColors.AccentCyan,
            startAngle = rotation,
            sweepAngle = 72f,
            useCenter = false,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun AlignmentMiniIllustration() {
    val reduceMotion = rememberReduceMotion()
    val transition = rememberInfiniteTransition(label = "alignmentIllustration")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduceMotion) 0f else 1f,
        animationSpec = infiniteRepeatable(tween(2_400, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "alignmentPhase",
    )
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(VecturaiShapes.Medium)
            .background(VecturaiColors.SurfaceElevated.copy(alpha = 0.72f)),
    ) {
        val phoneX = size.width * 0.2f + sin(phase * Math.PI * 2).toFloat() * 14.dp.toPx()
        val posterX = size.width * 0.7f
        drawRoundRect(
            color = VecturaiColors.BorderStrong,
            topLeft = Offset(posterX, 16.dp.toPx()),
            size = Size(54.dp.toPx(), 44.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
        )
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(
                    VecturaiColors.GradientStart,
                    VecturaiColors.GradientMid,
                    VecturaiColors.GradientEnd,
                ),
            ),
            topLeft = Offset(posterX + 8.dp.toPx(), 24.dp.toPx()),
            size = Size(38.dp.toPx(), 28.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
        )
        drawRoundRect(
            color = VecturaiColors.SurfaceOverlay,
            topLeft = Offset(phoneX, 22.dp.toPx()),
            size = Size(34.dp.toPx(), 46.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
        )
        drawCircle(
            VecturaiColors.AccentCyan.copy(alpha = 0.28f),
            radius = 28.dp.toPx(),
            center = Offset(phoneX + 17.dp.toPx(), 45.dp.toPx()),
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

@Composable
private fun ConfettiBurst() {
    if (rememberReduceMotion()) return
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val particles = remember {
        mutableStateListOf<Offset>().apply {
            repeat(30) { index ->
                add(Offset((index * 37 % 100) / 100f, (index * 19 % 100) / 100f))
            }
        }
    }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(1_500, easing = FastOutSlowInEasing),
        label = "confettiProgress",
    )
    Canvas(Modifier.fillMaxSize()) {
        particles.forEachIndexed { index, seed ->
            val angle = index * 0.61f
            val radius = progress * size.minDimension * 0.42f
            val center = Offset(size.width / 2f, size.height * 0.34f)
            drawCircle(
                color = if (index % 3 == 0) VecturaiColors.AccentAmber.copy(alpha = 1f - progress) else VecturaiColors.AccentCyan.copy(alpha = 1f - progress),
                radius = 2.dp.toPx(),
                center = Offset(
                    center.x + cos(angle) * radius + seed.x * 8.dp.toPx(),
                    center.y + sin(angle) * radius + seed.y * 8.dp.toPx(),
                ),
            )
        }
    }
}

private fun actionIcon(icon: NavigationActionIcon): ImageVector = when (icon) {
    NavigationActionIcon.Straight -> Icons.Default.Straight
    NavigationActionIcon.TurnLeft -> Icons.Default.SubdirectoryArrowLeft
    NavigationActionIcon.TurnRight -> Icons.Default.SubdirectoryArrowRight
    NavigationActionIcon.UTurn -> Icons.Default.UTurnLeft
    NavigationActionIcon.Destination -> Icons.Default.Flag
}

private fun trackingIcon(icon: TrackingStatusIcon): ImageVector = when (icon) {
    TrackingStatusIcon.Location -> Icons.Default.MyLocation
    TrackingStatusIcon.HoldSteady -> Icons.Default.PanTool
    TrackingStatusIcon.Eye -> Icons.Default.VisibilityOff
    TrackingStatusIcon.Recenter -> Icons.Default.Sync
    TrackingStatusIcon.Lost -> Icons.Default.LocationOn
}

private fun formatEta(seconds: Double): String {
    if (seconds < 60.0) return "< 1 min"
    return "~${ceil(seconds / 60.0).toInt()} min"
}

private fun Double.roundMeters(): String = "%.0f".format(this)

private fun Int.stepLabel(): String = if (this == 1) "Step" else "Steps"

@Composable
fun FloorTransitionOverlay(
    transition: com.vecturai.android.data.AndroidReviewedPackageLoader.FloorTransition,
    onConfirm: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070D18))
    ) {
        // ArrivalDotBackground() // Removed unresolved reference
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = if (transition.isUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = null,
                tint = Color(0xFF6B8AFF),
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFF6B8AFF).copy(alpha = 0.1f), CircleShape)
                    .padding(20.dp),
            )
            Spacer(Modifier.height(32.dp))
            Text(
                text = "Floor Transition",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(16.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF151F31),
                border = BorderStroke(1.dp, Color(0xFF233149)),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val directionText = if (transition.isUp) "UP" else "DOWN"
                    val floorDiffText = if (transition.floorDiff > 1) "${transition.floorDiff} floors" else "1 floor"
                    
                    Text(
                        text = "Please go $directionText $floorDiffText to",
                        color = Color(0xFFB6BFCE),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = transition.toFloorName,
                        color = Color(0xFFF4A515),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
            
            Spacer(Modifier.height(48.dp))
            
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F63DF)),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("I am on ${transition.toFloorName}", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
