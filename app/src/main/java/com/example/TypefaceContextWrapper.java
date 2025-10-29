package com.example.oneuiapp;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

/**
 * يطبق الخط فقط على TextView.
 */
public class TypefaceContextWrapper extends ContextWrapper {

    private final Typeface typeface;

    public TypefaceContextWrapper(Context base, Typeface typeface) {
        super(base);
        this.typeface = typeface;
    }

    @Override
    public Object getSystemService(String name) {
        if (LAYOUT_INFLATER_SERVICE.equals(name)) {
            LayoutInflater inflater = (LayoutInflater) super.getSystemService(name);
            return inflater.cloneInContext(this);
        }
        return super.getSystemService(name);
    }

    /**
     * استدعِ هذه الدالة من BaseActivity بعد setContentView
     * لتطبيق الخط على كل الـ Views.
     */
    public void applyFont(View root) {
        if (root == null || typeface == null) return;

        if (root instanceof TextView) {
            ((TextView) root).setTypeface(typeface);
        } else if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup vg = (android.view.ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyFont(vg.getChildAt(i));
            }
        }
    }
}
