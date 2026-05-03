package com.Vectura AI.android.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.Vectura AI.android.navigation.ArCameraFlowViewModel
import com.Vectura AI.designsystem.IconChip
import com.Vectura AI.designsystem.Spacing
import com.Vectura AI.designsystem.Vectura AIBrush
import com.Vectura AI.designsystem.Vectura AICard
import com.Vectura AI.designsystem.Vectura AIColors
import com.Vectura AI.designsystem.Vectura AIPrimaryButton
import com.Vectura AI.designsystem.Vectura AISecondaryButton
import com.Vectura AI.designsystem.Vectura AIShapes

@Composable
fun QRScanScreen(
    flowModel: ArCameraFlowViewModel,
    onCancel: () -> Unit,
    onSimulateScan: (() -> Unit)? = null,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Transparent),
    ) {
        ScanVignette()
        QRScanChrome(
            flowModel = flowModel,
            onRetry = flowModel::clearQRError,
            onCancel = onCancel,
            onSimulateScan = onSimulateScan,
        )
    }
}

@Composable
private fun QRScanChrome(
    flowModel: ArCameraFlowViewModel,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onSimulateScan: (() -> Unit)?,
) {
    val error by flowModel.qrError.collectAsState()
    val detected by flowModel.qrDetected.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = Spacing.xl, vertical = Spacing.md),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconChip(
                icon = Icons.Default.ArrowBack,
                contentDescription = "Back",
                onClick = onCancel,
            )
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(
                    "Scanning...",
                    color = Vectura AIColors.TextPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    "Find the entrance poster",
                    color = Vectura AIColors.TextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        AnimatedScanReticle(
            hasError = error != null,
            hasDetected = detected,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(252.dp),
        )

        Spacer(Modifier.weight(1f))

        AnimatedContent(
            targetState = QRPanelState(error = error, detected = detected),
            transitionSpec = {
                fadeIn(tween(180, easing = FastOutSlowInEasing)) togetherWith
                    fadeOut(tween(120, easing = FastOutSlowInEasing))
            },
            label = "qrStatus",
        ) { state ->
            QRStatusPanel(
                error = state.error,
                detected = state.detected,
                onRetry = onRetry,
                onSimulateScan = onSimulateScan,
            )
        }
    }
}

private data class QRPanelState(
    val error: String?,
    val detected: Boolean,
)

@Composable
private fun QRStatusPanel(
    error: String?,
    detected: Boolean,
    onRetry: () -> Unit,
    onSimulateScan: (() -> Unit)?,
) {
    Vectura AICard(glass = true) {
        if (detected) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(Vectura AIShapes.Medium)
                        .background(Vectura AIColors.AccentGreen.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = "Entrance code found",
                        tint = Vectura AIColors.AccentGreen,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(Spacing.sm))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Entrance code found",
                        color = Vectura AIColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "Confirming your starting point",
                        color = Vectura AIColors.TextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else if (error == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(Spacing.sm))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Looking for entrance code",
                        color = Vectura AIColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "Point your camera at the entrance poster",
                        color = Vectura AIColors.TextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (onSimulateScan != null) {
                Spacer(Modifier.height(Spacing.md))
                Vectura AISecondaryButton(
                    text = "Simulate Entrance Scan",
                    onClick = onSimulateScan,
                    leadingIcon = Icons.Default.QrCodeScanner,
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(Vectura AIShapes.Medium)
                        .background(Vectura AIColors.AccentAmber.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Code not recognized",
                        tint = Vectura AIColors.AccentAmber,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(Spacing.sm))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Code not recognized",
                        color = Vectura AIColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        error,
                        color = Vectura AIColors.TextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.md))
            Vectura AIPrimaryButton(text = "Try Again", onClick = onRetry)
        }
    }
}

@Composable
private fun AnimatedScanReticle(
    hasError: Boolean,
    hasDetected: Boolean,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = rememberReduceMotion()
    val transition = rememberInfiniteTransition(label = "scanReticle")
    val breath by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (reduceMotion) 1f else 1.04f,
        animationSpec = infiniteRepeatable(tween(1_600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scanBreath",
    )
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduceMotion || hasDetected) 0f else 1f,
        animationSpec = infiniteRepeatable(tween(2_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scanSweep",
    )
    val successRipple by animateFloatAsState(
        targetValue = if (hasDetected) 1f else 0f,
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label = "qrSuccessRipple",
    )
    val accent = when {
        hasDetected -> Vectura AIColors.AccentGreen
        hasError -> Vectura AIColors.AccentAmber
        else -> Vectura AIColors.AccentCyan
    }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(Vectura AIShapes.XLarge)
                .background(Vectura AIColors.SurfaceCanvas.copy(alpha = 0.18f))
                .border(BorderStroke(1.dp, accent.copy(alpha = 0.34f)), Vectura AIShapes.XLarge),
        ) {
            val margin = 22.dp.toPx()
            val bracket = 54.dp.toPx() * breath
            val stroke = 4.dp.toPx()
            val top = margin
            val left = margin
            val right = size.width - margin
            val bottom = size.height - margin
            drawLine(accent, Offset(left, top), Offset(left + bracket, top), stroke, StrokeCap.Round)
            drawLine(accent, Offset(left, top), Offset(left, top + bracket), stroke, StrokeCap.Round)
            drawLine(accent, Offset(right, top), Offset(right - bracket, top), stroke, StrokeCap.Round)
            drawLine(accent, Offset(right, top), Offset(right, top + bracket), stroke, StrokeCap.Round)
            drawLine(accent, Offset(left, bottom), Offset(left + bracket, bottom), stroke, StrokeCap.Round)
            drawLine(accent, Offset(left, bottom), Offset(left, bottom - bracket), stroke, StrokeCap.Round)
            drawLine(accent, Offset(right, bottom), Offset(right - bracket, bottom), stroke, StrokeCap.Round)
            drawLine(accent, Offset(right, bottom), Offset(right, bottom - bracket), stroke, StrokeCap.Round)

            val sweepY = top + (bottom - top) * sweep
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, accent.copy(alpha = 0.9f), Color.Transparent),
                ),
                topLeft = Offset(left, sweepY),
                size = androidx.compose.ui.geometry.Size(right - left, 2.dp.toPx()),
            )
            if (hasError) {
                drawCircle(
                    color = accent.copy(alpha = 0.14f),
                    radius = size.minDimension * 0.35f,
                    center = Offset(size.width / 2f, size.height / 2f),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
            if (hasDetected) {
                repeat(3) { index ->
                    val local = (successRipple - index * 0.18f).coerceIn(0f, 1f)
                    drawCircle(
                        color = Vectura AIColors.AccentGreen.copy(alpha = (1f - local) * 0.28f),
                        radius = size.minDimension * (0.18f + local * 0.36f),
                        center = Offset(size.width / 2f, size.height / 2f),
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }
        }
        Icon(
            Icons.Default.QrCodeScanner,
            contentDescription = null,
            tint = accent.copy(alpha = 0.82f),
            modifier = Modifier.size(56.dp),
        )
    }
}

@Composable
private fun ScanVignette() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.74f),
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.78f),
                ),
            ),
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.18f),
            radius = size.maxDimension * 0.62f,
            center = Offset(size.width / 2f, size.height / 2f),
            style = Stroke(width = size.maxDimension * 0.24f),
        )
    }
}
