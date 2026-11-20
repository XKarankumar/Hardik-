package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.AppleStyleConnectionPopup
import com.example.ui.components.dotMatrixBackground
import com.example.ui.navigation.CinemaRoute
import com.example.ui.navigation.DevicesRoute
import com.example.ui.navigation.HomeRoute
import com.example.ui.navigation.SettingsRoute
import com.example.ui.navigation.SoundRoute
import com.example.ui.screens.CinemaSpatialScreen
import com.example.ui.screens.DevicesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SoundStudioScreen
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.HardikBudsTheme
import com.example.ui.viewmodel.HardikBudsViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val useRail = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium
            val vm: HardikBudsViewModel = viewModel()
            HardikBudsRoot(
                viewModel = vm,
                useNavigationRail = useRail,
                onLaunchIntent = { intent -> runCatching { startActivity(intent) } },
            )
        }
    }
}

private data class NavTabItem(
    val route: Any,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

@Composable
fun HardikBudsRoot(
    viewModel: HardikBudsViewModel,
    useNavigationRail: Boolean,
    onLaunchIntent: (android.content.Intent) -> Unit,
) {
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val bluetoothState by viewModel.bluetoothState.collectAsStateWithLifecycle()
    val audioStatus by viewModel.audioStatus.collectAsStateWithLifecycle()
    val soundState by viewModel.soundStudio.collectAsStateWithLifecycle()
    val theatreState by viewModel.homeTheatre.collectAsStateWithLifecycle()
    val presets by viewModel.allPresets.collectAsStateWithLifecycle()
    val history by viewModel.connectionHistory.collectAsStateWithLifecycle()
    val spectrumBins by viewModel.spectrumBins.collectAsStateWithLifecycle()
    val connectionPopup by viewModel.connectionPopup.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val previewVisualStyle by viewModel.previewVisualStyle.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        viewModel.onBluetoothPermissionsUpdated()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onBluetoothPermissionsUpdated()
                viewModel.refreshInstallPermissionAfterSettings()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    val tabs = listOf(
        NavTabItem(HomeRoute, "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
        NavTabItem(SoundRoute, "Sound", Icons.Filled.GraphicEq, Icons.Outlined.GraphicEq, "nav_sound"),
        NavTabItem(CinemaRoute, "Cinema", Icons.Filled.Movie, Icons.Outlined.Movie, "nav_cinema"),
        NavTabItem(DevicesRoute, "Devices", Icons.Filled.Bluetooth, Icons.Outlined.Bluetooth, "nav_devices"),
        NavTabItem(SettingsRoute, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings"),
    )

    fun navigateToTab(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    HardikBudsTheme(
        themePreference = appSettings.themePreference,
        highRefreshMotion = appSettings.highRefreshMotion,
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .dotMatrixBackground(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        windowInsets = WindowInsets.navigationBars,
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentDest?.hasRoute(tab.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = CrimsonAccent,
                                    indicatorColor = CrimsonAccent,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                                modifier = Modifier.testTag(tab.testTag),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(modifier = Modifier.fillMaxSize()) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentDest?.hasRoute(tab.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = CrimsonAccent,
                                    indicatorColor = CrimsonAccent,
                                ),
                                modifier = Modifier.testTag(tab.testTag),
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = HomeRoute,
                    ) {
                        composable<HomeRoute> {
                            HomeScreen(
                                bluetoothState = bluetoothState,
                                audioStatus = audioStatus,
                                soundState = soundState,
                                theatreState = theatreState,
                                spectrumBins = spectrumBins,
                                previewVisualStyle = previewVisualStyle,
                                onSelectVisualStyle = viewModel::setDeviceVisualStyle,
                                onPreviewConnectionPopup = {
                                    viewModel.showConnectionPopupForDevice(
                                        bluetoothState.activeDevice,
                                        isPreview = bluetoothState.activeDevice == null,
                                    )
                                },
                                onToggleMasterBypass = viewModel::setMasterBypass,
                                onToggleReferenceTone = { viewModel.toggleReferenceTone() },
                                onSelectReferenceMode = viewModel::setReferenceToneMode,
                                onNavigateToSound = { navigateToTab(SoundRoute) },
                                onNavigateToCinema = { navigateToTab(CinemaRoute) },
                                onNavigateToDevices = { navigateToTab(DevicesRoute) },
                                onRequestBluetoothPermissions = {
                                    permissionLauncher.launch(viewModel.requiredBluetoothPermissions())
                                },
                                contentPadding = innerPadding,
                            )
                        }

                        composable<SoundRoute> {
                            SoundStudioScreen(
                                soundState = soundState,
                                audioStatus = audioStatus,
                                presets = presets,
                                onUpdateBand = viewModel::updateEqBand,
                                onSelectPreset = viewModel::applyPreset,
                                onSaveCustomProfile = viewModel::saveCustomProfile,
                                onDeleteCustomProfile = viewModel::deleteCustomProfile,
                                onSetBassBoost = viewModel::setBassBoost,
                                onSetTrebleBoost = viewModel::setTrebleBoost,
                                onSetVocalEnhancer = viewModel::setVocalEnhancer,
                                onSetClarityEnhancer = viewModel::setClarityEnhancer,
                                onSetBalance = viewModel::setBalance,
                                onSetVolumeRatio = viewModel::setMediaVolumeRatio,
                                onSetMasterBypass = viewModel::setMasterBypass,
                                onToggleReferenceTone = { viewModel.toggleReferenceTone() },
                                contentPadding = innerPadding,
                            )
                        }

                        composable<CinemaRoute> {
                            CinemaSpatialScreen(
                                theatreState = theatreState,
                                audioStatus = audioStatus,
                                spectrumBins = spectrumBins,
                                onSetSpatialMode = viewModel::setSpatialMode,
                                onSetRoomSimulation = viewModel::setRoomSimulation,
                                onSetCinemaModeEnabled = viewModel::setCinemaModeEnabled,
                                onApplyMovieProfile = viewModel::applyMovieProfile,
                                onSetVirtualSubwoofer = viewModel::setVirtualSubwoofer,
                                onSetDeepBass = viewModel::setDeepBass,
                                onSetDialogueEnhancement = viewModel::setDialogueEnhancement,
                                onSetStereoWidening = viewModel::setStereoWidening,
                                onSetTheatreBassDb = viewModel::setTheatreBassDb,
                                onSetTheatreTrebleDb = viewModel::setTheatreTrebleDb,
                                onToggleReferenceTone = { viewModel.toggleReferenceTone() },
                                onOpenSystemSoundSettings = viewModel::openSystemSoundSettings,
                                contentPadding = innerPadding,
                            )
                        }

                        composable<DevicesRoute> {
                            DevicesScreen(
                                bluetoothState = bluetoothState,
                                history = history,
                                onRequestPermissions = {
                                    permissionLauncher.launch(viewModel.requiredBluetoothPermissions())
                                },
                                onRefresh = viewModel::refreshBluetoothDevices,
                                onStartDiscovery = viewModel::startDiscovery,
                                onStopDiscovery = viewModel::stopDiscovery,
                                onPairDevice = viewModel::pairDevice,
                                onConnectDevice = viewModel::connectOrReconnectDevice,
                                onDisconnectDevice = viewModel::disconnectDevice,
                                onRenameDevice = viewModel::renameDevice,
                                onOpenBluetoothSettings = viewModel::openSystemBluetoothSettings,
                                onShowPopupForDevice = { dev ->
                                    viewModel.showConnectionPopupForDevice(dev, isPreview = false)
                                },
                                contentPadding = innerPadding,
                            )
                        }

                        composable<SettingsRoute> {
                            SettingsScreen(
                                settings = appSettings,
                                updateState = updateState,
                                onSetTheme = viewModel::setThemePreference,
                                onSetHighRefreshMotion = viewModel::setHighRefreshMotion,
                                onSetAutoPopup = viewModel::setAutoShowConnectionPopup,
                                onSaveGitHubRepo = viewModel::setGitHubRepository,
                                onCheckForUpdates = viewModel::checkForUpdates,
                                onDownloadUpdateApk = viewModel::downloadUpdateApk,
                                onLaunchInstallApk = { file ->
                                    onLaunchIntent(viewModel.buildInstallApkIntent(file))
                                },
                                onRequestInstallPermission = {
                                    onLaunchIntent(viewModel.buildManageUnknownSourcesIntent())
                                },
                                onDismissUpdateDialog = viewModel::dismissUpdateDialog,
                                onResetAll = viewModel::resetAllData,
                                contentPadding = innerPadding,
                            )
                        }
                    }
                }
            }
        }

        AppleStyleConnectionPopup(
            popup = connectionPopup,
            onDismiss = viewModel::dismissConnectionPopup,
            onOpenSoundStudio = { navigateToTab(SoundRoute) },
            onOpenDevices = { navigateToTab(DevicesRoute) },
        )
    }
}
