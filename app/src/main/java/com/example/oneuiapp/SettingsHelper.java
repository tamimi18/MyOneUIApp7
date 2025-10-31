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
import androidx.preference.PreferenceManager;

import java.util.Locale;

public class SettingsHelper {

    private static final String PREFSNAME = "com.example.oneuiapppreferences";

    private static final String KEYLANGUAGEMODE = "language_mode";
    private static final String KEYTHEMEMODE = "theme_mode";
    private static final String KEYFONTMODE = "font_mode";
    private static final String KEYNOTIFICATIONSENABLED = "notifications_enabled";
    private static final String KEYPREVIEWTEXT = "preview_text";

    // مفتاح جديد لمسار الخط المخصص (يُستخدم إذا اخترت تطبيق خط من الإعدادات مخزن في النظام كمسار)
    private static final String KEY_CUSTOM_FONT_PATH = "custom_font_path";

    public static final int LANGUAGE_SYSTEM = 0;
    public static final int LANGUAGE_ARABIC = 1;
    public static final int LANGUAGE_ENGLISH = 2;

    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    public static final int FONT_SYSTEM = 0;
    public static final int FONT_WF = 1;
    public static final int FONT_SAMSUNG = 2;
    public static final int FONT_CUSTOM = 3;

    private final SharedPreferences prefs;
    private final Context context;

    public SettingsHelper(Context context) {
        this.context = context.getApplicationContext();
        // توحيد استخدام DefaultSharedPreferences لكي يتشارك SettingsFragment و SettingsHelper نفس المصدر
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

    /**
     * عند تغيير وضع الخط نستخدم commit لضمان أن القيمة كتبت فعلياً قبل قراءة القيمة من أماكن أخرى (مثل FontHelper.applyFont)
     * commit هنا يضمن تزامن الكتابة لأن apply غير متزامن وقد يسبب حالات سباق صغيرة.
     */
    public void setFontMode(int mode) {
        prefs.edit().putString(KEYFONTMODE, String.valueOf(mode)).commit();
    }

    /**
     * حفظ مسار خط مخصص (مسار ملف داخل storage التطبيق).
     * يستخدم هذا إذا اخترت خطاً مخصصاً من الإعدادات وتريد تطبيقه على كامل التطبيق.
     */
    public void setCustomFontPath(String path) {
        prefs.edit().putString(KEY_CUSTOM_FONT_PATH, path).apply();
    }

    public String getCustomFontPath() {
        return prefs.getString(KEY_CUSTOM_FONT_PATH, null);
    }

    /**
     * احصل على Typeface استناداً إلى وضع الخط الحالي.
     * - يدعم موارد R.font (WF و Samsung)
     * - يدعم مسار خط مخصص (المتوقع أن يكون ملفاً داخل storage ويمكن قراءته)
     * - إرجاع null تعني استخلاص الخط الافتراضي للنظام
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
                case FONT_CUSTOM: {
                    String path = sh.getCustomFontPath();
                    if (path != null && !path.isEmpty()) {
                        try {
                            java.io.File f = new java.io.File(path);
                            if (f.exists() && f.canRead()) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    // API >= 26: Typeface.Builder(File) يعمل بشكل جيد
                                    try {
                                        return Typeface.createFromFile(f);
                                    } catch (Exception exBuilder) {
                                        // fallback to createFromFile
                                        return Typeface.createFromFile(f);
                                    }
                                } else {
                                    return Typeface.createFromFile(f);
                                }
                            } else {
                                android.util.Log.w("SettingsHelper", "Custom font path not accessible: " + path);
                            }
                        } catch (Exception e) {
                            android.util.Log.w("SettingsHelper", "load custom font failed", e);
                        }
                    }
                    return null;
                }
                case FONT_SYSTEM:
                default:
                    return null;
            }
        } catch (Exception e) {
            android.util.Log.w("SettingsHelper", "getTypeface failed, fallback", e);
            return null;
        }
    }

    // ---------------- Preview text ----------------
    private String getPreviewTextInternal() {
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
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        String modeStr = p.getString(KEYLANGUAGEMODE, String.valueOf(LANGUAGE_SYSTEM));
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
