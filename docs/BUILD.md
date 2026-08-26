# Build Instructions

Complete guide for building ScriptHost from source.

## Prerequisites

### Required Software

- **Android Studio**: Arctic Fox (2020.3.1) or later
- **JDK**: Version 11 or later
- **Kotlin**: 1.9.20 (included with Android Studio)
- **Gradle**: 8.2.0 (wrapper included)
- **Git**: For version control

### Optional Tools

- **Android SDK**: API 24-34 (installed via Android Studio)
- **Android NDK**: For native library compilation (if needed)
- **Emulator**: For testing without physical device

## Setup

### 1. Clone Repository

```bash
git clone https://github.com/scripthost/scripthost.git
cd scripthost
```

### 2. Open in Android Studio

```bash
# Linux/Mac
studio android/

# Windows
start android-studio android\
```

Or use File → Open in Android Studio and select the `android/` directory.

### 3. Sync Gradle

Android Studio will automatically prompt to sync Gradle. If not:

- Click "Sync Project with Gradle Files" in toolbar
- Or: File → Sync Project with Gradle Files

### 4. Download Dependencies

Gradle will automatically download all dependencies:

- AndroidX libraries
- Kotlin standard library
- J2V8 JavaScript engine
- Material Components
- Testing libraries

This may take several minutes on first run.

## Building

### Debug Build

#### Via Android Studio

1. Select "app" configuration
2. Click Run (▶) or press Shift+F10
3. Select target device/emulator

#### Via Command Line

```bash
cd android
./gradlew assembleDebug
```

Output: `android/app/build/outputs/apk/debug/app-debug.apk`

### Release Build

#### Generate Signing Key

```bash
keytool -genkey -v -keystore scripthost-release.keystore \
  -alias scripthost -keyalg RSA -keysize 2048 -validity 10000
```

#### Configure Signing

Create `android/keystore.properties`:

```properties
storeFile=/path/to/scripthost-release.keystore
storePassword=your_store_password
keyAlias=scripthost
keyPassword=your_key_password
```

The release build reads this file via `signingConfigs.release` in
`android/app/build.gradle.kts`. The file is gitignored; when it is absent the
release build still succeeds but produces an **unsigned** APK.

#### Build Release APK

```bash
cd android
./gradlew assembleRelease
```

Output: `android/app/build/outputs/apk/release/app-release.apk`

#### Build App Bundle (for Play Store)

```bash
cd android
./gradlew bundleRelease
```

Output: `android/app/build/outputs/bundle/release/app-release.aab`

## Testing

### Run Unit Tests

```bash
cd android
./gradlew testDebugUnitTest
```

A single `testDebugUnitTest` invocation runs both the plain JUnit suites and
the Robolectric-backed suites (Android framework classes simulated on the JVM
via `org.robolectric:robolectric`) — no emulator or device required. Tests use
Truth for assertions and mockito-kotlin for mocking; `work-testing` covers the
WorkManager worker.

Notes:

- The first run needs network access: Robolectric downloads its `android-all`
  jars on demand.
- J2V8 loads a native `.so` library, so no JVM/Robolectric test can construct
  a V8 runtime. Bridge `register()`/`unregister()` paths are untestable on the
  JVM; bridges are tested via direct method calls instead.

### Run Instrumented Tests

```bash
cd android
./gradlew connectedAndroidTest
```

Requires connected device or running emulator.

### Generate Test Coverage

```bash
cd android
./gradlew jacocoTestReport
```

Report: `android/app/build/reports/jacoco/jacocoTestReport/html/index.html`

### Run Lint

```bash
cd android
./gradlew lint
```

Report: `android/app/build/reports/lint-results.html`

## Installation

### Install on Device

#### Via Android Studio

1. Connect device via USB
2. Enable USB debugging on device
3. Click Run in Android Studio

#### Via Command Line

```bash
cd android
./gradlew installDebug

# Or install release
./gradlew installRelease
```

#### Via ADB

```bash
adb install android/app/build/outputs/apk/debug/app-debug.apk
```

### Install on Emulator

1. Start emulator from AVD Manager
2. Run app from Android Studio
3. Or use `adb install` as above

## Troubleshooting

### Gradle Sync Failed

**Problem**: Gradle sync fails with dependency errors

**Solution**:
```bash
cd android
./gradlew clean
./gradlew --refresh-dependencies
```

### Build Failed: Out of Memory

**Problem**: Gradle runs out of memory

**Solution**: Increase heap size in `android/gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=512m
```

