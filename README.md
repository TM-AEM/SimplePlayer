# SimplePlayer 🎬
> مشغل فيديو عصري، خفيف، وخالٍ تماماً من الإعلانات لأجهزة أندرويد.
> Modern, lightweight, and ad-free Android video player powered by Jetpack Compose & Media3 ExoPlayer.

---

## 🌟 نبذة عن التطبيق | Overview
**SimplePlayer** هو تطبيق مشغل وسائط صُمم لتقديم تجربة مشاهدة نقية ومباشرة دون تعقيدات، مع دعم كامل للتحكم التفاعلي بالإيماءات، وتخصيص سرعة التشغيل، والترجمات، والعمل في الخلفية ووضع الصورة في الصورة (PiP)، مع احترام كامل لخصوصية المستخدم وخلوه من أي تتبع أو إعلانات.

---

## 🚀 الميزات الرئيسية | Key Features

### 🇸🇦 بالعربية:
- **تحكم متقدم بالإيماءات (One UI / YouTube Style)**:
  - سحب عمودي على الجانب الأيمن لضبط مستوى الصوت مع مؤشر كبسولة عمودية نابضة.
  - سحب عمودي على الجانب الأيسر لضبط سطوع الشاشة مع أيقونة شمس ديناميكية.
  - نقر مزدوج على جانبي الشاشة للتقديم والتأخير السريع مع تأثير تموج أنيق.
  - ضغط مطول لتسريع الفيديو فورياً (2x أو حسب الإعدادات) مع مؤشر سرعة علوي ورجاج خفيف.
- **تخصيص كامل من الإعدادات (Settings Persistence)**:
  - إمكانية تغيير مدة القفز عند النقر المزدوج (من 5 إلى 60 ثانية).
  - إمكانية تحديد سرعة الضغط المطول (1.5x / 2.0x / 2.5x / 3.0x).
  - حفظ فوري ودائم لجميع الإعدادات عبر Jetpack DataStore.
- **صورة في صورة (Picture-in-Picture - PiP)**: متابعة مشاهدة الفيديو بنافذة عائمة أثناء استخدام تطبيقات أخرى.
- **تشغيل بالخلفية (Background Playback)**: استمرار تشغيل الصوت عند قفل الشاشة أو الخروج من التطبيق عبر MediaSession.
- **دعم الترجمة ومسارات الصوت**: اختيار الترجمات المدمجة ومسارات الصوت المختلفة بسهولة.
- **حماية تشغيل الصوت (Audio Focus)**: توقف تلقائي عند ورود مكالمة هاتفية واستئناف بعدها، وإيقاف مؤقت فوري عند فصل السماعات.
- **خصوصية مطلقة (100% Offline & Private)**: بدون إعلانات، بدون تتبع، وبدون جمع أي بيانات.

### 🇬🇧 English:
- **Modern Gesture Controls**: Vertical slide for volume (right) & brightness (left) with smooth capsule HUD, double tap to seek, long press to speed up.
- **Customizable Playback Settings**: Adjust seek duration (5s-60s) and long-press speed (1.5x - 3.0x) persisted with DataStore.
- **Picture-in-Picture (PiP)**: Keep watching in a floating window while multitasking.
- **Background Media Session**: Continue audio playback when the screen is locked or app is minimized.
- **Subtitle & Audio Track Selector**: Seamless selection of embedded subtitles and alternate audio streams.
- **Smart Audio Focus**: Automatically pauses during incoming calls and when headphones are disconnected.
- **Zero Tracking & Completely Ad-Free**: Respects privacy, operates 100% offline without telemetry.

---

## 🛠️ البناء الآلي المباشر عبر GitHub (GitHub Actions CI/CD)

تم ضبط مستودع المشروع بأتمتة كاملة (GitHub Actions) للبناء والتوقيع واستخراج ملف الـ APK أونلاين على سيرفرات GitHub مجاناً وبدون الحاجة لتثبيت أي برامج على جهازك!

