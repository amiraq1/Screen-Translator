# Nabd Screen Translate | نبض ترجمة الشاشة

Real-time on-device screen translation for Android. Capture any text on screen, extract it with OCR, and translate it instantly — all offline, no data leaves your device.

## Features

- **Floating Button** — Draggable overlay button, always accessible
- **Screen Capture** — MediaProjection-based screenshot capture
- **OCR** — ML Kit Text Recognition (Latin scripts) + Tesseract (Arabic script)
- **Translation** — ML Kit offline translation (50+ languages → Arabic)
- **Region Selection** — Long-press to select specific screen area
- **Translation History** — Auto-save, search, copy, delete
- **Display Modes:**
  - **Overlay Bubbles** — Translated text in floating bubbles near original
  - **Bottom Sheet** — Scrollable panel at screen bottom
  - **Both** — Bubbles + sheet combined
  - **Visual Replace** — Covers original text and renders translation in-place (Google Lens style)
- **Overlay Declutter** — Groups nearby text, filters noise, limits max bubbles
- **Settings** — Overlay opacity, vibration, history toggle, dark mode, display mode
- **Privacy-First** — 100% on-device processing, no network calls

## Permissions

| Permission | Purpose |
|-----------|---------|
| `SYSTEM_ALERT_WINDOW` | Floating button & translation overlay |
| `FOREGROUND_SERVICE` | Keep capture service alive |
| `FOREGROUND_SERVICE_MEDIA_PROJECTION` | Screen capture |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Floating button service |
| `VIBRATE` | Haptic feedback on translation |

## Privacy

- ❌ No screenshots saved to storage
- ❌ No data sent to any server
- ❌ No accessibility service used
- ✅ All processing happens on-device
- ✅ ML Kit models run locally

## How to Build

```bash
# Debug build (all ABIs, no minification)
./gradlew assembleDebug

# Release build (minified, ABI splits)
./gradlew assembleRelease

# APK locations
app/build/outputs/apk/release/app-arm64-v8a-release.apk   # 37 MB — recommended
app/build/outputs/apk/debug/app-universal-debug.apk        # 142 MB — all ABIs
```

**Requirements:**
- Android Studio Hedgehog+ or JDK 17
- Android SDK 34
- Gradle 8.x

## How to Test

```bash
# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk

# Watch logs
adb logcat -s NabdScreenTranslate

# Grant overlay permission (if needed)
adb shell appops set com.ammar.nabdscreentranslate SYSTEM_ALERT_WINDOW allow
```

### Manual Test Flow:
1. Launch app → Tap "بدء الترجمة"
2. Grant overlay permission
3. Open any app with English text
4. Tap floating button (ت)
5. Accept MediaProjection consent
6. Wait for translation overlay

## Architecture

```
├── capture/          # MediaProjection, ScreenCaptureManager
├── core/             # Permissions, BitmapUtils, Result types
├── data/             # Room DB, DataStore, DAO
├── domain/           # Use cases (TranslateScreen, SaveTranslation, ObserveHistory)
├── ocr/              # ML Kit OCR engine
├── overlay/          # FloatingButton, TranslationOverlay, RegionSelector
├── translate/        # ML Kit Translation engine
└── ui/               # Compose screens (Home, History, Settings)
```

## Known Limitations

- Android 14+ requires fresh MediaProjection consent per capture
- First translation is slow (ML Kit model download ~30MB)
- Debug APK is large (~142MB) due to bundled ML Kit models + Tesseract native libs
- Release arm64 APK is ~37MB (recommended for sharing)
- DRM-protected content cannot be captured
- Arabic OCR accuracy depends on text clarity and font size

## Tech Stack

- Kotlin 1.9 + Jetpack Compose
- ML Kit Text Recognition 16.0.0
- ML Kit Translation 17.0.2
- Tesseract4Android 4.9.0 (Arabic OCR)
- Room 2.6.1 + KSP
- DataStore Preferences
- MediaProjection API
- WindowManager Overlay

## License

Private — All rights reserved.
