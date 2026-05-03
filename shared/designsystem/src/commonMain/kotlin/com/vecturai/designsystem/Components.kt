package com.vecturai.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val LocalVecturaiHapticsEnabled = staticCompositionLocalOf { true }

@Composable
fun VecturaiHapticsGate(enabled: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalVecturaiHapticsEnabled provides enabled, content = content)
}

@Composable
fun VecturaiPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    val hapticsEnabled = LocalVecturaiHapticsEnabled.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "primaryButtonScale",
    )
    Button(
        onClick = {
            if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.5f
            }
            .clip(VecturaiShapes.Medium),
        enabled = enabled && !loading,
        shape = VecturaiShapes.Medium,
        interactionSource = interactionSource,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContentColor = Color.White.copy(alpha = 0.72f),
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VecturaiBrush.Primary),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp,
                    color = Color.White,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(Spacing.sm))
                    }
                    Text(
                        text = text,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@Composable
fun VecturaiSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(VecturaiShapes.Medium)
            .background(VecturaiColors.SurfaceElevated.copy(alpha = if (enabled) 0.96f else 0.55f))
            .border(BorderStroke(1.dp, VecturaiColors.BorderStrong), VecturaiShapes.Medium)
            .vecturaiTap(enabled = enabled, onClick = onClick)
            .padding(horizontal = Spacing.md),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(Spacing.xs))
        }
        Text(text = text, color = VecturaiColors.TextSecondary, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun VecturaiGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(VecturaiShapes.Medium)
            .vecturaiTap(enabled = enabled, onClick = onClick)
            .padding(horizontal = Spacing.md),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(Spacing.xs))
        }
        Text(text = text, color = VecturaiColors.TextSecondary, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun IconChip(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(VecturaiShapes.Medium)
            .background(VecturaiColors.SurfaceElevated.copy(alpha = 0.94f))
            .border(BorderStroke(1.dp, VecturaiColors.BorderSubtle), VecturaiShapes.Medium)
            .vecturaiTap(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(21.dp),
        )
    }
}

@Composable
fun VecturaiCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    glass: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tapModifier = if (onClick != null) Modifier.vecturaiTap(onClick = onClick) else Modifier
    // Compose has no native backdrop blur; a graphicsLayer BlurEffect would smear the
    // panel's own text. The "glass" variant simulates frosted glass with a translucent
    // fill and a brighter inset border so it reads against camera/AR backdrops.
    val fill = if (glass) VecturaiColors.SurfaceCard.copy(alpha = 0.55f) else VecturaiColors.SurfaceCard.copy(alpha = 0.96f)
    val borderColor = if (glass) VecturaiColors.BorderStrong else VecturaiColors.BorderSubtle
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(tapModifier),
        shape = VecturaiShapes.Large,
        color = fill,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            content = content,
        )
    }
}

@Composable
fun StatPill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    containerAlpha: Float = 0.16f,
) {
    Surface(
        modifier = modifier,
        shape = VecturaiShapes.Pill,
        color = color.copy(alpha = containerAlpha),
        border = BorderStroke(1.dp, color.copy(alpha = 0.42f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            color = color,
            style = VecturaiTypography.overline(),
            maxLines = 1,
        )
    }
}

@Composable
fun CategoryBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    StatPill(text = text, color = color, modifier = modifier, containerAlpha = 0.14f)
}

