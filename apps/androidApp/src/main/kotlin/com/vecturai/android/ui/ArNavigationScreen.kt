package com.vecturai.android.ui

import android.opengl.GLSurfaceView
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Straight
import androidx.compose.material.icons.filled.SubdirectoryArrowLeft
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.UTurnLeft
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vecturai.android.ar.AndroidArNavigationViewModel
import com.vecturai.android.ar.ArCoreCameraRenderer
import com.vecturai.android.ar.ArNavigationUiState
import com.vecturai.android.ar.NavigationActionIcon
import com.vecturai.android.ar.TrackingStatusIcon
import com.vecturai.android.data.AndroidReviewedPackageLoader
import com.vecturai.android.data.ArrowPlacementType
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun ArNavigationScreen(
    viewModel: AndroidArNavigationViewModel,
    routePackage: AndroidReviewedPackageLoader.LoadedPackage,
    entranceMarker: AndroidReviewedPackageLoader.PackageMarker?,
    destinationName: String,
    onEnd: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    var configured by remember { mutableStateOf(false) }
    var glSurfaceView by remember { mutableStateOf<GLSurfaceView?>(null) }

    LaunchedEffect(routePackage, entranceMarker, destinationName) {
        configured = false
        viewModel.configure(routePackage, entranceMarker)
        configured = true
    }

    DisposableEffect(lifecycleOwner, glSurfaceView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    glSurfaceView?.onResume()
                    viewModel.resumeSession()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.pauseSession()
                    glSurfaceView?.onPause()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            glSurfaceView?.onPause()
            viewModel.endNavigation()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (configured) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    GLSurfaceView(ctx).apply {
                        setEGLContextClientVersion(2)
                        preserveEGLContextOnPause = true
                        setRenderer(ArCoreCameraRenderer(ctx.applicationContext, viewModel))
                        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                        glSurfaceView = this
                    }
                },
            )
        }

        ProjectedArrowLayer(uiState)

        Column(Modifier.fillMaxSize()) {
            ArTopBar(uiState = uiState, onEnd = onEnd)
            Spacer(Modifier.weight(1f))
            when {
                uiState.markerAssetError != null -> ConfigErrorOverlay(uiState.markerAssetError.orEmpty(), onEnd)
                uiState.hasArrived -> ArrivalOverlay(uiState, onEnd)
                !uiState.isAligned -> AlignmentOverlay(
                    uiState = uiState,
                    onRetry = { viewModel.retryAlignment(context.applicationContext) },
                    onCancel = onEnd,
                    onSimulate = viewModel::simulateAlignment,
                )
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
private fun ProjectedArrowLayer(uiState: ArNavigationUiState) {
    val density = LocalDensity.current
    Box(Modifier.fillMaxSize()) {
        uiState.projectedArrows.forEach { arrow ->
            val sizeDp = when (arrow.type) {
                ArrowPlacementType.FOLLOW -> 34.dp
                ArrowPlacementType.TURN_LEFT, ArrowPlacementType.TURN_RIGHT -> 42.dp
                ArrowPlacementType.U_TURN, ArrowPlacementType.DESTINATION -> 46.dp
            }
            val sizePx = with(density) { sizeDp.toPx() * arrow.scale }
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (arrow.xPx - sizePx / 2f).roundToInt(),
                            (arrow.yPx - sizePx / 2f).roundToInt(),
                        )
                    }
                    .size(sizeDp)
                    .graphicsLayer {
                        alpha = arrow.alpha
                        scaleX = arrow.scale
                        scaleY = arrow.scale
                    }
                    .clip(CircleShape)
                    .background(colorForArrow(arrow.type).copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = iconForArrow(arrow.type),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(sizeDp * 0.58f),
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!uiState.isAligned) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.14f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (uiState.markerAssetError == null) Color(0xFFF59E0B) else Color(0xFFEF4444))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(uiState.sessionStateLabel, color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        if (uiState.isSimulated) {
            Spacer(Modifier.width(8.dp))
            Surface(shape = RoundedCornerShape(50), color = Color(0xFFF59E0B).copy(alpha = 0.2f)) {
                Text(
                    "DEMO",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = Color(0xFFF59E0B),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onEnd) {
            Icon(Icons.Default.Close, contentDescription = "End Route", tint = Color.White)
        }
    }
}

@Composable
private fun AlignmentOverlay(
    uiState: ArNavigationUiState,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onSimulate: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 32.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (uiState.alignmentTimedOut) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(28.dp))
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (uiState.alignmentTimedOut) uiState.timeoutReasonMessage else "Looking for entrance sign...",
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (uiState.alignmentTimedOut) uiState.timeoutHintMessage else "Point your camera at the entrance poster",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (uiState.alignmentTimedOut) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onRetry, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Retry")
                    }
                    TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                }
            }

            Button(onClick = onSimulate, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Simulate Scan")
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
        InstructionBanner(uiState, modifier = Modifier.padding(horizontal = 12.dp))
        Spacer(Modifier.weight(1f))
        BottomHud(uiState = uiState, onEnd = onEnd, onAdvance = onAdvance)
    }
}

