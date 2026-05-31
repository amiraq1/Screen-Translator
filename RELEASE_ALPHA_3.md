# Nabd Screen Translate — Alpha 3 Release (Live Translation + Visual Replace)

## معلومات الإصدار
| الحقل | القيمة |
|-------|--------|
| **اسم التطبيق** | نبض ترجمة الشاشة (Nabd Screen Translate) |
| **المرحلة** | Alpha 3 |
| **رقم النسخة** | 1.0.1-alpha2 (versionCode 2) |
| **تاريخ البناء** | 2026-05-31 |
| **APK (arm64 release)** | `release-apks/nabd-screen-translate-alpha3-live-arm64.apk` |
| **حجم APK (arm64 release)** | ~37.5 MB |
| **Min SDK** | Android 8.0 (API 26) |
| **Target SDK** | Android 14 (API 34) |

---

## ما الجديد في Alpha 3

### ✅ الترجمة الفورية (Smart Live Translation)
- ترجمة مستمرة للشاشة بدون ضغط متكرر
- كشف تغيّر النص تلقائياً (لا يترجم إذا النص لم يتغير)
- 3 سرعات: سريع (1ث)، متوازن (2ث)، توفير بطارية (3ث)
- MediaProjection consent مرة واحدة فقط عند التشغيل
- إيقاف تلقائي بعد 3 أخطاء متتالية

### ✅ الاستبدال البصري (Visual Replace)
- وضع عرض يحاكي Google Lens
- يغطي النص الأصلي بلون الخلفية
- يكتب الترجمة مكان النص الأصلي
- Background color sampling (8 نقاط)
- Auto-fit text (18sp → 10sp)

### ✅ تحسين العربية (Arabic Text Polisher)
- تصحيح إملائي تلقائي
- تحسين الأسلوب ("قم بكتابة" → "اكتب")
- إصلاح علامات الترقيم العربية
- حفظ أسماء المنتجات (ChatGPT, Google, etc.)

### ✅ تقليل ازدحام الترجمة (Overlay Declutter)
- تجميع النصوص القريبة
- تصفية العناصر غير المهمة
- حد أقصى 7 فقاعات inline
- الباقي في bottom sheet

---

## طريقة تشغيل Live Mode

1. افتح التطبيق → اضغط "بدء الترجمة"
2. افتح أي تطبيق فيه نص
3. **اضغط مطوّلاً** على الزر العائم (ت)
4. وافق على إذن التقاط الشاشة (مرة واحدة)
5. Live Mode يبدأ — لون حدود الزر يتغير
6. التطبيق يترجم تلقائياً عند تغيّر النص
7. **اضغط مطوّلاً مرة ثانية** لإيقاف Live Mode

### سرعة التحديث (من الإعدادات):
| الخيار | الفاصل | الاستخدام |
|--------|--------|-----------|
| سريع | 1 ثانية | ChatGPT أثناء التوليد |
| متوازن | 2 ثانية | استخدام عام (الافتراضي) |
| توفير | 3 ثوانٍ | قراءة طويلة |

---

## تحذيرات

### البطارية
- Live Mode يستهلك بطارية أكثر من الترجمة اليدوية
- الأفضل استخدام "متوازن (2ث)" للاستخدام العام
- استخدم "توفير (3ث)" للقراءة الطويلة

### القيود
| # | القيد |
|---|-------|
| 1 | Live Mode يحتاج consent مرة عند التشغيل |
| 2 | يستهلك بطارية أكثر من الترجمة اليدوية |
| 3 | بعض التطبيقات قد تمنع أو تخفي overlay |
| 4 | DRM-protected content لا يمكن التقاطه |
| 5 | Android 14+ يتطلب consent جديد عند كل تشغيل لـ Live Mode |
| 6 | Visual Replace قد لا يكون دقيقاً على خلفيات معقدة |
| 7 | Arabic OCR (Tesseract) لا يوفر bounding boxes دقيقة |

---

## أوضاع العرض

| الوضع | الوصف |
|-------|-------|
| `overlay` | فقاعات ترجمة فوق النص (الافتراضي) |
| `sheet` | لوحة سفلية قابلة للتمرير |
| `both` | فقاعات + لوحة سفلية |
| `visual_replace` | استبدال بصري — يغطي النص ويكتب الترجمة مكانه |

---

## الإعدادات الجديدة

| الإعداد | الوصف | الافتراضي |
|---------|-------|-----------|
| الترجمة الفورية | تشغيل Live Mode | OFF |
| سرعة التحديث | فاصل بين كل capture | متوازن (2ث) |
| ترجمة عند تغيّر النص فقط | تخطي إذا النص لم يتغير | ON |
| استبدال بصري | وضع عرض Google Lens | OFF |
| تحسين العربية | تصحيح وتحسين الترجمة | ON |
| تقليل ازدحام الترجمة | تجميع وتصفية | ON |

---

## Logs المتوقعة

### Live Mode:
```
D/NabdScreenTranslate: Live mode started (interval=2000ms)
D/NabdScreenTranslate: Live: persistent projection setup complete (1220x2712)
D/NabdScreenTranslate: Live capture tick
D/NabdScreenTranslate: Live: text changed, translating
D/NabdScreenTranslate: Live translation success (3 blocks)
D/NabdScreenTranslate: Live: unchanged text, skipping
D/NabdScreenTranslate: Live mode stopped
```

### Visual Replace:
```
D/NabdScreenTranslate: VisualReplace mode enabled, groups count = 3
D/NabdScreenTranslate: VisualReplace: background sampled rgb(254,254,254) lum=1.00
D/NabdScreenTranslate: VisualReplace: group 0 auto-fit 14.0sp, lines=1
D/NabdScreenTranslate: VisualReplace: 3 cover blocks built
```

### Arabic Polisher:
```
D/NabdScreenTranslate: ArabicPolish: before=45 chars, after=42 chars, corrections=2
```

---

## Build Result
```
./gradlew clean assembleRelease → BUILD SUCCESSFUL in 12m 14s
./gradlew assembleDebug         → BUILD SUCCESSFUL in 6m 9s
```

---

## الملفات الرئيسية

```
├── capture/
│   ├── LiveTranslationController.kt    ← جديد: متحكم Live Mode
│   ├── MediaProjectionHolder.kt
│   ├── MediaProjectionRequestActivity.kt
│   ├── ScreenCaptureManager.kt
│   └── ScreenCaptureService.kt
├── overlay/
│   ├── FloatingButtonService.kt        ← معدل: Live Mode + long press
│   ├── VisualReplaceOverlayView.kt     ← جديد: استبدال بصري
│   ├── TranslationOverlayManager.kt
│   ├── NoiseFilter.kt
│   ├── TextBlockGrouper.kt
│   └── BubblePrioritizer.kt
├── translate/
│   ├── ArabicTextPolisher.kt           ← جديد: تحسين العربية
│   └── MlKitTranslationEngine.kt
└── ui/settings/
    ├── SettingsScreen.kt               ← معدل: أقسام جديدة
    └── SettingsViewModel.kt
```
