package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.domain.model.EarbudVisualStyle
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LocalMotionConfig
import com.example.ui.theme.StatusConnected
import com.example.ui.viewmodel.ConnectionPopupUi

@Composable
fun Modifier.dotMatrixBackground(): Modifier {
    val dotColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.055f)
    return this.drawBehind {
        val spacing = 22.dp.toPx()
        val radius = 1.1.dp.toPx()
        var y = spacing / 2f
        while (y < size.height) {
            var x = spacing / 2f
            while (x < size.width) {
                drawCircle(color = dotColor, radius = radius, center = Offset(x, y))
                x += spacing
            }
            y += spacing
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    accentGlow: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val surface = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (accentGlow) {
        CrimsonAccent.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
    }

    val shape = RoundedCornerShape(cornerRadius)
    val baseModifier = modifier
        .clip(shape)
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    surface.copy(alpha = 0.88f),
                    surfaceVariant.copy(alpha = 0.72f),
                )
            )
        )
        .border(width = 1.dp, color = borderColor, shape = shape)
        .let { m -> if (onClick != null) m.clickable(onClick = onClick) else m }

    Column(
        modifier = baseModifier.padding(18.dp),
        content = content,
    )
}

@Composable
fun DotMatrixSectionHeader(
    title: String,
    subtitle: String? = null,
    accentActive: Boolean = true,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (accentActive) CrimsonAccent
                            else MaterialTheme.colorScheme.outline
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun Earbuds3DShowcase(
    visualStyle: EarbudVisualStyle,
    isConnected: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val motionEnabled = LocalMotionConfig.current.highRefreshMotion
    val infiniteTransition = rememberInfiniteTransition(label = "earbuds_float")

    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (motionEnabled) 2400 else 3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "floatY",
    )

    val tiltAngle by infiniteTransition.animateFloat(
        initialValue = -2.8f,
        targetValue = 2.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (motionEnabled) 3200 else 4800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "tilt",
    )

    val ledPulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ledPulse",
    )

    val scaleSpring by animateFloatAsState(
        targetValue = if (isConnected) 1.03f else 0.97f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "connectScale",
    )

    val drawableRes = when (visualStyle) {
        EarbudVisualStyle.NOTHING_TRANSPARENT,
        EarbudVisualStyle.OBSIDIAN_STUDIO,
        EarbudVisualStyle.CRIMSON_PULSE -> R.drawable.img_earbuds_nothing
        EarbudVisualStyle.PEARL_STEM_PRO -> R.drawable.img_earbuds_pro
    }

    val colorFilter = when (visualStyle) {
        EarbudVisualStyle.OBSIDIAN_STUDIO -> ColorFilter.colorMatrix(
            ColorMatrix().apply { setToSaturation(0.15f) }
        )
        EarbudVisualStyle.CRIMSON_PULSE -> ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    1.15f, 0f, 0f, 0f, 14f,
                    0f, 0.85f, 0f, 0f, 0f,
                    0f, 0f, 0.85f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f,
                )
            )
        )
        else -> null
    }

    val boxHeight = if (compact) 170.dp else 230.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(boxHeight)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF09090B))
            .border(
                width = 1.dp,
                color = if (isConnected) CrimsonAccent.copy(alpha = 0.5f) else Color(0xFF272730),
                shape = RoundedCornerShape(22.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Subtle radial studio stage halo
        Canvas(modifier = Modifier.fillMaxSize()) {
            val haloColor = if (visualStyle == EarbudVisualStyle.CRIMSON_PULSE || isConnected) {
                CrimsonAccent.copy(alpha = 0.16f)
            } else {
                Color.White.copy(alpha = 0.08f)
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(haloColor, Color.Transparent),
                    center = center,
                    radius = size.minDimension * 0.58f,
                ),
                radius = size.minDimension * 0.58f,
                center = center,
            )
        }

        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = "${visualStyle.displayName} 3D earbuds illustration",
            contentScale = ContentScale.Fit,
            colorFilter = colorFilter,
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .graphicsLayer {
                    translationY = if (motionEnabled) floatOffset else 0f
                    rotationZ = if (motionEnabled) tiltAngle else 0f
                    scaleX = scaleSpring
                    scaleY = scaleSpring
                },
        )

        // Top-left model badge
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xAA16161B))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(
                        if (isConnected) StatusConnected.copy(alpha = ledPulse)
                        else CrimsonAccent.copy(alpha = ledPulse)
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = visualStyle.brandTag,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFF4F4F6),
            )
        }

        // Bottom-right live status dot
        Text(
            text = if (isConnected) "LINK ACTIVE" else "STANDBY",
            style = MaterialTheme.typography.labelSmall,
            color = if (isConnected) StatusConnected else Color(0xFFA1A1AA),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xB316161B))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun AcousticWaveVisualizer(
    bins: List<Float>,
    isLive: Boolean,
    modifier: Modifier = Modifier,
) {
    val barCount = bins.size.coerceAtLeast(1)
    val inactiveColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val highColor = CrimsonAccent
    val midColor = MaterialTheme.colorScheme.onBackground

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
    ) {
        val gap = 4.dp.toPx()
        val barWidth = ((size.width - gap * (barCount - 1)) / barCount).coerceAtLeast(2f)
        val dotRadius = (barWidth * 0.36f).coerceAtLeast(1.8f)

        for (i in 0 until barCount) {
            val level = bins[i].coerceIn(0.05f, 1f)
            val barHeight = size.height * level
            val x = i * (barWidth + gap)
            val topY = size.height - barHeight
            val color = when {
                !isLive -> inactiveColor
                level > 0.65f -> highColor
                else -> midColor.copy(alpha = 0.78f)
            }

            drawRoundRect(
                color = color,
                topLeft = Offset(x, topY),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(dotRadius, dotRadius),
            )
        }
    }
}

