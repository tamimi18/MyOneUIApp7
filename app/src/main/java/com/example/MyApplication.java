package com.example.oneuiapp;

import android.app.Application;
import android.content.Context;
import android.util.Log;

public class MyApplication extends Application {

    private static final String TAG = "MyApplication";
    private static MyApplication sInstance;

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
    }

    public static MyApplication getInstance() {
        return sInstance;
    }

    /**
     * Best-effort notify all activities to recreate by broadcasting a font-change action.
     */
    public void recreateAllActivities() {
        try {
            FontChangeBroadcaster.sendFontChangeBroadcast(this);
        } catch (Exception e) {
            Log.e(TAG, "recreateAllActivities failed", e);
        }
    }
}
