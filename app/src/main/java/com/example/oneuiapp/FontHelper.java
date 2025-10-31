package com.example.oneuiapp;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;

import androidx.core.content.res.ResourcesCompat;

import java.lang.reflect.Field;

public class FontHelper {

    private static final String TAG = "FontHelper";

    public static void applyFont(Context context) {
        Typeface custom = SettingsHelper.getTypeface(context);

        if (custom == null) {
            resetToSystemFonts();
            return;
        }

        try {
            replaceTypefaceField("DEFAULT", custom);
            replaceTypefaceField("DEFAULT_BOLD", Typeface.create(custom, Typeface.BOLD));
            replaceTypefaceField("SANS_SERIF", custom);
            replaceTypefaceField("SERIF", custom);
            replaceTypefaceField("MONOSPACE", custom);
        } catch (Exception e) {
            Log.e(TAG, "applyFont failed", e);
        }
    }

    private static void replaceTypefaceField(String fieldName, Typeface newTf) {
        try {
            Field field = Typeface.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, newTf);
        } catch (NoSuchFieldException nsf) {
            Log.w(TAG, "NoSuchFieldException for " + fieldName);
        } catch (IllegalAccessException ia) {
            Log.w(TAG, "IllegalAccessException for " + fieldName, ia);
        } catch (Exception e) {
            Log.w(TAG, "replaceTypefaceField failed for " + fieldName, e);
        }
    }

    private static void resetToSystemFonts() {
        try {
            Typeface def = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
            Typeface defBold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
            Typeface sans = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
            Typeface serif = Typeface.create(Typeface.SERIF, Typeface.NORMAL);
            Typeface mono = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL);

            replaceTypefaceField("DEFAULT", def);
            replaceTypefaceField("DEFAULT_BOLD", defBold);
            replaceTypefaceField("SANS_SERIF", sans);
            replaceTypefaceField("SERIF", serif);
            replaceTypefaceField("MONOSPACE", mono);
        } catch (Exception e) {
            Log.e(TAG, "resetToSystemFonts failed", e);
        }
    }
}
