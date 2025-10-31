package com.example.oneuiapp;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * MyApplication - تنفيذ حل Android Developers الرسمي عبر recreate للأنشطة.
 *
 * عند تغيير الخط: ندعو applyFontChange(null) لخط النظام أو applyFontChange(customTypeface) لخط مخصص.
 * الكود يقوم ب:
 *  1) تحديث حقول Typeface عبر FontHelper
 *  2) استدعاء recreate() على كل Activity مسجّل
 *  3) إرسال بث ACTION_FONT_CHANGED لتمكين أي Fragment/Activity من ضبط أي خصائص محلية إن لزم
 */
public class MyApplication extends Application {

    private static final String TAG = "MyApplication";
    private static MyApplication sInstance;

    private final List<WeakReference<Activity>> activities = new ArrayList<>();

    public static final String ACTION_FONT_CHANGED = "com.example.oneuiapp.ACTION_FONT_CHANGED";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(SettingsHelper.wrapContext(base));
        sInstance = this;

        // تطبيق الخط الابتدائي قبل أي Activity
        try {
            FontHelper.applyFont(base);
        } catch (Exception e) {
            Log.w(TAG, "applyFont failed in attachBaseContext: " + e.getMessage(), e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;

        SettingsHelper.initializeFromSettings(this);

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                activities.add(new WeakReference<>(activity));
            }
            @Override public void onActivityStarted(Activity activity) {}
            @Override public void onActivityResumed(Activity activity) {}
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
            @Override public void onActivityDestroyed(Activity activity) {}
        });
    }

    public static MyApplication getInstance() {
        return sInstance;
    }

    /**
     * يَنفّذ تغيّر الخط العام.
     * @param chosenTypeface null يعني System، وإلا يعني خط مخصص سبق تحميله.
     */
    public void applyFontChange(Typeface chosenTypeface) {
        try {
            // 1) تحديث حقول النظام/الخطوط
            if (chosenTypeface == null) {
                FontHelper.resetToSystemFonts();
            } else {
                FontHelper.applyFont(this);
            }

            // 2) إعادة إنشاء كل الأنشطة المسجلة لإجبار النظام على إعادة تحميل الموارد
            List<WeakReference<Activity>> snapshot = new ArrayList<>(activities);
            for (WeakReference<Activity> ref : snapshot) {
                Activity act = (ref == null) ? null : ref.get();
                if (act == null || act.isFinishing()) continue;
                try {
                    act.runOnUiThread(() -> {
                        try {
                            act.recreate();
                        } catch (Exception e) {
                            Log.w(TAG, "recreate failed for " + act.getClass().getName(), e);
                        }
                    });
                } catch (Exception e) {
                    Log.w(TAG, "scheduling recreate failed for activity", e);
                }
            }

            // 3) إرسال بث عام لإعلام أي Fragment/Activity لضبط خصائص محلية إذا لزم
            Intent i = new Intent(ACTION_FONT_CHANGED);
            sendBroadcast(i);

            Log.i(TAG, "applyFontChange: recreate requested for all activities. system=" + (chosenTypeface == null));
        } catch (Exception e) {
            Log.w(TAG, "applyFontChange failed: " + e.getMessage(), e);
        }

        cleanupActivityList();
    }

    private void cleanupActivityList() {
        Iterator<WeakReference<Activity>> it = activities.iterator();
        while (it.hasNext()) {
            WeakReference<Activity> ref = it.next();
            if (ref == null || ref.get() == null) it.remove();
        }
    }
}
