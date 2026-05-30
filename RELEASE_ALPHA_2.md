# Nabd Screen Translate — Alpha 2 Release (Arabic OCR)

## معلومات الإصدار
| الحقل | القيمة |
|-------|--------|
| **اسم التطبيق** | نبض ترجمة الشاشة (Nabd Screen Translate) |
| **المرحلة** | Alpha 2 |
| **رقم النسخة** | 1.0.0 (versionCode 1) |
| **تاريخ البناء** | 2026-05-30 |
| **APK (arm64 release)** | `release-apks/nabd-screen-translate-alpha2-release-arm64.apk` |
| **حجم APK (arm64 release)** | ~37.3 MB |
| **حجم APK (universal debug)** | ~142.5 MB |
| **Min SDK** | Android 8.0 (API 26) |
| **Target SDK** | Android 14 (API 34) |

---

## ما الجديد في Alpha 2

### ✅ دعم OCR العربي
- إضافة محرك Tesseract4Android لقراءة النصوص العربية
- محرك هجين (Hybrid) يختار تلقائياً بين ML Kit و Tesseract
- يعمل offline بالكامل — لا يحتاج اتصال إنترنت

### استراتيجية المحرك الهجين (HybridOcrEngine)
| لغة المصدر | المحرك المستخدم |
|------------|----------------|
| `auto` | ML Kit أولاً → إذا لم يجد نص كافي → Tesseract Arabic |
| `ar` | Tesseract Arabic مباشرة |
| `en`, `fr`, `de`, etc. | ML Kit مباشرة |

---

## الحالات المدعومة

| الحالة | النتيجة |
|--------|---------|
| نص إنجليزي → ترجمة عربية | ✅ ML Kit OCR → ML Kit Translation |
| نص عربي → عرض/حفظ | ✅ Tesseract Arabic OCR |
| Auto مع نص عربي | ✅ Hybrid fallback to Arabic |
| Auto مع نص إنجليزي | ✅ ML Kit (fast path) |
| شاشة بلا نص | ✅ رسالة "لم يتم العثور على نص واضح" |

---

## التقنية المستخدمة للـ OCR العربي

| الحقل | القيمة |
|-------|--------|
| **المكتبة** | Tesseract4Android 4.9.0 |
| **محرك Tesseract** | 5.5.1 |
| **ملف اللغة** | `ara.traineddata` (Tesseract 4.0.0 format) |
| **حجم ملف اللغة** | 2.38 MB |
| **يعمل Offline** | ✅ نعم |
| **Thread Safety** | Instance واحد، يستخدم `clear()` بين العمليات |

---

## البنية المعمارية (OCR Layer)

```
ocr/
├── OcrEngine.kt              # Interface
├── MlKitOcrEngine.kt         # Latin scripts (ML Kit)
├── ArabicOcrEngine.kt        # Arabic script (Tesseract)
└── HybridOcrEngine.kt        # Smart selection + fallback
```

### HybridOcrEngine Logic:
```
setSourceLanguage(lang)
  ├── "ar"   → ArabicOcrEngine.recognizeText()
  ├── "auto" → MlKitOcrEngine first
  │            └── if chars < 10 → fallback to ArabicOcrEngine
  └── other  → MlKitOcrEngine.recognizeText()
```

---

## Logs المتوقعة

```
D/NabdScreenTranslate: HybridOCR source language set to: auto
D/NabdScreenTranslate: Selected OCR engine: Hybrid (source=auto)
D/NabdScreenTranslate: Hybrid: MLKit found insufficient text (0 chars), fallback to Arabic OCR
D/NabdScreenTranslate: Extracting Arabic traineddata from assets...
D/NabdScreenTranslate: Arabic traineddata extracted: 2495193 bytes
D/NabdScreenTranslate: Arabic OCR engine initialized successfully
D/NabdScreenTranslate: Arabic OCR started
D/NabdScreenTranslate: Arabic OCR completed - found 3 text blocks (156 chars), confidence: 72%
D/NabdScreenTranslate: Hybrid: Using Arabic OCR result (156 chars > 0 chars)
```

---

## القيود المعروفة

| # | القيد | التفاصيل |
|---|-------|----------|
| 1 | إذن MediaProjection متكرر | Android 14+ يتطلب موافقة جديدة عند كل ترجمة |
| 2 | تحميل نموذج ML Kit | أول ترجمة قد تكون بطيئة |
| 3 | دقة OCR العربي | تعتمد على وضوح النص وحجم الخط |
| 4 | حجم APK كبير | ~142MB (debug) بسبب Tesseract native libs + ML Kit |
| 5 | أول استخدام للعربي | يحتاج استخراج traineddata (~2.4MB) من assets |
| 6 | Bounding boxes | Tesseract لا يوفر bounding boxes دقيقة per-block |

---

## أوامر المراقبة

```bash
# مراقبة logs OCR
adb logcat -s NabdScreenTranslate | grep -i "ocr\|arabic\|hybrid\|engine"

# مراقبة كل logs التطبيق
adb logcat -s NabdScreenTranslate
```

---

## Build Result
```
./gradlew clean assembleDebug
BUILD SUCCESSFUL in 4m 52s
```
