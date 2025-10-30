package com.example.oneuiapp;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Locale;

public class TypefaceContextWrapper extends ContextWrapper {

    private final Typeface typeface;

    public TypefaceContextWrapper(Context base, Typeface tf) {
        super(base);
        this.typeface = tf;
    }

    public static Context wrap(Context context, Typeface tf) {
        return new TypefaceContextWrapper(context, tf);
    }

    // Recursively apply typeface to view tree (used after inflate)
    public static void applyFontRecursively(View root, Typeface tf) {
        if (root == null || tf == null) return;
        if (root instanceof TextView) {
            ((TextView) root).setTypeface(tf);
        } else if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyFontRecursively(vg.getChildAt(i), tf);
            }
        }
    }

    @Override
    public Resources getResources() {
        return super.getResources();
    }
}
