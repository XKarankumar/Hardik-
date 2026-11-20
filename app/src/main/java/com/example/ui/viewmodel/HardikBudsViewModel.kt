package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HardikBudsApp
import com.example.data.audio.AudioEffectsEngine
import com.example.data.bluetooth.BluetoothHardwareState
import com.example.domain.model.AppSettingsState
import com.example.domain.model.AudioEngineStatus
import com.example.domain.model.BluetoothDeviceUi
import com.example.domain.model.BuiltInPresets
import com.example.domain.model.ConnectionHistoryItem
import com.example.domain.model.EarbudVisualStyle
import com.example.domain.model.EqPreset
import com.example.domain.model.HomeTheatreState
import com.example.domain.model.MovieSoundProfile
import com.example.domain.model.ReferenceToneMode
import com.example.domain.model.RoomSimulation
import com.example.domain.model.SoundStudioState
import com.example.domain.model.SpatialMode
import com.example.domain.model.ThemePreference
import com.example.domain.model.UpdateCheckState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class ConnectionPopupUi(
    val visible: Boolean = false,
    val deviceName: String = "HARDIK BUDS",
    val deviceAddress: String = "",
    val isConnected: Boolean = false,
    val unifiedBattery: Int? = null,
    val batterySourceLabel: String = "",
    val visualStyle: EarbudVisualStyle = EarbudVisualStyle.NOTHING_TRANSPARENT,
    val isPreview: Boolean = false,
)

class HardikBudsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as HardikBudsApp
    private val bluetoothManager = app.bluetoothManager
    private val audioEngine = app.audioEngine
    private val localRepo = app.localRepository
    private val prefsRepo = app.preferencesRepository
    private val updateRepo = app.updateRepository

    val bluetoothState: StateFlow<BluetoothHardwareState> = bluetoothManager.hardwareState
    val audioStatus: StateFlow<AudioEngineStatus> = audioEngine.engineStatus
    val spectrumBins: StateFlow<List<Float>> = audioEngine.spectrumBins

    val appSettings: StateFlow<AppSettingsState> = prefsRepo.appSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettingsState())

    val customProfiles: StateFlow<List<EqPreset>> = localRepo.customProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPresets: StateFlow<List<EqPreset>> = combine(
        MutableStateFlow(BuiltInPresets.defaults),
        customProfiles,
    ) { builtIn, custom -> builtIn + custom }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BuiltInPresets.defaults)

    val connectionHistory: StateFlow<List<ConnectionHistoryItem>> = localRepo.connectionHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _soundStudio = MutableStateFlow(SoundStudioState())
    val soundStudio: StateFlow<SoundStudioState> = combine(
        _soundStudio,
        audioEngine.volumeSnapshot,
    ) { sound, vol ->
        sound.copy(
            volumeRatio = vol.ratio,
            currentVolumeStep = vol.currentStep,
            maxVolumeStep = vol.maxStep,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SoundStudioState())

    private val _homeTheatre = MutableStateFlow(HomeTheatreState())
    val homeTheatre: StateFlow<HomeTheatreState> = _homeTheatre.asStateFlow()

    private val _connectionPopup = MutableStateFlow(ConnectionPopupUi())
    val connectionPopup: StateFlow<ConnectionPopupUi> = _connectionPopup.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateState: StateFlow<UpdateCheckState> = _updateState.asStateFlow()

    private val _previewVisualStyle = MutableStateFlow(EarbudVisualStyle.NOTHING_TRANSPARENT)
    val previewVisualStyle: StateFlow<EarbudVisualStyle> = _previewVisualStyle.asStateFlow()

    init {
        viewModelScope.launch {
            prefsRepo.soundStudioSettings.collect { saved ->
                _soundStudio.value = saved
                audioEngine.applyProcessing(saved, _homeTheatre.value)
            }
        }
        viewModelScope.launch {
            prefsRepo.homeTheatreSettings.collect { saved ->
                _homeTheatre.value = saved
                audioEngine.applyProcessing(_soundStudio.value, saved)
            }
        }
        viewModelScope.launch {
            bluetoothManager.connectionPopupEvents.collect { device ->
                if (appSettings.value.autoShowConnectionPopup) {
                    showConnectionPopupForDevice(device, isPreview = false)
                }
            }
        }
    }

    // --- Bluetooth Actions ---
    fun requiredBluetoothPermissions(): Array<String> = bluetoothManager.requiredPermissions()
    fun onBluetoothPermissionsUpdated() = bluetoothManager.onPermissionsUpdated()
    fun refreshBluetoothDevices() = bluetoothManager.refreshDevices(triggerPopupOnNewActive = false)
    fun startDiscovery() = bluetoothManager.startDiscovery()
    fun stopDiscovery() = bluetoothManager.stopDiscovery()
    fun pairDevice(address: String) = bluetoothManager.pairDevice(address)
    fun connectOrReconnectDevice(address: String) = bluetoothManager.connectOrReconnectDevice(address)
    fun disconnectDevice(address: String) = bluetoothManager.disconnectDevice(address)
    fun renameDevice(address: String, alias: String) = bluetoothManager.renameDevice(address, alias)
    fun openSystemBluetoothSettings() = bluetoothManager.openSystemBluetoothSettings()
    fun clearBluetoothStatusMessage() = bluetoothManager.clearStatusMessage()

    fun setDeviceVisualStyle(style: EarbudVisualStyle) {
        _previewVisualStyle.value = style
        val active = bluetoothState.value.activeDevice
        if (active != null) {
            bluetoothManager.setDeviceVisualStyle(active.address, active.systemName, style)
        }
    }

    fun showConnectionPopupForDevice(device: BluetoothDeviceUi?, isPreview: Boolean) {
        if (device != null) {
            _connectionPopup.value = ConnectionPopupUi(
                visible = true,
                deviceName = device.displayName,
                deviceAddress = device.address,
                isConnected = device.isConnected,
                unifiedBattery = device.unifiedBatteryPercent,
                batterySourceLabel = device.batterySourceLabel,
                visualStyle = device.visualStyle,
                isPreview = isPreview,
            )
        } else {
            _connectionPopup.value = ConnectionPopupUi(
                visible = true,
                deviceName = "HARDIK BUDS",
                deviceAddress = "NO HARDWARE CONNECTED",
                isConnected = false,
                unifiedBattery = null,
                batterySourceLabel = "Connect Bluetooth earbuds for live battery",
                visualStyle = _previewVisualStyle.value,
                isPreview = true,
            )
        }
    }

    fun dismissConnectionPopup() {
        _connectionPopup.update { it.copy(visible = false) }
    }

    // --- Sound Studio Actions ---
    fun updateEqBand(index: Int, valueDb: Float) {
        val current = _soundStudio.value
        val updatedBands = current.eqBandsDb.toMutableList().apply {
            if (index in indices) this[index] = valueDb.coerceIn(-12f, 12f)
        }
        updateSoundStudio(current.copy(eqBandsDb = updatedBands, activePresetId = "custom_unsaved"))
    }

    fun applyPreset(preset: EqPreset) {
        val current = _soundStudio.value
        updateSoundStudio(
            current.copy(
                eqBandsDb = preset.bandsDb,
                activePresetId = preset.id,
                bassBoost = preset.bassBoost,
                trebleBoost = preset.trebleBoost,
                vocalEnhancer = preset.vocalEnhance,
                clarityEnhancer = preset.clarityEnhance,
                masterBypass = false,
            )
        )
    }

    fun saveCustomProfile(name: String) {
        val clean = name.trim()
        if (clean.isBlank()) return
        val current = _soundStudio.value
        val id = "custom_${UUID.randomUUID().toString().take(8)}"
        val preset = EqPreset(
            id = id,
            name = clean,
            bandsDb = current.eqBandsDb,
            bassBoost = current.bassBoost,
            trebleBoost = current.trebleBoost,
            vocalEnhance = current.vocalEnhancer,
            clarityEnhance = current.clarityEnhancer,
            isCustom = true,
        )
        viewModelScope.launch {
            localRepo.saveCustomProfile(preset)
            updateSoundStudio(current.copy(activePresetId = id))
        }
    }

    fun deleteCustomProfile(id: String) {
        viewModelScope.launch {
            localRepo.deleteCustomProfile(id)
            if (_soundStudio.value.activePresetId == id) {
                updateSoundStudio(_soundStudio.value.copy(activePresetId = "custom_unsaved"))
            }
        }
    }

    fun setBassBoost(value: Float) = updateSoundStudio(_soundStudio.value.copy(bassBoost = value.coerceIn(0f, 1f)))
    fun setTrebleBoost(value: Float) = updateSoundStudio(_soundStudio.value.copy(trebleBoost = value.coerceIn(0f, 1f)))
    fun setVocalEnhancer(value: Float) = updateSoundStudio(_soundStudio.value.copy(vocalEnhancer = value.coerceIn(0f, 1f)))
    fun setClarityEnhancer(value: Float) = updateSoundStudio(_soundStudio.value.copy(clarityEnhancer = value.coerceIn(0f, 1f)))
    fun setBalance(value: Float) = updateSoundStudio(_soundStudio.value.copy(balance = value.coerceIn(-1f, 1f)))
    fun setMasterBypass(bypass: Boolean) = updateSoundStudio(_soundStudio.value.copy(masterBypass = bypass))
    fun setMediaVolumeRatio(ratio: Float) = audioEngine.setMediaVolumeRatio(ratio)

    fun toggleReferenceTone(mode: ReferenceToneMode? = null) = audioEngine.toggleReferenceTone(mode)
    fun setReferenceToneMode(mode: ReferenceToneMode) = audioEngine.setReferenceToneMode(mode)

    private fun updateSoundStudio(newState: SoundStudioState) {
        _soundStudio.value = newState
        audioEngine.applyProcessing(newState, _homeTheatre.value)
        viewModelScope.launch { prefsRepo.saveSoundStudio(newState) }
    }

    // --- Cinema & Spatial Actions ---
    fun setSpatialMode(mode: SpatialMode) {
        updateHomeTheatre(_homeTheatre.value.copy(spatialMode = mode))
    }

    fun setRoomSimulation(room: RoomSimulation) {
        updateHomeTheatre(_homeTheatre.value.copy(roomSimulation = room))
    }

    fun setCinemaModeEnabled(enabled: Boolean) {
        updateHomeTheatre(_homeTheatre.value.copy(cinemaModeEnabled = enabled))
    }

    fun applyMovieProfile(profile: MovieSoundProfile) {
        updateHomeTheatre(
            _homeTheatre.value.copy(
                cinemaModeEnabled = true,
                activeMovieProfile = profile,
                spatialMode = if (_homeTheatre.value.spatialMode == SpatialMode.OFF) {
                    SpatialMode.CINEMA
                } else {
                    _homeTheatre.value.spatialMode
                },
                virtualSubwoofer = profile.subwooferBoost,
                dialogueEnhancement = profile.dialogueBoost,
                stereoWidening = profile.stereoWidth,
                theatreBassDb = profile.bassDb,
                theatreTrebleDb = profile.trebleDb,
            )
        )
    }

    fun setVirtualSubwoofer(value: Float) = updateHomeTheatre(
        _homeTheatre.value.copy(virtualSubwoofer = value.coerceIn(0f, 1f), activeMovieProfile = null)
    )

    fun setDeepBass(value: Float) = updateHomeTheatre(
        _homeTheatre.value.copy(deepBass = value.coerceIn(0f, 1f), activeMovieProfile = null)
    )

    fun setDialogueEnhancement(value: Float) = updateHomeTheatre(
        _homeTheatre.value.copy(dialogueEnhancement = value.coerceIn(0f, 1f), activeMovieProfile = null)
    )

    fun setStereoWidening(value: Float) = updateHomeTheatre(
        _homeTheatre.value.copy(stereoWidening = value.coerceIn(0f, 1f), activeMovieProfile = null)
    )

    fun setTheatreBassDb(value: Float) = updateHomeTheatre(
        _homeTheatre.value.copy(theatreBassDb = value.coerceIn(-6f, 9f), activeMovieProfile = null)
    )

    fun setTheatreTrebleDb(value: Float) = updateHomeTheatre(
        _homeTheatre.value.copy(theatreTrebleDb = value.coerceIn(-6f, 9f), activeMovieProfile = null)
    )

    fun openSystemSoundSettings() = audioEngine.openSystemSoundSettings()

    private fun updateHomeTheatre(newState: HomeTheatreState) {
        _homeTheatre.value = newState
        audioEngine.applyProcessing(_soundStudio.value, newState)
        viewModelScope.launch { prefsRepo.saveHomeTheatre(newState) }
    }

    // --- Settings & Auto Update Actions ---
    fun setThemePreference(theme: ThemePreference) {
        saveAppSettings(appSettings.value.copy(themePreference = theme))
    }

    fun setHighRefreshMotion(enabled: Boolean) {
        saveAppSettings(appSettings.value.copy(highRefreshMotion = enabled))
    }

    fun setAutoShowConnectionPopup(enabled: Boolean) {
        saveAppSettings(appSettings.value.copy(autoShowConnectionPopup = enabled))
    }

    fun setGitHubRepository(owner: String, repo: String) {
        saveAppSettings(appSettings.value.copy(githubOwner = owner, githubRepo = repo))
    }

    private fun saveAppSettings(updated: AppSettingsState) {
        viewModelScope.launch { prefsRepo.saveAppSettings(updated) }
    }

    fun checkForUpdates() {
        val currentSettings = appSettings.value
        _updateState.value = UpdateCheckState.Checking
        viewModelScope.launch {
            _updateState.value = updateRepo.checkForUpdates(
                owner = currentSettings.githubOwner,
                repo = currentSettings.githubRepo,
            )
        }
    }

    fun downloadUpdateApk(url: String, version: String, fileName: String?) {
        viewModelScope.launch {
            updateRepo.downloadReleaseApk(url, version, fileName).collect { state ->
                _updateState.value = state
            }
        }
    }

    fun canRequestPackageInstalls(): Boolean = updateRepo.canRequestPackageInstalls()
    fun buildManageUnknownSourcesIntent(): Intent = updateRepo.buildManageUnknownSourcesIntent()
    fun buildInstallApkIntent(file: File): Intent = updateRepo.buildInstallApkIntent(file)

    fun refreshInstallPermissionAfterSettings() {
        val current = _updateState.value
        if (current is UpdateCheckState.ReadyToInstall) {
            _updateState.value = current.copy(
                requiresPermissionGrant = !updateRepo.canRequestPackageInstalls()
            )
        }
    }

    fun dismissUpdateDialog() {
        _updateState.value = UpdateCheckState.Idle
    }

    fun resetAllData() {
        viewModelScope.launch {
            prefsRepo.resetAll()
            localRepo.clearAll()
            val cleanSound = SoundStudioState()
            val cleanTheatre = HomeTheatreState()
            _soundStudio.value = cleanSound
            _homeTheatre.value = cleanTheatre
            audioEngine.applyProcessing(cleanSound, cleanTheatre)
        }
    }
}
