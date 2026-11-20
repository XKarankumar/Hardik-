package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsBluetooth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.bluetooth.BluetoothHardwareState
import com.example.domain.model.BluetoothDeviceUi
import com.example.domain.model.ConnectionHistoryItem
import com.example.ui.components.DotMatrixSectionHeader
import com.example.ui.components.GlassCard
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DevicesScreen(
    bluetoothState: BluetoothHardwareState,
    history: List<ConnectionHistoryItem>,
    onRequestPermissions: () -> Unit,
    onRefresh: () -> Unit,
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit,
    onPairDevice: (String) -> Unit,
    onConnectDevice: (String) -> Unit,
    onDisconnectDevice: (String) -> Unit,
    onRenameDevice: (String, String) -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onShowPopupForDevice: (BluetoothDeviceUi) -> Unit,
    contentPadding: PaddingValues,
) {
    var renameTarget by remember { mutableStateOf<BluetoothDeviceUi?>(null) }
    var renameText by remember { mutableStateOf("") }

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
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "DEVICES",
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        Text(
                            text = "Bluetooth Discovery, Pairing & Multi-Device Link",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row {
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.testTag("devices_refresh_button"),
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh Bluetooth devices")
                        }
                        IconButton(
                            onClick = onOpenBluetoothSettings,
                            modifier = Modifier.testTag("devices_system_bt_button"),
                        ) {
                            Icon(Icons.Filled.SettingsBluetooth, contentDescription = "Android Bluetooth settings")
                        }
                    }
                }
            }

            // Adapter / Permission Card
            item {
                GlassCard(accentGlow = !bluetoothState.hasPermissions || !bluetoothState.isBluetoothEnabled) {
                    DotMatrixSectionHeader(
                        title = "Bluetooth Radio Status",
                        subtitle = when {
                            !bluetoothState.hasBluetoothHardware -> "Bluetooth hardware unavailable"
                            !bluetoothState.hasPermissions -> "Runtime permissions required"
                            !bluetoothState.isBluetoothEnabled -> "Bluetooth is turned off"
                            else -> "Adapter ready • ${bluetoothState.bondedDevices.size} paired audio device(s)"
                        },
                        accentActive = bluetoothState.isBluetoothEnabled && bluetoothState.hasPermissions,
                    )
                    if (!bluetoothState.statusMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = bluetoothState.statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = CrimsonAccent,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (!bluetoothState.hasPermissions) {
                            Button(
                                onClick = onRequestPermissions,
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent, contentColor = Color.White),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("devices_grant_permissions_button"),
                            ) {
                                Text("Grant Permissions")
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (bluetoothState.isScanning) onStopDiscovery() else onStartDiscovery()
                                },
                                enabled = bluetoothState.isBluetoothEnabled,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (bluetoothState.isScanning) MaterialTheme.colorScheme.secondaryContainer else CrimsonAccent,
                                    contentColor = if (bluetoothState.isScanning) MaterialTheme.colorScheme.onSecondaryContainer else Color.White,
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("devices_scan_button"),
                            ) {
                                if (bluetoothState.isScanning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Stop Scan")
                                } else {
                                    Icon(Icons.Filled.BluetoothSearching, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Scan Nearby")
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = onOpenBluetoothSettings,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("devices_open_settings_button"),
                        ) {
                            Text("OS Settings")
                        }
                    }
                }
            }

            // Paired / Multi-Device Management
            item {
                DotMatrixSectionHeader(
                    title = "Paired Audio Devices",
                    subtitle = "Tap Connect, Reconnect, or Rename alias",
                    accentActive = bluetoothState.activeDevice != null,
                )
            }

            if (bluetoothState.bondedDevices.isEmpty()) {
                item {
                    GlassCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Bluetooth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (bluetoothState.hasPermissions && bluetoothState.isBluetoothEnabled) {
                                    "No paired Bluetooth earbuds found yet. Tap Scan Nearby or pair in OS Settings."
                                } else {
                                    "Enable Bluetooth and grant permissions to manage paired earbuds."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(bluetoothState.bondedDevices, key = { it.address }) { device ->
                    PairedDeviceCard(
                        device = device,
                        onConnect = { onConnectDevice(device.address) },
                        onDisconnect = { onDisconnectDevice(device.address) },
                        onRename = {
                            renameTarget = device
                            renameText = device.displayName
                        },
                        onPreviewPopup = { onShowPopupForDevice(device) },
                    )
                }
            }

            // Discovered Nearby Devices
            if (bluetoothState.discoveredDevices.isNotEmpty()) {
                item {
                    DotMatrixSectionHeader(
                        title = "Discovered Nearby",
                        subtitle = "${bluetoothState.discoveredDevices.size} device(s) found",
                    )
                }
                items(bluetoothState.discoveredDevices, key = { "disc_${it.address}" }) { device ->
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = device.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${device.address} • ${device.deviceTypeLabel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Button(
                                onClick = { onPairDevice(device.address) },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent, contentColor = Color.White),
                                modifier = Modifier.height(48.dp),
                            ) {
                                Text("Pair")
                            }
                        }
                    }
                }
            }

            // Supported Features & Hardware Inspector
            item {
                GlassCard {
                    DotMatrixSectionHeader(
                        title = "Supported Feature Matrix",
                        subtitle = "Standard Android API vs Manufacturer Vendor APIs",
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FeatureSupportRow("A2DP / HFP Discovery & Pairing", true)
                    FeatureSupportRow("10-Band EQ & DynamicsProcessing", true)
                    FeatureSupportRow("Virtual 5.1 / 7.1 & Room Reverb", true)
                    FeatureSupportRow("Unified BT / BLE Battery (0x180F)", true)
                    FeatureSupportRow("Split L/R/Case Battery", false, "Requires Vendor GATT / Fast Pair")
                    FeatureSupportRow("ANC / Transparency / Wind Mode", false, "Requires Manufacturer Protocol")
                }
            }

            // Connection History
            item {
                DotMatrixSectionHeader(
                    title = "Connection History",
                    subtitle = "Recent Bluetooth link events stored locally",
                    accentActive = history.isNotEmpty(),
                )
            }

            if (history.isEmpty()) {
                item {
                    GlassCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "No connection events recorded yet. Events are logged automatically when earbuds connect or disconnect.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(history.take(15), key = { it.id }) { event ->
                    val timeLabel = remember(event.timestampMs) {
                        SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()).format(Date(event.timestampMs))
                    }
                    GlassCard(cornerRadius = 16.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = event.deviceName,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${event.eventType} • $timeLabel",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (event.batteryAtEvent != null) {
                                Text(
                                    text = "${event.batteryAtEvent}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = CrimsonAccent,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (renameTarget != null) {
        val target = renameTarget!!
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename Earbuds") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Device Alias") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_device_input"),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRenameDevice(target.address, renameText)
                        renameTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent, contentColor = Color.White),
                    modifier = Modifier.testTag("confirm_rename_device_button"),
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun PairedDeviceCard(
    device: BluetoothDeviceUi,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onRename: () -> Unit,
    onPreviewPopup: () -> Unit,
) {
    GlassCard(accentGlow = device.isConnected) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (device.isConnected) StatusConnected.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (device.isConnected) Icons.Filled.BluetoothConnected else Icons.Filled.Bluetooth,
                        contentDescription = null,
                        tint = if (device.isConnected) StatusConnected else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = buildString {
                            append(if (device.isConnected) "Connected" else "Paired")
                            append(" • ${device.deviceTypeLabel}")
                            device.unifiedBatteryPercent?.let { append(" • $it%") }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (device.isConnected) StatusConnected else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            IconButton(
                onClick = onRename,
                modifier = Modifier.testTag("rename_${device.address}"),
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "Rename ${device.displayName}")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (device.isConnected) {
                OutlinedButton(
                    onClick = onPreviewPopup,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                ) {
                    Text("Card")
                }
                Button(
                    onClick = onDisconnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                ) {
                    Text("Disconnect")
                }
            } else {
                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent, contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reconnect")
                }
            }
        }
    }
}

@Composable
private fun FeatureSupportRow(
    feature: String,
    supported: Boolean,
    note: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = feature, style = MaterialTheme.typography.bodyMedium)
            if (!note.isNullOrBlank()) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = if (supported) "SUPPORTED" else "VENDOR API",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (supported) StatusConnected else StatusWarning,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(
                    (if (supported) StatusConnected else StatusWarning).copy(alpha = 0.14f)
                )
                .border(
                    1.dp,
                    (if (supported) StatusConnected else StatusWarning).copy(alpha = 0.4f),
                    RoundedCornerShape(50),
                )
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}
