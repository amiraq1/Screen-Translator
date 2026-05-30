# Nabd Screen Translate - Real Device QA Checklist

## متطلبات الاختبار
- جهاز Android 8.0+ (API 26+)
- اتصال إنترنت (لتحميل نماذج ML Kit أول مرة)
- APK: `app/build/outputs/apk/debug/app-debug.apk`

## خطوات التثبيت
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Checklist الاختبار

### 1. التثبيت والفتح
- [ ] تثبيت APK بنجاح
- [ ] فتح التطبيق بدون crash
- [ ] ظهور الشاشة الرئيسية بتصميم داكن
- [ ] النصوص العربية تظهر بشكل صحيح (RTL)

### 2. صلاحية الإشعارات (Android 13+)
- [ ] ظهور طلب صلاحية الإشعارات عند أول فتح
- [ ] التطبيق يعمل سواء تم القبول أو الرفض

### 3. صلاحية الظهور فوق التطبيقات
- [ ] الضغط على زر "تشغيل الزر العائم"
- [ ] ظهور شاشة إعدادات النظام لمنح الصلاحية
- [ ] بعد المنح، العودة للتطبيق
- [ ] ظهور رسالة تأكيد أو بدء الخدمة

### 4. الزر العائم
- [ ] ظهور الزر العائم فوق التطبيقات
- [ ] الزر دائري بلون بنفسجي مع حرف "ت"
- [ ] الزر قابل للسحب والتحريك
- [ ] الزر لا يختفي عند التنقل بين التطبيقات
- [ ] ظهور إشعار "ترجمة الشاشة نشطة"

### 5. التقاط الشاشة والترجمة
- [ ] فتح تطبيق آخر (Chrome, Settings, أي تطبيق بنص إنجليزي)
- [ ] الضغط على الزر العائم
- [ ] ظهور طلب التقاط الشاشة (MediaProjection consent)
- [ ] الموافقة على الطلب
- [ ] عدم حدوث crash
- [ ] ظهور مؤشر تحميل على الزر
- [ ] ظهور نتيجة الترجمة أو رسالة خطأ واضحة

### 6. نتيجة الترجمة
- [ ] ظهور نافذة الترجمة العائمة أسفل الشاشة
- [ ] عرض النص الأصلي (إن كان قصيرًا)
- [ ] عرض الترجمة العربية
- [ ] زر "نسخ" يعمل
- [ ] زر "حفظ" يعمل
- [ ] زر "✕" يغلق النافذة

### 7. حالات الخطأ
- [ ] شاشة بدون نص → رسالة "لم يتم العثور على نص واضح"
- [ ] نموذج غير محمّل → رسالة "تأكد من تحميل نموذج اللغة"
- [ ] الرسائل تختفي تلقائيًا بعد 3 ثوانٍ

### 8. تحميل النماذج
- [ ] الضغط على "تحميل نماذج الترجمة" من الشاشة الرئيسية
- [ ] ظهور مؤشر تحميل
- [ ] ظهور رسالة نجاح أو فشل
- [ ] بعد التحميل، الترجمة تعمل بدون إنترنت

### 9. سجل الترجمات
- [ ] فتح شاشة "السجل"
- [ ] ظهور الترجمات المحفوظة
- [ ] البحث يعمل
- [ ] النسخ يعمل
- [ ] الحذف يعمل
- [ ] "حذف الكل" يعمل

### 10. الإعدادات
- [ ] فتح شاشة "الإعدادات"
- [ ] تغيير شفافية النافذة
- [ ] تفعيل/إيقاف حفظ السجل
- [ ] تفعيل/إيقاف الاهتزاز
- [ ] الإعدادات تُحفظ بعد إغلاق التطبيق

### 11. إيقاف الخدمة
- [ ] الضغط على "إيقاف الزر العائم" من الشاشة الرئيسية
- [ ] اختفاء الزر العائم
- [ ] اختفاء الإشعار
- [ ] أو: الضغط على "إيقاف" من الإشعار

### 12. الضغط المطوّل (تحديد منطقة)
- [ ] الضغط المطوّل على الزر العائم
- [ ] ظهور overlay شفاف لتحديد المنطقة
- [ ] السحب لتحديد مستطيل
- [ ] ترجمة المنطقة المحددة فقط
- [ ] زر "إلغاء" يعمل

### 13. الضغط المزدوج
- [ ] الضغط مرتين سريعًا على الزر العائم
- [ ] إظهار/إخفاء آخر ترجمة

---

## مراقبة Logs
```bash
adb logcat -s NabdScreenTranslate
```

### Logs المتوقعة:
```
D/NabdScreenTranslate: FloatingButtonService created
D/NabdScreenTranslate: Starting floating button service
D/NabdScreenTranslate: Floating button tapped
D/NabdScreenTranslate: MediaProjection permission not granted, requesting...
D/NabdScreenTranslate: Capturing screen: 1080x2400 @ 420dpi
D/NabdScreenTranslate: Screen capture successful
D/NabdScreenTranslate: OCR found 5 text blocks
D/NabdScreenTranslate: Translating 150 chars: auto -> ar
D/NabdScreenTranslate: Translation successful: ...
D/NabdScreenTranslate: Showing translation overlay
```

---

## مشاكل Runtime متوقعة

