# ─── ML Kit ────────────────────────────────────────────────────────────
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-keep class com.google.android.gms.internal.mlkit_vision_text.** { *; }
-dontwarn com.google.android.gms.internal.mlkit_vision_text.**

# ML Kit Translation
-keep class com.google.mlkit.nl.translate.** { *; }
-dontwarn com.google.mlkit.nl.translate.**

# ─── Tesseract4Android ─────────────────────────────────────────────────
-keep class com.googlecode.tesseract.android.** { *; }
-keep class com.googlecode.leptonica.android.** { *; }
-dontwarn com.googlecode.tesseract.android.**
-dontwarn com.googlecode.leptonica.android.**

# ─── Room ──────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.**

# ─── DataStore ─────────────────────────────────────────────────────────
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ─── Compose ──────────────────────────────────────────────────────────
# Compose generally works with R8 out of the box, but keep stability annotations
-dontwarn androidx.compose.**

# ─── Kotlin Coroutines ─────────────────────────────────────────────────
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# ─── App classes ───────────────────────────────────────────────────────
# Keep data classes used with Room
-keep class com.ammar.nabdscreentranslate.data.TranslationHistoryEntity { *; }

# Keep services declared in manifest
-keep class com.ammar.nabdscreentranslate.overlay.FloatingButtonService { *; }
-keep class com.ammar.nabdscreentranslate.capture.ScreenCaptureService { *; }
-keep class com.ammar.nabdscreentranslate.capture.MediaProjectionRequestActivity { *; }

# ─── General ──────────────────────────────────────────────────────────
# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
