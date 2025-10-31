package com.example.oneuiapp;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MyApplication extends Application {

    private static final String TAG = "MyApplication";
    private static MyApplication sInstance;

    // قائمة لتخزين الأنشطة
    private static final List<WeakReference<Activity>> activities = new ArrayList<>();

    @Override
    protected void attachBaseContext(Context base) {
        // apply locale wrapping early
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

        // تسجيل الأنشطة
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                activities.add(new WeakReference<>(activity));
                // تطبيق الخط مباشرة عند إنشاء أي Activity
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
     * إعادة إنشاء كل الأنشطة المفتوحة لتطبيق الخط الجديد
     * الآن يتم استدعاء recreate() على كل Activity داخل الـ UI thread لضمان إعادة بناء الواجهات فوراً.
     */
    public void recreateAllActivities() {
        for (WeakReference<Activity> ref : activities) {
            Activity act = ref.get();
            if (act != null && !act.isFinishing()) {
                try {
                    // نفذ recreate داخل UI thread الخاص بالنشاط
                    act.runOnUiThread(() -> {
                        try {
                            act.recreate();
                        } catch (Exception e) {
                            Log.w(TAG, "Activity.recreate failed for " + act.getClass().getName(), e);
                        }
                    });
                } catch (Exception e) {
                    Log.w(TAG, "Failed scheduling recreate for activity", e);
                }
            }
        }
    }
}
