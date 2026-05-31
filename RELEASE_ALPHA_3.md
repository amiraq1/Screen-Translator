# Nabd Screen Translate — Alpha 3 Release (Visual Replace Mode)

## معلومات الإصدار
| الحقل | القيمة |
|-------|--------|
| **اسم التطبيق** | نبض ترجمة الشاشة (Nabd Screen Translate) |
| **المرحلة** | Alpha 3 |
| **رقم النسخة** | 1.0.0 (versionCode 1) |
| **تاريخ البناء** | 2026-05-31 |
| **APK (arm64 release)** | `release-apks/nabd-screen-translate-alpha3-visual-replace-arm64.apk` |
| **حجم APK (arm64 release)** | ~37.5 MB |
| **Min SDK** | Android 8.0 (API 26) |
| **Target SDK** | Android 14 (API 34) |

---

## ما الجديد في Alpha 3

### ✅ وضع الاستبدال البصري (Visual Replace)
- وضع عرض جديد يحاكي Google Lens
- يغطي النص الأصلي بلون قريب من الخلفية
- يكتب الترجمة في نفس مكان النص الأصلي
- يعمل مع النصوص المجمّعة (grouped blocks) فقط

### ✅ دمج PR #2 (Overlay Declutter)
- تقليل ازدحام الفقاعات
- تجميع النصوص القريبة
- تصفية العناصر غير المهمة (UI labels, أرقام, رموز)
- حد أقصى 7 فقاعات inline

---

## أوضاع العرض المتاحة

| الوضع | الوصف |
|-------|-------|
| `overlay` | فقاعات ترجمة فوق النص (الافتراضي) |
| `sheet` | لوحة سفلية قابلة للتمرير |
| `both` | فقاعات + لوحة سفلية |
| `visual_replace` | استبدال بصري — يغطي النص ويكتب الترجمة مكانه |

---

## كيف يعمل Visual Replace

### Pipeline
```
Screenshot → OCR → NoiseFilter → TextBlockGrouper → BubblePrioritizer
    → Background Sampling (8 points) → Cover Rectangle → Auto-fit Text → Display
```

### Background Sampling
- 8 نقاط عينة حول كل مجموعة نصية
- حساب متوسط اللون
- Alpha: 0.94 (فاتح) أو 0.96 (داكن)

### Auto-fit
- أحجام: 18sp → 16sp → 14sp → 12sp → 10sp
- حد أقصى 5 أسطر
- Ellipsis إذا لم يكفِ

### Text Color
- خلفية فاتحة → `#111111`
- خلفية داكنة → `#FFFFFF`

### Fallback
- إذا لا توجد bounding boxes → bottom sheet تلقائياً

---

## التفاعل

| الإجراء | النتيجة |
|---------|---------|
| ضغط على نص مستبدل | popup: نص أصلي + ترجمة + نسخ |
| ضغط على مساحة فارغة | إخفاء overlay مؤقتاً |
| ضغط مرة ثانية | إعادة overlay |

---

## الملفات الجديدة/المعدلة

| الملف | التغيير |
|-------|---------|
| `overlay/VisualReplaceOverlayView.kt` | **جديد** — View الاستبدال البصري |
| `overlay/TranslationOverlayManager.kt` | إضافة `showVisualReplaceTranslation()` |
| `overlay/FloatingButtonService.kt` | دعم `visual_replace` display mode |
| `data/SettingsDataStore.kt` | ثابت `DISPLAY_MODE_VISUAL_REPLACE` |
| `ui/settings/SettingsScreen.kt` | خيار "استبدال بصري" |
| `README.md` | تحديث قائمة الميزات |
| `VISUAL_REPLACE_MODE.md` | **جديد** — توثيق الوضع |

---

## القيود

| # | القيد |
|---|-------|
| 1 | الخلفيات المعقدة (صور/تدرجات) قد لا تُغطى بدقة 100% |
| 2 | النصوص الطويلة جداً قد تُقص |
| 3 | الغطاء مستطيل فقط (لا يتبع انحناءات) |
| 4 | Tesseract Arabic قد لا يوفر bounding boxes → fallback to sheet |

---

## Build Result
```
./gradlew clean assembleDebug   → BUILD SUCCESSFUL in 4m 47s
./gradlew assembleRelease       → BUILD SUCCESSFUL in 13m 48s
```

---

## Logs المتوقعة

```
D/NabdScreenTranslate: VisualReplace mode enabled
D/NabdScreenTranslate: VisualReplace: sampled colors for X groups
D/NabdScreenTranslate: VisualReplace: background sampled rgb(R,G,B) lum=X.XX
D/NabdScreenTranslate: VisualReplace: group X bg=XXXXXXXX lum=X.XX textColor=dark/light
D/NabdScreenTranslate: VisualReplace: group X auto-fit XXsp, lines=X
D/NabdScreenTranslate: VisualReplace: X cover blocks built
D/NabdScreenTranslate: VisualReplace: visual replace shown with X groups
```
