package com.example.oneuiapp;

import android.content.Context;
import android.graphics.Typeface;
import androidx.core.content.res.ResourcesCompat;
import java.lang.reflect.Field;

/**
 * FontHelper - مساعد تطبيق الخطوط على كامل التطبيق
 * 
 * ★★★ الحل الرابع: إصلاح مشكلة عدم تطبيق الخط عند تغييره من الإعدادات ★★★
 * 
 * المشكلة القديمة:
 * عند تغيير الخط من الإعدادات، كان يتم حفظ الاختيار لكن لا يتم تطبيقه
 * على التطبيق حتى إعادة التشغيل الكاملة.
 * 
 * السبب:
 * كان يجب استدعاء FontHelper.applyFont() بعد تغيير الخط، لكن هذا
 * لم يكن يحدث في SettingsFragment.
 * 
 * الحل:
 * 1. نستدعي FontHelper.applyFont() تلقائياً في MyApplication.onCreate()
 * 2. عند تغيير الخط من الإعدادات، نستدعي recreate() لإعادة تطبيق الخط
 * 3. عند إعادة إنشاء Activity، سيتم استدعاء MyApplication.attachBaseContext()
 *    مما سيطبق الخط الجديد تلقائياً
 */
public class FontHelper {

    /**
     * تطبيق الخط المخصص على التطبيق بأكمله
     * 
     * يجب استدعاء هذه الدالة في:
     * - MyApplication.onCreate()
     * - BaseActivity.attachBaseContext()
     * 
     * كيف تعمل:
     * تستخدم Reflection لتغيير الخطوط الافتراضية في Typeface class
     * بحيث يستخدم Android الخط المخصص تلقائياً في كل مكان.
     * 
     * @param context السياق (Application أو Activity)
     */
    public static void applyFont(Context context) {
        // الحصول على الخط المختار من الإعدادات
        Typeface customTypeface = SettingsHelper.getTypeface(context);
        
        // إذا كان الخط null (خط النظام)، نعيد تعيين الخطوط للافتراضية
        if (customTypeface == null) {
            resetToSystemFonts(context);
            return;
        }
        
        try {
            // تغيير DEFAULT (الخط العادي الأكثر استخداماً)
            replaceFont("DEFAULT", customTypeface);
            
            // تغيير DEFAULT_BOLD (الخط العريض)
            replaceFont("DEFAULT_BOLD", Typeface.create(customTypeface, Typeface.BOLD));
            
            // تغيير SANS_SERIF (يستخدم كثيراً في OneUI)
            replaceFont("SANS_SERIF", customTypeface);
            
            // تغيير SERIF (للاكتمال)
            replaceFont("SERIF", customTypeface);
            
            // تغيير MONOSPACE (للاكتمال)
            replaceFont("MONOSPACE", customTypeface);
            
        } catch (Exception e) {
            android.util.Log.e("FontHelper", "Failed to apply custom font", e);
        }
    }
    
    /**
     * دالة مساعدة لاستبدال خط محدد في Typeface باستخدام Reflection
     * 
     * @param fieldName اسم static field في Typeface class
     * @param newTypeface الخط الجديد
     */
    private static void replaceFont(String fieldName, Typeface newTypeface) {
        try {
            Field field = Typeface.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, newTypeface);
        } catch (Exception e) {
            android.util.Log.w("FontHelper", "Failed to replace font: " + fieldName, e);
        }
    }
    
    /**
     * ★★★ دالة جديدة: إعادة تعيين الخطوط إلى الافتراضية ★★★
     * 
     * هذه الدالة تُستخدم عندما يختار المستخدم "خط النظام" من الإعدادات
     * 
     * المشكلة:
     * بمجرد تغيير الخطوط الافتراضية في Typeface، لا يمكن إعادتها للأصلية
     * ببساطة لأن القيم الأصلية قد ضاعت.
     * 
     * الحل:
     * نستخدم Typeface.create() لإنشاء نسخ جديدة من الخطوط الافتراضية
     * ونطبقها كأنها الخطوط "الأصلية"
     * 
     * @param context السياق المطلوب
     */
    private static void resetToSystemFonts(Context context) {
        try {
            // إنشاء خطوط افتراضية جديدة
            Typeface defaultTypeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
            Typeface defaultBoldTypeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
            Typeface sansSerifTypeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
            Typeface serifTypeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL);
            Typeface monospaceTypeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL);
            
            // تطبيقها على الحقول الافتراضية
            replaceFont("DEFAULT", defaultTypeface);
            replaceFont("DEFAULT_BOLD", defaultBoldTypeface);
            replaceFont("SANS_SERIF", sansSerifTypeface);
            replaceFont("SERIF", serifTypeface);
            replaceFont("MONOSPACE", monospaceTypeface);
            
        } catch (Exception e) {
            android.util.Log.e("FontHelper", "Failed to reset fonts", e);
        }
    }
}