| المشكلة | السبب | الحل |
|---------|-------|------|
| Crash عند بدء الخدمة على Android 14 | FOREGROUND_SERVICE_MEDIA_PROJECTION | مُعالج في Manifest |
| لا تظهر الترجمة أول مرة | النموذج غير محمّل | حمّل النموذج أولًا |
| SecurityException عند MediaProjection | Android 14 يتطلب consent كل مرة | مُعالج عبر MediaProjectionRequestActivity |
| ANR عند OCR لصورة كبيرة | معالجة على Main thread | مُعالج عبر Dispatchers.Default |

---

## ملاحظات
- التطبيق لا يحفظ أي صور شاشة
- المعالجة تتم على الجهاز فقط (ML Kit on-device + Tesseract)
- الإنترنت مطلوب فقط لتحميل نماذج ML Kit أول مرة
- OCR العربي يعمل offline بالكامل (Tesseract + traineddata مدمج)
- حجم APK كبير (~142MB) بسبب ML Kit + Tesseract native libs

---

## اختبار OCR العربي (Phase 20)

### 14. قراءة نص عربي
- [ ] فتح تطبيق الإعدادات بالعربية
- [ ] الضغط على الزر العائم
- [ ] التأكد من قراءة النص العربي
- [ ] ظهور النص في نافذة الترجمة

### 15. Auto-detect مع نص عربي
- [ ] اللغة المصدر = "تلقائي"
- [ ] فتح صفحة عربية
- [ ] الضغط على الزر العائم
- [ ] ML Kit يفشل → fallback إلى Tesseract Arabic
- [ ] النص العربي يُقرأ بنجاح

### 16. نص إنجليزي (لم يتأثر)
- [ ] اللغة المصدر = "تلقائي" أو "en"
- [ ] فتح صفحة إنجليزية
- [ ] ML Kit يقرأ النص بنجاح (لا fallback)
- [ ] الترجمة تعمل كالمعتاد

### 17. Logs OCR العربي
```bash
adb logcat -s NabdScreenTranslate | grep -i "arabic\|hybrid\|engine"
```

#### Logs المتوقعة (نص عربي):
```
D/NabdScreenTranslate: HybridOCR source language set to: auto
D/NabdScreenTranslate: Selected OCR engine: Hybrid (source=auto)
D/NabdScreenTranslate: Hybrid: MLKit found insufficient text (0 chars), fallback to Arabic OCR
D/NabdScreenTranslate: Arabic OCR started
D/NabdScreenTranslate: Arabic OCR completed - found 3 text blocks (156 chars), confidence: 72%
D/NabdScreenTranslate: Hybrid: Using Arabic OCR result (156 chars > 0 chars)
```

#### Logs المتوقعة (نص إنجليزي):
```
D/NabdScreenTranslate: HybridOCR source language set to: auto
D/NabdScreenTranslate: Selected OCR engine: Hybrid (source=auto)
D/NabdScreenTranslate: Hybrid: MLKit found sufficient text (263 chars, 5 blocks)
```

---

## End-to-End Test (أهم اختبار)

### السيناريو الكامل:
1. افتح التطبيق → اضغط "تحميل نماذج الترجمة" → انتظر النجاح
2. اضغط "تشغيل الزر العائم" → امنح صلاحية Overlay
3. افتح Chrome → اذهب لصفحة إنجليزية (مثل wikipedia.org)
4. اضغط الزر العائم (ت)
5. وافق على التقاط الشاشة
6. انتظر (2-5 ثوانٍ)
7. يجب أن تظهر نافذة الترجمة العربية أسفل الشاشة
8. اضغط "نسخ" → تأكد أن النص منسوخ
9. ارجع للتطبيق → افتح "السجل" → تأكد أن الترجمة محفوظة
10. كرر الخطوات 4-7 خمس مرات (كل مرة ستطلب إذن التقاط)
11. تأكد عدم حدوث crash

### مراقبة Logs أثناء الاختبار:
```bash
adb logcat -s NabdScreenTranslate
```

### التسلسل المتوقع في Logs:
```
D NabdScreenTranslate: Floating button tapped - starting capture flow
D NabdScreenTranslate: Requesting MediaProjection permission
D NabdScreenTranslate: Requesting MediaProjection consent from user
D NabdScreenTranslate: MediaProjection consent GRANTED
D NabdScreenTranslate: MediaProjection granted - performing capture
D NabdScreenTranslate: Step 1: Capturing screen...
D NabdScreenTranslate: Starting screen capture: 1080x2400 @ 420dpi
D NabdScreenTranslate: Screen capture successful: 1080x2400
D NabdScreenTranslate: Screen captured: 1080x2400
D NabdScreenTranslate: Step 2: Languages - source=auto, target=ar
D NabdScreenTranslate: Step 3: Running OCR + Translation...
D NabdScreenTranslate: OCR found 12 text blocks
D NabdScreenTranslate: Translating 350 chars: auto -> ar
D NabdScreenTranslate: Step 4: Translation SUCCESS - showing overlay
D NabdScreenTranslate: Showing translation overlay
D NabdScreenTranslate: Translation saved to history
```

### إذا فشل الاختبار:
- **لا تظهر نافذة الترجمة**: تحقق من تحميل النموذج أولًا
- **Crash عند الضغط**: راجع logcat للخطأ المحدد
- **"تعذر التقاط الشاشة"**: أعد المحاولة - قد يكون timeout
- **نص فارغ**: جرب صفحة بنص أكبر وأوضح