### خطوات البناء عبر موقع GitHub.com:

1. **رفع المشروع إلى مستودعك على GitHub**:
   - يمكنك ربط المستودع ورفعه مباشرة عبر زر **Push to GitHub** من قائمة إعدادات AI Studio (أو باستخدام أوامر Git).
2. **الذهاب إلى تبويب Actions**:
   - افتح مستودعك على `github.com`.
   - اضغط على تبويب **Actions** في الشريط العلوي.
3. **تشغيل البناء يدوياً (Run workflow)**:
   - في القائمة الجانبية اليسرى، اضغط على **Build Android APK**.
   - اضغط على زر **Run workflow** الموجود في الجهة اليمنى.
   - اختر نوع البناء (`release` أو `debug`) ثم اضغط على الزر الأخضر **Run workflow**.
4. **تنزيل ملف الـ APK الجاهز (Download Artifact)**:
   - سيقوم سيرفر GitHub بتحميل كل الحزم وبناء وتوقيع التطبيق تلقائياً خلال نحو دقيقتين.
   - بعد ظهور علامة النجاح الخضراء (✔)، اضغط على اسم البناء.
   - انزل لأسفل الصفحة في قسم **Artifacts** واضغط على **SimplePlayer-APKs** لتنزيل ملف الـ APK الموقّع فوراً!
5. **نشر Release تلقائي**:
   - إذا أنشأت Tag باسم إصدار يبدأ بـ `v` (مثل `v1.0.0`) ودفعته إلى GitHub، سيقوم الـ Action تلقائياً بإنشاء **GitHub Release** وإرفاق ملفات الـ APK فيه ليكون متاحاً للتحميل المباشر للجميع.

---

## 📱 التثبيت اليدوي على أجهزة سامسونج وأندرويد (Sideloading Guide)

لتثبيت ملف الـ APK المباشر على هاتفك (خصوصاً أجهزة Samsung Galaxy بنظام One UI):

1. **تحميل ملف الـ APK**:
   - قم بتحميل ملف `SimplePlayer-release.apk` من صفحة الإصدارات (Releases) على هاتفك.
2. **فتح الملف وبدء التثبيت**:
   - افتح تطبيق **ملفاتي (My Files)** أو تطبيق المتصفح وانقر على ملف الـ APK المحمل.
3. **تفعيل التثبيت من مصادر غير معروفة (Install from Unknown Sources)**:
   - ستظهر لك نافذة أمان من نظام أندرويد: *"لأسباب أمنية، غير مسموح لهاتفك بتثبيت تطبيقات غير معروفة من هذا المصدر"*.
   - اضغط على **الضبط (Settings)**.
   - فعّل خيار **السماح من هذا المصدر (Allow from this source)** بجانب المتصفح أو تطبيق "ملفاتي".
4. **تأكيد التثبيت**:
   - ارجع خطوة للخلف واضغط على **تثبيت (Install)**.
5. **جاهز للاستخدام!**:
   - سيظهر التطبيق في قائمة تطبيقاتك بأيقونته الرسمية وجاهزاً للعمل مباشرة.

---

## 🔒 سياسة الخصوصية | Privacy Policy
- **SimplePlayer** يحترم خصوصيتك بالكامل.
- **لا يجمع التطبيق أو ينقل أي بيانات شخصية أو معلومات عن جهازك على الإطلاق**.
- يتم الوصول إلى مقاطع الفيديو المحلية على جهازك فقط من أجل عرضها وتشغيلها بطلب مباشر منك.
- لا توجد أي أدوات تحليل (Analytics)، ولا شبكات إعلانية (Ad Networks)، ولا اتصالات خلفية مجهولة.

---

## 📄 الترخيص | License

هذا المشروع مرخص بموجب رخصة **MIT License**:

```text
MIT License

Copyright (c) 2026 SimplePlayer Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
