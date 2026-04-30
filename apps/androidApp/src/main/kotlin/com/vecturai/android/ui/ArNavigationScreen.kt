package com.vecturai.android.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vecturai.android.ar.AndroidArNavigationViewModel
import com.vecturai.android.ar.ArNavigationUiState
import com.vecturai.android.ar.NavigationActionIcon
import com.vecturai.android.ar.TrackingStatusIcon
import kotlin.math.ceil

@Composable
fun ArNavigationScreen(
    viewModel: AndroidArNavigationViewModel,
    isEmulator: Boolean,
    onEnd: () -> Unit,
    onRetryActivity: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(Modifier.fillMaxSize()) {
        if (uiState.hasArrived) {
            ArrivalOverlay(uiState, onEnd)
        } else {
            when {
                uiState.markerAssetError != null ->
                    ConfigErrorOverlay(uiState.markerAssetError.orEmpty(), onEnd)
                uiState.sessionErrorMessage != null ->
                    SessionErrorOverlay(
                        message = uiState.sessionErrorMessage.orEmpty(),
                        isArCoreInstall = uiState.sessionErrorIsArCoreInstall,
                        onRetry = onRetryActivity,
                        onEnd = onEnd,
                    )
                !uiState.isAligned -> Column(Modifier.fillMaxSize()) {
                    ArTopBar(uiState = uiState, onEnd = onEnd)
                    Spacer(Modifier.weight(1f))
                    AlignmentOverlay(
                        uiState = uiState,
                        onRetry = onRetryActivity,
                        onCancel = onEnd,
                        onSimulate = viewModel::simulateAlignment,
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!uiState.isAligned) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF121A28).copy(alpha = 0.92f),
                border = BorderStroke(1.dp, Color(0xFF253149)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (uiState.markerAssetError == null) Color(0xFFF59E0B) else Color(0xFFEF4444))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        uiState.sessionStateLabel,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
        if (uiState.isSimulated) {
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF3B2A08),
                border = BorderStroke(1.dp, Color(0xFF7A4B04)),
            ) {
                Text(
                    "DEMO",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    color = Color(0xFFF59E0B),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF121A28).copy(alpha = 0.92f))
                .border(1.dp, Color(0xFF253149), CircleShape)
                .clickable(role = Role.Button, onClick = onEnd),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Close, contentDescription = "End Route", tint = Color(0xFFB6BFCE), modifier = Modifier.size(19.dp))
        }
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 32.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF151F31).copy(alpha = 0.96f),
        border = BorderStroke(1.dp, Color(0xFF233149)),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (uiState.alignmentTimedOut) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(28.dp))
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp, color = Color(0xFF168BFF))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (uiState.alignmentTimedOut) uiState.timeoutReasonMessage else "Looking for entrance sign...",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (uiState.alignmentTimedOut) uiState.timeoutHintMessage else "Point your camera at the entrance poster",
                        color = Color(0xFF8A95A8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (uiState.alignmentTimedOut) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF168BFF)),
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Retry", fontWeight = FontWeight.ExtraBold)
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable(role = Role.Button, onClick = onCancel),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF121A28),
                        border = BorderStroke(1.dp, Color(0xFF2B3952)),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Cancel", color = Color(0xFFB6BFCE), fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            if (allowSimulation) {
                Button(
                    onClick = onSimulate,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF168BFF)),
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Simulate Scan", fontWeight = FontWeight.ExtraBold)
                }
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
                .padding(start = 12.dp, top = 8.dp, end = 12.dp),
        )
        Spacer(Modifier.weight(1f))
        BottomHud(uiState = uiState, onEnd = onEnd, onAdvance = onAdvance)
    }
}

