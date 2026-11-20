package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.bluetooth.BluetoothHardwareState
import com.example.domain.model.AudioEngineStatus
import com.example.domain.model.EarbudVisualStyle
import com.example.domain.model.HomeTheatreState
import com.example.domain.model.ReferenceToneMode
import com.example.domain.model.SoundStudioState
import com.example.ui.components.AcousticWaveVisualizer
import com.example.ui.components.DotMatrixSectionHeader
import com.example.ui.components.Earbuds3DShowcase
import com.example.ui.components.GlassCard
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusDisconnected
import com.example.ui.theme.StatusWarning

@Composable
fun HomeScreen(
    bluetoothState: BluetoothHardwareState,
    audioStatus: AudioEngineStatus,
    soundState: SoundStudioState,
    theatreState: HomeTheatreState,
    spectrumBins: List<Float>,
    previewVisualStyle: EarbudVisualStyle,
    onSelectVisualStyle: (EarbudVisualStyle) -> Unit,
    onPreviewConnectionPopup: () -> Unit,
    onToggleMasterBypass: (Boolean) -> Unit,
    onToggleReferenceTone: () -> Unit,
    onSelectReferenceMode: (ReferenceToneMode) -> Unit,
    onNavigateToSound: () -> Unit,
    onNavigateToCinema: () -> Unit,
    onNavigateToDevices: () -> Unit,
    onRequestBluetoothPermissions: () -> Unit,
    contentPadding: PaddingValues,
) {
    val active = bluetoothState.activeDevice
    val effectiveStyle = active?.visualStyle ?: previewVisualStyle

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "HARDIK BUDS",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            active != null -> StatusConnected
                                            bluetoothState.isBluetoothEnabled -> StatusWarning
                                            else -> StatusDisconnected
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = active?.let { "${it.displayName} • ${it.profileSummary}" }
                                    ?: if (bluetoothState.hasPermissions) "No earbuds linked" else "Bluetooth permission needed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onPreviewConnectionPopup,
                            modifier = Modifier.testTag("preview_popup_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Headphones,
                                contentDescription = "Open Air-style connection card",
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        IconButton(
                            onClick = onNavigateToDevices,
                            modifier = Modifier.testTag("home_devices_shortcut_button"),
                        ) {
                            Icon(
                                imageVector = if (active != null) Icons.Filled.BluetoothConnected else Icons.Filled.Bluetooth,
                                contentDescription = "Manage Bluetooth devices",
                                tint = if (active != null) StatusConnected else CrimsonAccent,
                            )
                        }
                    }
                }
            }

            // Permission CTA banner if Bluetooth permission missing
            if (!bluetoothState.hasPermissions) {
                item {
                    GlassCard(accentGlow = true) {
                        DotMatrixSectionHeader(
                            title = "Bluetooth Access Required",
                            subtitle = "Required for device discovery, pairing, and live battery status",
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onRequestBluetoothPermissions,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrimsonAccent,
                                contentColor = Color.White,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("grant_bluetooth_permission_button"),
                        ) {
                            Text("Grant Bluetooth Permissions")
                        }
                    }
                }
            }

            // 3D Earbuds Showcase + Brand Visual Customizer
            item {
                GlassCard(accentGlow = active != null) {
                    Earbuds3DShowcase(
                        visualStyle = effectiveStyle,
                        isConnected = active != null,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = active?.displayName ?: effectiveStyle.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = if (active != null) {
                                    "Connected • ${active.deviceTypeLabel}"
                                } else {
                                    "Tap Devices to pair or connect your earbuds"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedButton(
                            onClick = onNavigateToDevices,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("home_connect_button"),
                        ) {
                            Text(if (active != null) "Switch" else "Connect")
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "HARDWARE VISUAL SKIN",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        EarbudVisualStyle.entries.forEach { style ->
                            FilterChip(
                                selected = effectiveStyle == style,
                                onClick = { onSelectVisualStyle(style) },
                                label = { Text(style.displayName, style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CrimsonAccent,
                                    selectedLabelColor = Color.White,
                                ),
                                modifier = Modifier.testTag("visual_style_${style.name.lowercase()}"),
                            )
                        }
                    }
                }
            }

            // Battery Dashboard
            item {
                GlassCard {
                    DotMatrixSectionHeader(
                        title = "Battery Dashboard",
                        subtitle = active?.batterySourceLabel ?: "Connect earbuds to read Android Bluetooth battery",
                        accentActive = active?.unifiedBatteryPercent != null,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val unified = active?.unifiedBatteryPercent
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        BatteryTile(
                            label = "LEFT BUD",
                            percent = active?.leftBatteryPercent ?: unified,
                            subtext = when {
                                active?.leftBatteryPercent != null -> "BLE / Vendor"
                                unified != null -> "Unified BT"
                                else -> "Unavailable"
                            },
                            modifier = Modifier.weight(1f),
                        )
                        BatteryTile(
                            label = "RIGHT BUD",
                            percent = active?.rightBatteryPercent ?: unified,
                            subtext = when {
                                active?.rightBatteryPercent != null -> "BLE / Vendor"
                                unified != null -> "Unified BT"
                                else -> "Unavailable"
                            },
                            modifier = Modifier.weight(1f),
                        )
                        BatteryTile(
                            label = "CASE",
                            percent = active?.caseBatteryPercent,
                            subtext = if (active?.caseBatteryPercent != null) "Vendor BLE" else "Vendor Only",
                            modifier = Modifier.weight(1f),
                        )
                    }

                    if (active != null && active.leftBatteryPercent == null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Note: Standard Android Bluetooth HFP/BAS exposes a single unified battery value. Individual L/R and Case percentages require manufacturer GATT or Fast Pair extensions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Quick Controls & Acoustic Audition
            item {
                GlassCard {
                    DotMatrixSectionHeader(
                        title = "Quick Acoustic Controls",
                        subtitle = audioStatus.engineName,
                        accentActive = !soundState.masterBypass,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DSP Audio Engine",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = if (soundState.masterBypass) "Bypassed (Pure Direct)" else "Active • ${audioStatus.sessionScope}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = !soundState.masterBypass,
                            onCheckedChange = { enabled -> onToggleMasterBypass(!enabled) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CrimsonAccent),
                            modifier = Modifier.testTag("home_dsp_switch"),
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    AcousticWaveVisualizer(
                        bins = spectrumBins,
                        isLive = audioStatus.isReferenceTonePlaying || !soundState.masterBypass,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        QuickNavPill(
                            icon = { Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = CrimsonAccent) },
                            title = "10-Band EQ",
                            subtitle = soundState.activePresetId.uppercase(),
                            onClick = onNavigateToSound,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_sound_studio_pill"),
                        )
                        QuickNavPill(
                            icon = { Icon(Icons.Filled.SurroundSound, contentDescription = null, tint = CrimsonAccent) },
                            title = "Spatial",
                            subtitle = theatreState.spatialMode.channelBadge,
                            onClick = onNavigateToCinema,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_spatial_pill"),
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "ACOUSTIC AUDITION PLAYER",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ReferenceToneMode.entries.forEach { mode ->
                                val selected = audioStatus.referenceTrackMode == mode
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(if (selected) CrimsonAccent else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { onSelectReferenceMode(mode) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onToggleReferenceTone,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (audioStatus.isReferenceTonePlaying) CrimsonAccent else MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if (audioStatus.isReferenceTonePlaying) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("home_audition_button"),
                    ) {
                        Icon(
                            imageVector = if (audioStatus.isReferenceTonePlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (audioStatus.isReferenceTonePlaying) "Stop DSP Reference Audio"
                            else "Play Real-Time DSP Reference Audio"
                        )
                    }
                }
            }

            // ANC & Transparency Hardware Capability Card (Never Fake!)
            item {
                val anc = bluetoothState.ancReport
                GlassCard {
                    DotMatrixSectionHeader(
                        title = "ANC & Transparency",
                        subtitle = if (anc.supportsAnc) "Vendor protocol active" else "Hardware / Vendor API Check",
                        accentActive = anc.supportsAnc,
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "Hardware API capability notice",
                            tint = CrimsonAccent,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = anc.detectedProfileNotes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CapabilityBadge("ANC", anc.supportsAnc, Modifier.weight(1f))
                        CapabilityBadge("Transparency", anc.supportsTransparency, Modifier.weight(1f))
                        CapabilityBadge("Adaptive", anc.supportsAdaptiveAnc, Modifier.weight(1f))
                        CapabilityBadge("Wind Cut", anc.supportsWindReduction, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun BatteryTile(
    label: String,
    percent: Int?,
    subtext: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.BatteryStd,
                contentDescription = label,
                tint = if (percent != null) CrimsonAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = percent?.let { "$it%" } ?: "--%",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (percent ?: 0) / 100f },
            color = if ((percent ?: 0) <= 20) CrimsonAccent else StatusConnected,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtext,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun QuickNavPill(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CapabilityBadge(
    label: String,
    supported: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (supported) StatusConnected.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
            .border(
                1.dp,
                if (supported) StatusConnected.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp),
            )
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        Text(
            text = if (supported) "READY" else "GATED",
            style = MaterialTheme.typography.labelSmall,
            color = if (supported) StatusConnected else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