@Composable
fun AppleStyleConnectionPopup(
    popup: ConnectionPopupUi,
    onDismiss: () -> Unit,
    onOpenSoundStudio: () -> Unit,
    onOpenDevices: () -> Unit,
) {
    if (!popup.visible) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.56f))
                .clickable(onClick = onDismiss)
                .navigationBarsPadding()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            AnimatedVisibility(
                visible = popup.visible,
                enter = slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ) + fadeIn(tween(220)) + scaleIn(initialScale = 0.94f),
                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(tween(150)),
            ) {
                Surface(
                    shape = RoundedCornerShape(30.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (popup.isConnected) CrimsonAccent.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.outline,
                    ),
                    tonalElevation = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = false, onClick = {})
                        .testTag("connection_popup_card"),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.BluetoothConnected,
                                    contentDescription = "Bluetooth connection status",
                                    tint = if (popup.isConnected) StatusConnected else CrimsonAccent,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (popup.isPreview) "AIR-STYLE PREVIEW" else "DEVICE LINKED",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("close_connection_popup_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close connection popup",
                                )
                            }
                        }

                        Text(
                            text = popup.deviceName,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = popup.deviceAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Earbuds3DShowcase(
                            visualStyle = popup.visualStyle,
                            isConnected = popup.isConnected,
                            compact = true,
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Battery Readout Row
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 18.dp,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.BatteryChargingFull,
                                        contentDescription = "Battery",
                                        tint = CrimsonAccent,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "EARBUDS BATTERY",
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                        Text(
                                            text = popup.batterySourceLabel,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Text(
                                    text = popup.unifiedBattery?.let { "$it%" } ?: "--%",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (popup.unifiedBattery != null) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onOpenDevices()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("popup_devices_button"),
                            ) {
                                Text("Devices")
                            }
                            Button(
                                onClick = {
                                    onDismiss()
                                    onOpenSoundStudio()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CrimsonAccent,
                                    contentColor = Color.White,
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("popup_sound_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.GraphicEq,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sound Studio")
                            }
                        }
                    }
                }
            }
        }
    }
}
