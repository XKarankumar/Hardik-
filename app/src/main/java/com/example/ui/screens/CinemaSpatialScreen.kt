package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AudioEngineStatus
import com.example.domain.model.HomeTheatreState
import com.example.domain.model.MovieSoundProfile
import com.example.domain.model.RoomSimulation
import com.example.domain.model.SpatialMode
import com.example.ui.components.AcousticWaveVisualizer
import com.example.ui.components.DotMatrixSectionHeader
import com.example.ui.components.GlassCard
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.StatusConnected
import kotlin.math.roundToInt

@Composable
fun CinemaSpatialScreen(
    theatreState: HomeTheatreState,
    audioStatus: AudioEngineStatus,
    spectrumBins: List<Float>,
    onSetSpatialMode: (SpatialMode) -> Unit,
    onSetRoomSimulation: (RoomSimulation) -> Unit,
    onSetCinemaModeEnabled: (Boolean) -> Unit,
    onApplyMovieProfile: (MovieSoundProfile) -> Unit,
    onSetVirtualSubwoofer: (Float) -> Unit,
    onSetDeepBass: (Float) -> Unit,
    onSetDialogueEnhancement: (Float) -> Unit,
    onSetStereoWidening: (Float) -> Unit,
    onSetTheatreBassDb: (Float) -> Unit,
    onSetTheatreTrebleDb: (Float) -> Unit,
    onToggleReferenceTone: () -> Unit,
    onOpenSystemSoundSettings: () -> Unit,
    contentPadding: PaddingValues,
) {
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
                        text = "CINEMA & SPATIAL",
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Text(
                        text = "Android Virtualizer, PresetReverb & OS Spatializer Telemetry",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Hero Spatial Soundstage & Live Visualizer
            item {
                GlassCard(accentGlow = theatreState.spatialMode != SpatialMode.OFF || theatreState.cinemaModeEnabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(155.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF09090B)),
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_cinema_stage),
                            contentDescription = "3D spatial audio stage artwork",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color(0xDD09090B))
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(14.dp),
                        ) {
                            Text(
                                text = theatreState.spatialMode.title.uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                            )
                            Text(
                                text = theatreState.spatialMode.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD4D4D8),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    AcousticWaveVisualizer(
                        bins = spectrumBins,
                        isLive = audioStatus.isReferenceTonePlaying || theatreState.spatialMode != SpatialMode.OFF,
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onToggleReferenceTone,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (audioStatus.isReferenceTonePlaying) CrimsonAccent else MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if (audioStatus.isReferenceTonePlaying) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("cinema_audition_button"),
                    ) {
                        Icon(
                            imageVector = if (audioStatus.isReferenceTonePlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (audioStatus.isReferenceTonePlaying) "Stop Spatial Reference Audio"
                            else "Audition Spatial & Cinema Stage"
                        )
                    }
                }
            }

            // Spatial Modes (Virtual 5.1, 7.1, Wide Stereo, Cinema, Concert, Gaming)
            item {
                GlassCard {
                    DotMatrixSectionHeader(
                        title = "3D Spatial & Surround Modes",
                        subtitle = if (audioStatus.isVirtualizerSupported) {
                            "Android Virtualizer Engine Active"
                        } else {
                            "Virtualizer unavailable on current session"
                        },
                        accentActive = theatreState.spatialMode != SpatialMode.OFF,
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SpatialMode.entries.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                pair.forEach { mode ->
                                    val selected = theatreState.spatialMode == mode
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (selected) CrimsonAccent.copy(alpha = 0.16f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                            )
                                            .border(
                                                1.dp,
                                                if (selected) CrimsonAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                                RoundedCornerShape(14.dp),
                                            )
                                            .clickable { onSetSpatialMode(mode) }
                                            .padding(horizontal = 12.dp, vertical = 12.dp)
                                            .testTag("spatial_mode_${mode.name.lowercase()}"),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = mode.channelBadge,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (selected) CrimsonAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = mode.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                        )
                                    }
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Android System Spatializer & Head Tracking Diagnostics
            item {
                GlassCard {
                    DotMatrixSectionHeader(
                        title = "OS Spatializer & Head Tracking",
                        subtitle = audioStatus.immersiveAudioLevelLabel,
                        accentActive = audioStatus.isSystemSpatializerEnabled,
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatusChipTile(
                            title = "OS Spatializer",
                            value = when {
                                audioStatus.isSystemSpatializerEnabled -> "ENABLED"
                                audioStatus.isSystemSpatializerAvailable -> "AVAILABLE"
                                else -> "UNSUPPORTED"
                            },
                            active = audioStatus.isSystemSpatializerEnabled,
                            modifier = Modifier.weight(1f),
                        )
                        StatusChipTile(
                            title = "Head Tracking",
                            value = if (audioStatus.isHeadTrackerAvailable) "SUPPORTED" else "NO SENSOR",
                            active = audioStatus.isHeadTrackerAvailable,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Android Spatializer & Head Tracking are hardware-gated and managed in system sound settings. Virtual 5.1/7.1 modes above use Android Virtualizer without claiming proprietary Dolby Atmos certification.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onOpenSystemSoundSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("open_system_sound_settings_button"),
                    ) {
                        Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Android Sound Settings")
                    }
                }
            }

            // Home Theatre Studio
            item {
                GlassCard(accentGlow = theatreState.cinemaModeEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Movie, contentDescription = null, tint = CrimsonAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Home Theatre Engine", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = if (theatreState.cinemaModeEnabled) "Subwoofer & Stage Active" else "Enable for cinema EQ & room sim",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Switch(
                            checked = theatreState.cinemaModeEnabled,
                            onCheckedChange = onSetCinemaModeEnabled,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CrimsonAccent),
                            modifier = Modifier.testTag("cinema_mode_switch"),
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "MOVIE SOUND PROFILES",
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
                        MovieSoundProfile.entries.forEach { profile ->
                            FilterChip(
                                selected = theatreState.activeMovieProfile == profile,
                                onClick = { onApplyMovieProfile(profile) },
                                label = { Text(profile.title) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CrimsonAccent,
                                    selectedLabelColor = Color.White,
                                ),
                                modifier = Modifier.testTag("movie_profile_${profile.name.lowercase()}"),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "ROOM ACOUSTIC SIMULATION (PRESET REVERB)",
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
                        RoomSimulation.entries.forEach { room ->
                            FilterChip(
                                selected = theatreState.roomSimulation == room,
                                onClick = { onSetRoomSimulation(room) },
                                label = { Text(room.title) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CrimsonAccent,
                                    selectedLabelColor = Color.White,
                                ),
                                modifier = Modifier.testTag("room_sim_${room.name.lowercase()}"),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    StudioSliderRow(
                        label = "Virtual Subwoofer (31Hz–62Hz)",
                        valueText = "${(theatreState.virtualSubwoofer * 100).roundToInt()}%",
                        value = theatreState.virtualSubwoofer,
                        onValueChange = onSetVirtualSubwoofer,
                        testTag = "slider_virtual_subwoofer",
                    )
                    StudioSliderRow(
                        label = "Deep Bass Warmth",
                        valueText = "${(theatreState.deepBass * 100).roundToInt()}%",
                        value = theatreState.deepBass,
                        onValueChange = onSetDeepBass,
                        testTag = "slider_theatre_deep_bass",
                    )
                    StudioSliderRow(
                        label = "Dialogue Enhancement",
                        valueText = "${(theatreState.dialogueEnhancement * 100).roundToInt()}%",
                        value = theatreState.dialogueEnhancement,
                        onValueChange = onSetDialogueEnhancement,
                        testTag = "slider_dialogue_enhancement",
                    )
                    StudioSliderRow(
                        label = "Stereo Widening",
                        valueText = "${(theatreState.stereoWidening * 100).roundToInt()}%",
                        value = theatreState.stereoWidening,
                        onValueChange = onSetStereoWidening,
                        testTag = "slider_stereo_widening",
                    )
                    StudioSliderRow(
                        label = "Theatre Bass Trim",
                        valueText = "${if (theatreState.theatreBassDb >= 0f) "+" else ""}${theatreState.theatreBassDb.roundToInt()} dB",
                        value = theatreState.theatreBassDb,
                        valueRange = -6f..9f,
                        onValueChange = onSetTheatreBassDb,
                        testTag = "slider_theatre_bass_db",
                    )
                    StudioSliderRow(
                        label = "Theatre Treble Trim",
                        valueText = "${if (theatreState.theatreTrebleDb >= 0f) "+" else ""}${theatreState.theatreTrebleDb.roundToInt()} dB",
                        value = theatreState.theatreTrebleDb,
                        valueRange = -6f..9f,
                        onValueChange = onSetTheatreTrebleDb,
                        testTag = "slider_theatre_treble_db",
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChipTile(
    title: String,
    value: String,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .border(
                1.dp,
                if (active) StatusConnected.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                RoundedCornerShape(14.dp),
            )
            .padding(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = if (active) StatusConnected else MaterialTheme.colorScheme.onSurface,
        )
    }
}
