package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.example.domain.model.AudioEngineStatus
import com.example.domain.model.BuiltInPresets
import com.example.domain.model.EqPreset
import com.example.domain.model.SoundStudioState
import com.example.ui.components.DotMatrixSectionHeader
import com.example.ui.components.GlassCard
import com.example.ui.theme.CrimsonAccent
import kotlin.math.roundToInt

@Composable
fun SoundStudioScreen(
    soundState: SoundStudioState,
    audioStatus: AudioEngineStatus,
    presets: List<EqPreset>,
    onUpdateBand: (Int, Float) -> Unit,
    onSelectPreset: (EqPreset) -> Unit,
    onSaveCustomProfile: (String) -> Unit,
    onDeleteCustomProfile: (String) -> Unit,
    onSetBassBoost: (Float) -> Unit,
    onSetTrebleBoost: (Float) -> Unit,
    onSetVocalEnhancer: (Float) -> Unit,
    onSetClarityEnhancer: (Float) -> Unit,
    onSetBalance: (Float) -> Unit,
    onSetVolumeRatio: (Float) -> Unit,
    onSetMasterBypass: (Boolean) -> Unit,
    onToggleReferenceTone: () -> Unit,
    contentPadding: PaddingValues,
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var customProfileName by remember { mutableStateOf("") }

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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SOUND STUDIO",
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        Text(
                            text = audioStatus.engineName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onToggleReferenceTone,
                            modifier = Modifier.testTag("sound_audition_toggle"),
                        ) {
                            Icon(
                                imageVector = if (audioStatus.isReferenceTonePlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                contentDescription = "Toggle DSP reference audio",
                                tint = if (audioStatus.isReferenceTonePlaying) CrimsonAccent else MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        Switch(
                            checked = !soundState.masterBypass,
                            onCheckedChange = { onSetMasterBypass(!it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CrimsonAccent),
                            modifier = Modifier.testTag("sound_bypass_switch"),
                        )
                    }
                }
            }

            // Presets + Save Custom Profile
            item {
                GlassCard {
                    DotMatrixSectionHeader(
                        title = "Audio Profiles",
                        subtitle = "Built-in studio curves & saved user profiles",
                        trailing = {
                            OutlinedButton(
                                onClick = { showSaveDialog = true },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("save_eq_profile_button"),
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save")
                            }
                        },
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        presets.forEach { preset ->
                            val selected = soundState.activePresetId == preset.id
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectPreset(preset) },
                                label = { Text(preset.name) },
                                trailingIcon = if (preset.isCustom) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Delete ${preset.name}",
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { onDeleteCustomProfile(preset.id) },
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CrimsonAccent,
                                    selectedLabelColor = Color.White,
                                    selectedTrailingIconColor = Color.White,
                                ),
                                modifier = Modifier.testTag("eq_preset_${preset.id}"),
                            )
                        }
                    }
                }
            }

            // 10-Band Equalizer
            item {
                GlassCard(accentGlow = !soundState.masterBypass) {
                    DotMatrixSectionHeader(
                        title = "10-Band Parametric Equalizer",
                        subtitle = audioStatus.sessionScope,
                        trailing = {
                            IconButton(
                                onClick = { onSelectPreset(BuiltInPresets.defaults.first()) },
                                modifier = Modifier.testTag("reset_eq_flat_button"),
                            ) {
                                Icon(Icons.Filled.RestartAlt, contentDescription = "Reset EQ to Flat")
                            }
                        },
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    EqResponseCurveCanvas(
                        bandsDb = soundState.eqBandsDb,
                        enabled = !soundState.masterBypass,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        soundState.eqBandsDb.forEachIndexed { idx, valueDb ->
                            EqBandColumn(
                                freqLabel = BuiltInPresets.freqLabels[idx],
                                gainDb = valueDb,
                                enabled = !soundState.masterBypass,
                                onChange = { onUpdateBand(idx, it) },
                                modifier = Modifier.testTag("eq_band_$idx"),
                            )
                        }
                    }
                }
            }

            // Dynamic Boosters & Enhancements
            item {
                GlassCard {
                    DotMatrixSectionHeader(
                        title = "Acoustic Enhancements",
                        subtitle = "Real-time DynamicsProcessing / AudioFX stages",
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    StudioSliderRow(
                        label = "Bass Booster",
                        valueText = "${(soundState.bassBoost * 100).roundToInt()}%",
                        value = soundState.bassBoost,
                        onValueChange = onSetBassBoost,
                        testTag = "slider_bass_boost",
                    )
                    StudioSliderRow(
                        label = "Treble Booster",
                        valueText = "${(soundState.trebleBoost * 100).roundToInt()}%",
                        value = soundState.trebleBoost,
                        onValueChange = onSetTrebleBoost,
                        testTag = "slider_treble_boost",
                    )
                    StudioSliderRow(
                        label = "Vocal Enhancer",
                        valueText = "${(soundState.vocalEnhancer * 100).roundToInt()}%",
                        value = soundState.vocalEnhancer,
                        onValueChange = onSetVocalEnhancer,
                        testTag = "slider_vocal_enhancer",
                    )
                    StudioSliderRow(
                        label = "Clarity Enhancement (MBC)",
                        valueText = "${(soundState.clarityEnhancer * 100).roundToInt()}%",
                        value = soundState.clarityEnhancer,
                        onValueChange = onSetClarityEnhancer,
                        testTag = "slider_clarity_enhancer",
                    )
                }
            }

            // Balance & System Volume
            item {
                GlassCard {
                    DotMatrixSectionHeader(
                        title = "Channel Balance & Output Volume",
                        subtitle = "Per-channel gain & Android STREAM_MUSIC",
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val balPct = (soundState.balance * 100).roundToInt()
                    val balText = when {
                        balPct < -2 -> "L ${-balPct}%"
                        balPct > 2 -> "R $balPct%"
                        else -> "CENTER"
                    }
                    StudioSliderRow(
                        label = "Left / Right Balance",
                        valueText = balText,
                        value = soundState.balance,
                        valueRange = -1f..1f,
                        onValueChange = onSetBalance,
                        testTag = "slider_lr_balance",
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VolumeUp,
                            contentDescription = "Media Volume",
                            tint = CrimsonAccent,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Media Volume",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${soundState.currentVolumeStep} / ${soundState.maxVolumeStep}",
                            style = MaterialTheme.typography.labelMedium,
                            color = CrimsonAccent,
                        )
                    }
                    Slider(
                        value = soundState.volumeRatio,
                        onValueChange = onSetVolumeRatio,
                        colors = SliderDefaults.colors(
                            thumbColor = CrimsonAccent,
                            activeTrackColor = CrimsonAccent,
                        ),
                        modifier = Modifier.testTag("slider_media_volume"),
                    )
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Custom EQ Profile") },
            text = {
                OutlinedTextField(
                    value = customProfileName,
                    onValueChange = { customProfileName = it },
                    label = { Text("Profile Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_profile_name_input"),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveCustomProfile(customProfileName)
                        customProfileName = ""
                        showSaveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent, contentColor = Color.White),
                    modifier = Modifier.testTag("confirm_save_profile_button"),
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun EqResponseCurveCanvas(
    bandsDb: List<Float>,
    enabled: Boolean,
) {
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val lineColor = if (enabled) CrimsonAccent else MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        val midY = size.height / 2f
        // Zero dB baseline + +/-6dB guides
        drawLine(gridColor, Offset(0f, midY), Offset(size.width, midY), strokeWidth = 1.5f)
        drawLine(gridColor.copy(alpha = 0.25f), Offset(0f, size.height * 0.25f), Offset(size.width, size.height * 0.25f))
        drawLine(gridColor.copy(alpha = 0.25f), Offset(0f, size.height * 0.75f), Offset(size.width, size.height * 0.75f))

        val points = bandsDb.mapIndexed { i, db ->
            val x = (i.toFloat() / (bandsDb.size - 1).coerceAtLeast(1)) * size.width
            val norm = ((12f - db.coerceIn(-12f, 12f)) / 24f)
            Offset(x, norm * size.height)
        }

        val strokePath = Path()
        val fillPath = Path()
        points.forEachIndexed { idx, pt ->
            if (idx == 0) {
                strokePath.moveTo(pt.x, pt.y)
                fillPath.moveTo(pt.x, size.height)
                fillPath.lineTo(pt.x, pt.y)
            } else {
                val prev = points[idx - 1]
                val cx = (prev.x + pt.x) / 2f
                strokePath.cubicTo(cx, prev.y, cx, pt.y, pt.x, pt.y)
                fillPath.cubicTo(cx, prev.y, cx, pt.y, pt.x, pt.y)
            }
        }
        fillPath.lineTo(points.last().x, size.height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.28f), Color.Transparent),
            ),
        )
        drawPath(
            path = strokePath,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
        )
        points.forEach { pt ->
            drawCircle(color = lineColor, radius = 4.dp.toPx(), center = pt)
        }
    }
}

@Composable
private fun EqBandColumn(
    freqLabel: String,
    gainDb: Float,
    enabled: Boolean,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "${if (gainDb >= 0f) "+" else ""}${gainDb.roundToInt()}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (enabled && gainDb != 0f) CrimsonAccent else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .height(148.dp)
                .width(44.dp),
            contentAlignment = Alignment.Center,
        ) {
            VerticalSlider(
                value = gainDb,
                valueRange = -12f..12f,
                enabled = enabled,
                onValueChange = onChange,
            )
        }
        Text(
            text = freqLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun VerticalSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
) {
    Layout(
        content = {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                enabled = enabled,
                colors = SliderDefaults.colors(
                    thumbColor = CrimsonAccent,
                    activeTrackColor = CrimsonAccent,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline,
                ),
                modifier = Modifier.graphicsLayer { rotationZ = -90f },
            )
        }
    ) { measurables, constraints ->
        val rotatedConstraints = Constraints(
            minWidth = constraints.minHeight,
            maxWidth = constraints.maxHeight,
            minHeight = constraints.minWidth,
            maxHeight = constraints.maxWidth,
        )
        val placeable = measurables.first().measure(rotatedConstraints)
        layout(placeable.height, placeable.width) {
            placeable.place(
                x = -(placeable.width - placeable.height) / 2,
                y = (placeable.width - placeable.height) / 2,
            )
        }
    }
}

@Composable
fun StudioSliderRow(
    label: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    testTag: String,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                color = CrimsonAccent,
                fontWeight = FontWeight.Bold,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = CrimsonAccent,
                activeTrackColor = CrimsonAccent,
                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
            ),
            modifier = Modifier.testTag(testTag),
        )
    }
}
