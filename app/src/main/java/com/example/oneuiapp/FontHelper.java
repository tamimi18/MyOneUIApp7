package com.example.oneuiapp;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;

import java.lang.reflect.Field;

/**
 * FontHelper (مبسَّط) - تغيير آمن لحقول Typeface أو إعادة ضبطها إلى قيم النظام.
 *
 * هذا الكلاس لا يحاول تعديل Views مباشرة. تطبيق التغيير الفعلي للواجهة يتم عبر recreate() على
 * الأنشطة (الطريقة الرسمية الموصى بها من Android Developers).
 */
public class FontHelper {

    private static final String TAG = "FontHelper";

    /**
     * يطبّق اختيار الخط العام. إذا كانت getTypeface ترجع null فهذا يعني System.
     * لا يقوم هذا الكود بمحاولات لإعادة رسم Views؛ ذلك يتم عبر recreate في MyApplication.
     */
    public static void applyFont(Context context) {
        if (context == null) return;
        try {
            Typeface chosen = SettingsHelper.getTypeface(context); // null => System
            if (chosen == null) {
                resetToSystemFonts();
            } else {
                // استبدال الحقول الأساسية بخط مخصص
                replaceTypefaceField("DEFAULT", chosen);
                replaceTypefaceField("DEFAULT_BOLD", Typeface.create(chosen, Typeface.BOLD));
                replaceTypefaceField("SANS_SERIF", chosen);
                replaceTypefaceField("SERIF", chosen);
                replaceTypefaceField("MONOSPACE", chosen);
            }
            Log.i(TAG, "applyFont completed. custom=" + (SettingsHelper.getTypeface(context) != null));
        } catch (Exception e) {
            Log.w(TAG, "applyFont failed: " + e.getMessage(), e);
            resetToSystemFonts();
        }
    }

    private static boolean replaceTypefaceField(String fieldName, Typeface newTf) {
        try {
            Field f = Typeface.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(null, newTf);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "replaceTypefaceField failed for " + fieldName + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * إعادة الحقول المعروفة إلى قيمها الافتراضية الآمنة.
     */
    public static void resetToSystemFonts() {
        try {
            Typeface def = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
            Typeface defBold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
            Typeface sans = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
            Typeface serif = Typeface.create(Typeface.SERIF, Typeface.NORMAL);
            Typeface mono = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL);

            replaceTypefaceField("DEFAULT", def);
            replaceTypefaceField("DEFAULT_BOLD", defBold);
            replaceTypefaceField("SANS_SERIF", sans);
            replaceTypefaceField("SERIF", serif);
            replaceTypefaceField("MONOSPACE", mono);

            Log.i(TAG, "resetToSystemFonts: done");
        } catch (Exception e) {
            Log.w(TAG, "resetToSystemFonts failed: " + e.getMessage(), e);
        }
    }
}
