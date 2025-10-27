package com.example.oneuiapp;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.res.ResourcesCompat;

import java.util.Locale;

/**
 * Complete SettingsHelper for your project.
 *
 * Responsibilities:
 * - central SharedPreferences access (single prefs name used across app)
 * - strong compatibility with ListPreference (stores entryValues as strings)
 * - expose static helpers used by FontHelper, BaseActivity, MyApplication and fragments
 *
 * Replace your existing app/src/main/java/com/example/oneuiapp/SettingsHelper.java with this file.
 * Ensure res/font/wf_rglr and res/font/samsung_one exist or adjust resource IDs below.
 */
public class SettingsHelper {

    private static final String PREFS_NAME = "com.example.oneuiapp_preferences";

    private static final String KEY_LANGUAGE_MODE = "language_mode";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_FONT_MODE = "font_mode";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_PREVIEW_TEXT = "preview_text";

    public static final int LANGUAGE_SYSTEM = 0;
    public static final int LANGUAGE_ARABIC = 1;
    public static final int LANGUAGE_ENGLISH = 2;

    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    public static final int FONT_SYSTEM = 0;
    public static final int FONT_WF = 1;
    public static final int FONT_SAMSUNG = 2;

    private final SharedPreferences prefs;
    private final Context context;

    public SettingsHelper(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Language
    public int getLanguageMode() {
        String v = prefs.getString(KEY_LANGUAGE_MODE, String.valueOf(LANGUAGE_SYSTEM));
        try { return Integer.parseInt(v); } catch (Exception e) { return LANGUAGE_SYSTEM; }
    }
    public void setLanguageMode(int mode) {
        prefs.edit().putString(KEY_LANGUAGE_MODE, String.valueOf(mode)).apply();
    }
    public void applyLanguage(Activity activity) {
        // wrap + recreate handled by BaseActivity via wrapContext/attachBaseContext
        activity.recreate();
    }

    // Theme
    public int getThemeMode() {
        String v = prefs.getString(KEY_THEME_MODE, String.valueOf(THEME_SYSTEM));
        try { return Integer.parseInt(v); } catch (Exception e) { return THEME_SYSTEM; }
    }
    public void setThemeMode(int mode) {
        prefs.edit().putString(KEY_THEME_MODE, String.valueOf(mode)).apply();
    }
    public void applyTheme() {
        int mode = getThemeMode();
        switch (mode) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    // Font
    public int getFontMode() {
        String v = prefs.getString(KEY_FONT_MODE, String.valueOf(FONT_SYSTEM));
        try { return Integer.parseInt(v); } catch (Exception e) { return FONT_SYSTEM; }
    }
    public void setFontMode(int mode) {
        prefs.edit().putString(KEY_FONT_MODE, String.valueOf(mode)).apply();
    }

    /**
     * Public static getter used by FontHelper and other classes.
     * Returns Typeface from res/font when a bundled font is selected, otherwise null to use system fonts.
     */
    public static Typeface getTypeface(Context ctx) {
        SettingsHelper sh = new SettingsHelper(ctx);
        int mode = sh.getFontMode();
        try {
            switch (mode) {
                case FONT_WF:
                    return ResourcesCompat.getFont(ctx, R.font.wf_rglr);
                case FONT_SAMSUNG:
                    return ResourcesCompat.getFont(ctx, R.font.samsung_one);
                case FONT_SYSTEM:
                default:
                    return null;
            }
        } catch (Exception e) {
            // If resource missing or cannot load, fallback to null -> system default
            android.util.Log.w("SettingsHelper", "getTypeface failed, fallback to system", e);
            return null;
        }
    }

    // Preview text
    private String getPreviewTextInternal() {
        String def = context.getString(R.string.settings_preview_text_default);
        return prefs.getString(KEY_PREVIEW_TEXT, def);
    }
    public static String getPreviewText(Context ctx) {
        SettingsHelper sh = new SettingsHelper(ctx);
        return sh.getPreviewTextInternal();
    }
    public void setPreviewText(String text) {
        prefs.edit().putString(KEY_PREVIEW_TEXT, text == null ? "" : text).apply();
    }

    // Notifications
    public boolean areNotificationsEnabled() {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }
    public void setNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply();
    }

    // Locale helpers used by BaseActivity / Application
    public static java.util.Locale getLocale(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String modeStr = p.getString(KEY_LANGUAGE_MODE, String.valueOf(LANGUAGE_SYSTEM));
        int mode;
        try { mode = Integer.parseInt(modeStr); } catch (Exception e) { mode = LANGUAGE_SYSTEM; }

        switch (mode) {
            case LANGUAGE_ARABIC: return new Locale("ar");
            case LANGUAGE_ENGLISH: return new Locale("en");
            case LANGUAGE_SYSTEM:
            default:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    return Resources.getSystem().getConfiguration().getLocales().get(0);
                } else {
                    return Resources.getSystem().getConfiguration().locale;
                }
        }
    }

    @SuppressWarnings("deprecation")
    public static Context wrapContext(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String modeStr = p.getString(KEY_LANGUAGE_MODE, String.valueOf(LANGUAGE_SYSTEM));
        int mode;
        try { mode = Integer.parseInt(modeStr); } catch (Exception e) { mode = LANGUAGE_SYSTEM; }

        Locale locale;
        switch (mode) {
            case LANGUAGE_ARABIC: locale = new Locale("ar"); break;
            case LANGUAGE_ENGLISH: locale = new Locale("en"); break;
            case LANGUAGE_SYSTEM:
            default:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    locale = Resources.getSystem().getConfiguration().getLocales().get(0);
                } else {
                    locale = Resources.getSystem().getConfiguration().locale;
                }
                break;
        }

        Locale.setDefault(locale);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.createConfigurationContext(config);
        } else {
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
            return context;
        }
    }

    public static void initializeFromSettings(Context context) {
        SettingsHelper helper = new SettingsHelper(context);
        helper.applyTheme();
    }
}
