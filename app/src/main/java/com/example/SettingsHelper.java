package com.example.oneuiapp;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Build;

import androidx.core.content.res.ResourcesCompat;

import java.util.Locale;

public class SettingsHelper {

    private static final String PREFSNAME = "com.example.oneuiapppreferences";

    private static final String KEYLANGUAGEMODE = "language_mode";
    private static final String KEYFONTMODE = "font_mode";
    private static final String KEYPREVIEWTEXT = "preview_text";
    private static final String KEYNOTIFICATIONSENABLED = "notifications_enabled";

    public static final int LANGUAGE_SYSTEM = 0;
    public static final int LANGUAGE_ARABIC = 1;
    public static final int LANGUAGE_ENGLISH = 2;

    public static final int FONT_SYSTEM = 0;
    public static final int FONT_WF = 1;
    public static final int FONT_SAMSUNG = 2;

    private final SharedPreferences prefs;
    private final Context context;

    public SettingsHelper(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFSNAME, Context.MODE_PRIVATE);
    }

    // Language
    public int getLanguageMode() {
        String v = prefs.getString(KEYLANGUAGEMODE, String.valueOf(LANGUAGE_SYSTEM));
        try { return Integer.parseInt(v); } catch (Exception e) { return LANGUAGE_SYSTEM; }
    }

    public void setLanguageMode(int mode) {
        prefs.edit().putString(KEYLANGUAGEMODE, String.valueOf(mode)).apply();
    }

    public static Locale getLocale(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFSNAME, Context.MODE_PRIVATE);
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

    // Font
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

    // Preview text
    public void setPreviewText(String text) {
        prefs.edit().putString(KEYPREVIEWTEXT, text == null ? "" : text).apply();
    }

    public static String getPreviewText(Context ctx) {
        SettingsHelper sh = new SettingsHelper(ctx);
        String def = ctx.getString(R.string.settings_preview_text_default);
        return sh.prefs.getString(KEYPREVIEWTEXT, def);
    }

    // Notifications
    public boolean areNotificationsEnabled() {
        return prefs.getBoolean(KEYNOTIFICATIONSENABLED, true);
    }

    public void setNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEYNOTIFICATIONSENABLED, enabled).apply();
    }

    // Context wrapping: فقط Locale هنا. لا نُغلّف بالسياق الخاص بالخط أثناء attachBaseContext
    public static Context wrapContext(Context context) {
        Locale locale = getLocale(context);
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

    // إذا غيّرت اللغة من الإعدادات، أعد إنشاء الـ Activity
    public void applyLanguage(Activity activity) {
        activity.recreate();
    }
}
