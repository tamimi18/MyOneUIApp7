package com.example.oneuiapp;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * MyApplication: يحتفظ بقائمة الأنشطة ويعيد تطبيق الخط فوراً عبر تطبيق Typeface على Views.
 * تحسين: بعد recreate نُنفّذ تطبيق Typeface أيضاً بعد تأخيرين للتأكد من أن كل إعادة رسم تمت.
 */
public class MyApplication extends Application {

    private static final String TAG = "MyApplication";
    private static MyApplication sInstance;

    private static final List<WeakReference<Activity>> activities = new ArrayList<>();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(SettingsHelper.wrapContext(base));
        sInstance = this;
        try {
            FontHelper.applyFont(base);
        } catch (Exception e) {
            Log.e(TAG, "applyFont failed in attachBaseContext", e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;

        CrashHandler.init(this);
        SettingsHelper.initializeFromSettings(this);

        try {
            FontHelper.applyFont(this);
        } catch (Exception e) {
            Log.e(TAG, "applyFont failed in onCreate", e);
        }

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                activities.add(new WeakReference<>(activity));
                try {
                    FontHelper.applyFont(activity);
                } catch (Exception e) {
                    Log.w(TAG, "FontHelper.applyFont on activityCreated failed", e);
                }
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
     * إعادة إنشاء كل الأنشطة المفتوحة وتطبيق Typeface مباشرة على Views لضمان تغيير فوري.
     * بعد recreate نطبق أيضاً تطبيقين متأخرين (150ms و 450ms) للتأكد من أن أي عمليات إعادة رسم لاحقة قد انتهت.
     */
    public void recreateAllActivities() {
        Typeface tf = SettingsHelper.getTypeface(this);

        for (WeakReference<Activity> ref : activities) {
            Activity act = ref.get();
            if (act != null && !act.isFinishing()) {
                try {
                    act.runOnUiThread(() -> {
                        try {
                            act.recreate();
                        } catch (Exception e) {
                            Log.w(TAG, "Activity.recreate failed for " + act.getClass().getName(), e);
                        }

                        // تطبيق فوري الآن أيضاً
                        try {
                            Typeface applyTf = SettingsHelper.getTypeface(act);
                            // مرّر null لإعادة خط النظام الحقيقي إن لم يوجد tf
                            FontHelper.applyTypefaceToActivity(act, applyTf);
                        } catch (Exception e) {
                            Log.w(TAG, "Failed to apply typeface to activity views immediately for " + act.getClass().getName(), e);
                        }

                        // تطبيق متأخر 150ms
                        mainHandler.postDelayed(() -> {
                            try {
                                Typeface delayedTf = SettingsHelper.getTypeface(act);
                                FontHelper.applyTypefaceToActivity(act, delayedTf);
                            } catch (Exception e) {
                                Log.w(TAG, "Failed to apply delayed typeface (150ms) to activity views for " + act.getClass().getName(), e);
                            }
                        }, 150);

                        // تطبيق متأخر ثاني 450ms لتغطية حالات إعادة رسم طويلة
                        mainHandler.postDelayed(() -> {
                            try {
                                Typeface delayedTf2 = SettingsHelper.getTypeface(act);
                                FontHelper.applyTypefaceToActivity(act, delayedTf2);
                            } catch (Exception e) {
                                Log.w(TAG, "Failed to apply delayed typeface (450ms) to activity views for " + act.getClass().getName(), e);
                            }
                        }, 450);
                    });
                } catch (Exception e) {
                    Log.w(TAG, "Failed scheduling recreate for activity", e);
                }
            }
        }
    }
}