@Composable
private fun InstructionBanner(uiState: ArNavigationUiState, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    actionIcon(uiState.nextActionIcon),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    uiState.nextActionText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val distance = uiState.nextActionDistance
                if (distance != null && distance < 30.0) {
                    Text(
                        "in ${distance.roundMeters()} m",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (uiState.isLowConfidence) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFF59E0B).copy(alpha = 0.14f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            trackingIcon(uiState.trackingStatusIcon),
                            contentDescription = uiState.trackingStatusLabel,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            uiState.trackingStatusLabel,
                            color = Color(0xFFF59E0B),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomHud(uiState: ArNavigationUiState, onEnd: () -> Unit, onAdvance: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 32.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 20.dp),
            ) {
                if (uiState.remainingDistance > 0.0) {
                    Text(formatEta(uiState.remainingDistance / 1.2), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "${uiState.remainingDistance.roundMeters()} m - ${uiState.destinationLabel}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(uiState.destinationLabel, fontWeight = FontWeight.SemiBold)
                }
            }

            if (uiState.isSimulated) {
                IconButton(onClick = onAdvance) {
                    Icon(Icons.Default.FastForward, contentDescription = "Advance", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Button(
                onClick = onEnd,
                modifier = Modifier.padding(end = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
            ) {
                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("End Route")
            }
        }
    }
}

@Composable
private fun ArrivalOverlay(uiState: ArNavigationUiState, onEnd: () -> Unit) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        appeared = true
    }
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.5f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "arrivalScale",
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 32.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                    tint = Color(0xFF10B981),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("You've Arrived", fontWeight = FontWeight.SemiBold)
                    Text(
                        uiState.destinationLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Button(onClick = onEnd, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Done")
            }
        }
    }
}

@Composable
private fun ConfigErrorOverlay(message: String, onEnd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(56.dp), tint = Color(0xFFEF4444))
        Spacer(Modifier.height(20.dp))
        Text("Setup needed", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(message, color = Color.White.copy(alpha = 0.8f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onEnd) {
            Text("Go Back")
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

private fun iconForArrow(type: ArrowPlacementType): ImageVector = when (type) {
    ArrowPlacementType.FOLLOW -> Icons.Default.Navigation
    ArrowPlacementType.TURN_LEFT -> Icons.Default.SubdirectoryArrowLeft
    ArrowPlacementType.TURN_RIGHT -> Icons.Default.SubdirectoryArrowRight
    ArrowPlacementType.U_TURN -> Icons.Default.UTurnLeft
    ArrowPlacementType.DESTINATION -> Icons.Default.Flag
}

private fun colorForArrow(type: ArrowPlacementType): Color = when (type) {
    ArrowPlacementType.FOLLOW -> Color(0xFF2563EB)
    ArrowPlacementType.TURN_LEFT, ArrowPlacementType.TURN_RIGHT -> Color(0xFFF59E0B)
    ArrowPlacementType.U_TURN -> Color(0xFFEA580C)
    ArrowPlacementType.DESTINATION -> Color(0xFF10B981)
}

private fun formatEta(seconds: Double): String {
    if (seconds < 60.0) return "< 1 min"
    return "~${ceil(seconds / 60.0).toInt()} min"
}

private fun Double.roundMeters(): String = "%.0f".format(this)
