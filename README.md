# HARDIK BUDS

**HARDIK BUDS** is an Android wireless earbuds companion application built with **Kotlin**, **Jetpack Compose**, **Material 3**, and **Clean Architecture**. Inspired by Nothing OS dot-matrix industrial minimalism and Apple AirPods-style spring connection cards, it integrates directly with Android's real Bluetooth, AudioEffects, Spatializer, and PackageInstaller APIs.

---

## Features & Real Android API Architecture

### 1. Connectivity & Device Dashboard

- **Bluetooth Discovery & Pairing**: Uses `BluetoothAdapter.startDiscovery()`, `BluetoothDevice.createBond()`, `BluetoothA2dp` / `BluetoothHeadset` profile proxies, and `AudioManager.registerAudioDeviceCallback()`.
- **Battery Telemetry**: Reads unified headset battery via Android's `BluetoothDevice.getBatteryLevel()`, `BATTERY_LEVEL_CHANGED` broadcasts, and BLE GATT Battery Service (`0x180F` / `0x2A19`) when exposed by the device.
- **Multi-Device Management & History**: Rename devices (`BluetoothDevice.setAlias` on Android 11+ and local Room alias), switch visual earbud styles, and inspect connection history stored in **Room**.
- **Air-Style Spring Connection Popup**: Automatically triggers on Bluetooth audio connection events (or manually via the Home preview button) with spring physics and customizable 3D hardware skins.

### 2. Sound Studio (Real Android DSP)

- **10-Band Parametric Equalizer (`31Hz - 16kHz`)**: Powered by Android 9+ **`DynamicsProcessing`** (`preEq`, `postEq`, 3-band `Mbc`, `Limiter`), with automatic fallback to `Equalizer` + `BassBoost` + `LoudnessEnhancer` on Android 7.0 and 8.
- **Real-Time Shaping**: Bass Booster, Treble Booster, Vocal Enhancer, Clarity Enhancement (Multi-Band Compressor), per-channel Left/Right Balance (`setInputGainbyChannel`), and system `AudioManager.STREAM_MUSIC` volume sync.
- **Presets & Custom Profiles**: Built-in curves (*Flat, Pop, Rock, EDM, Classical, Jazz, Cinema, Gaming, Deep Bass*) plus user-saved custom profiles persisted in Room.
- **Acoustic Reference Audition Player**: Built-in `AudioTrack` harmonic/cinema-sub/vocal synthesizer attached directly to the DSP chain so you can audition EQ and spatial changes immediately on any device.

### 3. Cinema & Spatial Audio

- **Virtual Surround & Room Simulation**: Real Android `Virtualizer` (*Stereo Direct, Wide Stereo, Virtual 5.1, Virtual 7.1, Cinema, Concert Hall, Gaming*) and `PresetReverb` room simulation.
- **Home Theatre Controls**: Virtual Subwoofer, Deep Bass, Dialogue Enhancement, Stereo Widening, Movie Sound Profiles, and Bass/Treble trim.
- **OS Spatializer & Head Tracking Diagnostics**: Queries Android 13+ (`API 33+`) `AudioManager.spatializer` for `isAvailable`, `isEnabled`, `immersiveAudioLevel`, and `isHeadTrackerAvailable`.

### 4. Hardware & Vendor API Transparency

HARDIK BUDS **never** fakes unsupported controls:

- **ANC, Transparency, Adaptive ANC, and Wind Noise Modes**: Standard public Android Bluetooth APIs do not expose proprietary vendor ANC registers. Unless a supported manufacturer GATT/RFCOMM protocol is implemented for a specific chipset, ANC controls remain clearly marked as **Vendor API Required** rather than simulated.
- **Split Left / Right / Case Battery**: Shown only when reported over BLE GATT / vendor extensions; otherwise displays the standard Android unified Bluetooth battery level.
- **No Unlicensed Branding Claims**: Spatializer modes do not claim licensed Dolby Atmos certification.

### 5. GitHub Releases Auto-Updater

- Queries `https://api.github.com/repos/{owner}/{repo}/releases/latest` via Retrofit + Moshi.
- Compares semantic version tags against `BuildConfig.VERSION_NAME`, renders release changelogs, streams `.apk` assets with progress to app cache, checks `PackageManager.canRequestPackageInstalls()`, and launches the standard Android `FileProvider` package installer (never silent install).

---

## Permissions

- Android 12+ (`API 31+`): `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` (`neverForLocation`)
- Android 7.0-11 (`API 24-30`): `BLUETOOTH`, `BLUETOOTH_ADMIN`, `ACCESS_FINE_LOCATION` (capped at `maxSdkVersion="30"`)
- Audio & Updates: `MODIFY_AUDIO_SETTINGS`, `INTERNET`, `ACCESS_NETWORK_STATE`, `REQUEST_INSTALL_PACKAGES`

---

## Building Debug & Signed Release APKs

> **Prerequisites:** JDK 21, Android SDK Platform 36 (`compileSdk` 36.1), and system Gradle (`gradle` 9.3.1). If you prefer `./gradlew` locally in Android Studio, run `gradle wrapper` once first.

### Debug APK

```bash
gradle :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Signed Release APK

1. Generate an upload keystore:

```bash
keytool -genkey -v -keystore my-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

2. Export environment variables and build:

```bash
export KEYSTORE_PATH="$(pwd)/my-upload-key.jks"
export STORE_PASSWORD="your-store-password"
export KEY_PASSWORD="your-key-password"
gradle :app:assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

### GitHub Actions CI/CD

Configure repository secrets `KEYSTORE_BASE64`, `STORE_PASSWORD`, and `KEY_PASSWORD` to automatically build signed Release APKs and attach them to GitHub Releases whenever a `v*` tag is pushed (see `.github/workflows/android-build.yml`).
