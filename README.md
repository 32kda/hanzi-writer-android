# OpenHanziWriter — Android App

Chinese Character Writing Trainer for Android (API 26+).

---

## User Guide

### What is Hanzi Writer?

Hanzi Writer is a Chinese character writing trainer that helps you learn and practice writing Hànzì (Chinese characters) using the correct stroke order. Each character is split into individual strokes; you draw them one by one and the app checks if you got the right stroke in the right direction.

### Choosing a Character Set

When you first launch the app, you'll see the **Set Selector** screen — a list of character sets you can study.

**Built-in sets:**
- **HSK 1** through **HSK 6** — vocabulary for each HSK level (174 to 976 characters)
- **HSK 1 (RU)** — HSK 1 with Russian translations

Tap a set to select it and you'll be taken to the **Home screen**. Your choice is remembered; next time you open the app you'll go straight to Home.

You can switch sets anytime by tapping **Change Set** on the Home screen. The back arrow at the top will take you back without losing your current selection.

### Learn Mode

Learn mode introduces **2 new characters** at a time. Each character goes through 6 rounds of practice with progressively less help:

1. **Full hint** (x2) — the complete character outline is visible so you can follow along
2. **Half hint** (x2) — only the first half of strokes are shown; you draw the rest from memory
3. **No hint** (x2) — no reference strokes at all; draw the entire character from memory

The **first time** you see a new character in Learn mode, a demo animation plays showing the correct stroke order. Tap **Next** to start drawing when you're ready.

### Drill Mode

Drill mode picks **5 characters** that you've practiced the least (or that need the most review) and gives each character 2 rounds:

1. **Grayed hint** — all strokes are dimmed as a faint guide
2. **No hint** — completely from memory

This mode is for reinforcing characters you've already learned.

### Quiz Mode

Quiz mode is the hardest — **10 characters**, each tested once with **no hints at all**. You must draw each character entirely from memory.

### Writing on the Canvas

- Draw **one stroke at a time** by dragging your finger across the canvas
- The grid is a traditional **Tián Zì Gé** (田字格) — a square divided by cross and diagonal lines to help you position strokes correctly
- Stroke order matters — you must draw strokes in the correct sequence
- If you make mistakes, the app helps you out: after **2 mistakes** on the same stroke, stroke numbers appear; after **4 mistakes**, a faint outline of the correct stroke is shown

When you finish a session, you'll see a results screen with your accuracy for each character and an overall score.

### Creating and Importing Custom Character Sets

You can import your own vocabulary lists from CSV or ZIP files.

**CSV format:**
Each line has 3 comma-separated fields:

```
"character","pinyin","definition"
```

Example:
```
"我","wǒ","I, me"
"你","nǐ","you"
"好","hǎo","good"
```

- The **character** is required (a single Hanzi)
- Pinyin and definition are optional but recommended
- Use double-quotes around values that contain commas

**ZIP format:**
A ZIP archive containing:
- A CSV file named `<name>.csv` (matching the ZIP filename)
- Optionally a `<name>_properties.toml` file with `name = "Display Name"` and `description = "..."`

**How to import:**
1. On the Set Selector screen, tap the **+** button (bottom-right)
2. Pick a CSV or ZIP file from your device
3. If a set with the same name already exists, you'll be asked whether to overwrite it
4. Once imported, the new set appears in your list

Imported sets show a trash icon — you can delete them anytime. Built-in sets cannot be deleted.

### Streak

A **streak** is a count of consecutive days you've completed at least one session (Learn, Drill, or Quiz).

- Your current streak is displayed on the Home screen as "Streak: X days" with a fire icon
- Missing a day resets the streak back to 0
- The app also tracks your **longest streak** ever

**How to see your streak history:**
Tap the streak text on the Home screen to open the **Calendar view**. Days you practiced are marked with a red circle. Navigate months with the arrow buttons at the top.

The Home screen also shows **today's engagement** (minutes practiced), split into three levels:
- **Light** — less than 10 minutes
- **Moderate** — 10–19 minutes
- **Strong** — 20+ minutes

---

## Technical Description

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
adb shell am start -n org.openhanziwriter.app/.MainActivity
```

### 4. Debugging Tips

**View logs:**
```bash
# Filter by app package
adb logcat -s OpenHanziWriter
adb logcat --pid=$(adb shell pidof -s org.openhanziwriter.app)

# Filter by priority (show only warnings and errors)
adb logcat *:W

# Clear log buffer first
adb logcat -c && adb logcat -s OpenHanziWriter
```

**Inspect database on device:**
```bash
# Copy database from device to computer
adb exec-out run-as org.openhanziwriter.app cat databases/app_database.db > app_database.db

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
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | `adb uninstall org.openhanziwriter.app` then reinstall |
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
