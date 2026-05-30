# Nabd Screen Translate — Alpha Release

## معلومات الإصدار
| الحقل | القيمة |
|-------|--------|
| **اسم التطبيق** | نبض ترجمة الشاشة (Nabd Screen Translate) |
| **المرحلة** | Alpha |
| **رقم النسخة** | 1.0.0 (versionCode 1) |
| **تاريخ البناء** | 2026-05-30 |
| **APK** | `release-apks/nabd-screen-translate-alpha-debug.apk` |
| **حجم APK** | ~113 MB (debug, includes ML Kit models) |
| **Min SDK** | Android 8.0 (API 26) |
| **Target SDK** | Android 14 (API 34) |

---

## الميزات الحالية

### ✅ ترجمة الشاشة الفورية
- زر عائم قابل للسحب
- التقاط الشاشة عبر MediaProjection API
- استخراج النص (OCR) عبر ML Kit Text Recognition
- ترجمة فورية عبر ML Kit Translation (offline)
- عرض النتيجة في overlay شفاف

### ✅ تحديد منطقة (Region Selection)
- ضغط مطوّل على الزر العائم لتحديد منطقة معينة
- رسم مستطيل لاختيار جزء من الشاشة

### ✅ سجل الترجمات (History)
- حفظ تلقائي للترجمات
- بحث في السجل
- نسخ الترجمة
- حذف عنصر / حذف الكل

### ✅ الإعدادات
- شفافية نافذة الترجمة (50%-100%)
- حفظ السجل on/off
- اهتزاز عند الترجمة
- الوضع الداكن

### ✅ الخصوصية
- لا يتم حفظ صور الشاشة
- لا يتم إرسال بيانات لأي خادم
- المعالجة تتم على الجهاز فقط (offline)
- لا يستخدم خدمات إمكانية الوصول (Accessibility)

---

## خطوات التثبيت

1. **تفعيل مصادر غير معروفة:**
   - الإعدادات → الأمان → السماح بتثبيت تطبيقات من مصادر غير معروفة

2. **تثبيت APK:**
   ```
   adb install release-apks/nabd-screen-translate-alpha-debug.apk
   ```
   أو انقل الملف إلى الجهاز وافتحه.

3. **منح الصلاحيات:**
   - عند أول تشغيل: منح صلاحية "العرض فوق التطبيقات" (Overlay permission)
   - عند أول ترجمة: الموافقة على التقاط الشاشة (MediaProjection)

---

## خطوات الاختبار

### اختبار الترجمة الأساسي:
1. افتح التطبيق واضغط "بدء الترجمة"
2. سيظهر زر عائم (ت) على الشاشة
3. افتح أي تطبيق يحتوي نص إنجليزي
4. اضغط على الزر العائم
5. وافق على التقاط الشاشة
6. انتظر ظهور الترجمة في overlay أسفل الشاشة

### اختبار تحديد المنطقة:
1. اضغط مطوّلاً على الزر العائم
2. ارسم مستطيل حول النص المراد ترجمته
3. انتظر النتيجة

### اختبار السجل:
1. من الشاشة الرئيسية → سجل الترجمات
2. تأكد من ظهور الترجمات المحفوظة
3. جرّب النسخ والحذف

### مراقبة Logs:
```bash
adb logcat -s NabdScreenTranslate
```

---

## القيود المعروفة

| # | القيد | التفاصيل |
|---|-------|----------|
| 1 | إذن MediaProjection متكرر | Android 14+ يتطلب موافقة جديدة عند كل ترجمة (single-use token) |
| 2 | تحميل نموذج ML Kit | أول ترجمة قد تكون بطيئة (تحميل ~30MB model) |
| 3 | OCR عربي غير مدعوم | حالياً يدعم استخراج النص اللاتيني فقط |
| 4 | حجم APK كبير | ~113MB بسبب ML Kit models (سيقل في Release build) |
| 5 | لا يوجد اختيار لغة المصدر | يعتمد على Auto-detect |
| 6 | حجم نافذة الترجمة ثابت | عرض كامل الشاشة |
| 7 | لا يعمل على شاشات DRM | المحتوى المحمي لا يمكن التقاطه |

---

## أوامر مفيدة

```bash
# مراقبة logs التطبيق
adb logcat -s NabdScreenTranslate

# تثبيت APK
adb install -r release-apks/nabd-screen-translate-alpha-debug.apk

# إلغاء التثبيت
adb uninstall com.ammar.nabdscreentranslate

# بناء من المصدر
./gradlew clean assembleDebug
```

---

## التقنيات المستخدمة
- Kotlin + Jetpack Compose
- ML Kit Text Recognition (OCR)
- ML Kit Translation (Offline)
- MediaProjection API
- Room Database
- DataStore Preferences
- WindowManager Overlay
- Foreground Service
