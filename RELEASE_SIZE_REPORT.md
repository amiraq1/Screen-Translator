# Release Size Report — Alpha 2

## APK Size Comparison

| Variant | Size | vs Debug Universal | Notes |
|---------|------|-------------------|-------|
| **Debug Universal** | 142.5 MB | baseline | All ABIs, no minification |
| **Debug arm64** | 52.1 MB | -63% | Single ABI |
| **Release Universal** | 127.7 MB | -10% | Minified + shrunk |
| **Release arm64-v8a** | **37.3 MB** | **-74%** | ✅ Best for real devices |
| **Release armeabi-v7a** | 27.7 MB | -81% | Older 32-bit devices |
| **Release x86_64** | 40.1 MB | -72% | Emulator only |

### Key Insight
The biggest size reduction comes from **ABI splits** (not minification), because Tesseract4Android bundles native `.so` libraries for each architecture (~30-40 MB each).

---

## What Was Enabled

| Optimization | Status | Impact |
|-------------|--------|--------|
| `isMinifyEnabled = true` | ✅ Release only | Removes unused code via R8 |
| `isShrinkResources = true` | ✅ Release only | Removes unused resources |
| ProGuard/R8 rules | ✅ Comprehensive | Keeps ML Kit, Tesseract, Room, etc. |
| ABI splits | ✅ arm64-v8a, armeabi-v7a, x86_64 | Separate APK per architecture |
| Universal APK | ✅ Also produced | For compatibility testing |
| Debug unchanged | ✅ No minification | Fast iteration |

---

## ABI Splits Detail

| ABI | Target | Size (Release) |
|-----|--------|---------------|
| `arm64-v8a` | Modern phones (2017+) | 37.3 MB |
| `armeabi-v7a` | Older 32-bit phones | 27.7 MB |
| `x86_64` | Emulators | 40.1 MB |
| `universal` | All devices | 127.7 MB |

**Recommendation**: Distribute `arm64-v8a` for real device testing. Most modern Android phones use this architecture.

---

## ProGuard/R8 Rules

Protected libraries:
- ✅ ML Kit (text-recognition, translate)
- ✅ Tesseract4Android + Leptonica (native JNI)
- ✅ Room (entities, DAOs, database)
- ✅ DataStore
- ✅ Kotlin Coroutines
- ✅ App services (FloatingButtonService, ScreenCaptureService)
- ✅ Native methods
- ✅ Enums

### ProGuard Risks

| Risk | Mitigation |
|------|-----------|
| ML Kit reflection | `-keep class com.google.mlkit.** { *; }` |
| Tesseract JNI | `-keep class com.googlecode.tesseract.android.** { *; }` |
| Room entities stripped | `-keep @androidx.room.Entity class *` |
| Service not found | `-keep class ...FloatingButtonService { *; }` |

---

## Size Breakdown (Estimated)

| Component | Size (approx) |
|-----------|--------------|
| Tesseract native libs (per ABI) | ~25-30 MB |
| ML Kit OCR model | ~5 MB |
| ML Kit Translation runtime | ~3 MB |
| Arabic traineddata | 2.4 MB |
| App code + Compose | ~3-5 MB |
| Resources + assets | ~3 MB |

---

## How to Build

```bash
# Debug (fast, no minification)
./gradlew assembleDebug

# Release (minified, ABI splits)
./gradlew assembleRelease

# Release arm64 only
./gradlew assembleRelease
# → app/build/outputs/apk/release/app-arm64-v8a-release.apk
```

### Output Locations
```
app/build/outputs/apk/
├── debug/
│   ├── app-arm64-v8a-debug.apk      (52.1 MB)
│   ├── app-armeabi-v7a-debug.apk    (42.5 MB)
│   ├── app-universal-debug.apk      (142.5 MB)
│   └── app-x86_64-debug.apk         (55.0 MB)
└── release/
    ├── app-arm64-v8a-release.apk     (37.3 MB)  ← recommended
    ├── app-armeabi-v7a-release.apk   (27.7 MB)
    ├── app-universal-release.apk     (127.7 MB)
    └── app-x86_64-release.apk        (40.1 MB)
```

---

## Signing

Currently using **debug signing** for release builds (no keystore configured).

To configure release signing, add to `local.properties`:
```properties
RELEASE_STORE_FILE=/path/to/keystore.jks
RELEASE_STORE_PASSWORD=your_password
RELEASE_KEY_ALIAS=your_alias
RELEASE_KEY_PASSWORD=your_key_password
```

---

## Future Optimizations

1. **App Bundle (AAB)** — Google Play would deliver only the needed ABI (~37 MB)
2. **Dynamic delivery** — Download Arabic traineddata on demand instead of bundling
3. **ML Kit thin client** — Use Play Services delivery instead of bundled model
4. **ProGuard aggressive** — More aggressive shrinking (risk: runtime crashes)
5. **Remove x86_64** — Only needed for emulator testing
