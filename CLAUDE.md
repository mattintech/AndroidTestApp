# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Android application project using Gradle with Kotlin DSL. The project uses modern Android development practices with AndroidX libraries and targets Android 12+ (minSdk 32).

## Build Commands

```bash
# Clean build artifacts
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run all tests
./gradlew test connectedAndroidTest

# Check for lint issues
./gradlew lint

# Install debug APK on connected device
./gradlew installDebug

# Run a specific test class
./gradlew test --tests "com.mattintech.androidtestapp.ExampleUnitTest"

# Run tests with additional output
./gradlew test --info
```

## Project Structure

- **App Module**: `app/` - Main application module
  - Source code: `app/src/main/java/com/mattintech/androidtestapp/`
  - Unit tests: `app/src/test/java/com/mattintech/androidtestapp/`
  - Instrumented tests: `app/src/androidTest/java/com/mattintech/androidtestapp/`
  - Resources: `app/src/main/res/`
  - Manifest: `app/src/main/AndroidManifest.xml`

- **Gradle Configuration**:
  - Project-level: `build.gradle.kts`
  - App-level: `app/build.gradle.kts`
  - Version catalog: `gradle/libs.versions.toml`
  - Gradle wrapper: `gradle/wrapper/` (Gradle 8.11.1)

## Key Configuration

- **Package name**: `com.mattintech.androidtestapp`
- **Application ID**: `com.mattintech.androidtestapp`
- **Target SDK**: 35 (Android 15)
- **Min SDK**: 32 (Android 12)
- **Compile SDK**: 35
- **Test runner**: `androidx.test.runner.AndroidJUnitRunner`

## Development Notes

This is currently a minimal Android project template. When developing:

1. The project uses Gradle Version Catalog (`libs.versions.toml`) for dependency management
2. All build configuration uses Kotlin DSL (`.kts` files)
3. The project is set up for both unit tests (JUnit) and instrumented tests (Espresso)
4. No custom lint rules are configured - default Android lint rules apply

## App Features

This is a demo app for testing device stress scenarios with three main features:

### 1. Battery Draining Feature
Tests battery consumption through various hardware components:

**User-configurable options:**
- Test duration (minutes/hours)
- CPU intensive operations (mathematical calculations, loops)
- Screen (maximum brightness, prevent sleep)
- Flashlight (continuous or strobe)
- GPS (high accuracy location updates)
- BLE scanning (continuous discovery)
- WiFi scanning
- Network activity (continuous data transfers)
- Sensors (accelerometer, gyroscope, magnetometer - continuous reading)
- Camera (preview without recording)
- Audio (silent playback to keep audio subsystem active)
- Vibration (continuous patterns)

**Presets:**
- "Maximum Drain" - All features enabled
- "CPU & GPU Focus" - CPU calculations + screen brightness
- "Radio Focus" - GPS + BLE + WiFi scanning
- "Sensor Focus" - All sensors + vibration

### 2. Bad Behavior Feature
Intentionally causes app crashes and ANRs for testing:

**Crash types (user-selectable):**
- Division by zero
- Null pointer exception
- Out of memory error
- Stack overflow
- Array index out of bounds

**ANR triggers (user-selectable):**
- UI thread blocking (long-running operation on main thread)
- Broadcast receiver timeout
- Service timeout

**Settings:**
- Frequency: Manual trigger or random intervals (configurable)
- Auto-restart: App automatically restarts after crash

### 3. Downloader Feature
Schedules and executes file downloads:

**User-configurable options:**
- Download URL (with default test URLs)
- File size options (1MB, 10MB, 100MB, custom)
- Schedule type:
  - One-time download
  - Repeat every X minutes/hours
  - Specific times of day
- Network preference (WiFi only, cellular only, any)
- Storage behavior (save or discard after download)
- Background downloads (continue when app in background)
- Concurrent downloads (1-10 simultaneous)

**UI shows:**
- Download progress
- Download history
- Total data downloaded

## Architecture

- **MainActivity**: Launch screen with buttons to three feature activities
- **BatteryDrainActivity**: Configure and run battery drain tests
- **BadBehaviorActivity**: Configure and trigger crashes/ANRs
- **DownloaderActivity**: Configure and manage downloads
- **Services**: Background services for battery drain and downloads
- **Permissions**: Auto-request all required permissions on app launch

## Required Permissions

The app will need these permissions (auto-requested on launch):
- CAMERA
- ACCESS_FINE_LOCATION
- ACCESS_COARSE_LOCATION
- BLUETOOTH_SCAN
- BLUETOOTH_CONNECT
- FLASHLIGHT
- VIBRATE
- INTERNET
- ACCESS_NETWORK_STATE
- ACCESS_WIFI_STATE
- WAKE_LOCK
- FOREGROUND_SERVICE
- WRITE_EXTERNAL_STORAGE (for downloads)
- HIGH_SAMPLING_RATE_SENSORS