package com.example.oneuiapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;

import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;

/**
 * SettingsHelper - يوفر واجهة للحصول على وضع الخط الحالي وإرجاع Typeface المناسب.
 * مفاتيح: "font_mode" يجب أن تتوافق مع res/xml/preferences.xml
 */
public class SettingsHelper {

    public static final int FONT_DEFAULT = 0;
    public static final int FONT_WF = 1;
    public static final int FONT_SAMSUNG = 2;

    private static final String PREF_KEY_FONT_MODE = "font_mode";

    private final Context context;

    public SettingsHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    public int getFontMode() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            String s = prefs.getString(PREF_KEY_FONT_MODE, String.valueOf(FONT_DEFAULT));
            return Integer.parseInt(s);
        } catch (Exception e) {
            try {
                return prefs.getInt(PREF_KEY_FONT_MODE, FONT_DEFAULT);
            } catch (Exception ex) {
                return FONT_DEFAULT;
            }
        }
    }

    /**
     * Return Typeface according to current font mode.
     * If no custom embedded font is selected, returns null to mean "use system default".
     */
    public static Typeface getTypeface(Context ctx) {
        SettingsHelper helper = new SettingsHelper(ctx);
        int mode = helper.getFontMode();

        try {
            switch (mode) {
                case FONT_WF:
                    return ResourcesCompat.getFont(ctx, R.font.wf_rglr);
                case FONT_SAMSUNG:
                    return ResourcesCompat.getFont(ctx, R.font.samsung_one);
                default:
                    return null;
            }
        } catch (Exception e) {
            // fallback to system default
            return null;
        }
    }
}