@Composable
private fun InstructionBanner(uiState: ArNavigationUiState, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF151F31).copy(alpha = 0.96f),
        border = BorderStroke(1.dp, Color(0xFF233149)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0A3A66)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    actionIcon(uiState.nextActionIcon),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = Color(0xFF168BFF),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    uiState.nextActionText,
                    color = Color.White,
                    fontSize = 17.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val distance = uiState.nextActionDistance
                if (distance != null && distance < 30.0) {
                    Text(
                        "in ${distance.roundMeters()} m",
                        color = Color(0xFF8A95A8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (uiState.isLowConfidence) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF3B2A08),
                    border = BorderStroke(1.dp, Color(0xFF7A4B04)),
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
                            fontSize = 10.sp,
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
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 24.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF151F31).copy(alpha = 0.96f),
        border = BorderStroke(1.dp, Color(0xFF233149)),
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
                    Text(
                        formatEta(uiState.remainingDistance / 1.2),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        "${uiState.remainingDistance.roundMeters()} m - ${uiState.destinationLabel}",
                        color = Color(0xFF8A95A8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(uiState.destinationLabel, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }

            if (uiState.isSimulated) {
                IconButton(onClick = onAdvance) {
                    Icon(Icons.Default.FastForward, contentDescription = "Advance", tint = Color(0xFF168BFF))
                }
            }

            Box(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFEF4444))
                    .clickable(role = Role.Button, onClick = onEnd),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Stop,
                    contentDescription = "End Route",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020711)),
    ) {
        ArrivalDotBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = 22.dp, top = 24.dp, end = 22.dp, bottom = 128.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(124.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(124.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF06351F).copy(alpha = 0.55f))
                        .border(1.dp, Color(0xFF0D5E37).copy(alpha = 0.7f), CircleShape),
                )
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF073D25).copy(alpha = 0.85f))
                        .border(1.5.dp, Color(0xFF0B7C45), CircleShape),
                )
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0A5A37))
                        .border(2.dp, Color(0xFF19C56D), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF24E37A),
                        modifier = Modifier.size(32.dp),
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            Text(
                text = "ARRIVAL CONFIRMED",
                color = Color(0xFF12C86A),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.6.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "You've arrived",
                color = Color.White,
                fontSize = 30.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = uiState.destinationLabel,
                color = Color(0xFFB6BFCE),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ArrivalStatCard(
                    value = if (uiState.totalDistance > 0.0) "${uiState.totalDistance.roundMeters()} m" else "--",
                    label = "Distance",
                    modifier = Modifier.weight(1f),
                )
                ArrivalStatCard(
                    value = if (uiState.totalDistance > 0.0) formatEta(uiState.totalDistance / 1.2) else "--",
                    label = "Time",
                    modifier = Modifier.weight(1f),
                )
                ArrivalStatCard(
                    value = uiState.routeStepCount.coerceAtLeast(1).toString(),
                    label = uiState.routeStepCount.coerceAtLeast(1).stepLabel(),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(12.dp))

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
                .padding(start = 22.dp, end = 22.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                onClick = onEnd,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF168BFF),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = "Done",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Return home to start a new route",
                color = Color(0xFF566173),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 10.dp, end = 16.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF121722))
                .border(1.dp, Color(0xFF202735), CircleShape)
                .clickable(role = Role.Button, onClick = onEnd),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color(0xFFB6BFCE),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ArrivalDotBackground() {
    Canvas(Modifier.fillMaxSize()) {
        val step = 18.dp.toPx()
        val dotRadius = 1.05.dp.toPx()
        var y = 0f
        while (y <= size.height) {
            var x = 0f
            while (x <= size.width) {
                drawCircle(
                    color = Color(0xFF102033).copy(alpha = 0.38f),
                    radius = dotRadius,
                    center = Offset(x, y),
                )
                x += step
            }
            y += step
        }
    }
}

@Composable
private fun ArrivalStatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF151F31),
        border = BorderStroke(1.dp, Color(0xFF233149)),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = value,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = label,
                color = Color(0xFF6F7B8E),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ArrivalDestinationCard(destination: String, location: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF151F31),
        border = BorderStroke(1.dp, Color(0xFF233149)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color(0xFF3B2A08)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (destination.contains("Cafe", ignoreCase = true)) {
                        Icons.Default.Restaurant
                    } else {
                        Icons.Default.Flag
                    },
                    contentDescription = null,
                    tint = Color(0xFFF4A515),
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = destination,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = location,
                    color = Color(0xFF7B8698),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF0A5A37),
            ) {
                Text(
                    text = "Arrived",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    color = Color(0xFF24E37A),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

@Composable
private fun ConfigErrorOverlay(message: String, onEnd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070D18))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF151F31),
            border = BorderStroke(1.dp, Color(0xFF233149)),
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = Color(0xFFEF4444),
                )
                Text(
                    "Setup needed",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    message,
                    color = Color(0xFFB6BFCE),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                )
                Button(
                    onClick = onEnd,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF168BFF)),
                ) {
                    Text("Go Back", fontWeight = FontWeight.ExtraBold)
                }
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070D18).copy(alpha = 0.94f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF151F31),
            border = BorderStroke(1.dp, Color(0xFF233149)),
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(52.dp), tint = Color(0xFFF59E0B))
                Text("Camera Starting", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    message,
                    color = Color(0xFFB6BFCE),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                )
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF168BFF)),
                ) {
                    Text(if (isArCoreInstall) "Install / Update ARCore" else "Try Again", fontWeight = FontWeight.ExtraBold)
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable(role = Role.Button, onClick = onEnd),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF121A28),
                    border = BorderStroke(1.dp, Color(0xFF2B3952)),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Go Back", color = Color(0xFFB6BFCE), fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
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
