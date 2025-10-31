package com.example.oneuiapp;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * MyApplication: يحتفظ بقائمة الأنشطة ويعيد تطبيق الخط فوراً عبر تطبيق Typeface على Views.
 */
public class MyApplication extends Application {

    private static final String TAG = "MyApplication";
    private static MyApplication sInstance;

    private static final List<WeakReference<Activity>> activities = new ArrayList<>();

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
     */
    public void recreateAllActivities() {
        Typeface tf = SettingsHelper.getTypeface(this);

        for (WeakReference<Activity> ref : activities) {
            Activity act = ref.get();
            if (act != null && !act.isFinishing()) {
                try {
                    // أولاً: جدول إعادة الإنشاء داخل UI thread الخاص بالنشاط
                    act.runOnUiThread(() -> {
                        try {
                            // حاول إعادة الإنشاء أولاً
                            act.recreate();
                        } catch (Exception e) {
                            Log.w(TAG, "Activity.recreate failed for " + act.getClass().getName(), e);
                        }

                        // ثم مباشرة بعد ذلك، طبق الـ Typeface يدوياً على شجرة العرض لكي يحدث التغيير فوراً
                        try {
                            Typeface applyTf = SettingsHelper.getTypeface(act);
                            if (applyTf != null) {
                                FontHelper.applyTypefaceToActivity(act, applyTf);
                            } else {
                                // إذا لم يكن هناك typeface مخصص (System)، طبق reset: نستخدم default system typeface
                                FontHelper.applyTypefaceToActivity(act, Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Failed to apply typeface to activity views for " + act.getClass().getName(), e);
                        }
                    });
                } catch (Exception e) {
                    Log.w(TAG, "Failed scheduling recreate for activity", e);
                }
            }
        }
    }
                                                                   }
