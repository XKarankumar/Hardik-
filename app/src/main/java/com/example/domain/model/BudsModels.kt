package com.example.domain.model

import java.io.File

enum class ThemePreference(val label: String) {
    SYSTEM("SYSTEM"),
    DARK("OBSIDIAN"),
    LIGHT("ALABASTER"),
}

enum class EarbudVisualStyle(
    val displayName: String,
    val brandTag: String,
    val subtitle: String,
) {
    NOTHING_TRANSPARENT(
        displayName = "Transparent Zero",
        brandTag = "HARDIK // DOT-01",
        subtitle = "Smoke transparent shell & red LED",
    ),
    PEARL_STEM_PRO(
        displayName = "Pearl Stem Pro",
        brandTag = "PRO // TITANIUM",
        subtitle = "Ceramic pearl & matte capsule",
    ),
    OBSIDIAN_STUDIO(
        displayName = "Obsidian Studio",
        brandTag = "STUDIO // MATTE",
        subtitle = "Stealth carbon & brushed alloy",
    ),
    CRIMSON_PULSE(
        displayName = "Crimson Pulse",
        brandTag = "LTD // CRIMSON",
        subtitle = "Matte black with crimson core",
    ),
}

enum class AncSupportState {
    UNSUPPORTED_STANDARD_API,
    SUPPORTED_VENDOR_BRIDGE,
}

data class AncCapabilityReport(
    val state: AncSupportState = AncSupportState.UNSUPPORTED_STANDARD_API,
    val activeDeviceName: String? = null,
    val detectedProfileNotes: String = "No Bluetooth earbuds connected.",
    val supportsAnc: Boolean = false,
    val supportsTransparency: Boolean = false,
    val supportsAdaptiveAnc: Boolean = false,
    val supportsWindReduction: Boolean = false,
)

data class BluetoothDeviceUi(
    val address: String,
    val systemName: String,
    val customAlias: String? = null,
    val visualStyle: EarbudVisualStyle = EarbudVisualStyle.NOTHING_TRANSPARENT,
    val isConnected: Boolean = false,
    val isAudioActive: Boolean = false,
    val isBonded: Boolean = false,
    val isBleCapable: Boolean = false,
    val unifiedBatteryPercent: Int? = null,
    val leftBatteryPercent: Int? = null,
    val rightBatteryPercent: Int? = null,
    val caseBatteryPercent: Int? = null,
    val batterySourceLabel: String = "Not reported by device",
    val profileSummary: String = "A2DP / HFP",
    val deviceTypeLabel: String = "Bluetooth Audio",
    val lastConnectedMs: Long = 0L,
) {
    val displayName: String
        get() = customAlias?.takeIf { it.isNotBlank() } ?: systemName.ifBlank { address }
}

data class ConnectionHistoryItem(
    val id: Long = 0L,
    val deviceAddress: String,
    val deviceName: String,
    val eventType: String,
    val batteryAtEvent: Int?,
    val timestampMs: Long,
)

data class EqPreset(
    val id: String,
    val name: String,
    val bandsDb: List<Float>, // 10 bands: 31, 62, 125, 250, 500, 1k, 2k, 4k, 8k, 16k Hz
    val bassBoost: Float = 0f,
    val trebleBoost: Float = 0f,
    val vocalEnhance: Float = 0f,
    val clarityEnhance: Float = 0f,
    val isCustom: Boolean = false,
)

enum class SpatialMode(
    val title: String,
    val channelBadge: String,
    val description: String,
    val virtualizerStrength: Int,
) {
    OFF("Stereo Direct", "2.0", "Unprocessed stereo output path", 0),
    WIDE_STEREO("Wide Stereo", "2.0+", "Expanded stereo soundstage width", 420),
    VIRTUAL_5_1("Virtual 5.1", "5.1V", "Simulated 5.1 surround virtualizer", 720),
    VIRTUAL_7_1("Virtual 7.1", "7.1V", "Full 360-degree 7.1 virtual stage", 940),
    CINEMA("Cinema Spatial", "CINE", "Deep theater stage & LFE presence", 860),
    CONCERT_HALL("Concert Hall", "HALL", "Expansive acoustic hall reflections", 780),
    GAMING_SPATIAL("Gaming Spatial", "GAME", "Precision directional cues & footsteps", 680),
}

enum class RoomSimulation(
    val title: String,
    val presetId: Short,
) {
    NONE("Dry Studio", 0),
    SMALL_ROOM("Listening Room", 1),
    MEDIUM_HALL("Cinema Room", 3),
    LARGE_HALL("Concert Hall", 4),
    PLATE_ARENA("Wide Arena", 6),
}

enum class MovieSoundProfile(
    val title: String,
    val subtitle: String,
    val subwooferBoost: Float,
    val dialogueBoost: Float,
    val stereoWidth: Float,
    val bassDb: Float,
    val trebleDb: Float,
) {
    BALANCED_CINEMA("Cinema Standard", "Balanced LFE & clear stage", 0.55f, 0.40f, 0.65f, 3.0f, 1.5f),
    ACTION_THUNDER("Action & LFE", "Deep virtual subwoofer punch", 0.90f, 0.25f, 0.80f, 7.0f, 2.5f),
    DIALOGUE_CLEAR("Dialogue Focus", "Crisp 1kHz-4kHz speech clarity", 0.25f, 0.85f, 0.45f, 0.5f, 3.0f),
    LATE_NIGHT("Night Dynamic", "Controlled peaks, clear whispers", 0.20f, 0.70f, 0.40f, -1.5f, 2.0f),
    CONCERT_LIVE("Live Symphony", "Expansive stereo & airy highs", 0.50f, 0.30f, 0.92f, 2.5f, 4.5f),
}

