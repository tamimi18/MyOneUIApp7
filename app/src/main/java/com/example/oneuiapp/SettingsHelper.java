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
// ★★★ جديد: استيراد مدير التفضيلات الافتراضي ★★★
import androidx.preference.PreferenceManager;

import java.util.Locale;

public class SettingsHelper {

    // private static final String PREFSNAME = "com.example.oneuiapppreferences"; // ★★★ محذوف ★★★
    private static final String KEYLANGUAGEMODE = "language_mode";
    private static final String KEYTHEMEMODE = "theme_mode";
    private static final String KEYFONTMODE = "font_mode";
    private static final String KEYNOTIFICATIONSENABLED = "notifications_enabled";
    private static final String KEYPREVIEWTEXT = "preview_text";

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
        // ★★★ معدل: استخدام مدير التفضيلات الافتراضي ★★★
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
    }

    // ---------------- Language ----------------
    public int getLanguageMode() {
        String v = prefs.getString(KEYLANGUAGEMODE, String.valueOf(LANGUAGE_SYSTEM));
        try { return Integer.parseInt(v); } catch (Exception e) { return LANGUAGE_SYSTEM; }
    }

    public void setLanguageMode(int mode) {
        prefs.edit().putString(KEYLANGUAGEMODE, String.valueOf(mode)).apply();
    }

    public void applyLanguage(Activity activity) {
        activity.recreate();
    }

    // ---------------- Theme ----------------
    public int getThemeMode() {
        String v = prefs.getString(KEYTHEMEMODE, String.valueOf(THEME_SYSTEM));
        try { return Integer.parseInt(v); } catch (Exception e) { return THEME_SYSTEM; }
    }

    public void setThemeMode(int mode) {
        prefs.edit().putString(KEYTHEMEMODE, String.valueOf(mode)).apply();
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

    // ---------------- Font ----------------
    public int getFontMode() {
        String v = prefs.getString(KEYFONTMODE, String.valueOf(FONT_SYSTEM));
        try { return Integer.parseInt(v); } catch (Exception e) { return FONT_SYSTEM; }
    }

    public void setFontMode(int mode) {
        prefs.edit().putString(KEYFONTMODE, String.valueOf(mode)).apply();
    }

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
            android.util.Log.w("SettingsHelper", "getTypeface failed, fallback to system", e);
            return null;
        }
    }

    // ---------------- Preview text ----------------
    private String getPreviewTextInternal() {
        // ✅ الآن يستدعي resource الصحيح
        String def = context.getString(R.string.settings_preview_text_default);
        return prefs.getString(KEYPREVIEWTEXT, def);
    }

    public static String getPreviewText(Context ctx) {
        SettingsHelper sh = new SettingsHelper(ctx);
        return sh.getPreviewTextInternal();
    }

    public void setPreviewText(String text) {
        prefs.edit().putString(KEYPREVIEWTEXT, text == null ? "" : text).apply();
    }

    // ---------------- Notifications ----------------
    public boolean areNotificationsEnabled() {
        return prefs.getBoolean(KEYNOTIFICATIONSENABLED, true);
    }

    public void setNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEYNOTIFICATIONSENABLED, enabled).apply();
    }

    // ---------------- Locale helpers ----------------
    public static Locale getLocale(Context ctx) {
        // ★★★ معدل: استخدام مدير التفضيلات الافتراضي ★★★
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(ctx);
        String modeStr = p.getString(KEYLANGUAGEMODE, String.valueOf(LANGUAGE_SYSTEM));
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
        // ★★★ معدل: استخدام مدير التفضيلات الافتراضي ★★★
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        String modeStr = p.getString(KEYLANGUAGEMODE, String.valueOf(LANGUAGE_SYSTEM));
        int mode;
        try { mode = Integer.parseInt(modeStr); } catch (Exception e) { mode = LANGUAGE_SYSTEM; }

        Locale locale;
        switch (mode) {
            case LANGUAGE_ARABIC: locale = new Locale("ar");
                break;
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
