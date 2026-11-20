package com.example.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.data.local.LocalBudsRepository
import com.example.domain.model.AncCapabilityReport
import com.example.domain.model.AncSupportState
import com.example.domain.model.BluetoothDeviceUi
import com.example.domain.model.EarbudVisualStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class BluetoothHardwareState(
    val hasBluetoothHardware: Boolean = true,
    val isBluetoothEnabled: Boolean = false,
    val hasPermissions: Boolean = false,
    val isScanning: Boolean = false,
    val bondedDevices: List<BluetoothDeviceUi> = emptyList(),
    val discoveredDevices: List<BluetoothDeviceUi> = emptyList(),
    val activeDevice: BluetoothDeviceUi? = null,
    val ancReport: AncCapabilityReport = AncCapabilityReport(),
    val statusMessage: String? = null,
)

class BluetoothBudsManager(
    private val context: Context,
    private val localRepository: LocalBudsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var a2dpProxy: BluetoothA2dp? = null
    private var headsetProxy: BluetoothHeadset? = null
    private var activeGatt: BluetoothGatt? = null

    private val _rawBonded = MutableStateFlow<List<BluetoothDeviceUi>>(emptyList())
    private val _rawDiscovered = MutableStateFlow<List<BluetoothDeviceUi>>(emptyList())
    private val _gattBatteries = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _broadcastBatteries = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _isScanning = MutableStateFlow(false)
    private val _isEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled == true)
    private val _hasPermissions = MutableStateFlow(checkBluetoothPermissions())
    private val _statusMessage = MutableStateFlow<String?>(null)

    private val _hardwareState = MutableStateFlow(BluetoothHardwareState())
    val hardwareState: StateFlow<BluetoothHardwareState> = _hardwareState.asStateFlow()

    private val _connectionPopupEvents = MutableSharedFlow<BluetoothDeviceUi>(extraBufferCapacity = 4)
    val connectionPopupEvents: SharedFlow<BluetoothDeviceUi> = _connectionPopupEvents.asSharedFlow()

    private var lastConnectedAddress: String? = null
    private var receiverRegistered = false

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            refreshDevices(triggerPopupOnNewActive = true)
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            refreshDevices(triggerPopupOnNewActive = false)
        }
    }

    private val btReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent == null) return
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    _isEnabled.value = (state == BluetoothAdapter.STATE_ON)
                    refreshDevices(triggerPopupOnNewActive = false)
                }

                BluetoothDevice.ACTION_FOUND -> {
                    if (!checkBluetoothPermissions()) return
                    val device = getParcelableDevice(intent) ?: return
                    if (!isPotentialAudioOrWearable(device)) return
                    val ui = buildDeviceUi(device, isConnected = false, isAudioActive = false)
                    _rawDiscovered.update { current ->
                        if (current.any { it.address == ui.address } ||
                            _rawBonded.value.any { it.address == ui.address }
                        ) {
                            current
                        } else {
                            current + ui
                        }
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isScanning.value = false
                }

                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device = getParcelableDevice(intent)
                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
                    if (bondState == BluetoothDevice.BOND_BONDED && device != null) {
                        _rawDiscovered.update { list -> list.filterNot { it.address == device.address } }
                        _statusMessage.value = "Paired with ${safeDeviceName(device)}"
                    }
                    refreshDevices(triggerPopupOnNewActive = false)
                }

                BluetoothDevice.ACTION_ACL_CONNECTED,
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                    refreshDevices(triggerPopupOnNewActive = true)
                }

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = getParcelableDevice(intent)
                    if (device != null && device.address == lastConnectedAddress) {
                        scope.launch {
                            localRepository.logConnectionEvent(
                                address = device.address,
                                deviceName = safeDeviceName(device),
                                eventType = "DISCONNECTED",
                                batteryPercent = null,
                            )
                        }
                        lastConnectedAddress = null
                    }
                    refreshDevices(triggerPopupOnNewActive = false)
                }

                ACTION_BATTERY_LEVEL_CHANGED -> {
                    val device = getParcelableDevice(intent) ?: return
                    val level = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1)
                    if (level in 0..100) {
                        _broadcastBatteries.update { it + (device.address to level) }
                        refreshDevices(triggerPopupOnNewActive = false)
                    }
                }
            }
        }
    }

    init {
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
        observeCombinedState()
        registerReceiversIfPermitted()
        initProfileProxies()
        refreshDevices(triggerPopupOnNewActive = false)
    }

    private fun observeCombinedState() {
        scope.launch {
            combine(
                _rawBonded,
                _rawDiscovered,
                localRepository.savedDevices,
                _gattBatteries,
                _broadcastBatteries,
            ) { bonded, discovered, savedList, gattBat, bcastBat ->
                val savedMap = savedList.associateBy { it.address }
                val mergedBonded = bonded.map { dev ->
                    val saved = savedMap[dev.address]
                    val visual = saved?.visualStyleName?.let {
                        runCatching { EarbudVisualStyle.valueOf(it) }.getOrNull()
                    } ?: dev.visualStyle
                    val battery = dev.unifiedBatteryPercent
                        ?: bcastBat[dev.address]
                        ?: gattBat[dev.address]
                    val batSource = when {
                        dev.unifiedBatteryPercent != null -> "Android Bluetooth HFP/BAS"
                        bcastBat[dev.address] != null -> "Bluetooth Battery Event"
                        gattBat[dev.address] != null -> "BLE GATT Battery (0x180F)"
                        dev.isConnected -> "Not exposed over standard BT"
                        else -> "Disconnected"
                    }
                    dev.copy(
                        customAlias = saved?.customAlias,
                        visualStyle = visual,
                        unifiedBatteryPercent = battery,
                        batterySourceLabel = batSource,
                        lastConnectedMs = saved?.lastConnectedMs ?: dev.lastConnectedMs,
                    )
                }.sortedWith(
                    compareByDescending<BluetoothDeviceUi> { it.isConnected }
                        .thenByDescending { it.isAudioActive }
                        .thenByDescending { it.lastConnectedMs }
                        .thenBy { it.displayName.lowercase() }
                )

                val active = mergedBonded.firstOrNull { it.isConnected || it.isAudioActive }
                val ancReport = buildAncCapabilityReport(active)

                BluetoothHardwareState(
                    hasBluetoothHardware = bluetoothAdapter != null,
                    isBluetoothEnabled = _isEnabled.value,
                    hasPermissions = _hasPermissions.value,
                    isScanning = _isScanning.value,
                    bondedDevices = mergedBonded,
                    discoveredDevices = discovered,
                    activeDevice = active,
                    ancReport = ancReport,
                    statusMessage = _statusMessage.value,
                )
            }.collect { state ->
                _hardwareState.value = state
            }
        }
    }

    fun onPermissionsUpdated() {
        _hasPermissions.value = checkBluetoothPermissions()
        _isEnabled.value = bluetoothAdapter?.isEnabled == true
        registerReceiversIfPermitted()
        initProfileProxies()
        refreshDevices(triggerPopupOnNewActive = false)
    }

    fun checkBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT,
            ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN,
                ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun registerReceiversIfPermitted() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            addAction(ACTION_BATTERY_LEVEL_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(btReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(btReceiver, filter)
        }
        receiverRegistered = true
    }

    private fun initProfileProxies() {
        val adapter = bluetoothAdapter ?: return
        if (!checkBluetoothPermissions()) return
        runCatching {
            adapter.getProfileProxy(
                context,
                object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                        if (profile == BluetoothProfile.A2DP) {
                            a2dpProxy = proxy as? BluetoothA2dp
                            refreshDevices(triggerPopupOnNewActive = false)
                        }
                    }

                    override fun onServiceDisconnected(profile: Int) {
                        if (profile == BluetoothProfile.A2DP) a2dpProxy = null
                    }
                },
                BluetoothProfile.A2DP,
            )
            adapter.getProfileProxy(
                context,
                object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                        if (profile == BluetoothProfile.HEADSET) {
                            headsetProxy = proxy as? BluetoothHeadset
                            refreshDevices(triggerPopupOnNewActive = false)
                        }
                    }

                    override fun onServiceDisconnected(profile: Int) {
                        if (profile == BluetoothProfile.HEADSET) headsetProxy = null
                    }
                },
                BluetoothProfile.HEADSET,
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshDevices(triggerPopupOnNewActive: Boolean = false) {
        val hasPerm = checkBluetoothPermissions()
        _hasPermissions.value = hasPerm
        _isEnabled.value = bluetoothAdapter?.isEnabled == true

        val adapter = bluetoothAdapter
        if (adapter == null || !hasPerm || !_isEnabled.value) {
            _rawBonded.value = emptyList()
            return
        }

        val activeAudioAddresses = getActiveAudioDeviceAddresses()
        val connectedA2dp = runCatching {
            a2dpProxy?.connectedDevices?.map { it.address }?.toSet() ?: emptySet()
        }.getOrDefault(emptySet())
        val connectedHfp = runCatching {
            headsetProxy?.connectedDevices?.map { it.address }?.toSet() ?: emptySet()
        }.getOrDefault(emptySet())

        val bonded = runCatching { adapter.bondedDevices }.getOrNull().orEmpty()
        val audioBonded = bonded
            .filter { isPotentialAudioOrWearable(it) || it.address in connectedA2dp || it.address in connectedHfp }
            .map { device ->
                val isA2dp = device.address in connectedA2dp
                val isHfp = device.address in connectedHfp
                val isConnectedByReflection = isDeviceConnectedReflection(device)
                val isAudioRouted = device.address in activeAudioAddresses || isA2dp
                val isConnected = isA2dp || isHfp || isConnectedByReflection || isAudioRouted
                buildDeviceUi(
                    device = device,
                    isConnected = isConnected,
                    isAudioActive = isAudioRouted,
                )
            }

        _rawBonded.value = audioBonded

        val newActive = audioBonded.firstOrNull { it.isConnected || it.isAudioActive }
        if (newActive != null) {
            probeBleBatteryIfSupported(newActive.address)
            if (newActive.address != lastConnectedAddress) {
                lastConnectedAddress = newActive.address
                scope.launch {
                    localRepository.updateDeviceMetadata(
                        address = newActive.address,
                        systemName = newActive.systemName,
                        batteryPercent = newActive.unifiedBatteryPercent,
                        touchTimestamp = true,
                    )
                    localRepository.logConnectionEvent(
                        address = newActive.address,
                        deviceName = newActive.displayName,
                        eventType = "CONNECTED",
                        batteryPercent = newActive.unifiedBatteryPercent,
                    )
                }
                if (triggerPopupOnNewActive) {
                    _connectionPopupEvents.tryEmit(newActive)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        val adapter = bluetoothAdapter ?: run {
            _statusMessage.value = "Bluetooth adapter unavailable on this device."
            return
        }
        if (!checkBluetoothPermissions()) {
            _statusMessage.value = "Grant Bluetooth permissions to scan nearby earbuds."
            return
        }
        if (!adapter.isEnabled) {
            _statusMessage.value = "Enable Bluetooth in system settings first."
            return
        }
        _rawDiscovered.value = emptyList()
        if (adapter.isDiscovering) {
            adapter.cancelDiscovery()
        }
        val started = adapter.startDiscovery()
        _isScanning.value = started
        _statusMessage.value = if (started) "Scanning for nearby Bluetooth audio devices..." else "Unable to start discovery."
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        if (!checkBluetoothPermissions()) return
        runCatching { bluetoothAdapter?.cancelDiscovery() }
        _isScanning.value = false
    }

    @SuppressLint("MissingPermission")
    fun pairDevice(address: String) {
        if (!checkBluetoothPermissions()) return
        val device = runCatching { bluetoothAdapter?.getRemoteDevice(address) }.getOrNull() ?: return
        stopDiscovery()
        val initiated = runCatching { device.createBond() }.getOrDefault(false)
        _statusMessage.value = if (initiated) {
            "Pairing request sent to ${safeDeviceName(device)}..."
        } else {
            "Opening Bluetooth settings to complete pairing."
        }
        if (!initiated) openSystemBluetoothSettings()
    }

    @SuppressLint("MissingPermission")
    fun connectOrReconnectDevice(address: String) {
        if (!checkBluetoothPermissions()) return
        val device = runCatching { bluetoothAdapter?.getRemoteDevice(address) }.getOrNull() ?: return
        val connectedViaProxy = runCatching {
            val proxy = a2dpProxy ?: return@runCatching false
            val method = BluetoothA2dp::class.java.getMethod("connect", BluetoothDevice::class.java)
            method.invoke(proxy, device) as? Boolean ?: false
        }.getOrDefault(false)

        if (!connectedViaProxy) {
            _statusMessage.value = "Opening Android Bluetooth settings to connect ${safeDeviceName(device)}."
            openSystemBluetoothSettings()
        } else {
            _statusMessage.value = "Reconnecting to ${safeDeviceName(device)}..."
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnectDevice(address: String) {
        if (!checkBluetoothPermissions()) return
        val device = runCatching { bluetoothAdapter?.getRemoteDevice(address) }.getOrNull() ?: return
        val disconnectedViaProxy = runCatching {
            val proxy = a2dpProxy ?: return@runCatching false
            val method = BluetoothA2dp::class.java.getMethod("disconnect", BluetoothDevice::class.java)
            method.invoke(proxy, device) as? Boolean ?: false
        }.getOrDefault(false)

        if (!disconnectedViaProxy) {
            openSystemBluetoothSettings()
        } else {
            _statusMessage.value = "Disconnecting ${safeDeviceName(device)}..."
        }
    }

    @SuppressLint("MissingPermission")
    fun renameDevice(address: String, newAlias: String) {
        val clean = newAlias.trim()
        val device = if (checkBluetoothPermissions()) {
            runCatching { bluetoothAdapter?.getRemoteDevice(address) }.getOrNull()
        } else null
        val sysName = device?.let { safeDeviceName(it) } ?: address

        if (device != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && checkBluetoothPermissions()) {
            runCatching { device.setAlias(clean.ifBlank { null }) }
        }
        scope.launch {
            localRepository.setDeviceAlias(address = address, systemName = sysName, alias = clean)
            refreshDevices(triggerPopupOnNewActive = false)
        }
    }

    fun setDeviceVisualStyle(address: String, systemName: String, style: EarbudVisualStyle) {
        scope.launch {
            localRepository.updateDeviceMetadata(
                address = address,
                systemName = systemName,
                visualStyle = style,
            )
        }
    }

    fun openSystemBluetoothSettings() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    @SuppressLint("MissingPermission")
    private fun probeBleBatteryIfSupported(address: String) {
        if (!checkBluetoothPermissions()) return
        val device = runCatching { bluetoothAdapter?.getRemoteDevice(address) }.getOrNull() ?: return
        if (device.type != BluetoothDevice.DEVICE_TYPE_LE && device.type != BluetoothDevice.DEVICE_TYPE_DUAL) {
            return
        }
        if (activeGatt?.device?.address == address) return
        runCatching { activeGatt?.close() }
        activeGatt = runCatching {
            device.connectGatt(
                context,
                false,
                object : BluetoothGattCallback() {
                    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                        if (newState == BluetoothProfile.STATE_CONNECTED) {
                            gatt.discoverServices()
                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            gatt.close()
                        }
                    }

                    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                        if (status != BluetoothGatt.GATT_SUCCESS) return
                        val batteryService = gatt.getService(UUID_BATTERY_SERVICE) ?: return
                        val batteryChar = batteryService.getCharacteristic(UUID_BATTERY_LEVEL) ?: return
                        @Suppress("DEPRECATION")
                        gatt.readCharacteristic(batteryChar)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onCharacteristicRead(
                        gatt: BluetoothGatt,
                        characteristic: BluetoothGattCharacteristic,
                        status: Int,
                    ) {
                        if (status == BluetoothGatt.GATT_SUCCESS && characteristic.uuid == UUID_BATTERY_LEVEL) {
                            @Suppress("DEPRECATION")
                            val level = characteristic.value?.firstOrNull()?.toInt()?.and(0xFF)
                            if (level != null && level in 0..100) {
                                _gattBatteries.update { it + (gatt.device.address to level) }
                                refreshDevices(triggerPopupOnNewActive = false)
                            }
                        }
                    }
                },
                BluetoothDevice.TRANSPORT_LE,
            )
        }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    private fun buildDeviceUi(
        device: BluetoothDevice,
        isConnected: Boolean,
        isAudioActive: Boolean,
    ): BluetoothDeviceUi {
        val battery = getBatteryLevelReflection(device)?.takeIf { it in 0..100 }
        val deviceType = when (device.type) {
            BluetoothDevice.DEVICE_TYPE_LE -> "BLE Audio"
            BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual-Mode BT"
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic BT"
            else -> "Bluetooth"
        }
        val majorClass = device.bluetoothClass?.majorDeviceClass
        val profileSummary = when (majorClass) {
            BluetoothClass.Device.Major.AUDIO_VIDEO -> "A2DP / HFP Audio"
            BluetoothClass.Device.Major.WEARABLE -> "Wearable Audio"
            else -> deviceType
        }
        return BluetoothDeviceUi(
            address = device.address,
            systemName = safeDeviceName(device),
            isConnected = isConnected,
            isAudioActive = isAudioActive,
            isBonded = device.bondState == BluetoothDevice.BOND_BONDED,
            isBleCapable = device.type == BluetoothDevice.DEVICE_TYPE_LE ||
                device.type == BluetoothDevice.DEVICE_TYPE_DUAL,
            unifiedBatteryPercent = battery,
            profileSummary = profileSummary,
            deviceTypeLabel = deviceType,
        )
    }

    private fun buildAncCapabilityReport(active: BluetoothDeviceUi?): AncCapabilityReport {
        if (active == null) {
            return AncCapabilityReport(
                state = AncSupportState.UNSUPPORTED_STANDARD_API,
                activeDeviceName = null,
                detectedProfileNotes = "Connect Bluetooth earbuds to inspect supported GATT/Bluetooth profiles. Standard Android APIs do not expose vendor ANC registers.",
            )
        }
        return AncCapabilityReport(
            state = AncSupportState.UNSUPPORTED_STANDARD_API,
            activeDeviceName = active.displayName,
            detectedProfileNotes = "${active.displayName} (${active.profileSummary}) connected via standard Android Bluetooth. ANC, Transparency, Adaptive ANC, and Wind Noise modes require proprietary manufacturer GATT/RFCOMM protocols not exposed by Android's public audio API.",
            supportsAnc = false,
            supportsTransparency = false,
            supportsAdaptiveAnc = false,
            supportsWindReduction = false,
        )
    }

    private fun getActiveAudioDeviceAddresses(): Set<String> {
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .filter {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        (it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                            it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER))
            }
            .mapNotNull { info ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.address else null
            }
            .filter { it.isNotBlank() }
            .toSet()
    }

    @SuppressLint("MissingPermission")
    private fun isPotentialAudioOrWearable(device: BluetoothDevice): Boolean {
        val btClass = device.bluetoothClass ?: return true
        return when (btClass.majorDeviceClass) {
            BluetoothClass.Device.Major.AUDIO_VIDEO,
            BluetoothClass.Device.Major.WEARABLE,
            BluetoothClass.Device.Major.UNCATEGORIZED -> true
            else -> device.type == BluetoothDevice.DEVICE_TYPE_LE ||
                device.type == BluetoothDevice.DEVICE_TYPE_DUAL
        }
    }

    @SuppressLint("MissingPermission")
    private fun safeDeviceName(device: BluetoothDevice): String {
        val alias = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { device.alias }.getOrNull()
        } else null
        return alias?.takeIf { it.isNotBlank() }
            ?: runCatching { device.name }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: device.address
    }

    private fun getBatteryLevelReflection(device: BluetoothDevice): Int? {
        return runCatching {
            val method = BluetoothDevice::class.java.getMethod("getBatteryLevel")
            method.invoke(device) as? Int
        }.getOrNull()
    }

    private fun isDeviceConnectedReflection(device: BluetoothDevice): Boolean {
        return runCatching {
            val method = BluetoothDevice::class.java.getMethod("isConnected")
            method.invoke(device) as? Boolean ?: false
        }.getOrDefault(false)
    }

    @Suppress("DEPRECATION")
    private fun getParcelableDevice(intent: Intent): BluetoothDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }

    fun release() {
        runCatching { audioManager.unregisterAudioDeviceCallback(audioDeviceCallback) }
        if (receiverRegistered) {
            runCatching { context.unregisterReceiver(btReceiver) }
            receiverRegistered = false
        }
        runCatching { activeGatt?.close() }
        a2dpProxy?.let { bluetoothAdapter?.closeProfileProxy(BluetoothProfile.A2DP, it) }
        headsetProxy?.let { bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HEADSET, it) }
    }

    companion object {
        private const val ACTION_BATTERY_LEVEL_CHANGED =
            "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        private const val EXTRA_BATTERY_LEVEL =
            "android.bluetooth.device.extra.BATTERY_LEVEL"
        private val UUID_BATTERY_SERVICE: UUID =
            UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        private val UUID_BATTERY_LEVEL: UUID =
            UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
    }
}
