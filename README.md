# HanziWriter — Android App

Chinese Character Writing Trainer for Android (API 26+).

## Prerequisites

- **Android Studio** (Hedgehog 2023.1+)
- **JDK 17**
- **Android SDK** API 34
- **Android device** (physical or emulator) running Android 8.0+

## Building

```bash
cd android

# Build the pre-populated database (requires Python 3)
pip install -U pip
python build_scripts/generate_character_db.py

# Build debug APK
./gradlew assembleDebug

# Build and install directly on connected device
./gradlew installDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Debugging on an Android Phone

### 1. Enable Developer Options on Your Phone

1. Open **Settings** → **About phone**
2. Tap **Build number** 7 times until "You are now a developer!" appears
3. Go back to **Settings** → **System** → **Developer options**

### 2. Enable USB Debugging

1. In **Developer options**, toggle **USB debugging** ON
2. Connect your phone to your computer via USB cable
3. When prompted on the phone, **allow USB debugging** (check "Always allow from this computer")

### 3. Run the App

**Option A — From Android Studio:**
1. Select your device from the dropdown in the toolbar
2. Click the green **Run** button (or `Shift+F10`)

**Option B — From command line:**
```bash
# Verify device is connected
adb devices
# Output should show: <device_serial>    device

# Build and install
./gradlew installDebug

# Launch the app
adb shell am start -n com.hanziwriter.app/.MainActivity
```

### 4. Debugging Tips

**View logs:**
```bash
# Filter by app package
adb logcat -s HanziWriter
adb logcat --pid=$(adb shell pidof -s com.hanziwriter.app)

# Filter by priority (show only warnings and errors)
adb logcat *:W

# Clear log buffer first
adb logcat -c && adb logcat -s HanziWriter
```

**Inspect database on device:**
```bash
# Copy database from device to computer
adb exec-out run-as com.hanziwriter.app cat databases/app_database.db > app_database.db

# Or use Android Studio's Database Inspector:
# View → Tool Windows → App Inspection → Database Inspector
```

**Test on different screen sizes:**
```bash
# List connected devices
adb devices

# Capture screenshot
adb shell screencap /sdcard/screenshot.png
adb pull /sdcard/screenshot.png
```

**Wireless debugging (no USB cable):**
1. Connect via USB once, then:
```bash
adb tcpip 5555
adb connect <phone_ip>:5555
# Now unplug USB — you can debug wirelessly
```

### 5. Common Issues

| Issue | Fix |
|-------|-----|
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | `adb uninstall com.hanziwriter.app` then reinstall |
| `INSTALL_FAILED_INSUFFICIENT_STORAGE` | Free up space on device |
| "Device unauthorized" | Check phone screen for USB debugging authorization prompt |
| `FAILURE: Build failed with Java 21` | Set `JAVA_HOME` to JDK 17: `$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"` |
| ADB not recognized | Add `%LOCALAPPDATA%\Android\Sdk\platform-tools` to your PATH |

### 6. Running Tests

```bash
# Unit tests (run on JVM, no device needed)
./gradlew testDebugUnitTest

# Instrumented tests (require device/emulator)
./gradlew connectedDebugAndroidTest

# Run a specific test class
./gradlew testDebugUnitTest --tests "*StrokeMatcherTest*"
```

### 7. Build Variants

The project has two build types:
- **debug**: Unoptimized, debuggable, suitable for development
- **release**: Minified, optimized for production

Switch between them in Android Studio: **Build** → **Select Build Variant**
