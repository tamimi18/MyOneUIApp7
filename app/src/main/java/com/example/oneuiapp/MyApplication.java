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
                int overlayRes = getFontOverlayResIdForCurrentMode();
                applyOverlayToActivityIfNeeded(activity, overlayRes);
                try {
                    FontHelper.applyFont(activity);
                } catch (Exception e) {
                    Log.w(TAG, "FontHelper.applyFont failed onActivityCreated", e);
                }
            }

            @Override public void onActivityStarted(Activity activity) {}
            @Override public void onActivityResumed(Activity activity) {}
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
            @Override public void onActivityDestroyed(Activity activity) {
                for (int i = activities.size() - 1; i >= 0; i--) {
                    WeakReference<Activity> ref = activities.get(i);
                    Activity a = ref.get();
                    if (a == null || a == activity) {
                        activities.remove(i);
                    }
                }
            }
        });
    }

    public static MyApplication getInstance() {
        return sInstance;
    }

    private int getFontOverlayResIdForCurrentMode() {
        try {
            SettingsHelper sh = new SettingsHelper(this);
            int mode = sh.getFontMode();
            switch (mode) {
                case SettingsHelper.FONT_WF:
                    return R.style.AppFontOverlay_WP;
                case SettingsHelper.FONT_SAMSUNG:
                    return R.style.AppFontOverlay_SamsungOne;
                case SettingsHelper.FONT_SYSTEM:
                default:
                    return 0;
            }
        } catch (Exception e) {
            Log.w(TAG, "getFontOverlayResIdForCurrentMode failed", e);
            return 0;
        }
    }

    private void applyOverlayToActivityIfNeeded(Activity activity, int overlayResId) {
        if (activity == null || overlayResId == 0) return;
        try {
            activity.getTheme().applyStyle(overlayResId, true);
        } catch (Exception e) {
            Log.w(TAG, "applyOverlayToActivityIfNeeded failed", e);
        }
    }

    public void recreateAllActivities() {
        int overlay = getFontOverlayResIdForCurrentMode();
        for (WeakReference<Activity> ref : new ArrayList<>(activities)) {
            Activity act = ref.get();
            if (act != null && !act.isFinishing()) {
                applyOverlayToActivityIfNeeded(act, overlay);
                try {
                    act.recreate();
                } catch (Exception e) {
                    Log.w(TAG, "Failed to recreate activity " + act.getClass().getName(), e);
                }
            }
        }
    }
}
