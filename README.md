# Android Test App

A demonstration Android application designed for testing device stress scenarios and app behavior under various conditions. This app provides three main testing features: battery draining, intentional bad behavior (crashes/ANRs), and download scheduling.

## Features

### 1. Battery Drainer
Tests battery consumption through intensive use of device hardware components:

- **CPU Operations**: Mathematical calculations and intensive loops
- **Display**: Maximum brightness with screen always on
- **Flashlight**: Continuous or strobe patterns
- **GPS**: High-accuracy location updates
- **Bluetooth**: Continuous BLE scanning
- **WiFi**: Network scanning
- **Sensors**: Accelerometer, gyroscope, magnetometer continuous reading
- **Camera**: Preview without recording
- **Audio**: Silent playback to keep audio subsystem active
- **Vibration**: Continuous patterns

**Presets Available:**
- Maximum Drain (all features enabled)
- CPU & GPU Focus
- Radio Focus (GPS, BLE, WiFi)
- Sensor Focus

### 2. Bad Behavior Simulator
Intentionally triggers app crashes and ANRs for testing crash reporting and monitoring systems:

**Crash Types:**
- Division by zero
- Null pointer exception
- Out of memory error
- Stack overflow
- Array index out of bounds

**ANR Triggers:**
- UI thread blocking
- Broadcast receiver timeout
- Service timeout

**Configuration Options:**
- Manual or automatic triggering
- Random intervals
- Auto-restart after crash

### 3. Download Manager
Schedules and executes file downloads with configurable parameters:

- Custom or preset download URLs
- File size options (1MB, 10MB, 100MB, custom)
- Scheduling: one-time, repeating, or specific times
- Network preferences (WiFi only, cellular only, any)
- Concurrent download support (1-10 simultaneous)
- Background download capability
- Download history tracking

## Requirements

- Android 12+ (API level 32)
- Target SDK: Android 15 (API level 35)

## Permissions

The app requires the following permissions (automatically requested on launch):
- Camera
- Location (Fine and Coarse)
- Bluetooth (Scan and Connect)
- Internet and Network State
- WiFi State
- Vibrate
- Wake Lock
- Foreground Service
- External Storage (for downloads)
- High Sampling Rate Sensors

## Building the Project

### Prerequisites
- Android Studio (latest stable version recommended)
- JDK 17 or higher
- Android SDK with API level 35

### Build Commands

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

# Check for lint issues
./gradlew lint

# Install debug APK on connected device
./gradlew installDebug
```

## Project Structure

```
AndroidTestApp/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/mattintech/androidtestapp/
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   ├── test/
│   │   └── androidTest/
│   └── build.gradle.kts
├── gradle/
│   ├── wrapper/
│   └── libs.versions.toml
├── build.gradle.kts
└── settings.gradle.kts
```

## Architecture

- **MainActivity**: Launch screen with navigation to three feature activities
- **BatteryDrainActivity**: Configure and run battery drain tests
- **BadBehaviorActivity**: Configure and trigger crashes/ANRs
- **DownloaderActivity**: Configure and manage downloads
- **Background Services**: Handle battery drain and download operations
- **Permission Manager**: Handles runtime permission requests

## Usage

1. Launch the app
2. Grant requested permissions
3. Select one of the three testing features from the main menu
4. Configure test parameters as needed
5. Start the test and monitor results

## Development

This project uses:
- Kotlin as the primary language
- Gradle with Kotlin DSL for build configuration
- AndroidX libraries
- Version catalog for dependency management

## Warning

⚠️ **This app is designed for testing purposes only!** It intentionally:
- Drains battery rapidly
- Causes app crashes and system stress
- Consumes network bandwidth
- Uses device resources intensively

Use responsibly and only on test devices or when specifically needed for development/testing purposes.

## Contributing

Contributions are welcome! However, before investing significant time in a feature or change:

1. **Start a discussion first**: Open an issue or start a discussion in the project's Discussions tab to propose your idea
2. **Wait for feedback**: Get confirmation that the change aligns with the project's goals
3. **Fork and create a branch**: Work on your changes in a dedicated branch
4. **Submit a Pull Request**: Include a clear description of the changes and reference any related issues

### Contribution Guidelines

- Follow the existing code style and conventions
- Add tests for new functionality
- Update documentation as needed
- Ensure all tests pass before submitting
- Keep commits focused and atomic

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.