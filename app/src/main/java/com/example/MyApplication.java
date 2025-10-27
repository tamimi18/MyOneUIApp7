package com.example.oneuiapp;

import android.app.Application;
import android.content.Context;

/**
 * MyApplication: applies theme and font early so Activities pick them up.
 *
 * Replace your existing app/src/main/java/com/example/oneuiapp/MyApplication.java with this file.
 */
public class MyApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        // Apply language wrapping early
        super.attachBaseContext(SettingsHelper.wrapContext(base));
        // Try to apply font as early as possible using base context
        try {
            FontHelper.applyFont(base);
        } catch (Exception e) {
            android.util.Log.e("MyApplication", "applyFont failed in attachBaseContext", e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        CrashHandler.init(this);

        // Apply theme setting before activities are created
        SettingsHelper.initializeFromSettings(this);

        // Ensure font is applied when app process starts
        try {
            FontHelper.applyFont(this);
        } catch (Exception e) {
            android.util.Log.e("MyApplication", "applyFont failed in onCreate", e);
        }
    }
}
