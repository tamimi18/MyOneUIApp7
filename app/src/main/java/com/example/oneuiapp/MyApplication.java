package com.example.oneuiapp;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MyApplication extends Application {

    private static MyApplication sInstance;
    private static final List<WeakReference<Activity>> activities = new ArrayList<>();

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(SettingsHelper.wrapContext(base));
        sInstance = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        CrashHandler.init(this);
        SettingsHelper.initializeFromSettings(this);

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) { activities.add(new WeakReference<>(activity)); }
            @Override public void onActivityStarted(Activity activity) {}
            @Override public void onActivityResumed(Activity activity) {}
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
            @Override public void onActivityDestroyed(Activity activity) {}
        });
    }

    public static MyApplication getInstance() { return sInstance; }

    public void recreateAllActivities() {
        for (WeakReference<Activity> ref : activities) {
            Activity act = ref.get();
            if (act != null && !act.isFinishing()) act.recreate();
        }
    }
}
