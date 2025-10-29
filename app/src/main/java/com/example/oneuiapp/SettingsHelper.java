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

public class SettingsHelper {

    // اسم ملف SharedPreferences - يطابق الاسم الافتراضي من PreferenceManager
    private static final String PREFS_NAME = "com.example.oneuiapp_preferences";
    
    private static final String KEY_LANGUAGE_MODE = "language_mode";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_FONT_MODE = "font_mode";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_PREVIEW_TEXT = "preview_text";

    // خيارات اللغة
    public static final int LANGUAGE_SYSTEM = 0;
    public static final int LANGUAGE_ARABIC = 1;
    public static final int LANGUAGE_ENGLISH = 2;

    // خيارات الثيم
    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    // خيارات الخط
    public static final int FONT_SYSTEM = 0;
    public static final int FONT_WF = 1;
    public static final int FONT_SAMSUNG = 2;

    private SharedPreferences prefs;
    private Context context;

    public SettingsHelper(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ═══════════════════════════════════════════════════════════════════
    // دوال اللغة
    // ═══════════════════════════════════════════════════════════════════

    public int getLanguageMode() {
        String value = prefs.getString(KEY_LANGUAGE_MODE, String.valueOf(LANGUAGE_SYSTEM));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return LANGUAGE_SYSTEM;
        }
    }

    public void setLanguageMode(int mode) {
        prefs.edit().putString(KEY_LANGUAGE_MODE, String.valueOf(mode)).apply();
    }

    public void applyLanguage(Activity activity) {
        int mode = getLanguageMode();
        setLanguageMode(mode);
        activity.recreate();
    }

    // ═══════════════════════════════════════════════════════════════════
    // دوال الثيم
    // ═══════════════════════════════════════════════════════════════════

    public int getThemeMode() {
        String value = prefs.getString(KEY_THEME_MODE, String.valueOf(THEME_SYSTEM));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return THEME_SYSTEM;
        }
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
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // دوال الخط
    // ═══════════════════════════════════════════════════════════════════

    public int getFontMode() {
        String value = prefs.getString(KEY_FONT_MODE, String.valueOf(FONT_SYSTEM));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return FONT_SYSTEM;
        }
    }

    public void setFontMode(int mode) {
        prefs.edit().putString(KEY_FONT_MODE, String.valueOf(mode)).apply();
    }

    public static Typeface getTypeface(Context context) {
        SettingsHelper helper = new SettingsHelper(context);
        int fontMode = helper.getFontMode();
        
        switch (fontMode) {
            case FONT_WF:
                return ResourcesCompat.getFont(context, R.font.wf_rglr);
                
            case FONT_SAMSUNG:
                return ResourcesCompat.getFont(context, R.font.samsung_one);
                
            case FONT_SYSTEM:
            default:
                return null;
        }
    }

    public static void applyFontToDrawerLayout(Context context, dev.oneuiproject.oneui.layout.DrawerLayout drawerLayout) {
        if (drawerLayout == null) {
            return;
        }
        
        Typeface typeface = getTypeface(context);
        
        if (typeface != null) {
            try {
                android.view.View titleView = drawerLayout.findViewById(
                    context.getResources().getIdentifier("title", "id", context.getPackageName())
                );
                
                if (titleView instanceof android.widget.TextView) {
                    ((android.widget.TextView) titleView).setTypeface(typeface);
                }
            } catch (Exception e) {
                // تجاهل الخطأ
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // دوال الإشعارات
    // ═══════════════════════════════════════════════════════════════════

    public boolean areNotificationsEnabled() {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }

    public void setNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply();
    }

    // ═══════════════════════════════════════════════════════════════════
    // ★★★ دوال نص المعاينة - مصححة بالكامل ★★★
    // ═══════════════════════════════════════════════════════════════════

    /**
     * الحصول على نص المعاينة المحفوظ (دالة غير ثابتة)
     * 
     * هذه الدالة تُستخدم داخلياً عندما يكون لدينا instance من SettingsHelper
     */
    private String getPreviewTextInternal(Context context) {
        String defaultText = context.getString(R.string.settings_preview_text_default);
        return prefs.getString(KEY_PREVIEW_TEXT, defaultText);
    }

    /**
     * حفظ نص المعاينة الجديد
     */
    public void setPreviewText(String text) {
        prefs.edit().putString(KEY_PREVIEW_TEXT, text).apply();
    }

    /**
     * ★★★ الدالة الثابتة الوحيدة للحصول على نص المعاينة ★★★
     * 
     * هذه هي الدالة الوحيدة التي يجب استخدامها من الخارج
     * تم حل مشكلة التعريف المكرر بجعل هذه الدالة الوحيدة الثابتة (static)
     * والدالة الأخرى خاصة (private)
     * 
     * @param context السياق المطلوب
     * @return نص المعاينة المحفوظ، أو النص الافتراضي إذا لم يكن موجوداً
     */
    public static String getPreviewText(Context context) {
        SettingsHelper helper = new SettingsHelper(context);
        return helper.getPreviewTextInternal(context);
    }

    // ═══════════════════════════════════════════════════════════════════
    // دوال مساعدة عامة
    // ═══════════════════════════════════════════════════════════════════

    public static Locale getLocale(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String languageModeStr = prefs.getString(KEY_LANGUAGE_MODE, String.valueOf(LANGUAGE_SYSTEM));
        
        int languageMode;
        try {
            languageMode = Integer.parseInt(languageModeStr);
        } catch (NumberFormatException e) {
            languageMode = LANGUAGE_SYSTEM;
        }

        switch (languageMode) {
            case LANGUAGE_ARABIC:
                return new Locale("ar");
            case LANGUAGE_ENGLISH:
                return new Locale("en");
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
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String languageModeStr = prefs.getString(KEY_LANGUAGE_MODE, String.valueOf(LANGUAGE_SYSTEM));
        
        int languageMode;
        try {
            languageMode = Integer.parseInt(languageModeStr);
        } catch (NumberFormatException e) {
            languageMode = LANGUAGE_SYSTEM;
        }

        Locale locale;
        switch (languageMode) {
            case LANGUAGE_ARABIC:
                locale = new Locale("ar");
                break;
            case LANGUAGE_ENGLISH:
                locale = new Locale("en");
                break;
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