@Composable
fun VecturaiFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = HapticFeedbackType.TextHandleMove
    val hapticFeedback = LocalHapticFeedback.current
    val hapticsEnabled = LocalVecturaiHapticsEnabled.current
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(VecturaiShapes.Pill)
            .then(
                if (selected) {
                    Modifier.background(VecturaiBrush.Primary)
                } else {
                    Modifier
                        .background(VecturaiColors.SurfaceElevated)
                        .border(BorderStroke(1.dp, VecturaiColors.BorderSubtle), VecturaiShapes.Pill)
                },
            )
    ) {
        FilterChip(
            selected = selected,
            onClick = {
                if (hapticsEnabled) hapticFeedback.performHapticFeedback(haptic)
                onClick()
            },
            label = {
                Text(
                    text = text,
                    color = if (selected) Color.White else VecturaiColors.TextSecondary,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
            },
            modifier = Modifier.height(48.dp),
            shape = VecturaiShapes.Pill,
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selected,
                borderColor = VecturaiColors.BorderSubtle,
                selectedBorderColor = Color.Transparent,
                borderWidth = 1.dp,
                selectedBorderWidth = 0.dp,
            ),
            colors = FilterChipDefaults.filterChipColors(
                containerColor = Color.Transparent,
                selectedContainerColor = Color.Transparent,
                labelColor = VecturaiColors.TextSecondary,
                selectedLabelColor = Color.White,
            ),
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    trailing: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = VecturaiColors.TextMuted,
            style = VecturaiTypography.overline(),
        )
        if (trailing != null) {
            Spacer(Modifier.weight(1f))
            Text(
                text = trailing,
                color = VecturaiColors.TextDisabled,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
    showDots: Boolean = true,
) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(18_000, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "auroraPhase",
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(VecturaiColors.SurfaceCanvas)
        val motion = intensity.coerceIn(0f, 1f)
        val p = phase * 2f * PI.toFloat()
        val cyanCenter = Offset(
            x = size.width * (0.24f + 0.12f * cos(p * 1.3f) * motion),
            y = size.height * (0.2f + 0.1f * sin(p) * motion),
        )
        val blueCenter = Offset(
            x = size.width * (0.78f + 0.1f * sin(p * 0.8f) * motion),
            y = size.height * (0.7f + 0.12f * cos(p * 1.1f) * motion),
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(VecturaiColors.AccentCyan.copy(alpha = 0.16f), Color.Transparent),
                center = cyanCenter,
                radius = size.minDimension * 0.7f,
            ),
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(VecturaiColors.Primary.copy(alpha = 0.16f), Color.Transparent),
                center = blueCenter,
                radius = size.minDimension * 0.75f,
            ),
        )
        if (showDots) {
            val spacing = 22.dp.toPx()
            val radius = 0.85.dp.toPx()
            var x = 10.dp.toPx()
            while (x < size.width) {
                var y = 14.dp.toPx()
                while (y < size.height) {
                    drawCircle(
                        color = VecturaiColors.BorderStrong.copy(alpha = 0.34f),
                        radius = radius,
                        center = Offset(x, y),
                    )
                    y += spacing
                }
                x += spacing
            }
        }
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    VecturaiColors.SurfaceCanvas.copy(alpha = 0.74f),
                    Color.Transparent,
                    VecturaiColors.SurfaceCanvas.copy(alpha = 0.88f),
                ),
            ),
        )
    }
}

fun Modifier.vecturaiTap(
    enabled: Boolean = true,
    haptic: HapticFeedbackType = HapticFeedbackType.LongPress,
    onClick: () -> Unit,
): Modifier = composed {
    val hapticFeedback = LocalHapticFeedback.current
    val hapticsEnabled = LocalVecturaiHapticsEnabled.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "tapScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.clickable(
        interactionSource = interactionSource,
        indication = LocalIndication.current,
        enabled = enabled,
        role = Role.Button,
    ) {
        if (hapticsEnabled) hapticFeedback.performHapticFeedback(haptic)
        onClick()
    }
}

@Composable
fun AnimatedNumber(
    value: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = VecturaiTypography.numericLarge(),
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val animated by animateIntAsState(
        targetValue = value,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "animatedNumber",
    )
    Text(
        text = animated.toString(),
        modifier = modifier,
        style = style,
        color = color,
        maxLines = 1,
    )
}

@Composable
fun AnimatedGradientNumber(
    value: Int,
    suffix: String,
    modifier: Modifier = Modifier,
    style: TextStyle = VecturaiTypography.numericDisplay(),
    textAlign: TextAlign? = null,
) {
    val animated by animateIntAsState(
        targetValue = value,
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label = "animatedGradientNumber",
    )
    GradientText(
        text = "$animated$suffix",
        modifier = modifier,
        style = style,
        textAlign = textAlign,
    )
}

@Composable
fun GradientText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style.copy(brush = VecturaiBrush.Primary),
        textAlign = textAlign,
    )
}

@Composable
fun VecturaiButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    VecturaiPrimaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = icon,
    )
}

@Composable
fun VecturaiEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.56f),
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun VecturaiSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    SectionHeader(title = title, modifier = modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs))
}