### J2V8 Native Library Error

**Problem**: J2V8 native library not found

**Solution**: Ensure correct ABI is included in build.gradle:
```kotlin
android {
    defaultConfig {
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }
}
```

### SDK Not Found

**Problem**: Android SDK not found

**Solution**: Set SDK location in `android/local.properties`:
```properties
sdk.dir=/path/to/Android/Sdk
```

### Emulator Won't Start

**Problem**: Emulator fails to start

**Solution**:
1. Check virtualization is enabled in BIOS
2. Install Intel HAXM or AMD Hypervisor
3. Try creating new AVD with different API level

## Build Variants

### Debug

- Debuggable
- No obfuscation
- Includes logging
- Faster build time

### Release

- Not debuggable
- ProGuard/R8 obfuscation
- Logging removed
- Optimized
- Requires signing

## Build Configuration

### Gradle Properties

`android/gradle.properties`:
```properties
# Kotlin
kotlin.code.style=official

# AndroidX
android.useAndroidX=true
android.enableJetifier=true

# Build performance
org.gradle.jvmargs=-Xmx2048m
org.gradle.parallel=true
org.gradle.caching=true

# R8
android.enableR8.fullMode=true
```

### Build Types

Customize in `android/app/build.gradle.kts`:

```kotlin
buildTypes {
    debug {
        applicationIdSuffix = ".debug"
        versionNameSuffix = "-debug"
        isDebuggable = true
        isMinifyEnabled = false
    }

    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

### Product Flavors

Add flavors for different distributions:

```kotlin
flavorDimensions += "version"
productFlavors {
    create("free") {
        dimension = "version"
        applicationIdSuffix = ".free"
    }
    create("pro") {
        dimension = "version"
        applicationIdSuffix = ".pro"
    }
}
```

## Continuous Integration

### GitHub Actions

`.github/workflows/build.yml`:

```yaml
name: Build

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2

      - name: Set up JDK 11
        uses: actions/setup-java@v2
        with:
          java-version: '11'
          distribution: 'adopt'

      - name: Grant execute permission for gradlew
        run: chmod +x android/gradlew

      - name: Build with Gradle
        run: cd android && ./gradlew build

      - name: Run tests
        run: cd android && ./gradlew test

      - name: Upload APK
        uses: actions/upload-artifact@v2
        with:
          name: app-debug
          path: android/app/build/outputs/apk/debug/app-debug.apk
```

## Performance Optimization

### Build Speed

- Enable Gradle daemon
- Use parallel builds
- Enable build cache
- Increase heap size
- Use configuration cache

### APK Size

- Enable R8 shrinking
- Remove unused resources
- Use vector drawables
- Compress images
- Split APKs by ABI

## Clean Build

Remove all build artifacts:

```bash
cd android
./gradlew clean
rm -rf .gradle build app/build
```

## Build from Scratch

Complete clean build:

```bash
cd android
./gradlew clean
./gradlew --refresh-dependencies
./gradlew assembleDebug
```

## Verification

### Verify APK

```bash
# Check APK signature
apksigner verify --verbose app-release.apk

# List APK contents
unzip -l app-release.apk

# Check APK size
ls -lh app-release.apk
```

### Verify App Bundle

```bash
# Check bundle signature
jarsigner -verify -verbose app-release.aab

# Extract APKs from bundle
bundletool build-apks --bundle=app-release.aab \
  --output=app.apks --mode=universal
```

## Distribution

### Google Play Store

1. Build app bundle: `./gradlew bundleRelease`
2. Sign bundle with release key
3. Upload to Play Console
4. Complete store listing
5. Submit for review

### Direct Distribution

1. Build signed APK: `./gradlew assembleRelease`
2. Host on website or file sharing
3. Users must enable "Unknown sources"
4. Provide installation instructions

## Support

For build issues:

- Check [Troubleshooting](#troubleshooting) section
- Search [GitHub Issues](https://github.com/scripthost/scripthost/issues)
- Ask in [Discussions](https://github.com/scripthost/scripthost/discussions)
- Contact maintainers

## Next Steps

After successful build:

1. Run the app on device/emulator
2. Try example scripts
3. Read [API Documentation](docs/API.md)
4. Create your own scripts
5. Contribute improvements

## References

- [Android Developer Guide](https://developer.android.com/guide)
- [Gradle Build Tool](https://gradle.org/)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [J2V8 Documentation](https://github.com/eclipsesource/J2V8)
