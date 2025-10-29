package com.example.oneuiapp;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * FontHelper (نسخة مبسطة):
 * - لم نعد نستخدم Reflection لتغيير Typeface.
 * - SettingsHelper.wrapContext + BaseActivity يتكفلان بتطبيق الخط عالمياً.
 * - هذا الكلاس يبقى فقط للتوافق مع الكود القديم.
 */
public class FontHelper {

    public static void applyFont(Context context) {
        // لم نعد بحاجة لتطبيق الخط هنا
        // الخط يطبق تلقائياً عبر SettingsHelper.wrapContext
    }

    /**
     * دالة اختيارية: لو أردت تطبيق الخط يدوياً على View معين.
     */
    public static void applyFontToView(View view, Typeface typeface) {
        if (view == null || typeface == null) return;

        if (view instanceof TextView) {
            ((TextView) view).setTypeface(typeface);
        } else if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyFontToView(vg.getChildAt(i), typeface);
            }
        }
    }
}
