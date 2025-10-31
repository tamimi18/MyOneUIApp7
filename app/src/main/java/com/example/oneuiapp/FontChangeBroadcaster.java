package com.example.oneuiapp;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * FontChangeBroadcaster
 *
 * مزايا التعديل:
 * - يرسل البث باستخدام application context لتقليل فرص تسرب الـ Context.
 * - يسجّل عملية الإرسال لمساعدتك في تتبّع ما إذا وصل البث (مفيد عند فحص السجلات في GitHub Actions).
 * - يوفّر طريقتين: إرسال بسيط بدون بيانات، وإرسال مع بيانات وصفية عن الخط (realName, fileName).
 *
 * ملاحظة: المستقبِلون (مثل BaseActivity) يجب أن يستمعوا إلى نفس الـ ACTION_FONT_CHANGED.
 */
public class FontChangeBroadcaster {

    private static final String TAG = "FontChangeBroadcaster";

    public static final String ACTION_FONT_CHANGED = "com.example.oneuiapp.ACTION_FONT_CHANGED";

    // Optional extras keys
    public static final String EXTRA_FONT_REAL_NAME = "extra_font_real_name";
    public static final String EXTRA_FONT_FILE_NAME = "extra_font_file_name";
    public static final String EXTRA_FONT_MODE = "extra_font_mode";

    /**
     * إرسال بث بسيط يخبر التطبيق أن إعداد الخط قد تغيّر.
     */
    public static void sendFontChangeBroadcast(Context ctx) {
        if (ctx == null) return;
        try {
            Context appCtx = ctx.getApplicationContext();
            Intent i = new Intent(ACTION_FONT_CHANGED);
            appCtx.sendBroadcast(i);
            Log.i(TAG, "Sent font change broadcast (simple)");
        } catch (Exception e) {
            Log.w(TAG, "sendFontChangeBroadcast failed: " + e.getMessage(), e);
        }
    }

    /**
     * إرسال بث متقدّم يتضمّن بيانات عن الخط المطبّق (اسم العرض واسم الملف) ووضع الخط.
     * استخدم هذا الأسلوب عندما تريد إعطاء مستقبِلين مزيداً من السياق.
     */
    public static void sendFontChangeBroadcast(Context ctx, String fontRealName, String fontFileName, Integer fontMode) {
        if (ctx == null) return;
        try {
            Context appCtx = ctx.getApplicationContext();
            Intent i = new Intent(ACTION_FONT_CHANGED);
            if (fontRealName != null) i.putExtra(EXTRA_FONT_REAL_NAME, fontRealName);
            if (fontFileName != null) i.putExtra(EXTRA_FONT_FILE_NAME, fontFileName);
            if (fontMode != null) i.putExtra(EXTRA_FONT_MODE, fontMode);
            appCtx.sendBroadcast(i);
            Log.i(TAG, "Sent font change broadcast with extras: realName=" + fontRealName + " fileName=" + fontFileName + " mode=" + fontMode);
        } catch (Exception e) {
            Log.w(TAG, "sendFontChangeBroadcast with extras failed: " + e.getMessage(), e);
        }
    }
}
