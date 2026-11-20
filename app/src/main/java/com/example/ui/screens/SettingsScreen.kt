package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.domain.model.AppSettingsState
import com.example.domain.model.ThemePreference
import com.example.domain.model.UpdateCheckState
import com.example.ui.components.DotMatrixSectionHeader
import com.example.ui.components.GlassCard
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.StatusConnected
import java.io.File

@Composable
fun SettingsScreen(
    settings: AppSettingsState,
    updateState: UpdateCheckState,
    onSetTheme: (ThemePreference) -> Unit,
    onSetHighRefreshMotion: (Boolean) -> Unit,
    onSetAutoPopup: (Boolean) -> Unit,
    onSaveGitHubRepo: (String, String) -> Unit,
    onCheckForUpdates: () -> Unit,
    onDownloadUpdateApk: (url: String, version: String, fileName: String?) -> Unit,
    onLaunchInstallApk: (File) -> Unit,
    onRequestInstallPermission: () -> Unit,
    onDismissUpdateDialog: () -> Unit,
    onResetAll: () -> Unit,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    var ownerInput by remember(settings.githubOwner) { mutableStateOf(settings.githubOwner) }
    var repoInput by remember(settings.githubRepo) { mutableStateOf(settings.githubRepo) }
    var showResetConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(settings.githubOwner, settings.githubRepo) {
        ownerInput = settings.githubOwner
        repoInput = settings.githubRepo
    }

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
                Column {
                    Text(
                        text = "SETTINGS",
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Text(
                        text = "Appearance, Motion, GitHub Auto-Update & Diagnostics",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Themes
            item {
                GlassCard {
                    DotMatrixSectionHeader(
                        title = "Theme Palette",
                        subtitle = "Nothing OS Obsidian / Alabaster / System",
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ThemePreference.entries.forEach { pref ->
                            FilterChip(
                                selected = settings.themePreference == pref,
                                onClick = { onSetTheme(pref) },
                                label = { Text(pref.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CrimsonAccent,
                                    selectedLabelColor = Color.White,
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("theme_${pref.name.lowercase()}"),
                            )
                        }
                    }
                }
            }

            // Animation & Popup Settings
            item {
                GlassCard {
                    DotMatrixSectionHeader(
                        title = "Motion & Connection Popup",
                        subtitle = "60/120 FPS spring physics & Air-style modal",
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    SettingSwitchRow(
                        title = "High-Refresh Spring Motion",
                        subtitle = "Smooth 60/120 FPS animations where supported",
                        checked = settings.highRefreshMotion,
                        onCheckedChange = onSetHighRefreshMotion,
                        testTag = "switch_high_refresh",
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingSwitchRow(
                        title = "Auto-Show Connection Card",
                        subtitle = "Open animated popup when Bluetooth earbuds connect",
                        checked = settings.autoShowConnectionPopup,
                        onCheckedChange = onSetAutoPopup,
                        testTag = "switch_auto_popup",
                    )
                }
            }

            // GitHub Repository & Auto-Update
            item {
                GlassCard(accentGlow = updateState is UpdateCheckState.UpdateAvailable) {
                    DotMatrixSectionHeader(
                        title = "GitHub Releases Auto-Update",
                        subtitle = "Current Version: v${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE})",
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = ownerInput,
                            onValueChange = { ownerInput = it },
                            label = { Text("GitHub Owner") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("github_owner_input"),
                        )
                        OutlinedTextField(
                            value = repoInput,
                            onValueChange = { repoInput = it },
                            label = { Text("Repository") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("github_repo_input"),
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { onSaveGitHubRepo(ownerInput, repoInput) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("save_github_repo_button"),
                        ) {
                            Text("Save Repo")
                        }
                        Button(
                            onClick = {
                                onSaveGitHubRepo(ownerInput, repoInput)
                                onCheckForUpdates()
                            },
                            enabled = updateState !is UpdateCheckState.Checking && updateState !is UpdateCheckState.Downloading,
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent, contentColor = Color.White),
                            modifier = Modifier
                                .weight(1.4f)
                                .height(48.dp)
                                .testTag("check_updates_button"),
                        ) {
                            if (updateState is UpdateCheckState.Checking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Checking...")
                            } else {
                                Icon(Icons.Filled.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Check Updates")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    when (updateState) {
                        is UpdateCheckState.UpToDate -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = StatusConnected, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Up to date (v${updateState.currentVersion}).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatusConnected,
                                )
                            }
                        }
                        is UpdateCheckState.Error -> {
                            Text(
                                text = updateState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = CrimsonAccent,
                            )
                        }
                        else -> {
                            Text(
                                text = "Queries GitHub Releases API, downloads signed release APK, and invokes Android Package Installer only with user-granted permission.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // About & Reset
            item {
                GlassCard {
                    DotMatrixSectionHeader(
                        title = "About HARDIK BUDS",
                        subtitle = "v${BuildConfig.VERSION_NAME} • ${BuildConfig.APPLICATION_ID}",
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Built with Kotlin, Jetpack Compose, Material 3, Room, DataStore, Retrofit, Android Bluetooth A2DP/HFP/BLE GATT, DynamicsProcessing 10-band EQ, Virtualizer, PresetReverb, and OS Spatializer diagnostics.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = { showResetConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("reset_all_button"),
                    ) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset All Profiles & Settings")
                    }
                }
            }
        }
    }

    // Update Available / Download / Install Popup
    when (updateState) {
        is UpdateCheckState.UpdateAvailable -> {
            AlertDialog(
                onDismissRequest = onDismissUpdateDialog,
                title = {
                    Text(
                        text = "Update v${updateState.latestVersion} Available",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            text = "${updateState.releaseName} (Current: v${updateState.currentVersion})",
                            style = MaterialTheme.typography.labelMedium,
                            color = CrimsonAccent,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = updateState.changelog,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (updateState.apkSizeBytes != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val mb = updateState.apkSizeBytes / (1024f * 1024f)
                            Text(
                                text = "APK Package: ${updateState.apkFileName} (${String.format("%.1f", mb)} MB)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                confirmButton = {
                    if (updateState.apkDownloadUrl != null) {
                        Button(
                            onClick = {
                                onDownloadUpdateApk(
                                    updateState.apkDownloadUrl,
                                    updateState.latestVersion,
                                    updateState.apkFileName,
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent, contentColor = Color.White),
                        ) {
                            Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download APK")
                        }
                    } else {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateState.htmlUrl))
                                runCatching { context.startActivity(intent) }
                                onDismissUpdateDialog()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent, contentColor = Color.White),
                        ) {
                            Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Release")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissUpdateDialog) {
                        Text("Later")
                    }
                },
            )
        }

        is UpdateCheckState.Downloading -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Downloading v${updateState.latestVersion}") },
                text = {
                    Column {
                        LinearProgressIndicator(
                            progress = { updateState.progressPercent / 100f },
                            color = CrimsonAccent,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${updateState.progressPercent}% downloaded",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                },
                confirmButton = {},
            )
        }

        is UpdateCheckState.ReadyToInstall -> {
            AlertDialog(
                onDismissRequest = onDismissUpdateDialog,
                title = { Text("Install v${updateState.latestVersion}") },
                text = {
                    Text(
                        if (updateState.requiresPermissionGrant) {
                            "Android requires explicit permission to install apps from unknown sources for HARDIK BUDS. Grant permission in Settings, then return to launch the system package installer."
                        } else {
                            "Release APK downloaded to app cache. Tap Install to open the standard Android package installer."
                        }
                    )
                },
                confirmButton = {
                    if (updateState.requiresPermissionGrant) {
                        Button(
                            onClick = onRequestInstallPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent, contentColor = Color.White),
                        ) {
                            Text("Grant Install Permission")
                        }
                    } else {
                        Button(
                            onClick = { onLaunchInstallApk(updateState.apkFile) },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent, contentColor = Color.White),
                        ) {
                            Icon(Icons.Filled.InstallMobile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Install Update")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissUpdateDialog) {
                        Text("Cancel")
                    }
                },
            )
        }

        else -> Unit
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset HARDIK BUDS?") },
            text = {
                Text("This clears all saved custom EQ profiles, device aliases, connection history, and restores default audio settings.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetAll()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent, contentColor = Color.White),
                    modifier = Modifier.testTag("confirm_reset_all_button"),
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CrimsonAccent),
            modifier = Modifier.testTag(testTag),
        )
    }
}