data class AudioEngineStatus(
    val engineName: String = "Initializing...",
    val sessionScope: String = "Session 0 (Global Mix)",
    val bandCount: Int = 10,
    val isDynamicsProcessingActive: Boolean = false,
    val isLegacyEqActive: Boolean = false,
    val isVirtualizerSupported: Boolean = false,
    val isReverbSupported: Boolean = false,
    val isSystemSpatializerAvailable: Boolean = false,
    val isSystemSpatializerEnabled: Boolean = false,
    val isHeadTrackerAvailable: Boolean = false,
    val immersiveAudioLevelLabel: String = "Stereo / Unsupported",
    val isReferenceTonePlaying: Boolean = false,
    val referenceTrackMode: ReferenceToneMode = ReferenceToneMode.AMBIENT_CHORD,
    val lastError: String? = null,
)

enum class ReferenceToneMode(val label: String) {
    AMBIENT_CHORD("Harmonic Pad"),
    SUB_BASS_CINEMA("Cinema Sub"),
    VOCAL_PRESENCE("Mid/Vocal"),
}

data class SoundStudioState(
    val eqBandsDb: List<Float> = List(10) { 0f },
    val activePresetId: String = "flat",
    val bassBoost: Float = 0f,         // 0f..1f
    val trebleBoost: Float = 0f,       // 0f..1f
    val vocalEnhancer: Float = 0f,     // 0f..1f
    val clarityEnhancer: Float = 0f,   // 0f..1f
    val balance: Float = 0f,           // -1f (Left) .. +1f (Right)
    val volumeRatio: Float = 0.5f,     // 0f..1f mapped to AudioManager STREAM_MUSIC
    val currentVolumeStep: Int = 7,
    val maxVolumeStep: Int = 15,
    val masterBypass: Boolean = false,
)

data class HomeTheatreState(
    val spatialMode: SpatialMode = SpatialMode.OFF,
    val roomSimulation: RoomSimulation = RoomSimulation.NONE,
    val activeMovieProfile: MovieSoundProfile? = null,
    val cinemaModeEnabled: Boolean = false,
    val virtualSubwoofer: Float = 0.35f,     // 0f..1f
    val deepBass: Float = 0.30f,             // 0f..1f
    val dialogueEnhancement: Float = 0.25f,  // 0f..1f
    val stereoWidening: Float = 0.40f,       // 0f..1f
    val theatreBassDb: Float = 0f,           // -6f..+9f
    val theatreTrebleDb: Float = 0f,         // -6f..+9f
)

data class AppSettingsState(
    val themePreference: ThemePreference = ThemePreference.DARK,
    val highRefreshMotion: Boolean = true,
    val autoShowConnectionPopup: Boolean = true,
    val githubOwner: String = "hardik-buds",
    val githubRepo: String = "hardik-buds-android",
)

sealed interface UpdateCheckState {
    data object Idle : UpdateCheckState
    data object Checking : UpdateCheckState
    data class UpToDate(val currentVersion: String) : UpdateCheckState
    data class UpdateAvailable(
        val currentVersion: String,
        val latestVersion: String,
        val releaseName: String,
        val changelog: String,
        val apkDownloadUrl: String?,
        val apkFileName: String?,
        val apkSizeBytes: Long?,
        val htmlUrl: String,
    ) : UpdateCheckState
    data class Downloading(
        val latestVersion: String,
        val progressPercent: Int,
    ) : UpdateCheckState
    data class ReadyToInstall(
        val latestVersion: String,
        val apkFile: File,
        val requiresPermissionGrant: Boolean,
    ) : UpdateCheckState
    data class Error(val message: String) : UpdateCheckState
}

object BuiltInPresets {
    val freqLabels = listOf("31", "62", "125", "250", "500", "1K", "2K", "4K", "8K", "16K")
    val freqHz = listOf(31.25f, 62.5f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)

    val defaults: List<EqPreset> = listOf(
        EqPreset("flat", "Flat", listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
        EqPreset("pop", "Pop", listOf(1.5f, 2.5f, 1.5f, 0f, -1f, 0.5f, 2.5f, 3.5f, 3f, 2f), bassBoost = 0.25f, clarityEnhance = 0.2f),
        EqPreset("rock", "Rock", listOf(4f, 3.5f, 2f, -1f, -2f, 0f, 2.5f, 4f, 4.5f, 3.5f), bassBoost = 0.35f, trebleBoost = 0.3f),
        EqPreset("edm", "EDM", listOf(5.5f, 5f, 3.5f, 0.5f, -1.5f, 0f, 2f, 3.5f, 5f, 4.5f), bassBoost = 0.65f, trebleBoost = 0.4f),
        EqPreset("classical", "Classical", listOf(2f, 1.5f, 0.5f, 0f, 0f, 0.5f, 1.5f, 2.5f, 3.5f, 4f), clarityEnhance = 0.35f),
        EqPreset("jazz", "Jazz", listOf(2.5f, 2f, 1f, 2f, -1f, 1.5f, 2.5f, 2f, 2.5f, 3f), vocalEnhance = 0.25f),
        EqPreset("cinema", "Cinema", listOf(5f, 4f, 2.5f, 0.5f, -0.5f, 2f, 3.5f, 3f, 2.5f, 2f), bassBoost = 0.5f, vocalEnhance = 0.45f),
        EqPreset("gaming", "Gaming", listOf(2f, 1f, -1f, -2f, 0f, 2.5f, 4.5f, 5.5f, 4f, 3f), clarityEnhance = 0.6f, trebleBoost = 0.35f),
        EqPreset("deep_bass", "Deep Bass", listOf(7.5f, 7f, 5f, 2f, 0f, -0.5f, 0f, 1f, 1.5f, 1.5f), bassBoost = 0.85f),
    )
}
