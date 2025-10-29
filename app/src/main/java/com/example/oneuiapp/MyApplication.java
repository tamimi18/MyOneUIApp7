package com.example.oneuiapp;

import android.app.Application;
import android.content.Context;

/**
 * MyApplication - النسخة المحدثة مع تطبيق الخط عند بدء التطبيق
 * 
 * ★★★ الحل الرابع: تطبيق الخط تلقائياً عند بدء التطبيق ★★★
 */
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        
        // تهيئة معالج الأعطال
        CrashHandler.init(this);
        
        // تهيئة الإعدادات (الثيم)
        SettingsHelper.initializeFromSettings(this);
        
        // ★★★ تطبيق الخط المخصص على التطبيق بأكمله ★★★
        // هذا السطر هو المفتاح لحل المشكلة الرابعة!
        // 
        // ماذا يحدث هنا؟
        // 1. عند بدء التطبيق، يتم استدعاء onCreate() تلقائياً
        // 2. FontHelper.applyFont() يقرأ الخط المحفوظ من SharedPreferences
        // 3. يستخدم Reflection لتغيير الخطوط الافتراضية في Android
        // 4. من هذه اللحظة، كل النصوص في التطبيق ستستخدم الخط المختار
        // 
        // متى يتم استدعاء هذا؟
        // - عند فتح التطبيق لأول مرة
        // - عند العودة للتطبيق بعد إغلاقه من الذاكرة
        // - بعد استدعاء recreate() من الإعدادات (يتم إعادة إنشاء Application)
        FontHelper.applyFont(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        // تطبيق إعدادات اللغة قبل إنشاء Application
        super.attachBaseContext(SettingsHelper.wrapContext(base));
        
        // ★★★ تطبيق الخط هنا أيضاً للتأكيد ★★★
        // attachBaseContext يُستدعى قبل onCreate، لذلك نطبق الخط مرتين
        // للتأكد من أنه يعمل في جميع الحالات
        // 
        // ملاحظة: لا نستخدم 'this' هنا لأن Application لم يتم إنشاؤه بعد
        // نستخدم 'base' context بدلاً منه
        try {
            FontHelper.applyFont(base);
        } catch (Exception e) {
            // في حالة حدوث أي خطأ، نتجاهله - سيتم تطبيق الخط في onCreate()
            android.util.Log.e("MyApplication", "Failed to apply font in attachBaseContext", e);
        }
    }
}
