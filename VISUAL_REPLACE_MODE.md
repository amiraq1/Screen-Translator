# Visual Replace Mode — استبدال بصري

## الفكرة

وضع عرض جديد يحاكي Google Lens: بدلاً من عرض فقاعات ترجمة فوق النص الأصلي، يقوم التطبيق بـ:

1. **تغطية النص الأصلي** بمستطيل بلون قريب من الخلفية
2. **كتابة الترجمة** في نفس مكان النص الأصلي

النتيجة: يبدو وكأن النص الأصلي تم استبداله بالترجمة.

## طريقة العمل

### Pipeline

```
Screenshot → OCR → NoiseFilter → TextBlockGrouper → BubblePrioritizer
    → Background Color Sampling → Cover Rectangle → Auto-fit Text → Display
```

### Background Color Sampling

- يأخذ 8 عينات حول حدود كل مجموعة نصية (أركان + أوساط الحواف)
- يحسب متوسط اللون
- يحدد alpha حسب luminance:
  - خلفية فاتحة → alpha 0.94
  - خلفية داكنة → alpha 0.96

### Auto-fit Text

يجرب أحجام الخط بالترتيب: 18sp → 16sp → 14sp → 12sp → 10sp

- يختار أكبر حجم يناسب المساحة المتاحة
- يسمح بحد أقصى 5 أسطر
- إذا لم يكفِ، يقص النص مع ellipsis

### Text Color

- خلفية فاتحة (luminance > 0.5) → نص أسود `#111111`
- خلفية داكنة (luminance ≤ 0.5) → نص أبيض `#FFFFFF`

## التفاعل

- **الضغط على نص مستبدل**: يظهر popup يحتوي النص الأصلي + الترجمة + زر نسخ
- **الضغط على مساحة فارغة**: يخفي الـ overlay مؤقتاً لرؤية الشاشة الأصلية
- **الضغط مرة ثانية**: يعيد الـ overlay

## متى يستخدم Fallback

يتحول تلقائياً إلى Bottom Sheet عندما:

- لا توجد bounding boxes (مثل OCR عربي عبر Tesseract بدون إحداثيات)
- فشل في عرض الـ overlay

## القيود

- **الخلفيات المعقدة**: إذا كانت الخلفية تحتوي صور أو تدرجات، قد لا يكون اللون المُعاين دقيقاً 100%
- **النصوص الطويلة جداً**: قد يتم قصها إذا لم تكفِ المساحة
- **الخطوط المنحنية أو المائلة**: الغطاء مستطيل فقط، لا يتبع انحناءات النص
- **الشفافية**: الغطاء ليس 100% معتم لتجنب المظهر الاصطناعي

## الإعدادات

في الإعدادات → نافذة الترجمة → طريقة عرض الترجمة:
- فوق النص (overlay)
- لوحة سفلية (sheet)
- كلاهما (both)
- **استبدال بصري (visual_replace)** ← جديد

## الملفات

- `overlay/VisualReplaceOverlayView.kt` — View الرئيسي للاستبدال البصري
- `overlay/TranslationOverlayManager.kt` — إضافة `showVisualReplaceTranslation()`
- `overlay/FloatingButtonService.kt` — دعم display mode الجديد
- `data/SettingsDataStore.kt` — ثابت `DISPLAY_MODE_VISUAL_REPLACE`
- `ui/settings/SettingsScreen.kt` — خيار "استبدال بصري" في الإعدادات

## Logs

```
NabdScreenTranslate: VisualReplace mode enabled
NabdScreenTranslate: VisualReplace: sampled colors for X groups
NabdScreenTranslate: VisualReplace: background sampled rgb(R,G,B) lum=X.XX
NabdScreenTranslate: VisualReplace: group X bg=XXXXXXXX lum=X.XX textColor=dark/light
NabdScreenTranslate: VisualReplace: group X auto-fit XXsp, lines=X
NabdScreenTranslate: VisualReplace: X cover blocks built
NabdScreenTranslate: VisualReplace: visual replace shown with X groups
```
